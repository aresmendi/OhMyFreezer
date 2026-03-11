package com.ares.backend.dto;

import com.ares.backend.entity.Alerta;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para la respuesta de una alerta.
 *
 * @author Ares
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertaResponse {

    /**
     * Identificador único de la alerta.
     */
    private Long id;

    /**
     * Tipo de alerta (STOCK_BAJO, RECETA_NO_DISPONIBLE).
     */
    private String tipo;

    /**
     * Mensaje descriptivo de la alerta.
     */
    private String mensaje;

    /**
     * Ingrediente relacionado (puede ser null).
     */
    private IngredienteSimpleInfo ingrediente;

    /**
     * Receta relacionada (puede ser null).
     */
    private RecetaInfoEnAlerta receta;

    /**
     * Fecha de creación de la alerta.
     */
    private LocalDateTime fechaCreacion;

    /**
     * Indica si la alerta ha sido leída.
     */
    private Boolean leida;

    /**
     * Constructor que convierte una entidad Alerta a AlertaResponse.
     *
     * @param alerta Entidad Alerta a convertir
     */
    public AlertaResponse(Alerta alerta) {
        this.id = alerta.getId();
        this.tipo = alerta.getTipo();
        this.mensaje = alerta.getMensaje();
        this.ingrediente = alerta.getIngrediente() != null ?
                new IngredienteSimpleInfo(alerta.getIngrediente().getId(), alerta.getIngrediente().getNombre()) : null;
        this.receta = alerta.getReceta() != null ?
                new RecetaInfoEnAlerta(alerta.getReceta().getId(), alerta.getReceta().getNombre()) : null;
        this.fechaCreacion = alerta.getFechaCreacion();
        this.leida = alerta.getLeida();
    }

    /**
     * Clase interna para información simplificada de ingrediente.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IngredienteSimpleInfo {
        private Long id;
        private String nombre;
    }

    /**
     * Clase interna para información simplificada de receta.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecetaInfoEnAlerta {
        private Long id;
        private String nombre;
    }
}