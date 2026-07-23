package com.ares.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la creación de una cuenta de empleado (cocinero) por parte de un
 * jefe de cocina autenticado. A diferencia de {@link UsuarioRegisterRequest},
 * no lleva código de alta ni negocioId: el negocio del nuevo empleado se
 * hereda siempre del contexto de seguridad del jefe que hace la llamada
 * ({@code SecurityUtils.getNegocioId()}), nunca de este request.
 *
 * @author Ares
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmpleadoRegisterRequest {

    /**
     * Nombre de usuario del nuevo empleado, único dentro del negocio del jefe.
     */
    private String username;

    /**
     * Contraseña del nuevo empleado.
     */
    private String password;

    /**
     * Correo electrónico del nuevo empleado. Obligatorio: es el
     * identificador global de login (único en todo el sistema).
     */
    private String email;
}
