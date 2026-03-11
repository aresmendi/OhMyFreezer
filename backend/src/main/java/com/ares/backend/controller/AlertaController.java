package com.ares.backend.controller;

import com.ares.backend.dto.AlertaCountResponse;
import com.ares.backend.dto.AlertaResponse;
import com.ares.backend.service.AlertaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la gestión de alertas.
 * Maneja las consultas y actualización de alertas del sistema.
 *
 * @author Ares
 * @version 1.0
 */
@RestController
@RequestMapping("/api/alertas")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AlertaController {

    private final AlertaService alertaService;

    /**
     * Obtiene todas las alertas de un usuario.
     * GET /api/alertas/usuario/{usuarioId}
     *
     * @param usuarioId ID del usuario
     * @return Lista de alertas con código 200 (OK)
     */
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<AlertaResponse>> obtenerPorUsuario(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(alertaService.obtenerPorUsuario(usuarioId));
    }

    /**
     * Obtiene las alertas no leídas de un usuario.
     * GET /api/alertas/usuario/{usuarioId}/pendientes
     *
     * @param usuarioId ID del usuario
     * @return Lista de alertas no leídas con código 200 (OK)
     */
    @GetMapping("/usuario/{usuarioId}/pendientes")
    public ResponseEntity<List<AlertaResponse>> obtenerNoLeidasPorUsuario(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(alertaService.obtenerNoLeidasPorUsuario(usuarioId));
    }

    /**
     * Obtiene el conteo de alertas no leídas de un usuario.
     * GET /api/alertas/usuario/{usuarioId}/count
     *
     * @param usuarioId ID del usuario
     * @return Conteo de alertas pendientes con código 200 (OK)
     */
    @GetMapping("/usuario/{usuarioId}/count")
    public ResponseEntity<AlertaCountResponse> contarNoLeidas(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(alertaService.contarNoLeidas(usuarioId));
    }

    /**
     * Marca una alerta como leída.
     * PATCH /api/alertas/{id}/leer
     *
     * @param id ID de la alerta
     * @return Alerta actualizada con código 200 (OK)
     */
    @PatchMapping("/{id}/leer")
    public ResponseEntity<AlertaResponse> marcarComoLeida(@PathVariable Long id) {
        return ResponseEntity.ok(alertaService.marcarComoLeida(id));
    }

    /**
     * Marca todas las alertas de un usuario como leídas.
     * PATCH /api/alertas/usuario/{usuarioId}/leer-todas
     *
     * @param usuarioId ID del usuario
     * @return Código 204 (NO_CONTENT) si se actualizaron correctamente
     */
    @PatchMapping("/usuario/{usuarioId}/leer-todas")
    public ResponseEntity<Void> marcarTodasComoLeidas(@PathVariable Long usuarioId) {
        alertaService.marcarTodasComoLeidas(usuarioId);
        return ResponseEntity.noContent().build();
    }
}