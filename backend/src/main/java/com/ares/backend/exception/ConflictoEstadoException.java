package com.ares.backend.exception;

/**
 * Excepción lanzada cuando una operación de administración no puede
 * completarse porque el estado actual del recurso lo impide, sin que eso
 * signifique que el recurso no exista (eso sería {@link
 * RecursoNoEncontradoException}, 404) ni que la petición en sí sea inválida
 * (eso sería {@code IllegalArgumentException}, 400).
 * <p>
 * Caso de uso actual: intentar revocar un código de alta que ya fue
 * consumido ({@code usado=true}). Poner {@code activo=false} sobre un
 * código ya usado no cambia nada relevante para la seguridad — no
 * deshabilita la cuenta que ese código ya creó — así que devolver 200 daría
 * al operador una falsa sensación de haber "cortado el acceso". El 409
 * fuerza a que se dé cuenta de que la acción real (deshabilitar el Usuario)
 * queda fuera del alcance de esta operación.
 * <p>
 * Mapea a HTTP 409 en {@link GlobalExceptionHandler}.
 *
 * @author Ares
 * @version 1.0
 */
public class ConflictoEstadoException extends RuntimeException {

    public ConflictoEstadoException(String message) {
        super(message);
    }
}
