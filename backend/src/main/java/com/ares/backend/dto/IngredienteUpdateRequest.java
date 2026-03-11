package com.ares.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la solicitud de actualización de cantidad de un ingrediente.
 *
 * @author Ares
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngredienteUpdateRequest {

    /**
     * Nueva cantidad del ingrediente.
     */
    private Double cantidad;
}