package com.ares.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * DTO para la respuesta del token necesario para JWT
 *
 * @author Ares
 * @version 1.0
 */
@Getter
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private Long id;
    private String username;
    private Boolean esJefeCocina;
}