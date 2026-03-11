package com.ares.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para representar un ingrediente faltante al verificar una receta.
 *
 * @author Ares
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngredienteFaltanteDTO {

    /**
     * Información del ingrediente.
     */
    private IngredienteResponse ingrediente;

    /**
     * Cantidad necesaria para la receta.
     */
    private Double cantidadNecesaria;

    /**
     * Cantidad disponible actualmente.
     */
    private Double cantidadDisponible;
}