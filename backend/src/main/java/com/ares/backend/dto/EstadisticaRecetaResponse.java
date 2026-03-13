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

    /**
     * Total de veces que se ha elaborado
     */
    private Integer totalElaboraciones;

    /**
     * Total de veces que se ha completado la elaboración
     */
    private Integer elaboracionesCompletadas;

    /**
     * Tasa de acierto
     */
    private Double tasaCompletado;

    /**
     * Última elaboración
     */
    private String ultimaElaboracion;

    public EstadisticaRecetaResponse(Long recetaId, String recetaNombre, List<DatoEstadisticaDTO>datos, Integer totalElaboraciones, Integer elaboracionesCompletadas) {
        this.recetaId = recetaId;
        this.recetaNombre = recetaNombre;
        this.datos = datos;
        this.totalElaboraciones = totalElaboraciones;
        this.elaboracionesCompletadas = elaboracionesCompletadas;
        this.tasaCompletado = totalElaboraciones == 0 ? 0.0 : (double) elaboracionesCompletadas / totalElaboraciones;
        this.ultimaElaboracion = datos.isEmpty() ? "" : datos.get(datos.size() -1).getFecha().toString();

    }
}