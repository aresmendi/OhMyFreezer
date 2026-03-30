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
}