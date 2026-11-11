package com.ares.backend.dto;

import com.ares.backend.entity.TpvSkuMapping;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para un mapeo SKU de TPV.
 * Incluye los datos de la receta asignada cuando el mapeo está resuelto.
 *
 * @author Ares
 * @version 1.0
 */
@Data
@NoArgsConstructor
public class TpvSkuMappingResponse {

    /**
     * Identificador único del mapeo.
     */
    private Long id;

    /**
     * Código de producto tal como lo emite el TPV.
     */
    private String skuTpv;

    /**
     * Etiqueta del producto en el TPV (informativa).
     */
    private String nombreTpv;

    /**
     * ID de la receta asignada, o null si está pendiente de mapear.
     */
    private Long recetaId;

    /**
     * Nombre de la receta asignada, o null si está pendiente de mapear.
     */
    private String recetaNombre;

    /**
     * true si el mapeo tiene receta asignada; false si está pendiente.
     */
    private boolean mapeado;

    /**
     * Fecha y hora de creación del mapeo.
     */
    private LocalDateTime fechaCreacion;

    /**
     * Fecha y hora de la última actualización.
     */
    private LocalDateTime fechaActualizacion;

    /**
     * Constructor que convierte una entidad {@link TpvSkuMapping} a este DTO.
     *
     * @param mapping Entidad a convertir
     */
    public TpvSkuMappingResponse(TpvSkuMapping mapping) {
        this.id = mapping.getId();
        this.skuTpv = mapping.getSkuTpv();
        this.nombreTpv = mapping.getNombreTpv();
        this.recetaId = mapping.getReceta() != null ? mapping.getReceta().getId() : null;
        this.recetaNombre = mapping.getReceta() != null ? mapping.getReceta().getNombre() : null;
        this.mapeado = mapping.getReceta() != null;
        this.fechaCreacion = mapping.getFechaCreacion();
        this.fechaActualizacion = mapping.getFechaActualizacion();
    }
}
