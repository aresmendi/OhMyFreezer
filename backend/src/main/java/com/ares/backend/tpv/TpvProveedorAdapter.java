package com.ares.backend.tpv;

import com.ares.backend.dto.VentaTpvDto;

/**
 * Contrato que debe implementar cada adaptador de proveedor TPV. Un
 * adaptador sabe cómo identificarse ({@link #getNombre()}) y cómo
 * transformar el payload crudo HTTP en un {@link VentaTpvDto} ya
 * validado y tipado.
 * <p>
 * La implementación {@link GenericoTpvAdapter} sirve tanto como proveedor
 * por defecto (cuando no llega la cabecera {@code X-Tpv-Proveedor}) como
 * referencia de implementación para proveedores futuros.
 *
 * @author Ares
 * @version 1.0
 */
public interface TpvProveedorAdapter {

    /**
     * Nombre del proveedor en minúsculas (p. ej. {@code "generico"},
     * {@code "square"}, {@code "sumup"}). Es el valor que el cliente
     * envía en la cabecera {@code X-Tpv-Proveedor}.
     *
     * @return nombre del proveedor, nunca null ni blank
     */
    String getNombre();

    /**
     * Parsea el payload crudo del TPV y devuelve un DTO interno ya
     * validado. No toca base de datos ni invoca servicios de negocio.
     *
     * @param rawPayload cuerpo HTTP recibido del TPV, nunca null
     * @return DTO con los campos del evento
     * @throws IllegalArgumentException si el payload está malformado o
     *                                  faltan campos obligatorios
     */
    VentaTpvDto parsear(String rawPayload);
}
