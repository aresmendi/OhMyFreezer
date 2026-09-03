package com.ares.backend.entity;

/**
 * Estado de resultado de un evento TPV ({@link VentaTpv#getEstado()}).
 * Cada rama de la ingesta responde 2xx sea cual sea el estado; el estado
 * es la forma en que el negocio se entera de qué pasó realmente.
 *
 * @author Ares
 * @version 1.0
 */
public enum EstadoVentaTpv {

    /**
     * Fila reservada por {@code reservarIdempotencia()} antes de cualquier
     * mutación de stock. Si el proceso se cae justo después de esta
     * escritura, la venta queda visible como "en duda", nunca aplicada dos
     * veces (D-A del diseño).
     */
    RECIBIDA,

    /**
     * Venta procesada: stock descontado exactamente como un
     * {@code elaborar()} manual, con su snapshot en {@link VentaTpvLinea}.
     */
    PROCESADA,

    /**
     * Venta con SKU mapeado pero receta sin ingredientes suficientes.
     * No se crea movimiento de stock; no bloquea la caja.
     */
    SIN_STOCK,

    /**
     * SKU sin mapear a ninguna receta del negocio. Genera una fila
     * pendiente en {@code tpv_sku_mapping} (receta_id null) y una alerta.
     */
    SIN_MAPEO,

    /**
     * Venta original que fue revertida por una anulación válida.
     */
    ANULADA,

    /**
     * Anulación aplicada con éxito: restauró el snapshot de la venta
     * original.
     */
    ANULACION_APLICADA,

    /**
     * Anulación recibida sin que exista una venta original con ese
     * externalIdOriginal. Se registra igualmente, con alerta.
     */
    ANULACION_HUERFANA,

    /**
     * Anulación de una venta que nunca tuvo efecto en stock (por ejemplo,
     * una venta original en SIN_STOCK). No genera movimiento de stock.
     */
    ANULADA_SIN_EFECTO,

    /**
     * Estado de RESPUESTA únicamente — nunca se persiste en una fila. El
     * unique constraint de base de datos ({@code uk_ventas_tpv_external} o
     * {@code uk_ventas_tpv_original}) es quien detecta el replay; este
     * valor solo viaja en {@code ResultadoIngesta} para informarlo al
     * llamador.
     */
    DUPLICADA
}
