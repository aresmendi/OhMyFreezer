package com.ares.backend.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Manejo global de excepciones para toda la API.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Maneja errores de argumentos inválidos.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {

        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.BAD_REQUEST.value());
        error.put("error", "Bad Request");
        error.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Maneja conversiones entre unidades de medida de distinto tipo (dimensión
     * física). Se registra ANTES que {@link #handleIllegalArgument} para que
     * Spring elija este handler más específico y el cliente pueda distinguir
     * esta causa de un 400 genérico por su campo "error".
     */
    @ExceptionHandler(UnidadesIncompatiblesException.class)
    public ResponseEntity<Map<String, Object>> handleUnidadesIncompatibles(UnidadesIncompatiblesException ex) {

        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.BAD_REQUEST.value());
        error.put("error", "Unidades incompatibles");
        error.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Maneja el agotamiento de reintentos al generar un código de alta
     * único. Se registra ANTES que {@link #handleNotFound} porque, al
     * extender {@code RuntimeException} directamente (no {@code
     * IllegalArgumentException}), sin este handler específico caería en el
     * genérico y se reportaría como 404 en lugar del 500 real que
     * representa un fallo del generador, no una petición inválida ni un
     * recurso inexistente.
     */
    @ExceptionHandler(CodigoGeneracionException.class)
    public ResponseEntity<Map<String, Object>> handleCodigoGeneracion(CodigoGeneracionException ex) {

        log.error("Fallo al generar código de alta único: {}", ex.getMessage());

        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        error.put("error", "Internal Server Error");
        error.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Maneja conflictos de estado en operaciones de administración (p. ej.
     * revocar un código de alta ya usado). Se registra ANTES que {@link
     * #handleNotFound} por la misma razón que {@link #handleCodigoGeneracion}:
     * sin handler propio, una {@code RuntimeException} directa cae en el
     * genérico y se reportaría como 404 en vez del 409 real.
     */
    @ExceptionHandler(ConflictoEstadoException.class)
    public ResponseEntity<Map<String, Object>> handleConflictoEstado(ConflictoEstadoException ex) {

        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.CONFLICT.value());
        error.put("error", "Conflict");
        error.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Maneja recursos no encontrados (incluye cross-tenant: un id de otro
     * negocio se trata igual que un id inexistente, nunca como 403).
     */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleRecursoNoEncontrado(RecursoNoEncontradoException ex) {

        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.NOT_FOUND.value());
        error.put("error", "Not Found");
        error.put("message", "Recurso no encontrado");

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Maneja recursos no encontrados.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(RuntimeException ex) {

        log.warn("RuntimeException no controlada: {}", ex.getMessage());

        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.NOT_FOUND.value());
        error.put("error", "Not Found");
        error.put("message", "Recurso no encontrado");

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Maneja errores inesperados del servidor.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {

        log.error("Error inesperado", ex);

        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        error.put("error", "Internal Server Error");
        error.put("message", "Ha ocurrido un error inesperado");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

}