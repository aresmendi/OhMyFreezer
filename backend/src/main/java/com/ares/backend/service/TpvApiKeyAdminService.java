package com.ares.backend.service;

import com.ares.backend.config.TpvConstantes;
import com.ares.backend.entity.Negocio;
import com.ares.backend.entity.TpvApiKey;
import com.ares.backend.entity.Usuario;
import com.ares.backend.exception.RecursoNoEncontradoException;
import com.ares.backend.repository.NegocioRepository;
import com.ares.backend.repository.TpvApiKeyRepository;
import com.ares.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Servicio de provisión de credenciales TPV del superadmin de plataforma:
 * emite (con provisioning atómico del usuario sintético), revoca y lista
 * las credenciales de un Negocio. El admin de plataforma no es un {@code
 * Usuario} de ningún Negocio; en consecuencia, y siguiendo el mismo
 * principio que {@link NegocioAdminService} (D1-B de
 * "negocio-onboarding-admin"), este servicio SOLO declara como
 * colaboradores {@link TpvApiKeyRepository}, {@link UsuarioRepository},
 * {@link NegocioRepository} y {@link PasswordEncoder} (ver {@code
 * TpvApiKeyAdminServiceDependencyTest}, diseño D-G de "tpv-integration").
 * <p>
 * <b>Importante</b>: usa {@link UsuarioRepository} DIRECTAMENTE, nunca
 * {@code UsuarioService} — cuyos métodos invocan {@code SecurityUtils} y
 * lanzarían {@code ClassCastException} bajo el principal {@code String}
 * plano {@code "platform-admin"} que produce {@code AdminApiKeyFilter}.
 *
 * @author Ares
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class TpvApiKeyAdminService {

    private static final int LONGITUD_PREFIJO = 12;
    private static final int LONGITUD_SECRETO = 32;
    private static final String ALFABETO_BASE62 =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final TpvApiKeyRepository tpvApiKeyRepository;
    private final UsuarioRepository usuarioRepository;
    private final NegocioRepository negocioRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Emite una credencial TPV nueva para un Negocio. Provisiona
     * atómicamente el usuario sintético del sistema en la primera emisión
     * (email reservado {@code tpv+negocio-{id}@tpv.ohmyfreezer.invalid},
     * ver {@link TpvConstantes#DOMINIO_EMAIL_TPV}); en re-emisiones
     * reutiliza el mismo usuario sintético y revoca la credencial activa
     * anterior del negocio (D3 del diseño: como mucho una activa a la vez).
     *
     * @param negocioId Id del Negocio (tenant) para el que se emite
     * @return La credencial persistida más la key completa EN CLARO,
     *         recuperable solo en este instante
     * @throws RecursoNoEncontradoException Si el negocio no existe
     */
    @Transactional
    public TpvApiKeyEmitida emitir(Long negocioId) {
        Negocio negocio = negocioRepository.findById(negocioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Negocio no encontrado"));

        Usuario usuarioSistema = obtenerOProvisionarUsuarioSistema(negocio);

        // D3: como mucho una credencial activa por negocio. Revocar ANTES
        // de crear la nueva evita una ventana en la que ambas estén activas.
        tpvApiKeyRepository.findByNegocioIdAndActivaTrue(negocioId)
                .ifPresent(TpvApiKey::revocar);

        String prefijo = generarAleatorio(LONGITUD_PREFIJO);
        String secreto = generarAleatorio(LONGITUD_SECRETO);
        String secretoHash = passwordEncoder.encode(secreto);

        TpvApiKey nuevaClave = new TpvApiKey(negocio, prefijo, secretoHash, usuarioSistema);
        tpvApiKeyRepository.save(nuevaClave);

        String claveEnClaro = TpvConstantes.PREFIJO_API_KEY + prefijo + "_" + secreto;
        return new TpvApiKeyEmitida(nuevaClave, claveEnClaro);
    }

    /**
     * Revoca una credencial TPV. Idempotente: revocar una credencial ya
     * revocada no lanza excepción, solo vuelve a sellar
     * {@code fechaRevocacion} (ver {@link TpvApiKey#revocar()}).
     *
     * @param tpvApiKeyId Id de la credencial a revocar
     * @throws RecursoNoEncontradoException Si la credencial no existe
     */
    @Transactional
    public void revocar(Long tpvApiKeyId) {
        TpvApiKey clave = tpvApiKeyRepository.findById(tpvApiKeyId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Credencial TPV no encontrada"));

        clave.revocar();
        tpvApiKeyRepository.save(clave);
    }

    /**
     * Lista todas las credenciales (activas y revocadas) de un Negocio,
     * más reciente primero.
     *
     * @param negocioId Id del Negocio (tenant)
     * @return Credenciales del negocio ordenadas por fecha de creación descendente
     * @throws RecursoNoEncontradoException Si el negocio no existe
     */
    public List<TpvApiKey> listar(Long negocioId) {
        if (!negocioRepository.existsById(negocioId)) {
            throw new RecursoNoEncontradoException("Negocio no encontrado");
        }

        return tpvApiKeyRepository.findByNegocioIdOrderByFechaCreacionDesc(negocioId);
    }

    /**
     * Resuelve el usuario sintético del sistema para este Negocio,
     * creándolo si es la primera emisión. El email reservado es único
     * globalmente y determinista a partir del id del negocio, así que
     * también sirve de clave de idempotencia del provisioning.
     */
    private Usuario obtenerOProvisionarUsuarioSistema(Negocio negocio) {
        String email = "tpv+negocio-" + negocio.getId() + "@" + TpvConstantes.DOMINIO_EMAIL_TPV;

        Optional<Usuario> existente = usuarioRepository.findByEmail(email);
        if (existente.isPresent()) {
            return existente.get();
        }

        Usuario usuarioSistema = new Usuario();
        usuarioSistema.setUsername("tpv-system");
        // Sentinel NO bcrypt: UsuarioService.login() rechaza el dominio
        // reservado ANTES de llegar a comparar contraseña (ver tarea 3.3),
        // así que este valor nunca se compara con passwordEncoder.matches().
        usuarioSistema.setPassword("!TPV_SYSTEM_USER_NO_LOGIN!");
        usuarioSistema.setEsJefeCocina(false);
        usuarioSistema.setFechaRegistro(LocalDateTime.now());
        usuarioSistema.setEmail(email);
        usuarioSistema.setNegocio(negocio);

        return usuarioRepository.save(usuarioSistema);
    }

    /**
     * Genera una cadena aleatoria en alfabeto base62 (sin separadores),
     * usada tanto para el prefijo público como para el secreto. El
     * espacio de búsqueda (62^12 para el prefijo, 62^32 para el secreto)
     * hace estadísticamente inalcanzable una colisión; el índice único
     * {@code uk_tpv_api_keys_prefijo} sigue siendo el respaldo de
     * correctitud final.
     */
    private String generarAleatorio(int longitud) {
        StringBuilder sb = new StringBuilder(longitud);
        for (int i = 0; i < longitud; i++) {
            sb.append(ALFABETO_BASE62.charAt(RANDOM.nextInt(ALFABETO_BASE62.length())));
        }
        return sb.toString();
    }
}
