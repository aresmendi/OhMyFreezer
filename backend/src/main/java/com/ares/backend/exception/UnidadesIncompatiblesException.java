package com.ares.backend.exception;

/**
 * Excepción lanzada cuando se intenta convertir una cantidad entre dos
 * unidades de medida de distinta dimensión física (tipo), por ejemplo
 * MASA contra VOLUMEN. No existe conversión válida entre tipos distintos:
 * {@link com.ares.backend.service.ConversionService} nunca devuelve un
 * resultado forzado (1:1 o por defecto), siempre lanza esta excepción.
 * Mapea a HTTP 400 en {@link GlobalExceptionHandler}.
 *
 * @author Ares
 * @version 1.0
 */
public class UnidadesIncompatiblesException extends IllegalArgumentException {

    public UnidadesIncompatiblesException(String codigoDesde, String codigoHacia) {
        super("No se puede convertir de '" + codigoDesde + "' a '" + codigoHacia
                + "': las unidades pertenecen a dimensiones físicas distintas");
    }
}
