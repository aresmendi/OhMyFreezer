package com.ares.backend.controller;

import com.ares.backend.dto.TpvSkuMappingRequest;
import com.ares.backend.dto.TpvSkuMappingResponse;
import com.ares.backend.service.TpvSkuMappingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la gestión de mapeos SKU del TPV. Todas las rutas
 * viven bajo {@code /api/tpv/sku-mappings}, protegidas por
 * {@link com.ares.backend.config.TpvApiKeyFilter} (misma capa que el resto
 * de {@code /api/tpv/**}). Solo delega en {@link TpvSkuMappingService} y
 * mapea entidades a DTOs; no contiene lógica de negocio propia.
 *
 * @author Ares
 * @version 1.0
 */
@RestController
@RequestMapping("/api/tpv/sku-mappings")
@RequiredArgsConstructor
public class TpvSkuMappingController {

    private final TpvSkuMappingService tpvSkuMappingService;

    /**
     * Lista todos los mapeos SKU del negocio autenticado, más recientes primero.
     * GET /api/tpv/sku-mappings
     *
     * @return Lista de mapeos con código 200 (OK)
     */
    @Operation(
            summary = "Lista los mapeos SKU del negocio",
            description = "Devuelve todos los mapeos del negocio autenticado (mapeados y pendientes), "
                    + "más recientes primero. Protegido por X-Tpv-Api-Key."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de mapeos SKU del negocio"),
            @ApiResponse(responseCode = "403", description = "API key TPV ausente o incorrecta")
    })
    @GetMapping
    public ResponseEntity<List<TpvSkuMappingResponse>> listar() {
        List<TpvSkuMappingResponse> response = tpvSkuMappingService.listar().stream()
                .map(TpvSkuMappingResponse::new)
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Asigna una receta a un mapeo SKU del negocio autenticado.
     * PUT /api/tpv/sku-mappings/{id}
     *
     * @param id      ID del mapeo SKU a actualizar
     * @param request Receta a asignar
     * @return Mapeo actualizado con código 200 (OK)
     */
    @Operation(
            summary = "Asigna una receta a un mapeo SKU",
            description = "Resuelve un SKU pendiente asignándole una receta del mismo negocio. "
                    + "El caller no puede asignar recetas de otro negocio (IDOR check)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mapeo actualizado con la receta asignada"),
            @ApiResponse(responseCode = "400", description = "recetaId es nulo o inválido"),
            @ApiResponse(responseCode = "403", description = "API key TPV ausente o incorrecta"),
            @ApiResponse(responseCode = "404", description = "El mapeo o la receta no existen en este negocio")
    })
    @PutMapping("/{id}")
    public ResponseEntity<TpvSkuMappingResponse> asignarReceta(
            @PathVariable Long id,
            @Valid @RequestBody TpvSkuMappingRequest request) {
        var mapping = tpvSkuMappingService.asignarReceta(id, request.getRecetaId());
        return ResponseEntity.ok(new TpvSkuMappingResponse(mapping));
    }

    /**
     * Desmapea un mapeo SKU (elimina la asignación de receta, dejándolo pendiente).
     * DELETE /api/tpv/sku-mappings/{id}/receta
     *
     * @param id ID del mapeo SKU a desmapear
     * @return 204 NO_CONTENT
     */
    @Operation(
            summary = "Desmapea un SKU (elimina la receta asignada)",
            description = "Vuelve a dejar el mapeo en estado pendiente (receta = null). "
                    + "El caller no puede desmapear mapeos de otro negocio (IDOR check)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Mapeo desmapeado correctamente"),
            @ApiResponse(responseCode = "403", description = "API key TPV ausente o incorrecta"),
            @ApiResponse(responseCode = "404", description = "El mapeo no existe en este negocio")
    })
    @DeleteMapping("/{id}/receta")
    public ResponseEntity<Void> desmapear(@PathVariable Long id) {
        tpvSkuMappingService.desmapear(id);
        return ResponseEntity.noContent().build();
    }
}
