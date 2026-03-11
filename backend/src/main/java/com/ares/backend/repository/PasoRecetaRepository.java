package com.ares.backend.repository;

import com.ares.backend.entity.PasoReceta;
import com.ares.backend.entity.Receta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para la entidad PasoReceta.
 * Proporciona métodos para acceder y gestionar pasos de recetas en la base de datos.
 *
 * @author Ares
 * @version 1.0
 */
@Repository
public interface PasoRecetaRepository extends JpaRepository<PasoReceta, Long> {

    /**
     * Busca todos los pasos de una receta específica, ordenados por orden ascendente.
     *
     * @param receta Receta de la cual obtener los pasos
     * @return Lista de pasos ordenados secuencialmente
     */
    List<PasoReceta> findByRecetaOrderByOrdenAsc(Receta receta);

    /**
     * Elimina todos los pasos asociados a una receta específica.
     *
     * @param receta Receta cuyos pasos se eliminarán
     */
    void deleteByReceta(Receta receta);
}