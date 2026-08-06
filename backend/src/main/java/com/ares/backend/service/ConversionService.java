package com.ares.backend.service;

import com.ares.backend.entity.UnidadMedida;
import com.ares.backend.exception.UnidadesIncompatiblesException;
import org.springframework.stereotype.Service;

/**
 * Servicio de conversión entre unidades de medida.
 * Convierte una cantidad expresada en una unidad a su equivalente en otra
 * unidad de la MISMA dimensión física (tipo), usando el factor de cada
 * unidad respecto a la unidad base de su tipo. No depende de ningún
 * repositorio: es una función pura sobre las entidades recibidas.
 *
 * @author Ares
 * @version 1.0
 */
@Service
public class ConversionService {

    /**
     * Convierte {@code valor} desde la unidad {@code desde} a la unidad {@code hacia}.
     * Nunca devuelve un resultado por defecto ni forzado: ante cualquier dato
     * inválido o incompatible, lanza una excepción.
     *
     * @param valor  cantidad a convertir, expresada en la unidad {@code desde}
     * @param desde  unidad de origen
     * @param hacia  unidad de destino
     * @return el valor convertido a la unidad {@code hacia}
     * @throws IllegalArgumentException si {@code valor}, {@code desde} o {@code hacia} son nulos
     * @throws UnidadesIncompatiblesException si {@code desde} y {@code hacia} tienen distinto tipo
     */
    public Double convertir(Double valor, UnidadMedida desde, UnidadMedida hacia) {
        if (valor == null || desde == null || hacia == null) {
            throw new IllegalArgumentException("Conversión con valor o unidad nula");
        }
        if (desde.getId() != null && desde.getId().equals(hacia.getId())) {
            return valor;
        }
        if (desde.getTipo() == null || hacia.getTipo() == null
                || desde.getFactorABase() == null || hacia.getFactorABase() == null) {
            throw new IllegalArgumentException("Conversión con unidad incompleta (tipo o factorABase nulo)");
        }
        if (desde.getTipo() != hacia.getTipo()) {
            throw new UnidadesIncompatiblesException(desde.getCodigo(), hacia.getCodigo());
        }
        return valor * desde.getFactorABase() / hacia.getFactorABase();
    }
}
