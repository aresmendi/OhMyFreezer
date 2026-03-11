package com.ares.backend.dto;

import com.ares.backend.entity.RecetaIngrediente;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para representar un ingrediente necesario en una receta.
 *
 * @author Ares
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecetaIngredienteDTO {

    /**
     * ID del ingrediente.
     */
    private Long ingredienteId;

    /**
     * Nombre del ingrediente.
     */
    private String ingredienteNombre;

    /**
     * Cantidad necesaria del ingrediente.
     */
    private Double cantidadNecesaria;

    /**
     * Constructor que convierte una entidad RecetaIngrediente a RecetaIngredienteDTO.
     *
     * @param recetaIngrediente Entidad RecetaIngrediente a convertir
     */
    public RecetaIngredienteDTO(RecetaIngrediente recetaIngrediente) {
        this.ingredienteId = recetaIngrediente.getIngrediente().getId();
        this.ingredienteNombre = recetaIngrediente.getIngrediente().getNombre();
        this.cantidadNecesaria = recetaIngrediente.getCantidadNecesaria();
    }
}