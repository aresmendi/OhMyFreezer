package com.ares.backend.controller;

import com.ares.backend.dto.TpvApiKeyEmitidaResponse;
import com.ares.backend.dto.TpvApiKeyResponse;
import com.ares.backend.service.TpvApiKeyAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST de gestión de credenciales TPV del superadmin de
 * plataforma. Todas las rutas viven bajo {@code /api/admin}, protegidas por
 * {@code hasRole("PLATFORM_ADMIN")} + {@link com.ares.backend.config.AdminApiKeyFilter}
 * (ver {@code SecurityConfig}), el mismo mecanismo que
 * {@link NegocioAdminController} — un admin de plataforma no es un {@code
 * Usuario} de ningún Negocio. Únicamente delega en
 * {@link TpvApiKeyAdminService} y mapea sus entidades a los DTOs
 * administrativos; no contiene lógica de negocio propia.
 *
 * @author Ares
 * @version 1.0
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class TpvApiKeyAdminController {

    private final TpvApiKeyAdminService tpvApiKeyAdminService;

    /**
     * Emite (o re-emite, revocando la anterior — D3 del diseño) la
     * credencial TPV de un Negocio.
     * POST /api/admin/negocios/{id}/tpv-api-keys
     *
     * @param id Id del Negocio para el que se emite la credencial
     * @return Credencial emitida con la key en claro (recuperable solo aquí), código 201 (CREATED)
     */
    @Operation(
            summary = "Emite (o re-emite) la credencial TPV de un Negocio",
            description = "Solo accesible con credencial de superadmin de plataforma "
                    + "(cabecera X-Admin-Token). Re-emitir revoca la credencial activa anterior "
                    + "del negocio (como mucho una activa a la vez). La key en claro solo se "
                    + "devuelve en esta respuesta; el backend nunca vuelve a exponerla."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Credencial TPV emitida"),
            @ApiResponse(responseCode = "403", description = "Credencial de admin ausente/incorrecta"),
            @ApiResponse(responseCode = "404", description = "El Negocio no existe")
    })
    @PostMapping("/negocios/{id}/tpv-api-keys")
    public ResponseEntity<TpvApiKeyEmitidaResponse> emitir(@PathVariable Long id) {
        var emitida = tpvApiKeyAdminService.emitir(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(new TpvApiKeyEmitidaResponse(emitida));
    }

    /**
     * Lista todas las credenciales TPV (activas y revocadas) de un Negocio,
     * más reciente primero. Nunca expone el secreto ni su hash.
     * GET /api/admin/negocios/{id}/tpv-api-keys
     *
     * @param id Id del Negocio cuyas credenciales se listan
     * @return Lista de credenciales con código 200 (OK)
     */
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credenciales TPV del negocio"),
            @ApiResponse(responseCode = "404", description = "El Negocio no existe")
    })
    @GetMapping("/negocios/{id}/tpv-api-keys")
    public ResponseEntity<List<TpvApiKeyResponse>> listar(@PathVariable Long id) {
        List<TpvApiKeyResponse> claves = tpvApiKeyAdminService.listar(id).stream()
                .map(TpvApiKeyResponse::new)
                .toList();
        return ResponseEntity.ok(claves);
    }

    /**
     * Revoca una credencial TPV.
     * POST /api/admin/tpv-api-keys/{id}/revoke
     *
     * @param id Id de la credencial a revocar
     * @return Credencial en su estado posterior a la revocación, con código 200 (OK)
     */
    @Operation(
            summary = "Revoca una credencial TPV",
            description = "Idempotente: revocar una credencial ya revocada no falla, devuelve 200 "
                    + "de nuevo (mismo patrón que POST /api/admin/signup-codes/{id}/revoke)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credencial revocada (o ya estaba revocada, idempotente)"),
            @ApiResponse(responseCode = "403", description = "Credencial de admin ausente/incorrecta"),
            @ApiResponse(responseCode = "404", description = "La credencial no existe")
    })
    @PostMapping("/tpv-api-keys/{id}/revoke")
    public ResponseEntity<TpvApiKeyResponse> revocar(@PathVariable Long id) {
        var clave = tpvApiKeyAdminService.revocar(id);
        return ResponseEntity.ok(new TpvApiKeyResponse(clave));
    }
}
