package com.ares.backend.service;

import com.ares.backend.entity.Negocio;
import com.ares.backend.entity.TpvApiKey;
import com.ares.backend.entity.Usuario;
import com.ares.backend.repository.NegocioRepository;
import com.ares.backend.repository.TpvApiKeyRepository;
import com.ares.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reproduce, con hilos reales contra el datasource H2 real de test (no
 * mockeado), la condición de carrera que R4-001 detectó en {@code
 * TpvApiKeyAdminService.emitir()}: el invariante D3 ("como mucho una
 * credencial TPV activa por negocio") se comprobaba SOLO en código de
 * aplicación (buscar activa -> revocar -> insertar) dentro de un único
 * {@code @Transactional}, sin ningún respaldo a nivel de base de datos.
 * Bajo READ_COMMITTED, dos {@code emitir()} concurrentes para el mismo
 * negocio pueden leer el mismo estado "sin activa" (o "con esta activa") y
 * ambos insertar una fila activa nueva.
 * <p>
 * A propósito NO usa {@code @Transactional} a nivel de clase/método: cada
 * hilo debe abrir su PROPIA transacción real (la que abre el proxy de
 * {@code @Transactional} en {@code emitir()}), no compartir la del hilo de
 * test — de lo contrario nunca habría dos transacciones solapadas que
 * compitieran de verdad. La limpieza de datos se hace a mano en
 * {@link #limpiar()}.
 */
@SpringBootTest
class TpvApiKeyAdminServiceConcurrencyTest {

    private static final int HILOS = 8;

    @Autowired private TpvApiKeyAdminService tpvApiKeyAdminService;
    @Autowired private TpvApiKeyRepository tpvApiKeyRepository;
    @Autowired private NegocioRepository negocioRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    private Negocio negocio;

    @AfterEach
    void limpiar() {
        if (negocio == null) {
            return;
        }
        List<TpvApiKey> claves = tpvApiKeyRepository.findByNegocioIdOrderByFechaCreacionDesc(negocio.getId());
        tpvApiKeyRepository.deleteAll(claves);
        usuarioRepository.findByEmail("tpv+negocio-" + negocio.getId() + "@tpv.ohmyfreezer.invalid")
                .ifPresent(usuarioRepository::delete);
        negocioRepository.delete(negocio);
    }

    /**
     * Lanza {@link #HILOS} llamadas concurrentes a {@code emitir()} y
     * espera a que todas terminen (éxito o excepción), devolviendo cuántas
     * tuvieron éxito.
     */
    private AtomicInteger emitirConcurrentemente(Long negocioId) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(HILOS);
        CountDownLatch salida = new CountDownLatch(HILOS);
        CountDownLatch listos = new CountDownLatch(1);
        AtomicInteger exitos = new AtomicInteger(0);

        for (int i = 0; i < HILOS; i++) {
            executor.submit(() -> {
                try {
                    listos.await();
                    tpvApiKeyAdminService.emitir(negocioId);
                    exitos.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception esperadaBajoCarrera) {
                    // Una o más pérdidas de la carrera deben fallar aquí con
                    // DataIntegrityViolationException (constraint del backstop
                    // de BD, ver V7) — es el fix funcionando, no un fallo del test.
                } finally {
                    salida.countDown();
                }
            });
        }

        listos.countDown();
        assertThat(salida.await(30, TimeUnit.SECONDS))
                .as("las %d llamadas concurrentes a emitir() deben terminar dentro del timeout", HILOS)
                .isTrue();
        executor.shutdown();
        return exitos;
    }

    /**
     * Provisiona a mano el usuario sintético del negocio, con el mismo
     * email reservado que {@code obtenerOProvisionarUsuarioSistema}
     * calcularía. Necesario para que la ráfaga concurrente de la PRIMERA
     * emisión ejercite ÚNICAMENTE la carrera de D3 (tabla
     * {@code tpv_api_keys}), no la carrera —ya cubierta por el índice único
     * preexistente {@code uk_usuarios_negocio_username}— de "provisionar el
     * mismo usuario sintético dos veces", que de lo contrario serializaría
     * casi todos los hilos antes de llegar siquiera a la lógica de D3.
     */
    private void provisionarUsuarioSistema(Negocio negocio) {
        Usuario usuarioSistema = new Usuario("tpv-system", "!TPV_SYSTEM_USER_NO_LOGIN!", false);
        usuarioSistema.setEmail("tpv+negocio-" + negocio.getId() + "@tpv.ohmyfreezer.invalid");
        usuarioSistema.setNegocio(negocio);
        usuarioRepository.save(usuarioSistema);
    }

    /** Cuenta cuántas credenciales activas quedan, sin arrastrar entidades
     * lazy fuera de sesión (evita LazyInitializationException si un assert
     * falla y AssertJ intenta volcar el elemento en el mensaje). */
    private long contarActivas(Long negocioId) {
        return tpvApiKeyRepository.findByNegocioIdOrderByFechaCreacionDesc(negocioId)
                .stream().filter(TpvApiKey::getActiva).count();
    }

    @Test
    @DisplayName("primera emisión: N emitir() concurrentes para un negocio SIN credencial previa dejan exactamente una activa")
    void primeraEmisionConcurrente_dejaExactamenteUnaCredencialActiva() throws InterruptedException {
        negocio = negocioRepository.save(new Negocio("Negocio Concurrencia Primera", "concurrencia-primera@test.com"));
        provisionarUsuarioSistema(negocio);

        // Ninguna fila previa en tpv_api_keys que "bloquear": es exactamente
        // el hueco que un lock pesimista sobre findByNegocioIdAndActivaTrue
        // NO cerraría (no hay nada que lockear si la tabla no tiene filas
        // para este negocio todavía).
        AtomicInteger exitos = emitirConcurrentemente(negocio.getId());

        assertThat(exitos.get())
                .as("al menos un hilo debe haber emitido correctamente")
                .isGreaterThanOrEqualTo(1);

        assertThat(contarActivas(negocio.getId()))
                .as("el backstop de BD (V7) debe garantizar como mucho una credencial activa, incluso en la primera emisión")
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("re-emisión: N emitir() concurrentes para un negocio CON credencial activa previa dejan exactamente una activa")
    void reemisionConcurrente_dejaExactamenteUnaCredencialActiva() throws InterruptedException {
        negocio = negocioRepository.save(new Negocio("Negocio Concurrencia Reemision", "concurrencia-reemision@test.com"));
        tpvApiKeyAdminService.emitir(negocio.getId());

        AtomicInteger exitos = emitirConcurrentemente(negocio.getId());

        assertThat(exitos.get())
                .as("al menos un hilo debe haber reemitido correctamente")
                .isGreaterThanOrEqualTo(1);

        assertThat(contarActivas(negocio.getId()))
                .as("el backstop de BD (V7) debe garantizar como mucho una credencial activa tras la carrera de reemisión")
                .isEqualTo(1L);
    }
}
