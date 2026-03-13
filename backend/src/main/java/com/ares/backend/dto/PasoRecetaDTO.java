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
     * ID del paso.
     */
    private Long id;

    /**
     * Orden del paso en la secuencia.
     */
    private Integer orden;

    /**
     * Descripción del paso.
     */
    private String descripcion;

    /**
     * ID de la receta a la que pertenece el paso.
     */
    private Long recetaId;

    /**
     * Constructor que convierte una entidad PasoReceta a PasoRecetaDTO.
     *
     * @param pasoReceta Entidad PasoReceta a convertir
     */
    public PasoRecetaDTO(PasoReceta pasoReceta) {
        this.id = pasoReceta.getId();
        this.orden = pasoReceta.getOrden();
        this.descripcion = pasoReceta.getDescripcion();
        this.recetaId = pasoReceta.getReceta().getId();
    }
}