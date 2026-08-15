package com.ares.backend.dto;

import com.ares.backend.entity.NegocioSignupCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta para {@code POST /api/admin/negocios}: envuelve el
 * Negocio recién creado y su primer código de alta en una única respuesta,
 * reflejando que ambas filas se crean en una sola operación atómica (ver
 * {@code NegocioAdminService.crearNegocio}).
 *
 * @author Ares
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NegocioCreadoResponse {

    /**
     * Negocio recién creado.
     */
    private NegocioAdminResponse negocio;

    /**
     * Primer código de alta del negocio recién creado.
     */
    private SignupCodeResponse signupCode;

    /**
     * Constructor que convierte el NegocioSignupCode devuelto por
     * {@code NegocioAdminService.crearNegocio()} en la respuesta compuesta:
     * su {@code getNegocio()} expone el Negocio recién creado (referencia en
     * memoria, no un proxy lazy).
     *
     * @param primerCodigo Primer código de alta recién creado, con su Negocio asociado
     */
    public NegocioCreadoResponse(NegocioSignupCode primerCodigo) {
        this.negocio = new NegocioAdminResponse(primerCodigo.getNegocio());
        this.signupCode = new SignupCodeResponse(primerCodigo);
    }
}
