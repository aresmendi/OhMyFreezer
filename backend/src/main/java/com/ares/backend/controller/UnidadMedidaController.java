package com.ares.backend.controller;

import com.ares.backend.dto.UnidadMedidaResponse;
import com.ares.backend.repository.UnidadMedidaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador REST para el catálogo global de unidades de medida.
 * A diferencia del resto de controllers, este catálogo NO está scoped por
 * negocio: cualquier usuario autenticado, de cualquier tenant, recibe el
 * mismo conjunto de filas (ver decisión D1 del diseño). No requiere ningún
 * matcher especial en SecurityConfig: cae en {@code anyRequest().authenticated()}.
 *
 * @author Ares
 * @version 1.0
 */
@RestController
@RequestMapping("/api/unidades")
@RequiredArgsConstructor
public class UnidadMedidaController {

    private final UnidadMedidaRepository unidadMedidaRepository;

    /**
     * Obtiene el catálogo completo de unidades de medida.
     * GET /api/unidades
     *
     * @return Lista de unidades de medida con código 200 (OK)
     */
    @GetMapping
    public ResponseEntity<List<UnidadMedidaResponse>> obtenerTodas() {
        List<UnidadMedidaResponse> unidades = unidadMedidaRepository.findAll().stream()
                .map(UnidadMedidaResponse::new)
                .toList();
        return ResponseEntity.ok(unidades);
    }
}
