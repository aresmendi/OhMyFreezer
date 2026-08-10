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

    /**
     * Unidad de medida en la que se expresa {@link #cantidadNecesaria}
     * (catálogo global {@code unidades_medida}). Nullable: si no viene
     * informada, la unidad por defecto es la propia del ingrediente
     * ({@code unidadBaseId}). Añadido en la Fase 2 ("unidades-medida", PR2)
     * — el wiring de conversión real en {@code RecetaService} es alcance de
     * una fase posterior (PR3); este campo solo viaja en el DTO por ahora.
     */
    private Long unidadId;
}