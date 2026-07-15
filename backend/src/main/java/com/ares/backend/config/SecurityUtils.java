package com.ares.backend.config;

import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    public static Long getUsuarioId() {
        CustomUserDetails user =
                (CustomUserDetails) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        return user.getId();
    }

    /**
     * Devuelve el negocioId (tenant) del usuario autenticado en el contexto
     * de seguridad actual. Nunca se acepta un negocioId propuesto por el
     * cliente: siempre se deriva del principal ya autenticado por JwtFilter.
     *
     * @throws NullPointerException si no hay autenticación en el contexto
     * @throws ClassCastException si el principal no es un CustomUserDetails
     */
    public static Long getNegocioId() {
        CustomUserDetails user =
                (CustomUserDetails) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        return user.getNegocioId();
    }
}