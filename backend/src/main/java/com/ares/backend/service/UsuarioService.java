package com.ares.backend.service;

import com.ares.backend.config.SecurityUtils;
import com.ares.backend.dto.UsuarioLoginRequest;
import com.ares.backend.dto.UsuarioRegisterRequest;
import com.ares.backend.dto.UsuarioResponse;
import com.ares.backend.entity.Usuario;
import com.ares.backend.repository.RecetaFavoritaRepository;
import com.ares.backend.repository.RegistroUsoRecetaRepository;
import com.ares.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para la gestión de usuarios.
 *
 * @author Ares
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final RegistroUsoRecetaRepository registroUsoRepository;
    private final RecetaFavoritaRepository recetaFavoritaRepository;

    /**
     * Código de verificación para registrarse como jefe de cocina.
     * Todo En producción, esto debería estar en variables de entorno.
     */
    private static final String CODIGO_JEFE_COCINA = "CHEF2024";

    /**
     * Registra un nuevo usuario en el sistema.
     *
     * @param request Datos del usuario a registrar
     * @return Usuario registrado
     * @throws IllegalArgumentException Si el username ya existe o el código de jefe es inválido
     */
    @Transactional
    public UsuarioResponse registrar(UsuarioRegisterRequest request) {
        // Validar que el username no exista
        if (usuarioRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("El nombre de usuario ya existe");
        }

        // Validar código de jefe de cocina si aplica
        if (Boolean.TRUE.equals(request.getEsJefeCocina())) {
            if (request.getCodigoJefe() == null || !request.getCodigoJefe().equals(CODIGO_JEFE_COCINA)) {
                throw new IllegalArgumentException("Código de jefe de cocina inválido");
            }
        }

        // Crear usuario
        Usuario usuario = new Usuario();
        usuario.setUsername(request.getUsername());
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setEsJefeCocina(request.getEsJefeCocina() != null ? request.getEsJefeCocina() : false);
        usuario.setFechaRegistro(LocalDateTime.now());

        Usuario usuarioGuardado = usuarioRepository.save(usuario);
        return new UsuarioResponse(usuarioGuardado);
    }

    /**
     * Autentica un usuario en el sistema.
     *
     * @param request Credenciales del usuario
     * @return Usuario autenticado
     * @throws IllegalArgumentException Si las credenciales son inválidas
     */
    public UsuarioResponse login(UsuarioLoginRequest request) {
        Usuario usuario = usuarioRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Usuario o contraseña incorrectos"));

        // Compara la contraseña en texto plano con el hash almacenado
        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            throw new IllegalArgumentException("Usuario o contraseña incorrectos");
        }

        return new UsuarioResponse(usuario);
    }

    /**
     * Obtiene todos los usuarios del sistema.
     *
     * @return Lista de usuarios
     */
    public List<UsuarioResponse> obtenerTodos() {
        return usuarioRepository.findAll().stream()
                .map(UsuarioResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene un usuario por su ID.
     *
     * @return Usuario encontrado
     * @throws IllegalArgumentException Si el usuario no existe
     */
    public UsuarioResponse obtenerPorId() {
        Long usuarioId = SecurityUtils.getUsuarioId();
        buscarPorId(usuarioId);
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        return new UsuarioResponse(usuario);
    }

    /**
     * Busca un usuario por su ID (función interna del backend).
     *
     * @param id ID del usuario
     * @return Usuario encontrado
     * @throws IllegalArgumentException Si el usuario no existe
     */
    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + id));
    }

    /**
     * Verifica si un usuario es jefe de cocina.
     *
     * @return true si es jefe de cocina, false en caso contrario
     */
    public boolean esJefeCocina() {
        Long usuarioId = SecurityUtils.getUsuarioId();
        buscarPorId(usuarioId);
        Usuario usuario = buscarPorId(usuarioId);
        return Boolean.TRUE.equals(usuario.getEsJefeCocina());
    }

    /**
     * Elimina un usuario del sistema.
     * Solo los jefes de cocina pueden eliminar usuarios.
     *
     * @param id ID del usuario a eliminar
     * @throws IllegalArgumentException Si el usuario no existe o no tiene permisos
     */
    @Transactional
    public void eliminar(Long id) {
        Long usuarioId = SecurityUtils.getUsuarioId();
        Usuario chef = buscarPorId(usuarioId);
        
        if (!Boolean.TRUE.equals(chef.getEsJefeCocina())) {
            throw new IllegalArgumentException("Solo los jefes de cocina pueden eliminar usuarios");
        }

        Usuario usuario = buscarPorId(id);
        
        if (Boolean.TRUE.equals(usuario.getEsJefeCocina())) {
            throw new IllegalArgumentException("No se puede eliminar a un jefe de cocina");
        }
        
        if (id.equals(usuarioId)) {
            throw new IllegalArgumentException("No puedes eliminarte a ti mismo");
        }

        // Eliminar registros relacionados primero
        registroUsoRepository.deleteByUsuarioId(id);
        recetaFavoritaRepository.deleteByUsuarioId(id);

        usuarioRepository.delete(usuario);
    }
}