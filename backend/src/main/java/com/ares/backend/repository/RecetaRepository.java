package com.ares.backend.repository;

import com.ares.backend.entity.Receta;
import com.ares.backend.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

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
}