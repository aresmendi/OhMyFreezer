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
     * ID de la relación receta-ingrediente.
     */
    private Long id;

    /**
     * ID de la receta.
     */
    private Long recetaId;

    /**
     * Ingrediente necesario.
     */
    private IngredienteResponse ingrediente;

    /**
     * Cantidad necesaria del ingrediente.
     */
    private Double cantidadNecesaria;

    /**
     * Identificador de la unidad de medida en la que se expresa
     * {@link #cantidadNecesaria} (catálogo global {@code unidades_medida}).
     * {@code null} si el paso de receta todavía no tiene unidad tipada
     * asignada.
     */
    private Long unidadId;

    /**
     * Unidad de medida tipada del catálogo global. {@code null} si el paso
     * de receta todavía no tiene unidad tipada asignada.
     */
    private UnidadMedidaResponse unidad;

    /**
     * Constructor que convierte una entidad RecetaIngrediente a RecetaIngredienteDTO.
     *
     * @param recetaIngrediente Entidad RecetaIngrediente a convertir
     */
    public RecetaIngredienteDTO(RecetaIngrediente recetaIngrediente) {
        this.id = recetaIngrediente.getId();
        this.recetaId = recetaIngrediente.getReceta().getId();
        this.ingrediente = new IngredienteResponse(recetaIngrediente.getIngrediente());
        this.cantidadNecesaria = recetaIngrediente.getCantidadNecesaria();
        if (recetaIngrediente.getUnidad() != null) {
            this.unidadId = recetaIngrediente.getUnidad().getId();
            this.unidad = new UnidadMedidaResponse(recetaIngrediente.getUnidad());
        }
    }
}