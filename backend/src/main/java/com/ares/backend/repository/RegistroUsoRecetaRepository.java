package com.ares.backend.repository;

import com.ares.backend.entity.Receta;
import com.ares.backend.entity.RegistroUsoReceta;
import com.ares.backend.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio para la entidad RegistroUsoReceta.
 * Proporciona métodos para acceder y gestionar registros de uso de recetas.
 * Útil para generar estadísticas de consumo.
 *
 * @author Ares
 * @version 1.0
 */
@Repository
public interface RegistroUsoRecetaRepository extends JpaRepository<RegistroUsoReceta, Long> {

    /**
     * Busca todos los registros de uso de una receta específica.
     *
     * @param receta Receta de la cual obtener los registros
     * @return Lista de registros de uso
     */
    List<RegistroUsoReceta> findByReceta(Receta receta);

    /**
     * Busca todos los registros de uso de una receta específica por ID.
     *
     * @param recetaId ID de la receta
     * @return Lista de registros de uso
     */
    List<RegistroUsoReceta> findByRecetaId(Long recetaId);

    /**
     * Busca todos los registros de uso realizados por un usuario específico.
     *
     * @param usuario Usuario del cual obtener los registros
     * @return Lista de registros de uso
     */
    List<RegistroUsoReceta> findByUsuario(Usuario usuario);

    /**
     * Busca todos los registros de uso realizados por un usuario específico por ID.
     *
     * @param usuarioId ID del usuario
     * @return Lista de registros de uso
     */
    List<RegistroUsoReceta> findByUsuarioId(Long usuarioId);

    /**
     * Busca registros de uso de una receta en un rango de fechas.
     *
     * @param receta Receta a buscar
     * @param fechaInicio Fecha de inicio del rango
     * @param fechaFin Fecha de fin del rango
     * @return Lista de registros en el rango de fechas
     */
    List<RegistroUsoReceta> findByRecetaAndFechaElaboracionBetween(
            Receta receta,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin
    );

    /**
     * Busca registros de uso de una receta por ID en un rango de fechas.
     *
     * @param recetaId ID de la receta
     * @param fechaInicio Fecha de inicio del rango
     * @param fechaFin Fecha de fin del rango
     * @return Lista de registros en el rango de fechas
     */
    List<RegistroUsoReceta> findByRecetaIdAndFechaElaboracionBetween(
            Long recetaId,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin
    );

    /**
     * Busca todos los registros de uso en un rango de fechas.
     *
     * @param fechaInicio Fecha de inicio del rango
     * @param fechaFin Fecha de fin del rango
     * @return Lista de registros en el rango de fechas
     */
    List<RegistroUsoReceta> findByFechaElaboracionBetween(
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin
    );

    /**
     * Cuenta cuántas veces se ha elaborado una receta específica.
     *
     * @param receta Receta a contar
     * @return Número de veces que se ha elaborado
     */
    Long countByReceta(Receta receta);

    /**
     * Obtiene las recetas más utilizadas en un rango de fechas.
     *
     * @param fechaInicio Fecha de inicio del rango
     * @param fechaFin Fecha de fin del rango
     * @return Lista de objetos con receta y cantidad de usos
     */
    @Query("SELECT r.receta, COUNT(r) as usos FROM RegistroUsoReceta r " +
            "WHERE r.fechaElaboracion BETWEEN :fechaInicio AND :fechaFin " +
            "GROUP BY r.receta ORDER BY usos DESC")
    List<Object[]> findRecetasMasUsadas(
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin
    );

    /**
     * Elimina todos los registros de uso asociados a una receta específica.
     *
     * @param receta Receta cuyos registros de uso se eliminarán
     */
    void deleteByReceta(Receta receta);
}