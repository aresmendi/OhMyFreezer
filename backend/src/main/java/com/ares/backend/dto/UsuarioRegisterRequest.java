package com.ares.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la solicitud de registro de un nuevo usuario.
 *
 * @author Ares
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioRegisterRequest {

    /**
     * Nombre de usuario único.
     */
    private String username;

    /**
     * Contraseña del usuario.
     */
    private String password;

    /**
     * Indica si el usuario es jefe de cocina.
     */
    private Boolean esJefeCocina;

    /**
     * Código de alta (signup code) del negocio, provisionado manualmente por
     * el equipo para cada Negocio. Requerido para todo registro público: el
     * código resuelve a exactamente un Negocio y es de un solo uso.
     */
    private String codigoRegistro;

    /**
     * Correo electrónico del usuario.
     * Solo requerido si esJefeCocina es true.
     */
    private String email;
}