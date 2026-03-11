package com.ares.backend.dto;

import com.ares.backend.entity.RegistroUsoReceta;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para la respuesta de un registro de uso de receta.
 *
 * @author Ares
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistroUsoResponse {

    /**
     * Identificador único del registro.
     */
    private Long id;

    /**
     * Información simplificada de la receta.
     */
    private RecetaInfoEnRegistro receta;

    /**
     * Información del usuario que elaboró la receta.
     */
    private UsuarioResponse usuario;

    /**
     * Fecha y hora de elaboración.
     */
    private LocalDateTime fechaElaboracion;

    /**
     * Indica si se completó exitosamente.
     */
    private Boolean completada;

    /**
     * Constructor que convierte una entidad RegistroUsoReceta a RegistroUsoResponse.
     *
     * @param registro Entidad RegistroUsoReceta a convertir
     */
    public RegistroUsoResponse(RegistroUsoReceta registro) {
        this.id = registro.getId();
        this.receta = new RecetaInfoEnRegistro(registro.getReceta().getId(), registro.getReceta().getNombre());
        this.usuario = new UsuarioResponse(registro.getUsuario());
        this.fechaElaboracion = registro.getFechaElaboracion();
        this.completada = registro.getCompletada();
    }

    /**
     * Clase interna para información simplificada de receta.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecetaInfoEnRegistro {
        private Long id;
        private String nombre;
    }
}