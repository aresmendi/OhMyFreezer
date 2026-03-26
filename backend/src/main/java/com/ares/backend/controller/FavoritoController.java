package com.ares.backend.controller;

import com.ares.backend.dto.FavoritoRequest;
import com.ares.backend.dto.RecetaFavoritaResponse;
import com.ares.backend.service.FavoritoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para la gestión de recetas favoritas.
 * Permite marcar/desmarcar favoritos y consultar favoritos de usuarios.
 *
 * @author Ares
 * @version 1.0
 */
@RestController
@RequestMapping("/api/favoritos")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FavoritoController {

    private final FavoritoService favoritoService;

    /**
     * Marca una receta como favorita.
     * POST /api/favoritos
     *
     * @param request Datos del favorito (usuarioId, recetaId)
     * @return Favorito creado con código 201 (CREATED)
     */
    @PostMapping
    public ResponseEntity<RecetaFavoritaResponse> marcarFavorito(@RequestBody FavoritoRequest request) {
        RecetaFavoritaResponse favorito = favoritoService.marcarFavorito(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(favorito);
    }

    /**
     * Desmarca una receta como favorita.
     * DELETE /api/favoritos
     *
     * @param request Datos del favorito (usuarioId, recetaId)
     * @return Código 204 (NO_CONTENT) si se eliminó correctamente
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desmarcarFavorito(@RequestBody FavoritoRequest request) {
        favoritoService.desmarcarFavorito(request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Obtiene todas las recetas favoritas de un usuario.
     * GET /api/favoritos/usuario/{usuarioId}
     *
     * @param usuarioId ID del usuario
     * @return Lista de recetas favoritas con código 200 (OK)
     */
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<RecetaFavoritaResponse>> obtenerFavoritos(@PathVariable Long usuarioId) {
        List<RecetaFavoritaResponse> favoritos = favoritoService.obtenerFavoritosUsuario(usuarioId);
        return ResponseEntity.ok(favoritos);
    }

    /**
     * Verifica si una receta es favorita de un usuario.
     * GET /api/favoritos/verificar?usuarioId={usuarioId}&recetaId={recetaId}
     *
     * @param usuarioId ID del usuario
     * @param recetaId ID de la receta
     * @return JSON con campo "esFavorita" (true/false)
     */
    @GetMapping("/verificar")
    public ResponseEntity<Map<String, Boolean>> verificarFavorito(
            @RequestParam Long usuarioId,
            @RequestParam Long recetaId) {

        boolean esFavorita = favoritoService.esFavorita(usuarioId, recetaId);
        return ResponseEntity.ok(Map.of("esFavorita", esFavorita));
    }

    /**
     * Obtiene los IDs de las recetas favoritas de un usuario.
     * Útil para marcar favoritos en listados.
     * GET /api/favoritos/usuario/{usuarioId}/ids
     *
     * @param usuarioId ID del usuario
     * @return Lista de IDs de recetas favoritas
     */
    @GetMapping("/usuario/{usuarioId}/ids")
    public ResponseEntity<List<Long>> obtenerIdsFavoritos(@PathVariable Long usuarioId) {
        List<Long> ids = favoritoService.obtenerIdsFavoritosUsuario(usuarioId);
        return ResponseEntity.ok(ids);
    }
}