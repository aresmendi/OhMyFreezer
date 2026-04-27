package com.ares.backend.controller;

import com.ares.backend.dto.RegistroUsoResponse;
import com.ares.backend.service.RegistroUsoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la gestión de registros de uso de recetas.
 * Maneja las consultas de historial de elaboración.
 *
 * @author Ares
 * @version 1.0
 */
@RestController
@RequestMapping("/api/registros")
@RequiredArgsConstructor
public class RegistroUsoController {

    private final RegistroUsoService registroUsoService;

    /**
     * Obtiene todos los registros de uso.
     * GET /api/registros
     *
     * @return Lista de registros de uso con código 200 (OK)
     */
    @GetMapping
    public ResponseEntity<List<RegistroUsoResponse>> obtenerTodos() {
        List<RegistroUsoResponse> registros = registroUsoService.obtenerTodos();
        return ResponseEntity.ok(registros);
    }

    /**
     * Obtiene los registros de uso de una receta específica.
     * GET /api/registros/receta/{recetaId}
     *
     * @param recetaId ID de la receta
     * @return Lista de registros de uso de la receta con código 200 (OK)
     */
    @GetMapping("/receta/{recetaId}")
    public ResponseEntity<List<RegistroUsoResponse>> obtenerPorReceta(@PathVariable Long recetaId) {
        List<RegistroUsoResponse> registros = registroUsoService.obtenerPorReceta(recetaId);
        return ResponseEntity.ok(registros);
    }

    /**
     * Obtiene los registros de uso del usuario autenticado.
     * GET /api/registros/usuario
     *
     * @return Lista de registros de uso del usuario con código 200 (OK)
     */
    @GetMapping("/usuario")
    public ResponseEntity<List<RegistroUsoResponse>> obtenerPorUsuario() {
        return ResponseEntity.ok(registroUsoService.obtenerPorUsuario());
    }
}