package com.ares.backend.controller;
import com.ares.backend.dto.*;
import com.ares.backend.service.RecetaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * Controlador REST para la gestión de recetas.
 * Maneja las operaciones CRUD de recetas, verificación y elaboración.
 *
 * @author Ares
 * @version 1.0
 */
@RestController
@RequestMapping("/api/recetas")
@RequiredArgsConstructor
public class RecetaController {

    private final RecetaService recetaService;

    /**
     * Obtiene todas las recetas del sistema.
     * GET /api/recetas
     *
     * @return Lista de recetas simplificadas con código 200 (OK)
     */
    @GetMapping
    public ResponseEntity<List<RecetaDetailResponse>> obtenerTodas() {
        return ResponseEntity.ok(recetaService.obtenerTodas());
    }

    /**
     * Obtiene una receta por su ID con todos los detalles.
     * GET /api/recetas/{id}
     *
     * @param id ID de la receta
     * @return Receta detallada con código 200 (OK)
     */
    @GetMapping("/{id}")
    public ResponseEntity<RecetaDetailResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(recetaService.obtenerPorId(id));
    }

    /**
     * Crea una nueva receta.
     * Solo los jefes de cocina pueden crear recetas.
     * POST /api/recetas
     *
     * @param request Datos de la receta
     * @return Receta creada con código 201 (CREATED)
     */
    @PostMapping
    public ResponseEntity<RecetaDetailResponse> crear(@RequestBody RecetaRequest request) {
        RecetaDetailResponse receta = recetaService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(receta);
    }

    /**
     * Actualiza una receta existente.
     * Solo los jefes de cocina pueden actualizar recetas.
     * PUT /api/recetas/{id}
     *
     * @param id ID de la receta
     * @param request Nuevos datos de la receta
     * @return Receta actualizada con código 200 (OK)
     */
    @PutMapping("/{id}")
    public ResponseEntity<RecetaDetailResponse> actualizar(
            @PathVariable Long id,
            @RequestBody RecetaRequest request) {

        return ResponseEntity.ok(recetaService.actualizar(id, request));
    }

    /**
     * Elimina una receta.
     * Solo los jefes de cocina pueden eliminar recetas.
     * DELETE /api/recetas/{id}
     *
     * @param id ID de la receta
     * @return Código 204 (NO_CONTENT) si se eliminó correctamente
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id) {

        recetaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Verifica si una receta está disponible (hay stock suficiente).
     * POST /api/recetas/{id}/verificar
     *
     * @param id ID de la receta
     * @return Resultado de la verificación con código 200 (OK)
     */
    @PostMapping("/{id}/verificar")
    public ResponseEntity<VerificarRecetaResponse> verificarDisponibilidad(
            @PathVariable Long id) {

        return ResponseEntity.ok(recetaService.verificarDisponibilidadYNotificar(id));
    }

    /**
     * Elabora una receta (reduce el stock de ingredientes).
     * POST /api/recetas/{id}/elaborar
     *
     * @param id ID de la receta
     * @param request Datos de elaboración
     * @return Registro de uso creado con código 201 (CREATED)
     */
    @PostMapping("/{id}/elaborar")
    public ResponseEntity<RegistroUsoResponse> elaborar(
            @PathVariable Long id,
            @RequestBody ElaborarRecetaRequest request) {

        RegistroUsoResponse registro = recetaService.elaborar(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(registro);
    }

    /**
     * Obtiene los pasos de una receta.
     * GET /api/recetas/{id}/pasos
     *
     * @param id ID de la receta
     * @return Lista de pasos ordenados con código 200 (OK)
     */
    @GetMapping("/{id}/pasos")
    public ResponseEntity<List<PasoRecetaDTO>> obtenerPasos(@PathVariable Long id) {
        return ResponseEntity.ok(recetaService.obtenerPasos(id));
    }
}