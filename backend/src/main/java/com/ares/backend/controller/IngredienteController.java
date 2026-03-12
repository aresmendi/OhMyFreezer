package com.ares.backend.controller;
import com.ares.backend.dto.IngredienteRequest;
import com.ares.backend.dto.IngredienteResponse;
import com.ares.backend.dto.IngredienteUpdateRequest;
import com.ares.backend.service.IngredienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * Controlador REST para la gestión de ingredientes.
 * Maneja las operaciones CRUD de ingredientes y consultas de stock.
 *
 * @author Ares
 * @version 1.0
 */
@RestController
@RequestMapping("/api/ingredientes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class IngredienteController {

    private final IngredienteService ingredienteService;

    /**
     * Obtiene todos los ingredientes del sistema.
     * GET /api/ingredientes
     *
     * @return Lista de ingredientes con código 200 (OK)
     */
    @GetMapping
    public ResponseEntity<List<IngredienteResponse>> obtenerTodos() {
        return ResponseEntity.ok(ingredienteService.obtenerTodos());
    }

    /**
     * Obtiene un ingrediente por su ID.
     * GET /api/ingredientes/{id}
     *
     * @param id ID del ingrediente
     * @return Ingrediente encontrado con código 200 (OK)
     */
    @GetMapping("/{id}")
    public ResponseEntity<IngredienteResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ingredienteService.obtenerPorId(id));
    }

    /**
     * Crea un nuevo ingrediente.
     * POST /api/ingredientes
     *
     * @param request Datos del ingrediente
     * @return Ingrediente creado con código 201 (CREATED)
     */
    @PostMapping
    public ResponseEntity<IngredienteResponse> crear(@RequestBody IngredienteRequest request) {
        IngredienteResponse ingrediente = ingredienteService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ingrediente);
    }

    /**
     * Actualiza un ingrediente existente.
     * PUT /api/ingredientes/{id}
     *
     * @param id ID del ingrediente
     * @param request Nuevos datos del ingrediente
     * @return Ingrediente actualizado con código 200 (OK)
     */
    @PutMapping("/{id}")
    public ResponseEntity<IngredienteResponse> actualizar(
            @PathVariable Long id,
            @RequestBody IngredienteRequest request) {

        return ResponseEntity.ok(ingredienteService.actualizar(id, request));
    }

    /**
     * Actualiza solo la cantidad de un ingrediente.
     * PATCH /api/ingredientes/{id}/cantidad
     *
     * @param id ID del ingrediente
     * @param request Nueva cantidad
     * @return Ingrediente actualizado con código 200 (OK)
     */
    @PatchMapping("/{id}/cantidad")
    public ResponseEntity<IngredienteResponse> actualizarCantidad(
            @PathVariable Long id,
            @RequestBody IngredienteUpdateRequest request) {

        return ResponseEntity.ok(ingredienteService.actualizarCantidad(id, request));
    }

    /**
     * Elimina un ingrediente.
     * DELETE /api/ingredientes/{id}
     *
     * @param id ID del ingrediente
     * @return Código 204 (NO_CONTENT) si se eliminó correctamente
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id,
            @RequestParam Long usuarioId) {

        ingredienteService.eliminar(id, usuarioId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Obtiene todos los ingredientes con stock bajo.
     * GET /api/ingredientes/alertas
     *
     * @return Lista de ingredientes con stock bajo con código 200 (OK)
     */
    @GetMapping("/alertas")
    public ResponseEntity<List<IngredienteResponse>> obtenerConStockBajo() {
        return ResponseEntity.ok(ingredienteService.obtenerConStockBajo());
    }
}