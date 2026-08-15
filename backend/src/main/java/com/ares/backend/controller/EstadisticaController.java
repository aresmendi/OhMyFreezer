package com.ares.backend.controller;
import com.ares.backend.dto.EstadisticaRecetaResponse;
import com.ares.backend.service.EstadisticaService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Controlador REST para la gestión de estadísticas.
 * Maneja las consultas de estadísticas de uso de recetas.
 *
 * @author Ares
 * @version 1.0
 */
@RestController
@RequestMapping("/api/estadisticas")
@RequiredArgsConstructor
public class EstadisticaController {

    private final EstadisticaService estadisticaService;

    /**
     * Obtiene las estadísticas de uso de una receta en un rango de fechas.
     * GET /api/estadisticas/receta/{recetaId}
     *
     * @param recetaId ID de la receta
     * @param fechaInicio Fecha de inicio (formato: yyyy-MM-dd'T'HH:mm:ss)
     * @param fechaFin Fecha de fin (formato: yyyy-MM-dd'T'HH:mm:ss)
     * @return Estadísticas de la receta con código 200 (OK)
     */
    @GetMapping("/receta/{recetaId}")
    public ResponseEntity<EstadisticaRecetaResponse> obtenerEstadisticasReceta(
            @PathVariable Long recetaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin) {

        // Valores por defecto si no se envían
        if (fechaFin == null) fechaFin = LocalDateTime.now();
        if (fechaInicio == null) fechaInicio = fechaFin.minusMonths(1);

        return ResponseEntity.ok(
                estadisticaService.obtenerEstadisticasReceta(recetaId, fechaInicio, fechaFin)
        );
    }

    /**
     * Obtiene las estadísticas de todas las recetas en un rango de fechas.
     * GET /api/estadisticas/recetas
     *
     * @param fechaInicio Fecha de inicio (formato: yyyy-MM-dd'T'HH:mm:ss)
     * @param fechaFin Fecha de fin (formato: yyyy-MM-dd'T'HH:mm:ss)
     * @return Lista de estadísticas de todas las recetas con código 200 (OK)
     */
    @GetMapping("/recetas")
    public ResponseEntity<List<EstadisticaRecetaResponse>> obtenerEstadisticasTodasRecetas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin) {

        // Valores por defecto si no se envían
        if (fechaFin == null) fechaFin = LocalDateTime.now();
        if (fechaInicio == null) fechaInicio = fechaFin.minusMonths(1);

        return ResponseEntity.ok(
                estadisticaService.obtenerEstadisticasTodasRecetas(fechaInicio, fechaFin)
        );
    }
}