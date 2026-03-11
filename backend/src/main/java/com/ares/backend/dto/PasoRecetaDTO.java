package com.ares.backend.dto;

import com.ares.backend.entity.PasoReceta;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para representar un paso de una receta.
 *
 * @author Ares
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasoRecetaDTO {

    /**
     * Orden del paso en la secuencia.
     */
    private Integer orden;

    /**
     * Descripción del paso.
     */
    private String descripcion;

    /**
     * Constructor que convierte una entidad PasoReceta a PasoRecetaDTO.
     *
     * @param pasoReceta Entidad PasoReceta a convertir
     */
    public PasoRecetaDTO(PasoReceta pasoReceta) {
        this.orden = pasoReceta.getOrden();
        this.descripcion = pasoReceta.getDescripcion();
    }
}