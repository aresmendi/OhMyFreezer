package com.ares.backend.dto;

import com.ares.backend.service.TpvApiKeyEmitida;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para la emisión (o re-emisión) de una credencial TPV,
 * expuesto SOLO en la API del superadmin de plataforma ({@code
 * /api/admin/**}). Es el ÚNICO punto donde {@link #claveEnClaro} viaja en
 * texto plano: a partir de esta respuesta, la credencial solo se puede
 * consultar de nuevo por {@link #prefijo} (nunca el secreto).
 *
 * @author Ares
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TpvApiKeyEmitidaResponse {

    /**
     * Identificador único de la credencial recién emitida.
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
     * Key completa en claro, formato {@code omf_tpv_<prefijo>_<secreto>}.
     * Recuperable SOLO en esta respuesta; el backend nunca vuelve a
     * exponerla.
     */
    private String claveEnClaro;

    /**
     * Fecha y hora de emisión de la credencial.
     */
    private LocalDateTime fechaCreacion;

    /**
     * Constructor que convierte un {@link TpvApiKeyEmitida} a
     * TpvApiKeyEmitidaResponse.
     *
     * @param emitida Resultado de {@code TpvApiKeyAdminService.emitir()}
     */
    public TpvApiKeyEmitidaResponse(TpvApiKeyEmitida emitida) {
        this.id = emitida.clave().getId();
        this.negocioId = emitida.clave().getNegocio().getId();
        this.prefijo = emitida.clave().getPrefijo();
        this.claveEnClaro = emitida.claveEnClaro();
        this.fechaCreacion = emitida.clave().getFechaCreacion();
    }
}
