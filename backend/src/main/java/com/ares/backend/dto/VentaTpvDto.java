package com.ares.backend.dto;

import com.ares.backend.entity.TipoEventoTpv;

import java.time.LocalDateTime;

/**
 * DTO interno para transportar los datos de un evento TPV ya parseados
 * desde el payload crudo del proveedor. Solo contiene los campos de
 * negocio; los metadatos de auditoría (negocio, fechaRecepcion, estado)
 * los añade la capa de ingesta.
 *
 * @param tipo               VENTA o ANULACION.
 * @param externalId         Identidad de idempotencia de este evento.
 * @param externalIdOriginal externalId de la venta a anular (solo ANULACION, nullable).
 * @param skuTpv             Código de producto tal como llegó del TPV.
 * @param cantidad           Cantidad del evento.
 * @param fechaTpv           Fecha/hora reportada por el TPV (nullable, solo informativa).
 *
 * @author Ares
 * @version 1.0
 */
public record VentaTpvDto(
        TipoEventoTpv tipo,
        String externalId,
        String externalIdOriginal,
        String skuTpv,
        Integer cantidad,
        LocalDateTime fechaTpv
) {
}
