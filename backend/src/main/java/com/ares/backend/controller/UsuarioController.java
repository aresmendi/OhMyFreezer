package com.ares.backend.controller;

import com.ares.backend.config.JwtUtil;
import com.ares.backend.dto.EmpleadoRegisterRequest;
import com.ares.backend.dto.LoginResponse;
import com.ares.backend.dto.UsuarioEmailRequest;
import com.ares.backend.dto.UsuarioLoginRequest;
import com.ares.backend.dto.UsuarioRegisterRequest;
import com.ares.backend.dto.UsuarioResponse;
import com.ares.backend.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la gestión de usuarios.
 * Maneja las operaciones de registro, login y consulta de usuarios.
 *
 * @author Ares
 * @version 1.0
 */
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final JwtUtil jwtUtil;

    /**
     * Registra un nuevo usuario en el sistema.
     * POST /api/usuarios/register
     *
     * @param request Datos del usuario a registrar
     * @return Usuario registrado con código 201 (CREATED)
     */
    @Operation(
            summary = "Registra el primer jefe de cocina de un Negocio",
            description = "Alta pública, solo para ROLE_JEFE. Requiere un `codigoRegistro` "
                    + "(código de alta de un solo uso, se proveé fuera de banda por Negocio) "
                    + "que resuelve a exactamente un Negocio y queda consumido tras el registro. "
                    + "Reemplaza al antiguo mecanismo global BUSSINES_LOGIC_CODE."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Jefe registrado y vinculado al Negocio del código"),
            @ApiResponse(responseCode = "400", description = "Código de registro inválido/usado/revocado, "
                    + "username ya existente en ese Negocio, o email/datos faltantes")
    })
    @PostMapping("/register")
    public ResponseEntity<UsuarioResponse> registrar(@RequestBody UsuarioRegisterRequest request) {
        UsuarioResponse usuario = usuarioService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(usuario);
    }

    /**
     * Crea una cuenta de empleado (cocinero) perteneciente al negocio del
     * jefe de cocina autenticado que hace la llamada.
     * POST /api/usuarios/empleados
     * Solo accesible para ROLE_JEFE (ver SecurityConfig). No admite código de
     * alta ni negocioId por request: el negocio se hereda siempre del
     * contexto de seguridad del caller.
     *
     * @param request Datos del empleado a crear
     * @return Empleado creado con código 201 (CREATED)
     */
    @Operation(
            summary = "Crea un empleado (cocinero) en el Negocio del jefe autenticado",
            description = "Solo accesible para ROLE_JEFE. No admite código de alta ni negocioId por "
                    + "request: el Negocio del empleado se hereda siempre del contexto de seguridad "
                    + "del jefe que llama, nunca del body — cualquier negocioId incluido en el "
                    + "request es ignorado por completo.",
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Empleado creado en el Negocio del jefe caller"),
            @ApiResponse(responseCode = "400", description = "Username ya existente en ese Negocio"),
            @ApiResponse(responseCode = "403", description = "El caller no está autenticado o no es ROLE_JEFE")
    })
    @PostMapping("/empleados")
    public ResponseEntity<UsuarioResponse> crearEmpleado(@RequestBody EmpleadoRegisterRequest request) {
        UsuarioResponse usuario = usuarioService.crearEmpleado(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(usuario);
    }

    /**
     * Autentica un usuario en el sistema.
     * POST /api/usuarios/login
     *
     * @param request Credenciales del usuario
     * @return Usuario autenticado con código 200 (OK)
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody UsuarioLoginRequest request) {
        UsuarioResponse usuario = usuarioService.login(request);
        String token = jwtUtil.generarToken(usuario.getId(), usuario.getUsername(), usuario.getEsJefeCocina(), usuario.getNegocioId());
        return ResponseEntity.ok(new LoginResponse(token, usuario.getId(), usuario.getUsername(), usuario.getEsJefeCocina(), usuario.getEmail()));
    }

    /**
     * Obtiene todos los usuarios del sistema.
     * GET /api/usuarios
     *
     * @return Lista de usuarios con código 200 (OK)
     */
    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> obtenerTodos() {
        return ResponseEntity.ok(usuarioService.obtenerTodos());
    }

    /**
     * Elimina un usuario del sistema.
     * DELETE /api/usuarios/{id}
     * Solo los jefes de cocina pueden eliminar empleados.
     *
     * @param id ID del usuario a eliminar
     * @return Código 204 (NO_CONTENT) si se eliminó correctamente
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        usuarioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/email")
    public ResponseEntity<UsuarioResponse> actualizarEmail(@RequestBody UsuarioEmailRequest request) {
        return ResponseEntity.ok(usuarioService.actualizarEmail(request.getEmail()));
    }
}