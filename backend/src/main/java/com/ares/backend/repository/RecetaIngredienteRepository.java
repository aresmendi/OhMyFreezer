package com.ares.backend.repository;

import com.ares.backend.entity.Ingrediente;
import com.ares.backend.entity.Receta;
import com.ares.backend.entity.RecetaIngrediente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para la entidad RecetaIngrediente.
 * Proporciona métodos para acceder y gestionar relaciones entre recetas e ingredientes.
 *
 * @author Ares
 * @version 1.0
 */
@Repository
public interface RecetaIngredienteRepository extends JpaRepository<RecetaIngrediente, Long> {

    /**
     * Busca todos los ingredientes necesarios para una receta específica.
     *
     * @param receta Receta de la cual obtener los ingredientes
     * @return Lista de relaciones receta-ingrediente
     */
    List<RecetaIngrediente> findByReceta(Receta receta);

    /**
     * Busca todas las recetas que utilizan un ingrediente específico.
     *
     * @param ingrediente Ingrediente a buscar en las recetas
     * @return Lista de relaciones receta-ingrediente
     */
    List<RecetaIngrediente> findByIngrediente(Ingrediente ingrediente);

    /**
     * Elimina todas las relaciones de ingredientes asociadas a una receta específica.
     *
     * @param receta Receta cuyas relaciones se eliminarán
     */
    void deleteByReceta(Receta receta);
}