package com.ares.backend.exception;

/**
 * Excepción lanzada cuando un recurso identificado por id no existe o no
 * pertenece al negocio (tenant) del caller autenticado.
 *
 * Se usa como resultado de los finders scoped por negocio
 * (`repo.findByIdAndNegocioId(...)`): un id de otro negocio es, a todos los
 * efectos, indistinguible de un id inexistente. Mapea a HTTP 404 en
 * {@link GlobalExceptionHandler}, nunca a 403, para no revelar que el id
 * existe bajo otro tenant.
 *
 * @author Ares
 * @version 1.0
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String message) {
        super(message);
    }
}
