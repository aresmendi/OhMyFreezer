package com.ares.backend.tpv;

import com.ares.backend.exception.ProveedorDesconocidoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Registro de adaptadores TPV disponibles en el contexto de Spring. Se
 * construye automáticamente inyectando todas las implementaciones de
 * {@link TpvProveedorAdapter} registradas como beans.
 * <p>
 * Cuando el cliente omite la cabecera {@code X-Tpv-Proveedor} (o envía un
 * valor blank), se devuelve el adaptador {@code "generico"}. Un valor no
 * vacío que no coincida con ningún adaptador lanza
 * {@link ProveedorDesconocidoException} (400).
 *
 * @author Ares
 * @version 1.0
 */
@Slf4j
@Component
public class TpvProveedorRegistry {

    private static final String NOMBRE_GENERICO = "generico";

    private final Map<String, TpvProveedorAdapter> adaptadores;

    public TpvProveedorRegistry(List<TpvProveedorAdapter> adaptadores) {
        this.adaptadores = adaptadores.stream()
                .collect(Collectors.toMap(TpvProveedorAdapter::getNombre, Function.identity()));
        log.info("Adaptadores TPV registrados: {}", this.adaptadores.keySet());
    }

    /**
     * Resuelve el adaptador para el nombre de proveedor recibido.
     *
     * @param nombreProveedor valor de la cabecera {@code X-Tpv-Proveedor},
     *                        puede ser null o blank
     * @return adaptador correspondiente; nunca null
     * @throws ProveedorDesconocidoException si el nombre no es null/blank
     *                                       pero no existe ningún adaptador
     *                                       registrado con ese nombre
     */
    public TpvProveedorAdapter resolverAdaptador(@Nullable String nombreProveedor) {
        if (nombreProveedor == null || nombreProveedor.isBlank()) {
            return adaptadores.get(NOMBRE_GENERICO);
        }

        TpvProveedorAdapter adaptador = adaptadores.get(nombreProveedor);
        if (adaptador == null) {
            throw new ProveedorDesconocidoException(nombreProveedor);
        }
        return adaptador;
    }
}
