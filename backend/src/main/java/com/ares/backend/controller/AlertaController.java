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
     * Obtiene todas las alertas del usuario autenticado.
     * GET /api/alertas/usuario
     *
     * @return Lista de alertas con código 200 (OK)
     */
    @GetMapping("/usuario")
    public ResponseEntity<List<AlertaResponse>> obtenerPorUsuario() {
        return ResponseEntity.ok(alertaService.obtenerPorUsuario());
    }

    /**
     * Obtiene las alertas no leídas del usuario autenticado.
     * GET /api/alertas/usuario/pendientes
     *
     * @return Lista de alertas no leídas con código 200 (OK)
     */
    @GetMapping("/usuario/pendientes")
    public ResponseEntity<List<AlertaResponse>> obtenerNoLeidasPorUsuario() {
        return ResponseEntity.ok(alertaService.obtenerNoLeidasPorUsuario());
    }

    /**
     * Obtiene el conteo de alertas no leídas del usuario autenticado.
     * GET /api/alertas/usuario/count
     *
     * @return Conteo de alertas pendientes con código 200 (OK)
     */
    @GetMapping("/usuario/count")
    public ResponseEntity<AlertaCountResponse> contarNoLeidas() {
        return ResponseEntity.ok(alertaService.contarNoLeidas());
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
     * Marca todas las alertas del usuario autenticado como leídas.
     * PATCH /api/alertas/usuario/leer-todas
     *
     * @return Código 204 (NO_CONTENT) si se actualizaron correctamente
     */
    @PatchMapping("/usuario/leer-todas")
    public ResponseEntity<Void> marcarTodasComoLeidas() {
        alertaService.marcarTodasComoLeidas();
        return ResponseEntity.noContent().build();
    }
}