package com.ares.backend.dto;

import com.ares.backend.entity.TipoUnidad;
import com.ares.backend.entity.UnidadMedida;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta para una unidad de medida del catálogo global.
 *
 * @author Ares
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnidadMedidaResponse {

    /**
     * Identificador único de la unidad de medida.
     */
    private Long id;

    /**
     * Código corto de la unidad (ej: "kg").
     */
    private String codigo;

    /**
     * Nombre descriptivo de la unidad (ej: "Kilogramo").
     */
    private String nombre;

    /**
     * Dimensión física de la unidad (MASA, VOLUMEN o UNIDAD).
     */
    private TipoUnidad tipo;

    /**
     * Factor de conversión respecto a la unidad base de su tipo.
     */
    private Double factorABase;

    /**
     * Constructor que convierte una entidad UnidadMedida a UnidadMedidaResponse.
     *
     * @param unidad Entidad UnidadMedida a convertir
     */
    public UnidadMedidaResponse(UnidadMedida unidad) {
        this.id = unidad.getId();
        this.codigo = unidad.getCodigo();
        this.nombre = unidad.getNombre();
        this.tipo = unidad.getTipo();
        this.factorABase = unidad.getFactorABase();
    }
}
