package com.ares.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la solicitud de login de un usuario.
 *
 * @author Ares
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioLoginRequest {

    /**
     * Nombre de usuario.
     */
    private String username;

    /**
     * Contraseña del usuario.
     */
    private String password;
}