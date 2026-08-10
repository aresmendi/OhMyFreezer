package com.ares.backend.repository;

import com.ares.backend.entity.Negocio;
import com.ares.backend.entity.NegocioSignupCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba, contra una BD real (H2), que la reclamación de un código de alta
 * es realmente atómica a nivel de base de datos: dos llamadas consecutivas
 * a {@code marcarUsadoAtomico} sobre el mismo código sólo pueden ganar una
 * vez, sin depender de mocks ni de temporización de hilos (test
 * determinista en lugar de uno multi-hilo propenso a flakiness).
 */
@DataJpaTest
@Transactional
class NegocioSignupCodeRepositoryTest {

    @Autowired private NegocioRepository negocioRepository;
    @Autowired private NegocioSignupCodeRepository negocioSignupCodeRepository;

    private Negocio negocio;

    @BeforeEach
    void crearNegocio() {
        negocio = negocioRepository.save(new Negocio("Negocio X", "x@ares.dev"));
    }

    @Test
    @DisplayName("la primera reclamación de un código válido afecta 1 fila y la segunda afecta 0 (single-use real)")
    void segundaReclamacionDelMismoCodigoAfectaCeroFilas() {
        NegocioSignupCode codigo = new NegocioSignupCode(negocio, "CODIGO_ATOMICO");
        negocioSignupCodeRepository.saveAndFlush(codigo);

        int primeraReclamacion = negocioSignupCodeRepository.marcarUsadoAtomico("CODIGO_ATOMICO");
        int segundaReclamacion = negocioSignupCodeRepository.marcarUsadoAtomico("CODIGO_ATOMICO");

        assertThat(primeraReclamacion)
                .as("la primera llamada debe ganar la reclamación y marcar el código como usado")
                .isEqualTo(1);
        assertThat(segundaReclamacion)
                .as("la segunda llamada, aunque llegue justo después, no puede volver a reclamar el mismo código")
                .isEqualTo(0);

        NegocioSignupCode recargado = negocioSignupCodeRepository.findByCodigo("CODIGO_ATOMICO").orElseThrow();
        assertThat(recargado.getUsado()).isTrue();
    }

    @Test
    @DisplayName("marcarUsadoAtomico no reclama un código revocado (activo=false) aunque no esté usado")
    void noReclamaCodigoRevocado() {
        NegocioSignupCode codigo = new NegocioSignupCode(negocio, "CODIGO_REVOCADO");
        codigo.setActivo(false);
        negocioSignupCodeRepository.saveAndFlush(codigo);

        int reclamacion = negocioSignupCodeRepository.marcarUsadoAtomico("CODIGO_REVOCADO");

        assertThat(reclamacion).isEqualTo(0);
    }

    @Test
    @DisplayName("registrarUsuarioQueConsumio persiste el usuarioId y la fecha de uso sin tocar usado/activo")
    void registrarUsuarioQueConsumioPersisteMetadata() {
        NegocioSignupCode codigo = new NegocioSignupCode(negocio, "CODIGO_METADATA");
        negocioSignupCodeRepository.saveAndFlush(codigo);

        negocioSignupCodeRepository.marcarUsadoAtomico("CODIGO_METADATA");
        negocioSignupCodeRepository.registrarUsuarioQueConsumio("CODIGO_METADATA", 42L);

        NegocioSignupCode recargado = negocioSignupCodeRepository.findByCodigo("CODIGO_METADATA").orElseThrow();
        assertThat(recargado.getUsado()).isTrue();
        assertThat(recargado.getUsadoPorUsuarioId()).isEqualTo(42L);
        assertThat(recargado.getFechaUso()).isNotNull();
    }

    @Test
    @DisplayName("findByNegocioIdOrderByFechaCreacionDesc devuelve solo los códigos de ese negocio, más nuevo primero")
    void findByNegocioIdOrderByFechaCreacionDescOrdenaDescendente() throws InterruptedException {
        Negocio otroNegocio = negocioRepository.save(new Negocio("Negocio Y", "y@ares.dev"));

        NegocioSignupCode primero = negocioSignupCodeRepository.saveAndFlush(new NegocioSignupCode(negocio, "PRIMERO"));
        // Pequeña pausa para garantizar fechaCreacion estrictamente distinta
        // entre inserciones (LocalDateTime.now() en el constructor de la
        // entidad, no una columna generada por la BD).
        Thread.sleep(5);
        NegocioSignupCode segundo = negocioSignupCodeRepository.saveAndFlush(new NegocioSignupCode(negocio, "SEGUNDO"));
        negocioSignupCodeRepository.saveAndFlush(new NegocioSignupCode(otroNegocio, "DE_OTRO_NEGOCIO"));

        List<NegocioSignupCode> codigos =
                negocioSignupCodeRepository.findByNegocioIdOrderByFechaCreacionDesc(negocio.getId());

        assertThat(codigos).hasSize(2);
        assertThat(codigos).extracting(NegocioSignupCode::getCodigo).containsExactly("SEGUNDO", "PRIMERO");
        assertThat(codigos).extracting(NegocioSignupCode::getId)
                .containsExactly(segundo.getId(), primero.getId());
    }

    @Test
    @DisplayName("revocarAtomico pone activo=false y afecta 1 fila cuando el código está activo y sin usar")
    void revocarAtomicoRevocaCodigoActivoYSinUsar() {
        NegocioSignupCode codigo = negocioSignupCodeRepository.saveAndFlush(new NegocioSignupCode(negocio, "A_REVOCAR"));

        int filasAfectadas = negocioSignupCodeRepository.revocarAtomico(codigo.getId());

        assertThat(filasAfectadas).isEqualTo(1);
        NegocioSignupCode recargado = negocioSignupCodeRepository.findByCodigo("A_REVOCAR").orElseThrow();
        assertThat(recargado.getActivo()).isFalse();
    }

    @Test
    @DisplayName("revocarAtomico afecta 0 filas sobre un código ya usado (no lo toca)")
    void revocarAtomicoNoAfectaCodigoYaUsado() {
        NegocioSignupCode codigo = new NegocioSignupCode(negocio, "YA_USADO");
        codigo.marcarUsado(1L);
        negocioSignupCodeRepository.saveAndFlush(codigo);

        int filasAfectadas = negocioSignupCodeRepository.revocarAtomico(codigo.getId());

        assertThat(filasAfectadas).isEqualTo(0);
        NegocioSignupCode recargado = negocioSignupCodeRepository.findByCodigo("YA_USADO").orElseThrow();
        assertThat(recargado.getActivo()).isTrue();
    }

    @Test
    @DisplayName("revocarAtomico afecta 0 filas sobre un código ya revocado (idempotente a nivel de UPDATE)")
    void revocarAtomicoNoAfectaCodigoYaRevocado() {
        NegocioSignupCode codigo = new NegocioSignupCode(negocio, "YA_REVOCADO");
        codigo.setActivo(false);
        negocioSignupCodeRepository.saveAndFlush(codigo);

        int filasAfectadas = negocioSignupCodeRepository.revocarAtomico(codigo.getId());

        assertThat(filasAfectadas).isEqualTo(0);
        NegocioSignupCode recargado = negocioSignupCodeRepository.findByCodigo("YA_REVOCADO").orElseThrow();
        assertThat(recargado.getActivo()).isFalse();
    }
}
