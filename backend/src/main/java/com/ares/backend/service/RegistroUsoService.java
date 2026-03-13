package com.ares.backend.service;

import com.ares.backend.dto.RegistroUsoResponse;
import com.ares.backend.entity.Receta;
import com.ares.backend.entity.RegistroUsoReceta;
import com.ares.backend.entity.Usuario;
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

    /**
     * Crea un nuevo registro de uso de receta.
     *
     * @param receta Receta elaborada
     * @param usuario Usuario que elaboró la receta
     * @return Registro de uso creado
     */
    @Transactional
    public RegistroUsoResponse crear(Receta receta, Usuario usuario, boolean completada) {
        RegistroUsoReceta registro = new RegistroUsoReceta();
        registro.setReceta(receta);
        registro.setUsuario(usuario);
        registro.setFechaElaboracion(LocalDateTime.now());
        registro.setCompletada(completada);

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
     * Obtiene los registros de uso de una receta específica.
     *
     * @param recetaId ID de la receta
     * @return Lista de registros de uso de la receta
     */
    public List<RegistroUsoResponse> obtenerPorReceta(Long recetaId) {
        return registroUsoRecetaRepository.findByRecetaId(recetaId).stream()
                .map(RegistroUsoResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene los registros de uso de un usuario específico.
     *
     * @param usuarioId ID del usuario
     * @return Lista de registros de uso del usuario
     */
    public List<RegistroUsoResponse> obtenerPorUsuario(Long usuarioId) {
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