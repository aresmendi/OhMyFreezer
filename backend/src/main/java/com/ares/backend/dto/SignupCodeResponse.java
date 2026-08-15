package com.ares.backend.dto;

import com.ares.backend.entity.NegocioSignupCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para un código de alta, expuesto SOLO en la API del
 * superadmin de plataforma ({@code /api/admin/**}). Expone el
 * {@code negocioId} (no el objeto Negocio completo) y deliberadamente el
 * {@code codigo} en texto plano: la entrega al operador es manual
 * (WhatsApp/email), sin notificación automática (confirmed assumption #3).
 *
 * @author Ares
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignupCodeResponse {

    /**
     * Identificador único del código de alta.
     */
    private Long id;

    /**
     * Código de alta en texto plano.
     */
    private String codigo;

    /**
     * Id del Negocio al que resuelve este código de alta.
     */
    private Long negocioId;

    /**
     * Indica si el código ya fue consumido (single-use).
     */
    private Boolean usado;

    /**
     * Indica si el código sigue activo (no revocado por un admin).
     */
    private Boolean activo;

    /**
     * Id del usuario (jefe) que consumió el código, si ya se usó.
     */
    private Long usadoPorUsuarioId;

    /**
     * Fecha y hora en que se creó (provisionó) el código.
     */
    private LocalDateTime fechaCreacion;

    /**
     * Fecha y hora en que se consumió el código, si ya se usó.
     */
    private LocalDateTime fechaUso;

    /**
     * Constructor que convierte una entidad NegocioSignupCode a
     * SignupCodeResponse.
     *
     * @param signupCode Entidad NegocioSignupCode a convertir
     */
    public SignupCodeResponse(NegocioSignupCode signupCode) {
        this.id = signupCode.getId();
        this.codigo = signupCode.getCodigo();
        this.negocioId = signupCode.getNegocio().getId();
        this.usado = signupCode.getUsado();
        this.activo = signupCode.getActivo();
        this.usadoPorUsuarioId = signupCode.getUsadoPorUsuarioId();
        this.fechaCreacion = signupCode.getFechaCreacion();
        this.fechaUso = signupCode.getFechaUso();
    }
}
