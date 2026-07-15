package com.ares.backend.dto;

import com.ares.backend.entity.Usuario;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para la respuesta con información de un usuario.
 * No incluye la contraseña por seguridad.
 *
 * @author Ares
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponse {

    /**
     * Identificador único del usuario.
     */
    private Long id;

    /**
     * Nombre de usuario.
     */
    private String username;

    /**
     * Indica si el usuario es jefe de cocina.
     */
    private Boolean esJefeCocina;

    /**
     * Fecha de registro del usuario.
     */
    private LocalDateTime fechaRegistro;

    /**
     * Correo electrónico del usuario.
     */
    private String email;

    /**
     * Negocio (tenant) al que pertenece el usuario.
     */
    private Long negocioId;

    /**
     * Constructor que convierte una entidad Usuario a UsuarioResponse.
     *
     * @param usuario Entidad Usuario a convertir
     */
    public UsuarioResponse(Usuario usuario) {
        this.id = usuario.getId();
        this.username = usuario.getUsername();
        this.esJefeCocina = usuario.getEsJefeCocina();
        this.fechaRegistro = usuario.getFechaRegistro();
        this.email = usuario.getEmail();
        this.negocioId = usuario.getNegocio() != null ? usuario.getNegocio().getId() : null;
    }
}