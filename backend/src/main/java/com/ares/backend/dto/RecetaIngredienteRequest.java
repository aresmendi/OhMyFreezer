package com.ares.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la solicitud de ingrediente en una receta (usado en creación/edición).
 *
 * @author Ares
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecetaIngredienteRequest {

    /**
     * ID del ingrediente.
     */
    private Long ingredienteId;

    /**
     * Cantidad necesaria del ingrediente.
     */
    private Double cantidadNecesaria;
}