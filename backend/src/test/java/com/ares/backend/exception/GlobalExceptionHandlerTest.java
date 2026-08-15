package com.ares.backend.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios de {@link GlobalExceptionHandler}. Se invoca cada handler
 * directamente (sin contexto de Spring) para probar el mapeo status↔excepción
 * de forma aislada y rápida, siguiendo la convención de {@code
 * ConversionServiceTest} (MockitoExtension sin mocks necesarios).
 * <p>
 * Este test es especialmente importante para {@link CodigoGeneracionException}
 * y {@link ConflictoEstadoException}: ambas extienden {@code RuntimeException}
 * directamente, y sin un {@code @ExceptionHandler} propio caerían en {@link
 * GlobalExceptionHandler#handleNotFound(RuntimeException)}, que mapea a 404 —
 * un status incorrecto para "fallo al generar código" (500) y "conflicto de
 * estado" (409).
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("CodigoGeneracionException mapea a 500, no al 404 genérico de RuntimeException")
    void codigoGeneracionExceptionMapeaA500() {
        CodigoGeneracionException ex = new CodigoGeneracionException("no se pudo generar un código único");

        ResponseEntity<Map<String, Object>> response = handler.handleCodigoGeneracion(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("status", 500);
        assertThat(response.getBody().get("message")).isEqualTo("no se pudo generar un código único");
    }

    @Test
    @DisplayName("ConflictoEstadoException mapea a 409, no al 404 genérico de RuntimeException")
    void conflictoEstadoExceptionMapeaA409() {
        ConflictoEstadoException ex = new ConflictoEstadoException("el código ya fue usado");

        ResponseEntity<Map<String, Object>> response = handler.handleConflictoEstado(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("status", 409);
        assertThat(response.getBody().get("message")).isEqualTo("el código ya fue usado");
    }

    @Test
    @DisplayName("una RuntimeException genérica (no una de las dos excepciones dedicadas) sigue mapeando a 404")
    void runtimeExceptionGenericaSigueMapeandoA404() {
        RuntimeException ex = new RuntimeException("cualquier otra cosa");

        ResponseEntity<Map<String, Object>> response = handler.handleNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
