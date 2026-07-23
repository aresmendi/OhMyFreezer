package com.ares.backend.service;

import com.ares.backend.config.SecurityUtils;
import com.ares.backend.dto.EmpleadoRegisterRequest;
import com.ares.backend.dto.UsuarioLoginRequest;
import com.ares.backend.dto.UsuarioRegisterRequest;
import com.ares.backend.dto.UsuarioResponse;
import com.ares.backend.entity.Negocio;
import com.ares.backend.entity.NegocioSignupCode;
import com.ares.backend.entity.Usuario;
import com.ares.backend.exception.RecursoNoEncontradoException;
import com.ares.backend.repository.NegocioRepository;
import com.ares.backend.repository.NegocioSignupCodeRepository;
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
    private final NegocioRepository negocioRepository;
    private final NegocioSignupCodeRepository negocioSignupCodeRepository;

    /**
     * Registra el primer jefe de cocina de un Negocio a partir de un código
     * de alta (signup code) pre-provisionado. Reemplaza el antiguo mecanismo
     * global {@code BUSSINES_LOGIC_CODE}: cada Negocio tiene su propio
     * código, de un solo uso, que resuelve a exactamente ese Negocio.
     * <p>
     * Este endpoint público YA NO admite el alta de empleados sueltos
     * (esJefeCocina=false): un empleado no puede autoasignarse un negocio de
     * forma anónima. Los empleados se crean vía
     * {@link #crearEmpleado(EmpleadoRegisterRequest)}, que hereda el negocio
     * del jefe autenticado que hace la llamada.
     *
     * @param request Datos del jefe a registrar, incluyendo el código de alta
     * @return Usuario (jefe) registrado, vinculado al Negocio del código
     * @throws IllegalArgumentException Si el username ya existe en ese
     *                                   negocio, si el email ya existe en
     *                                   CUALQUIER negocio, si el código de
     *                                   alta es desconocido/usado/revocado,
     *                                   o si falta el email
     */
    @Transactional
    public UsuarioResponse registrar(UsuarioRegisterRequest request) {
        if (Boolean.FALSE.equals(request.getEsJefeCocina())) {
            throw new IllegalArgumentException(
                    "El registro público solo admite altas de jefe de cocina; "
                            + "los empleados se crean desde /api/usuarios/empleados");
        }

        if (request.getCodigoRegistro() == null || request.getCodigoRegistro().isBlank()) {
            throw new IllegalArgumentException("Código de registro inválido");
        }

        NegocioSignupCode signupCode = negocioSignupCodeRepository
                .findByCodigo(request.getCodigoRegistro())
                .orElseThrow(() -> new IllegalArgumentException("Código de registro inválido"));

        // Lectura en memoria solo para dar un mensaje de error más claro en
        // el caso de revocación; NO es la autoridad de concurrencia. La
        // autoridad real es la reclamación atómica marcarUsadoAtomico() más
        // abajo, que vuelve a comprobar activo=true a nivel de base de datos.
        if (!Boolean.TRUE.equals(signupCode.getActivo())) {
            throw new IllegalArgumentException("Código de registro inválido");
        }

        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("El correo electrónico es obligatorio");
        }

        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("El correo electrónico ya está en uso");
        }

        Negocio negocio = signupCode.getNegocio();

        if (usuarioRepository.existsByUsernameAndNegocioId(request.getUsername(), negocio.getId())) {
            throw new IllegalArgumentException("El nombre de usuario ya existe");
        }

        // Reclamación atómica: única sección crítica frente a concurrencia.
        // Si dos peticiones llegan aquí con el mismo código, el UPDATE
        // condicional de la base de datos garantiza que sólo una de ellas
        // afecta una fila (y por tanto solo una llega a crear el Usuario).
        // Se reclama ANTES de crear el Usuario para que una petición que
        // pierde la carrera nunca deje un jefe huérfano creado.
        if (negocioSignupCodeRepository.marcarUsadoAtomico(request.getCodigoRegistro()) == 0) {
            throw new IllegalArgumentException("Código de registro inválido");
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(request.getUsername());
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setEsJefeCocina(true);
        usuario.setFechaRegistro(LocalDateTime.now());
        usuario.setEmail(request.getEmail());
        usuario.setNegocio(negocio);

        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        negocioSignupCodeRepository.registrarUsuarioQueConsumio(request.getCodigoRegistro(), usuarioGuardado.getId());

        return new UsuarioResponse(usuarioGuardado);
    }

    /**
     * Crea una cuenta de empleado (cocinero) perteneciente al mismo Negocio
     * que el jefe de cocina autenticado que hace la llamada. No admite
     * código de alta ni negocioId por request: el negocio se hereda siempre
     * de {@code SecurityUtils.getNegocioId()}, cerrando así el último hueco
     * DEFAULT 1 de la tabla usuarios (el alta de empleados).
     *
     * @param request Datos del empleado a crear
     * @return Usuario (empleado) creado, vinculado al negocio del jefe caller
     * @throws IllegalArgumentException      Si el username ya existe en el
     *                                        negocio del jefe caller, si el
     *                                        email ya existe en CUALQUIER
     *                                        negocio, o si falta el email
     * @throws RecursoNoEncontradoException  Si el negocio del jefe caller no
     *                                        existe (inconsistencia de datos)
     */
    @Transactional
    public UsuarioResponse crearEmpleado(EmpleadoRegisterRequest request) {
        Long negocioId = SecurityUtils.getNegocioId();

        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("El correo electrónico es obligatorio");
        }

        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("El correo electrónico ya está en uso");
        }

        if (usuarioRepository.existsByUsernameAndNegocioId(request.getUsername(), negocioId)) {
            throw new IllegalArgumentException("El nombre de usuario ya existe");
        }

        Negocio negocio = negocioRepository.findById(negocioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Negocio no encontrado"));

        Usuario usuario = new Usuario();
        usuario.setUsername(request.getUsername());
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setEsJefeCocina(false);
        usuario.setFechaRegistro(LocalDateTime.now());
        usuario.setEmail(request.getEmail());
        usuario.setNegocio(negocio);

        Usuario usuarioGuardado = usuarioRepository.save(usuario);
        return new UsuarioResponse(usuarioGuardado);
    }

    /**
     * Autentica un usuario en el sistema.
     * <p>
     * Resuelve SIEMPRE por email, el identificador GLOBAL de login (único
     * en todo el sistema desde V3). Username ya no sirve para esto: solo es
     * único por negocio (desde V2), y dos negocios distintos pueden tener un
     * usuario con el mismo username Y la misma contraseña, en cuyo caso una
     * desambiguación por contraseña autenticaría arbitrariamente contra el
     * negocio equivocado (fuga cross-tenant real, cerrada por este cambio).
     *
     * @param request Credenciales del usuario (email + contraseña)
     * @return Usuario autenticado
     * @throws IllegalArgumentException Si no existe ningún usuario con ese
     *                                   email, o si la contraseña no
     *                                   coincide (mismo mensaje en ambos
     *                                   casos, para no revelar cuál de las
     *                                   dos causas fue)
     */
    public UsuarioResponse login(UsuarioLoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .filter(u -> passwordEncoder.matches(request.getPassword(), u.getPassword()))
                .orElseThrow(() -> new IllegalArgumentException("Usuario o contraseña incorrectos"));

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
        Usuario usuario = buscarPorId(usuarioId);
        return Boolean.TRUE.equals(usuario.getEsJefeCocina());
    }

    /**
     * Elimina un usuario del sistema, scoped al negocio del caller.
     * Solo los jefes de cocina pueden eliminar usuarios, y únicamente
     * empleados de SU PROPIO negocio.
     *
     * @param id ID del usuario a eliminar
     * @throws IllegalArgumentException     Si el que llama no es jefe de
     *                                       cocina, si el objetivo es otro
     *                                       jefe, o si intenta autoeliminarse
     * @throws RecursoNoEncontradoException  Si el usuario no existe o
     *                                        pertenece a otro negocio (fuga
     *                                        cross-tenant cerrada en Fase 8)
     */
    @Transactional
    public void eliminar(Long id) {
        Long usuarioId = SecurityUtils.getUsuarioId();
        Usuario chef = buscarPorId(usuarioId);

        if (!Boolean.TRUE.equals(chef.getEsJefeCocina())) {
            throw new IllegalArgumentException("Solo los jefes de cocina pueden eliminar usuarios");
        }

        Usuario usuario = usuarioRepository.findByIdAndNegocioId(id, SecurityUtils.getNegocioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

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

    /**
     * Actualiza el correo electrónico del usuario autenticado. Al ser el
     * email el identificador GLOBAL de login (único en todo el sistema desde
     * V3), se le aplican las mismas comprobaciones de unicidad que en
     * {@link #registrar(UsuarioRegisterRequest)} y
     * {@link #crearEmpleado(EmpleadoRegisterRequest)}, excluyendo el propio
     * email actual del usuario (reenviar el mismo email sin cambios es una
     * actualización no-op válida, no un duplicado).
     *
     * @param email Nuevo correo electrónico
     * @return Usuario con email actualizado
     * @throws IllegalArgumentException Si falta el email, o si ya pertenece
     *                                   a OTRO usuario del sistema
     */
    @Transactional
    public UsuarioResponse actualizarEmail(String email) {
        Long usuarioId = SecurityUtils.getUsuarioId();
        Usuario usuario = buscarPorId(usuarioId);

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("El correo electrónico es obligatorio");
        }

        if (!email.equals(usuario.getEmail()) && usuarioRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("El correo electrónico ya está en uso");
        }

        usuario.setEmail(email);
        Usuario usuarioActualizado = usuarioRepository.save(usuario);
        return new UsuarioResponse(usuarioActualizado);
    }
}