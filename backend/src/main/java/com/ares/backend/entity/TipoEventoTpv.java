package com.ares.backend.entity;

/**
 * Tipo de evento que un TPV puede enviar a {@code POST /api/tpv/ventas}.
 * Ambos tipos comparten el mismo endpoint y se distinguen por este campo,
 * cada uno con su propia identidad de idempotencia (ver {@link VentaTpv}).
 *
 * @author Ares
 * @version 1.0
 */
public enum TipoEventoTpv {

    /**
     * Venta normal: descuenta stock vía {@code RecetaService.elaborar()}.
     */
    VENTA,

    /**
     * Anulación de una venta previa, identificada por
     * {@link VentaTpv#getExternalIdOriginal()}. Restaura exactamente el
     * snapshot de {@link VentaTpvLinea} registrado en la venta original,
     * nunca una recomputación desde la receta.
     */
    ANULACION
}
