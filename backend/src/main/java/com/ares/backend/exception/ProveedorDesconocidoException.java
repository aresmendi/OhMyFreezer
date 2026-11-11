package com.ares.backend.exception;

/**
 * Excepción lanzada cuando el valor de la cabecera {@code X-Tpv-Proveedor}
 * no corresponde a ningún {@link com.ares.backend.tpv.TpvProveedorAdapter}
 * registrado. Es un error de petición (400): el cliente envió un nombre de
 * proveedor que no existe, no un fallo de servidor ni un recurso inexistente.
 *
 * @author Ares
 * @version 1.0
 */
public class ProveedorDesconocidoException extends RuntimeException {

    public ProveedorDesconocidoException(String nombreProveedor) {
        super("Proveedor TPV desconocido: '" + nombreProveedor + "'");
    }
}
