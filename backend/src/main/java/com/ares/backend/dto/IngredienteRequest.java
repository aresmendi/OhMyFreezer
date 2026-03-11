package com.ares.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la solicitud de creación o actualización de un ingrediente.
 *
 * @author Ares
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngredienteRequest {

    /**
     * Nombre del ingrediente.
     */
    private String nombre;

    /**
     * Cantidad disponible del ingrediente.
     */
    private Double cantidad;

    /**
     * Unidad de medida (gramos, litros, unidades, etc.).
     */
    private String unidadMedida;

    /**
     * Stock mínimo para generar alertas.
     */
    private Double stockMinimo;
}