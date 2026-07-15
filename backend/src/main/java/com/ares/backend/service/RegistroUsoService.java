package com.ares.backend.service;

import com.ares.backend.config.SecurityUtils;
import com.ares.backend.dto.RegistroUsoResponse;
import com.ares.backend.entity.Negocio;
import com.ares.backend.entity.Receta;
import com.ares.backend.entity.RegistroUsoReceta;
import com.ares.backend.entity.Usuario;
import com.ares.backend.exception.RecursoNoEncontradoException;
import com.ares.backend.repository.NegocioRepository;
import com.ares.backend.repository.RegistroUsoRecetaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para la gestión de registros de uso de recetas.
 *
 * @author Ares
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class RegistroUsoService {

    private final RegistroUsoRecetaRepository registroUsoRecetaRepository;
    private final UsuarioService usuarioService;
    private final NegocioRepository negocioRepository;

    /**
     * Crea un nuevo registro de uso de receta. El negocio (tenant) se
     * deriva siempre del caller autenticado, nunca del request — cierra el
     * hueco DEFAULT 1 de la migración V2 para registro_uso_recetas.
     *
     * @param receta Receta elaborada
     * @return Registro de uso creado
     */
    @Transactional
    public RegistroUsoResponse crear(Receta receta, boolean completada) {
        Long usuarioId = SecurityUtils.getUsuarioId();
        Usuario usuario = usuarioService.buscarPorId(usuarioId);
        Negocio negocio = negocioRepository.findById(SecurityUtils.getNegocioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Negocio no encontrado"));

        RegistroUsoReceta registro = new RegistroUsoReceta();
        registro.setReceta(receta);
        registro.setUsuario(usuario);
        registro.setFechaElaboracion(LocalDateTime.now());
        registro.setCompletada(completada);
        registro.setNegocio(negocio);

        RegistroUsoReceta registroGuardado = registroUsoRecetaRepository.save(registro);
        return new RegistroUsoResponse(registroGuardado);
    }

    /**
     * Obtiene todos los registros de uso.
     *
     * @return Lista de registros de uso
     */
    public List<RegistroUsoResponse> obtenerTodos() {
        return registroUsoRecetaRepository.findAll().stream()
                .map(RegistroUsoResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene los registros de uso de una receta específica, scoped al
     * negocio del caller. El recetaId llega del request del cliente: sin
     * este scoping un caller podía consultar el histórico de una receta de
     * OTRO negocio.
     *
     * @param recetaId ID de la receta
     * @return Lista de registros de uso de la receta en el negocio del caller
     */
    public List<RegistroUsoResponse> obtenerPorReceta(Long recetaId) {
        return registroUsoRecetaRepository.findByRecetaIdAndNegocioId(recetaId, SecurityUtils.getNegocioId()).stream()
                .map(RegistroUsoResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene los registros de uso de un usuario específico.
     * @return Lista de registros de uso del usuario
     */
    public List<RegistroUsoResponse> obtenerPorUsuario() {
        Long usuarioId = SecurityUtils.getUsuarioId();
        Usuario usuario = usuarioService.buscarPorId(usuarioId);
        return registroUsoRecetaRepository.findByUsuarioId(usuarioId).stream()
                .map(RegistroUsoResponse::new)
                .collect(Collectors.toList());
    }
    /**
     * Elimina todos los registros de uso asociados a una receta.
     *
     * @param receta Receta cuyos registros de uso se eliminarán
     */
    @Transactional
    public void eliminarPorReceta(Receta receta) {
        registroUsoRecetaRepository.deleteByReceta(receta);
    }
}