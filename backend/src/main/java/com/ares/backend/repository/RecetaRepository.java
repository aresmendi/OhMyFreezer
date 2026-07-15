package com.ares.backend.repository;

import com.ares.backend.entity.Receta;
import com.ares.backend.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad Receta.
 * Proporciona métodos para acceder y gestionar recetas en la base de datos.
 *
 * @author Ares
 * @version 1.0
 */
@Repository
public interface RecetaRepository extends JpaRepository<Receta, Long> {

    /**
     * Busca todas las recetas creadas por un usuario específico.
     *
     * @param creadaPor Usuario que creó las recetas
     * @return Lista de recetas creadas por ese usuario
     */
    List<Receta> findByCreadaPor(Usuario creadaPor);

    /**
     * Busca recetas por nombre (búsqueda parcial, case-insensitive).
     *
     * @param nombre Nombre o parte del nombre de la receta
     * @return Lista de recetas que coinciden con el nombre
     */
    List<Receta> findByNombreContainingIgnoreCase(String nombre);

    /**
     * Busca todas las recetas ordenadas por fecha de creación descendente.
     *
     * @return Lista de recetas ordenadas de más reciente a más antigua
     */
    List<Receta> findAllByOrderByFechaCreacionDesc();

    /**
     * Busca una receta por ID cargando ingredientes (y el ingrediente base).
     * Separada de pasos para evitar MultipleBagFetchException.
     */
    @Query("SELECT DISTINCT r FROM Receta r " +
           "LEFT JOIN FETCH r.ingredientes ri " +
           "LEFT JOIN FETCH ri.ingrediente " +
           "LEFT JOIN FETCH r.creadaPor " +
           "WHERE r.id = :id")
    Optional<Receta> findByIdWithIngredientes(@Param("id") Long id);

    /**
     * Busca una receta por ID cargando solo los pasos.
     * Separada de ingredientes para evitar MultipleBagFetchException.
     */
    @Query("SELECT DISTINCT r FROM Receta r " +
           "LEFT JOIN FETCH r.pasos " +
           "WHERE r.id = :id")
    Optional<Receta> findByIdWithPasos(@Param("id") Long id);

    /**
     * Busca todas las recetas cargando ingredientes (y el ingrediente base).
     * Separada de pasos para evitar MultipleBagFetchException.
     */
    @Query("SELECT DISTINCT r FROM Receta r " +
           "LEFT JOIN FETCH r.ingredientes ri " +
           "LEFT JOIN FETCH ri.ingrediente " +
           "LEFT JOIN FETCH r.creadaPor")
    List<Receta> findAllWithIngredientes();

    /**
     * Busca todas las recetas cargando solo los pasos.
     * Separada de ingredientes para evitar MultipleBagFetchException.
     */
    @Query("SELECT DISTINCT r FROM Receta r " +
           "LEFT JOIN FETCH r.pasos")
    List<Receta> findAllWithPasos();

    /**
     * Busca una receta por ID, scoped al negocio (tenant) del caller.
     * Un id perteneciente a otro negocio no puede cargarse por esta vía:
     * el Optional viene vacío exactamente igual que si el id no existiera.
     *
     * @param id ID de la receta
     * @param negocioId ID del negocio (tenant) del caller autenticado
     * @return Optional con la receta si existe y pertenece a ese negocio
     */
    Optional<Receta> findByIdAndNegocioId(Long id, Long negocioId);

    /**
     * Busca una receta por ID cargando ingredientes, scoped al negocio del caller.
     * Separada de pasos para evitar MultipleBagFetchException.
     */
    @Query("SELECT DISTINCT r FROM Receta r " +
           "LEFT JOIN FETCH r.ingredientes ri " +
           "LEFT JOIN FETCH ri.ingrediente " +
           "LEFT JOIN FETCH r.creadaPor " +
           "WHERE r.id = :id AND r.negocio.id = :negocioId")
    Optional<Receta> findByIdWithIngredientesAndNegocioId(@Param("id") Long id, @Param("negocioId") Long negocioId);

    /**
     * Busca una receta por ID cargando solo los pasos, scoped al negocio del caller.
     * Separada de ingredientes para evitar MultipleBagFetchException.
     */
    @Query("SELECT DISTINCT r FROM Receta r " +
           "LEFT JOIN FETCH r.pasos " +
           "WHERE r.id = :id AND r.negocio.id = :negocioId")
    Optional<Receta> findByIdWithPasosAndNegocioId(@Param("id") Long id, @Param("negocioId") Long negocioId);
}