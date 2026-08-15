package com.ares.backend.repository;

import com.ares.backend.entity.NegocioSignupCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad NegocioSignupCode.
 * Proporciona operaciones CRUD básicas sobre los códigos de alta de negocio.
 *
 * @author Ares
 * @version 1.0
 */
@Repository
public interface NegocioSignupCodeRepository extends JpaRepository<NegocioSignupCode, Long> {

    /**
     * Busca un código de alta por su valor literal. Usado por
     * {@code UsuarioService.registrar()} para resolver a qué Negocio
     * pertenece el código aportado en el registro público.
     *
     * @param codigo Código de alta a buscar
     * @return Optional con el código si existe
     */
    Optional<NegocioSignupCode> findByCodigo(String codigo);

    /**
     * Reclama atómicamente un código de alta a nivel de base de datos: lo
     * marca como usado en la MISMA sentencia UPDATE que comprueba que
     * todavía estaba activo y sin usar. Esto cierra la ventana de carrera
     * que existía al leer {@code esValido()} en memoria y persistir la
     * entidad recién al final del registro: dos peticiones concurrentes con
     * el mismo código ya no pueden ganar ambas la reclamación, porque el
     * UPDATE de la segunda afecta cero filas una vez que la primera ya
     * puso {@code usado=true}.
     *
     * @param codigo Código de alta a reclamar
     * @return 1 si esta llamada ganó la reclamación (el código quedó
     *         marcado como usado), 0 si ya estaba usado o fue revocado
     *         justo antes de esta llamada
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE NegocioSignupCode n SET n.usado = true "
            + "WHERE n.codigo = :codigo AND n.usado = false AND n.activo = true")
    int marcarUsadoAtomico(@Param("codigo") String codigo);

    /**
     * Registra qué usuario consumió un código de alta y cuándo. Se invoca
     * solo DESPUÉS de que {@link #marcarUsadoAtomico(String)} haya ganado la
     * reclamación y se haya creado el Usuario correspondiente: no forma
     * parte de la sección crítica de la carrera, es únicamente metadata
     * informativa sobre un código que ya quedó marcado como usado.
     *
     * @param codigo    Código de alta ya reclamado
     * @param usuarioId Id del usuario (jefe) que lo consumió
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE NegocioSignupCode n SET n.usadoPorUsuarioId = :usuarioId, n.fechaUso = CURRENT_TIMESTAMP "
            + "WHERE n.codigo = :codigo")
    void registrarUsuarioQueConsumio(@Param("codigo") String codigo, @Param("usuarioId") Long usuarioId);

    /**
     * Lista todos los códigos de alta de un Negocio (usados, sin usar y
     * revocados), más nuevo primero. Usado por el listado de códigos del
     * admin de plataforma.
     *
     * @param negocioId Id del Negocio cuyos códigos se listan
     * @return Códigos del negocio ordenados por fecha de creación descendente
     */
    List<NegocioSignupCode> findByNegocioIdOrderByFechaCreacionDesc(Long negocioId);

    /**
     * Revoca atómicamente un código de alta a nivel de base de datos: lo
     * marca como inactivo en la MISMA sentencia UPDATE que comprueba que
     * todavía estaba activo y sin usar, cerrando la misma ventana de carrera
     * que {@link #marcarUsadoAtomico(String)} cierra desde el otro lado (una
     * revocación y una reclamación concurrentes sobre el mismo código nunca
     * pueden ganar ambas).
     *
     * @param id Id del código de alta a revocar
     * @return 1 si esta llamada revocó el código (estaba activo y sin usar),
     *         0 si ya estaba usado o ya estaba revocado
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE NegocioSignupCode n SET n.activo = false "
            + "WHERE n.id = :id AND n.usado = false AND n.activo = true")
    int revocarAtomico(@Param("id") Long id);
}
