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
     * Correo electrónico del usuario. Identificador de login: username ya
     * no sirve para esto porque solo es único por negocio (desde V2), no
     * globalmente, y el login no conoce el negocio de antemano.
     */
    private String email;

    /**
     * Contraseña del usuario.
     */
    private String password;
}