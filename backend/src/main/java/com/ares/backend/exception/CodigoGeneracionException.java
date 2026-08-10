package com.ares.backend.exception;

/**
 * Excepción lanzada cuando {@code NegocioAdminService} agota los intentos
 * permitidos para generar un código de alta que no colisione con uno ya
 * existente en {@code negocio_signup_codes}. Con un alfabeto de 32 símbolos
 * y longitud 10 (≈2^50 combinaciones), esto es estadísticamente
 * inalcanzable en operación normal; su ocurrencia real indicaría un fallo
 * del generador de números aleatorios, no una condición de negocio
 * esperable. Mapea a HTTP 500 en {@link GlobalExceptionHandler}.
 *
 * @author Ares
 * @version 1.0
 */
public class CodigoGeneracionException extends RuntimeException {

    public CodigoGeneracionException(String message) {
        super(message);
    }
}
