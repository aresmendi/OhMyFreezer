package com.ares.backend.controller;

import com.ares.backend.config.JwtUtil;
import com.ares.backend.dto.LoginResponse;
import com.ares.backend.dto.UsuarioLoginRequest;
import com.ares.backend.dto.UsuarioRegisterRequest;
import com.ares.backend.dto.UsuarioResponse;
import com.ares.backend.service.UsuarioService;
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
@CrossOrigin(origins = "*")
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
    @PostMapping("/register")
    public ResponseEntity<UsuarioResponse> registrar(@RequestBody UsuarioRegisterRequest request) {
        UsuarioResponse usuario = usuarioService.registrar(request);
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
        String token = jwtUtil.generarToken(usuario.getId(),usuario.getUsername(), usuario.getEsJefeCocina());
        return ResponseEntity.ok(new LoginResponse(token, usuario.getId(), usuario.getUsername(), usuario.getEsJefeCocina()));
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
     * Obtiene un usuario por su ID.
     * GET /api/usuarios/{id}
     *
     *
     * @return Usuario encontrado con código 200 (OK)
     */
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> obtenerPorId() {
        return ResponseEntity.ok(usuarioService.obtenerPorId());
    }
}