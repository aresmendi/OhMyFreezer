package com.ares.backend.entity;

/**
 * Dimensión física de una unidad de medida.
 * Dos unidades solo son convertibles entre sí si comparten el mismo tipo
 * (ver {@link com.ares.backend.service.ConversionService}).
 *
 * @author Ares
 * @version 1.0
 */
public enum TipoUnidad {

    /**
     * Unidades de masa (base: gramo).
     */
    MASA,

    /**
     * Unidades de volumen (base: mililitro).
     */
    VOLUMEN,

    /**
     * Unidades de conteo/unidad suelta (base: unidad).
     */
    UNIDAD
}
