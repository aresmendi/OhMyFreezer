package com.ares.backend.repository;

import com.ares.backend.entity.Alerta;
import com.ares.backend.entity.Ingrediente;
import com.ares.backend.entity.Receta;
import com.ares.backend.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para la entidad Alerta.
 * Proporciona métodos para acceder y gestionar alertas del sistema.
 *
 * @author Ares
 * @version 1.0
 */
@Repository
public interface AlertaRepository extends JpaRepository<Alerta, Long> {

    /**
     * Busca todas las alertas pendientes (no leídas) de un usuario específico.
     *
     * @param destinatario Usuario jefe de cocina destinatario
     * @param leida Estado de lectura (false para pendientes)
     * @return Lista de alertas no leídas
     */
    List<Alerta> findByDestinatarioAndLeidaOrderByFechaCreacionDesc(Usuario destinatario, Boolean leida);

    /**
     * Busca todas las alertas de un usuario específico.
     *
     * @param destinatario Usuario jefe de cocina destinatario
     * @return Lista de todas las alertas del usuario
     */
    List<Alerta> findByDestinatarioOrderByFechaCreacionDesc(Usuario destinatario);

    /**
     * Busca todas las alertas de un usuario específico por ID.
     *
     * @param destinatarioId ID del usuario jefe de cocina destinatario
     * @return Lista de todas las alertas del usuario
     */
    List<Alerta> findByDestinatarioIdOrderByFechaCreacionDesc(Long destinatarioId);

    /**
     * Busca todas las alertas no leídas de un usuario específico por ID.
     *
     * @param destinatarioId ID del usuario jefe de cocina destinatario
     * @return Lista de alertas no leídas
     */
    List<Alerta> findByDestinatarioIdAndLeidaFalseOrderByFechaCreacionDesc(Long destinatarioId);

    /**
     * Busca todas las alertas no leídas de un usuario específico por ID (sin ordenar).
     *
     * @param destinatarioId ID del usuario jefe de cocina destinatario
     * @return Lista de alertas no leídas
     */
    List<Alerta> findByDestinatarioIdAndLeidaFalse(Long destinatarioId);

    /**
     * Busca alertas por tipo específico.
     *
     * @param tipo Tipo de alerta (STOCK_BAJO, RECETA_NO_DISPONIBLE)
     * @return Lista de alertas de ese tipo
     */
    List<Alerta> findByTipo(String tipo);

    /**
     * Busca alertas relacionadas con un ingrediente específico.
     *
     * @param ingrediente Ingrediente relacionado con la alerta
     * @return Lista de alertas del ingrediente
     */
    List<Alerta> findByIngrediente(Ingrediente ingrediente);

    /**
     * Busca alertas relacionadas con una receta específica.
     *
     * @param receta Receta relacionada con la alerta
     * @return Lista de alertas de la receta
     */
    List<Alerta> findByReceta(Receta receta);

    /**
     * Cuenta el número de alertas pendientes de un usuario.
     *
     * @param destinatario Usuario jefe de cocina destinatario
     * @param leida Estado de lectura (false para pendientes)
     * @return Número de alertas pendientes
     */
    Long countByDestinatarioAndLeida(Usuario destinatario, Boolean leida);

    /**
     * Cuenta el número de alertas pendientes de un usuario por ID.
     *
     * @param destinatarioId ID del usuario jefe de cocina destinatario
     * @return Número de alertas pendientes
     */
    Long countByDestinatarioIdAndLeidaFalse(Long destinatarioId);

    /**
     * Busca todas las alertas no leídas de un ingrediente específico por ID.
     *
     * @param ingredienteId ID del ingrediente
     * @return Lista de alertas no leídas del ingrediente
     */
    List<Alerta> findByIngredienteIdAndLeidaFalse(Long ingredienteId);

    /**
     * Verifica si existe una alerta no leída para un ingrediente específico.
     *
     * @param ingredienteId ID del ingrediente
     * @return true si existe, false en caso contrario
     */
    boolean existsByIngredienteIdAndLeidaFalse(Long ingredienteId);

    /**
     * Verifica si existe una alerta no leída para una receta específica.
     *
     * @param recetaId ID de la receta
     * @return true si existe, false en caso contrario
     */
    boolean existsByRecetaIdAndLeidaFalse(Long recetaId);

    /**
     * Elimina todas las alertas relacionadas con un ingrediente específico.
     *
     * @param ingrediente Ingrediente cuyas alertas se eliminarán
     */
    void deleteByIngrediente(Ingrediente ingrediente);

    /**
     * Elimina todas las alertas relacionadas con una receta específica.
     *
     * @param receta Receta cuyas alertas se eliminarán
     */
    void deleteByReceta(Receta receta);
}