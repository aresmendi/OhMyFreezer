package com.ares.backend.dto;

import com.ares.backend.entity.TpvApiKey;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para una credencial TPV, expuesto SOLO en la API del
 * superadmin de plataforma ({@code /api/admin/**}). Nunca incluye el
 * secreto ni su hash — solo {@link #prefijo}, que ya es público (D-D del
 * diseño de "tpv-integration").
 *
 * @author Ares
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TpvApiKeyResponse {

    /**
     * Identificador único de la credencial.
     */
    private Long id;

    /**
     * Id del Negocio (tenant) al que pertenece esta credencial.
     */
    private Long negocioId;

    /**
     * Prefijo público de la credencial, único globalmente.
     */
    private String prefijo;

    /**
     * Indica si la credencial sigue activa.
     */
    private Boolean activa;

    /**
     * Fecha y hora de emisión de la credencial.
     */
    private LocalDateTime fechaCreacion;

    /**
     * Fecha y hora de revocación, si la credencial ya no está activa.
     */
    private LocalDateTime fechaRevocacion;

    /**
     * Constructor que convierte una entidad TpvApiKey a TpvApiKeyResponse.
     *
     * @param clave Entidad TpvApiKey a convertir
     */
    public TpvApiKeyResponse(TpvApiKey clave) {
        this.id = clave.getId();
        this.negocioId = clave.getNegocio().getId();
        this.prefijo = clave.getPrefijo();
        this.activa = clave.getActiva();
        this.fechaCreacion = clave.getFechaCreacion();
        this.fechaRevocacion = clave.getFechaRevocacion();
    }
}
