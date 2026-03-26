package com.ares.backend.dto;

import com.ares.backend.entity.Receta;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO para la respuesta detallada de una receta (con pasos e ingredientes).
 * Usado al consultar una receta específica.
 *
 * @author Ares
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecetaDetailResponse {

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
     * Indica si la receta está disponible.
     */
    private Boolean disponible;

    /**
     * Lista de pasos de la receta.
     */
    private List<PasoRecetaDTO> pasos;

    /**
     * Lista de ingredientes necesarios.
     */
    private List<RecetaIngredienteDTO> ingredientes;

    /**
     * Fecha de creación de la receta.
     */
    private LocalDateTime fechaCreacion;

    /**
     * Usuario que creó la receta.
     */
    private UsuarioResponse creadaPor;

    /**
     * Indica si la receta está marcada como favorita por el usuario actual.
     * null si no se ha verificado.
     */
    private Boolean esFavorita;

    /**
     * Constructor que convierte una entidad Receta a RecetaDetailResponse.
     *
     * @param receta Entidad Receta a convertir
     * @param disponible Indica si la receta está disponible
     */
    public RecetaDetailResponse(Receta receta, Boolean disponible) {
        this.id = receta.getId();
        this.nombre = receta.getNombre();
        this.descripcion = receta.getDescripcion();
        this.disponible = disponible;
        this.pasos = receta.getPasos().stream()
                .map(PasoRecetaDTO::new)
                .collect(Collectors.toList());
        this.ingredientes = receta.getIngredientes().stream()
                .map(RecetaIngredienteDTO::new)
                .collect(Collectors.toList());
        this.fechaCreacion = receta.getFechaCreacion();
        this.creadaPor = new UsuarioResponse(receta.getCreadaPor());
    }
}