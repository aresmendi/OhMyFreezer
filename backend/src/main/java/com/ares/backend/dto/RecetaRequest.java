package com.ares.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para la solicitud de creación o actualización de una receta.
 *
 * @author Ares
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecetaRequest {

    /**
     * Nombre de la receta.
     */
    private String nombre;

    /**
     * Descripción de la receta.
     */
    private String descripcion;

    /**
     * ID del usuario que crea la receta (jefe de cocina).
     */
    private Long creadaPorId;

    /**
     * Lista de pasos de la receta.
     */
    private List<PasoRecetaDTO> pasos;

    /**
     * Lista de ingredientes necesarios.
     */
    private List<RecetaIngredienteRequest> ingredientes;
}