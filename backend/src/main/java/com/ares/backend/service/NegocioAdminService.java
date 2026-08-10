package com.ares.backend.service;

import com.ares.backend.entity.Negocio;
import com.ares.backend.entity.NegocioSignupCode;
import com.ares.backend.exception.CodigoGeneracionException;
import com.ares.backend.exception.ConflictoEstadoException;
import com.ares.backend.exception.RecursoNoEncontradoException;
import com.ares.backend.repository.NegocioRepository;
import com.ares.backend.repository.NegocioSignupCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de provisión del superadmin de plataforma: crea Negocios,
 * emite y revoca sus códigos de alta. NO es un {@code Usuario} ni
 * pertenece a ningún Negocio (tenant); en consecuencia, este servicio
 * SOLO declara como colaboradores {@link NegocioRepository}, {@link
 * NegocioSignupCodeRepository} y {@link SignupCodeGenerator} — nunca un
 * servicio tenant-scoped (ver {@code NegocioAdminServiceDependencyTest},
 * diseño D1-B de "negocio-onboarding-admin"). El controlador que expone
 * estos métodos tras {@code hasRole("PLATFORM_ADMIN")} llega en un cambio
 * posterior.
 *
 * @author Ares
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class NegocioAdminService {

    /**
     * Intentos máximos para generar un código de alta que no colisione con
     * uno ya existente antes de rendirse (ver {@link
     * CodigoGeneracionException}).
     */
    private static final int MAX_INTENTOS_GENERACION_CODIGO = 5;

    private final NegocioRepository negocioRepository;
    private final NegocioSignupCodeRepository negocioSignupCodeRepository;
    private final SignupCodeGenerator signupCodeGenerator;

    /**
     * Crea un Negocio y su primer código de alta en una única operación
     * atómica: o quedan persistidas ambas filas, o ninguna. No exige
     * unicidad de {@code nombre} entre Negocios.
     *
     * @param nombre        Nombre comercial del negocio
     * @param emailContacto Email de contacto (opcional)
     * @return El código de alta recién creado; {@code getNegocio()} sobre
     *         él expone el Negocio recién creado (referencia en memoria,
     *         no un proxy lazy, así que es seguro leerlo fuera de la
     *         transacción)
     * @throws CodigoGeneracionException Si se agotan los intentos de
     *                                    generar un código único
     */
    @Transactional
    public NegocioSignupCode crearNegocio(String nombre, String emailContacto) {
        Negocio negocio = negocioRepository.save(new Negocio(nombre, emailContacto));

        String codigo = generarCodigoUnico();

        return negocioSignupCodeRepository.save(new NegocioSignupCode(negocio, codigo));
    }

    /**
     * Emite un código de alta adicional para un Negocio ya existente. No
     * hay límite en la cantidad de códigos simultáneamente válidos por
     * Negocio, y emitir uno nuevo está permitido aunque el primer jefe de
     * ese Negocio ya se haya registrado.
     *
     * @param negocioId Id del Negocio para el que se emite el código
     * @return El código de alta recién creado
     * @throws RecursoNoEncontradoException Si el negocio no existe
     * @throws CodigoGeneracionException    Si se agotan los intentos de
     *                                       generar un código único
     */
    @Transactional
    public NegocioSignupCode generarCodigo(Long negocioId) {
        Negocio negocio = negocioRepository.findById(negocioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Negocio no encontrado"));

        String codigo = generarCodigoUnico();

        return negocioSignupCodeRepository.save(new NegocioSignupCode(negocio, codigo));
    }

    /**
     * Revoca un código de alta. La reclamación es atómica a nivel de base
     * de datos (ver {@link NegocioSignupCodeRepository#revocarAtomico}); si
     * no afecta ninguna fila, se relee el código para distinguir entre las
     * 3 causas posibles de esos 0 resultados: no existe (404), ya fue
     * usado (409 — revocarlo no desactiva la cuenta que ya creó, así que
     * un 200 silencioso daría al operador una falsa sensación de haber
     * cortado el acceso) o ya estaba revocado y sin usar (200 idempotente,
     * porque la intención ya estaba satisfecha).
     *
     * @param id Id del código de alta a revocar
     * @return El código de alta en su estado posterior a la revocación
     * @throws RecursoNoEncontradoException Si el código no existe
     * @throws ConflictoEstadoException     Si el código ya fue usado
     */
    @Transactional
    public NegocioSignupCode revocarCodigo(Long id) {
        if (negocioSignupCodeRepository.revocarAtomico(id) == 1) {
            return negocioSignupCodeRepository.findById(id)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Código de alta no encontrado"));
        }

        NegocioSignupCode codigo = negocioSignupCodeRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Código de alta no encontrado"));

        if (Boolean.TRUE.equals(codigo.getUsado())) {
            throw new ConflictoEstadoException(
                    "El código ya fue usado; revocarlo no desactiva la cuenta que ya creó");
        }

        // Único caso restante: activo=false, usado=false -> ya estaba
        // revocado. La intención del operador ya está satisfecha:
        // idempotente, se devuelve el estado actual sin error.
        return codigo;
    }

    /**
     * Lista todos los Negocios dados de alta, más reciente primero.
     *
     * @return Negocios ordenados por fecha de alta descendente
     */
    public List<Negocio> listarNegocios() {
        return negocioRepository.findAll(Sort.by(Sort.Direction.DESC, "fechaAlta"));
    }

    /**
     * Lista todos los códigos de alta de un Negocio (usados, sin usar y
     * revocados), más reciente primero.
     *
     * @param negocioId Id del Negocio cuyos códigos se listan
     * @return Códigos del negocio ordenados por fecha de creación descendente
     * @throws RecursoNoEncontradoException Si el negocio no existe
     */
    public List<NegocioSignupCode> listarCodigos(Long negocioId) {
        if (!negocioRepository.existsById(negocioId)) {
            throw new RecursoNoEncontradoException("Negocio no encontrado");
        }

        return negocioSignupCodeRepository.findByNegocioIdOrderByFechaCreacionDesc(negocioId);
    }

    /**
     * Genera un código de alta que no colisione con ninguno ya existente,
     * comprobándolo con una SELECT antes de cada intento (no un
     * catch-and-retry sobre la violación de unicidad de la base de datos:
     * una vez que esa violación surge en el flush, la transacción queda en
     * rollback-only y reintentar dentro del mismo método
     * {@code @Transactional} es imposible). La restricción única {@code
     * uk_negocio_signup_codes_codigo} sigue siendo el respaldo de
     * correctitud final ante una carrera genuina TOCTOU, estadísticamente
     * inalcanzable con este espacio de búsqueda (32^10 ≈ 2^50).
     *
     * @return Un código de alta libre
     * @throws CodigoGeneracionException Si {@value #MAX_INTENTOS_GENERACION_CODIGO}
     *                                    intentos consecutivos colisionan
     */
    private String generarCodigoUnico() {
        for (int intento = 0; intento < MAX_INTENTOS_GENERACION_CODIGO; intento++) {
            String candidato = signupCodeGenerator.generar();
            if (negocioSignupCodeRepository.findByCodigo(candidato).isEmpty()) {
                return candidato;
            }
        }
        throw new CodigoGeneracionException(
                "No se pudo generar un código de alta único tras "
                        + MAX_INTENTOS_GENERACION_CODIGO + " intentos");
    }
}
