package com.ares.backend.controller;

import com.ares.backend.dto.CrearNegocioRequest;
import com.ares.backend.dto.NegocioAdminResponse;
import com.ares.backend.dto.NegocioCreadoResponse;
import com.ares.backend.dto.SignupCodeResponse;
import com.ares.backend.service.NegocioAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST de provisión del superadmin de plataforma. Todas las
 * rutas viven bajo {@code /api/admin}, protegidas por
 * {@code hasRole("PLATFORM_ADMIN")} + {@link com.ares.backend.config.AdminApiKeyFilter}
 * (ver {@code SecurityConfig}) — un mecanismo de autenticación totalmente
 * separado del JWT de tenant, nunca representable como {@code Usuario}.
 * Únicamente delega en {@link NegocioAdminService} y mapea sus entidades a
 * los DTOs administrativos; no contiene lógica de negocio propia.
 *
 * @author Ares
 * @version 1.0
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class NegocioAdminController {

    private final NegocioAdminService negocioAdminService;

    /**
     * Crea un Negocio y su primer código de alta en una única operación
     * atómica.
     * POST /api/admin/negocios
     *
     * @param request Datos del negocio a crear
     * @return Negocio creado y su primer código de alta, con código 201 (CREATED)
     */
    @Operation(
            summary = "Crea un Negocio y su primer código de alta",
            description = "Solo accesible con credencial de superadmin de plataforma "
                    + "(cabecera X-Admin-Token). No exige unicidad de nombre entre Negocios."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Negocio creado con su primer código de alta"),
            @ApiResponse(responseCode = "403", description = "Credencial de admin ausente/incorrecta")
    })
    @PostMapping("/negocios")
    public ResponseEntity<NegocioCreadoResponse> crearNegocio(@RequestBody CrearNegocioRequest request) {
        var primerCodigo = negocioAdminService.crearNegocio(request.getNombre(), request.getEmailContacto());
        return ResponseEntity.status(HttpStatus.CREATED).body(new NegocioCreadoResponse(primerCodigo));
    }

    /**
     * Emite un código de alta adicional para un Negocio ya existente.
     * POST /api/admin/negocios/{id}/signup-codes
     *
     * @param id Id del Negocio para el que se emite el código
     * @return Código de alta recién creado, con código 201 (CREATED)
     */
    @Operation(
            summary = "Emite un código de alta adicional para un Negocio existente",
            description = "Sin límite de códigos simultáneamente válidos por Negocio; permitido "
                    + "aunque el primer jefe de ese Negocio ya se haya registrado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Código de alta creado"),
            @ApiResponse(responseCode = "404", description = "El Negocio no existe")
    })
    @PostMapping("/negocios/{id}/signup-codes")
    public ResponseEntity<SignupCodeResponse> generarCodigo(@PathVariable Long id) {
        var codigo = negocioAdminService.generarCodigo(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(new SignupCodeResponse(codigo));
    }

    /**
     * Lista todos los Negocios dados de alta, más reciente primero.
     * GET /api/admin/negocios
     *
     * @return Lista de negocios con código 200 (OK)
     */
    @GetMapping("/negocios")
    public ResponseEntity<List<NegocioAdminResponse>> listarNegocios() {
        List<NegocioAdminResponse> negocios = negocioAdminService.listarNegocios().stream()
                .map(NegocioAdminResponse::new)
                .toList();
        return ResponseEntity.ok(negocios);
    }

    /**
     * Lista todos los códigos de alta (usados, sin usar y revocados) de un
     * Negocio, más reciente primero.
     * GET /api/admin/negocios/{id}/signup-codes
     *
     * @param id Id del Negocio cuyos códigos se listan
     * @return Lista de códigos de alta con código 200 (OK)
     */
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Códigos de alta del negocio"),
            @ApiResponse(responseCode = "404", description = "El Negocio no existe")
    })
    @GetMapping("/negocios/{id}/signup-codes")
    public ResponseEntity<List<SignupCodeResponse>> listarCodigos(@PathVariable Long id) {
        List<SignupCodeResponse> codigos = negocioAdminService.listarCodigos(id).stream()
                .map(SignupCodeResponse::new)
                .toList();
        return ResponseEntity.ok(codigos);
    }

    /**
     * Revoca un código de alta.
     * POST /api/admin/signup-codes/{id}/revoke
     *
     * @param id Id del código de alta a revocar
     * @return Código de alta en su estado posterior a la revocación, con código 200 (OK)
     */
    @Operation(
            summary = "Revoca un código de alta",
            description = "Revocar un código ya usado devuelve 409 (revocarlo no desactiva la "
                    + "cuenta que ya creó); revocar uno ya revocado es idempotente (200)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Código revocado (o ya estaba revocado, idempotente)"),
            @ApiResponse(responseCode = "404", description = "El código no existe"),
            @ApiResponse(responseCode = "409", description = "El código ya fue usado")
    })
    @PostMapping("/signup-codes/{id}/revoke")
    public ResponseEntity<SignupCodeResponse> revocarCodigo(@PathVariable Long id) {
        var codigo = negocioAdminService.revocarCodigo(id);
        return ResponseEntity.ok(new SignupCodeResponse(codigo));
    }
}
