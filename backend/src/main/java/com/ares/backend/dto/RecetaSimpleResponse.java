package com.ares.backend.dto;

import com.ares.backend.entity.Receta;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para la respuesta simplificada de una receta (sin pasos ni ingredientes).
 * Usado en listados de recetas.
 *
 * @author Ares
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecetaSimpleResponse {

    /**
     * Identificador único de la receta.
     */
    private Long id;

    /**
     * Nombre de la receta.
     */
    private String nombre;

    /**
     * Descripción de la receta.
     */
    private String descripcion;

    /**
     * Indica si la receta está disponible (hay stock suficiente).
     */
    private Boolean disponible;

    /**
     * Fecha de creación de la receta.
     */
    private LocalDateTime fechaCreacion;

    /**
     * Usuario que creó la receta.
     */
    private UsuarioResponse creadaPor;

    /**
     * Constructor que convierte una entidad Receta a RecetaSimpleResponse.
     *
     * @param receta Entidad Receta a convertir
     * @param disponible Indica si la receta está disponible
     */
    public RecetaSimpleResponse(Receta receta, Boolean disponible) {
        this.id = receta.getId();
        this.nombre = receta.getNombre();
        this.descripcion = receta.getDescripcion();
        this.disponible = disponible;
        this.fechaCreacion = receta.getFechaCreacion();
        this.creadaPor = new UsuarioResponse(receta.getCreadaPor());
    }
}