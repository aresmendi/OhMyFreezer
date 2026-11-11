package com.ares.backend.dto;

import com.ares.backend.entity.EstadoVentaTpv;

/**
 * Respuesta del endpoint {@code POST /api/tpv/ventas}. El endpoint siempre
 * responde 200; el estado de negocio (PROCESADA, SIN_MAPEO, DUPLICADA…)
 * viaja aquí en el body.
 *
 * @param ventaTpvId ID de la fila {@code ventas_tpv} creada, o null si la
 *                   venta fue detectada como DUPLICADA a nivel de constraint
 *                   (en ese caso no hay fila nueva).
 * @param externalId Identidad de idempotencia del evento tal como llegó del TPV.
 * @param estado     Resultado del procesamiento.
 * @param detalle    Texto libre opcional con información adicional (p. ej. motivo
 *                   de SIN_STOCK o SIN_MAPEO). Null si no aplica.
 *
 * @author Ares
 * @version 1.0
 */
public record ResultadoIngestaResponse(
        Long ventaTpvId,
        String externalId,
        EstadoVentaTpv estado,
        String detalle
) {}
