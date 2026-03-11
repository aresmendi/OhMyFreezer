package com.ares.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para la respuesta de estadísticas de una receta.
 *
 * @author Ares
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstadisticaRecetaResponse {

    /**
     * ID de la receta.
     */
    private Long recetaId;

    /**
     * Nombre de la receta.
     */
    private String recetaNombre;

    /**
     * Lista de datos estadísticos (fecha y usos).
     */
    private List<DatoEstadisticaDTO> datos;
}