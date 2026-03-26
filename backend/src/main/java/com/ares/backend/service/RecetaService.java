package com.ares.backend.service;

import com.ares.backend.dto.*;
import com.ares.backend.entity.*;
import com.ares.backend.repository.RecetaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para la gestión de recetas.
 *
 * @author Ares
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class RecetaService {

    private final RecetaRepository recetaRepository;
    private final UsuarioService usuarioService;
    private final IngredienteService ingredienteService;
    private final RegistroUsoService registroUsoService;
    private final AlertaService alertaService;

    /**
     * Obtiene todas las recetas del sistema.
     *
     * @return Lista de recetas simplificadas
     */
    public List<RecetaDetailResponse> obtenerTodas() {
        return recetaRepository.findAll().stream()
                .map(receta -> new RecetaDetailResponse(receta, verificarDisponibilidad(receta)))
                .collect(Collectors.toList());
    }

    /**
     * Obtiene una receta por su ID con todos los detalles.
     *
     * @param id ID de la receta
     * @return Receta detallada
     * @throws IllegalArgumentException Si la receta no existe
     */
    public RecetaDetailResponse obtenerPorId(Long id) {
        Receta receta = recetaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Receta no encontrada"));
        return new RecetaDetailResponse(receta, verificarDisponibilidad(receta));
    }

    /**
     * Crea una nueva receta.
     * Solo los jefes de cocina pueden crear recetas.
     *
     * @param request Datos de la receta
     * @return Receta creada
     * @throws IllegalArgumentException Si el usuario no es jefe de cocina
     */
    @Transactional
    public RecetaDetailResponse crear(RecetaRequest request) {
        // Validar que el usuario sea jefe de cocina
        Usuario usuario = usuarioService.buscarPorId(request.getCreadaPorId());
        if (!Boolean.TRUE.equals(usuario.getEsJefeCocina())) {
            throw new IllegalArgumentException("Solo los jefes de cocina pueden crear recetas");
        }

        // Crear receta
        Receta receta = new Receta();
        receta.setNombre(request.getNombre());
        receta.setDescripcion(request.getDescripcion());
        receta.setCreadaPor(usuario);
        receta.setFechaCreacion(LocalDateTime.now());

        // Agregar pasos
        List<PasoReceta> pasos = new ArrayList<>();
        for (PasoRecetaDTO pasoDTO : request.getPasos()) {
            PasoReceta paso = new PasoReceta();
            paso.setOrden(pasoDTO.getOrden());
            paso.setDescripcion(pasoDTO.getDescripcion());
            paso.setReceta(receta);
            pasos.add(paso);
        }
        receta.setPasos(pasos);

        // Agregar ingredientes
        List<RecetaIngrediente> ingredientes = new ArrayList<>();
        for (RecetaIngredienteRequest ingredienteReq : request.getIngredientes()) {
            Ingrediente ingrediente = ingredienteService.buscarPorId(ingredienteReq.getIngredienteId());

            RecetaIngrediente recetaIngrediente = new RecetaIngrediente();
            recetaIngrediente.setReceta(receta);
            recetaIngrediente.setIngrediente(ingrediente);
            recetaIngrediente.setCantidadNecesaria(ingredienteReq.getCantidadNecesaria());
            ingredientes.add(recetaIngrediente);
        }
        receta.setIngredientes(ingredientes);

        Receta recetaGuardada = recetaRepository.save(receta);
        return new RecetaDetailResponse(recetaGuardada, verificarDisponibilidad(recetaGuardada));
    }

    /**
     * Actualiza una receta existente.
     * Solo los jefes de cocina pueden actualizar recetas.
     *
     * @param id ID de la receta
     * @param request Nuevos datos de la receta
     * @return Receta actualizada
     * @throws IllegalArgumentException Si la receta no existe o el usuario no es jefe de cocina
     */
    @Transactional
    public RecetaDetailResponse actualizar(Long id, RecetaRequest request) {
        // Validar que el usuario sea jefe de cocina
        Usuario usuario = usuarioService.buscarPorId(request.getCreadaPorId());
        if (!Boolean.TRUE.equals(usuario.getEsJefeCocina())) {
            throw new IllegalArgumentException("Solo los jefes de cocina pueden actualizar recetas");
        }

        Receta receta = recetaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Receta no encontrada"));

        receta.setNombre(request.getNombre());
        receta.setDescripcion(request.getDescripcion());

        // Actualizar pasos (eliminar los antiguos y crear nuevos)
        receta.getPasos().clear();
        for (PasoRecetaDTO pasoDTO : request.getPasos()) {
            PasoReceta paso = new PasoReceta();
            paso.setOrden(pasoDTO.getOrden());
            paso.setDescripcion(pasoDTO.getDescripcion());
            paso.setReceta(receta);
            receta.getPasos().add(paso);
        }

        // Actualizar ingredientes (eliminar los antiguos y crear nuevos)
        receta.getIngredientes().clear();
        for (RecetaIngredienteRequest ingredienteReq : request.getIngredientes()) {
            Ingrediente ingrediente = ingredienteService.buscarPorId(ingredienteReq.getIngredienteId());

            RecetaIngrediente recetaIngrediente = new RecetaIngrediente();
            recetaIngrediente.setReceta(receta);
            recetaIngrediente.setIngrediente(ingrediente);
            recetaIngrediente.setCantidadNecesaria(ingredienteReq.getCantidadNecesaria());
            receta.getIngredientes().add(recetaIngrediente);
        }

        Receta recetaGuardada = recetaRepository.save(receta);
        return new RecetaDetailResponse(recetaGuardada, verificarDisponibilidad(recetaGuardada));
    }

    /**
     * Elimina una receta.
     * Solo los jefes de cocina pueden eliminar recetas.
     *
     * @param id ID de la receta
     * @param usuarioId ID del usuario que elimina
     * @throws IllegalArgumentException Si la receta no existe o el usuario no es jefe de cocina
     */
    @Transactional
    public void eliminar(Long id, Long usuarioId) {
        // Validar que el usuario sea jefe de cocina
        if (!usuarioService.esJefeCocina(usuarioId)) {
            throw new IllegalArgumentException("Solo los jefes de cocina pueden eliminar recetas");
        }

        Receta receta = recetaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Receta no encontrada"));

        // Limpiar registros relacionados para evitar fallos por FK
        alertaService.eliminarPorReceta(receta);
        registroUsoService.eliminarPorReceta(receta);

        recetaRepository.delete(receta);
    }

    /**
     * Verifica si una receta está disponible (hay stock suficiente).
     *
     * @param id ID de la receta
     * @param request Datos de verificación
     * @return Resultado de la verificación
     * @throws IllegalArgumentException Si la receta no existe
     */
    public VerificarRecetaResponse verificarDisponibilidad(Long id, VerificarRecetaRequest request) {
        Receta receta = recetaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Receta no encontrada"));

        List<IngredienteFaltanteDTO> ingredientesFaltantes = new ArrayList<>();
        boolean disponible = true;

        for (RecetaIngrediente recetaIngrediente : receta.getIngredientes()) {
            Ingrediente ingrediente = recetaIngrediente.getIngrediente();
            Double cantidadNecesaria = recetaIngrediente.getCantidadNecesaria();
            Double cantidadDisponible = ingrediente.getCantidad();

            if (cantidadDisponible < cantidadNecesaria) {
                disponible = false;
                ingredientesFaltantes.add(new IngredienteFaltanteDTO(
                        new IngredienteResponse(ingrediente),
                        cantidadNecesaria,
                        cantidadDisponible
                ));
            }
        }

        // Si no está disponible, crear alerta
        if (!disponible) {
            alertaService.crearAlertaRecetaNoDisponible(receta, ingredientesFaltantes);
        }

        return new VerificarRecetaResponse(id, disponible, ingredientesFaltantes);
    }

    /**
     * Elabora una receta (reduce el stock de ingredientes).
     *
     * @param id ID de la receta
     * @param request Datos de elaboración
     * @return Registro de uso creado
     * @throws IllegalArgumentException Si la receta no existe o no hay stock suficiente
     */
    @Transactional
    public RegistroUsoResponse elaborar(Long id, ElaborarRecetaRequest request) {
        Receta receta = recetaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Receta no encontrada"));

        Usuario usuario = usuarioService.buscarPorId(request.getUsuarioId());

        // Verificar disponibilidad
        VerificarRecetaResponse verificacion = verificarDisponibilidad(id, new VerificarRecetaRequest(request.getUsuarioId()));
        if (!verificacion.getDisponible()) {
            //Guardar registro fallido y lanzar excepción
            registroUsoService.crear(receta, usuario, false);
            throw new IllegalArgumentException("No hay stock suficiente para elaborar esta receta");
        }

        // Reducir stock de ingredientes
        for (RecetaIngrediente recetaIngrediente : receta.getIngredientes()) {
            ingredienteService.reducirCantidad(
                    recetaIngrediente.getIngrediente(),
                    recetaIngrediente.getCantidadNecesaria()
            );
        }

        // Crear registro de uso
        return registroUsoService.crear(receta, usuario, true);
    }

    /**
     * Obtiene los pasos de una receta.
     *
     * @param id ID de la receta
     * @return Lista de pasos ordenados
     * @throws IllegalArgumentException Si la receta no existe
     */
    public List<PasoRecetaDTO> obtenerPasos(Long id) {
        Receta receta = recetaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Receta no encontrada"));

        return receta.getPasos().stream()
                .sorted((p1, p2) -> p1.getOrden().compareTo(p2.getOrden()))
                .map(PasoRecetaDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Verifica si una receta está disponible (método interno).
     *
     * @param receta Receta a verificar
     * @return true si está disponible, false en caso contrario
     */
    private boolean verificarDisponibilidad(Receta receta) {
        for (RecetaIngrediente recetaIngrediente : receta.getIngredientes()) {
            if (recetaIngrediente.getIngrediente().getCantidad() < recetaIngrediente.getCantidadNecesaria()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Busca una receta por su ID (método interno).
     *
     * @param id ID de la receta
     * @return Receta encontrada
     * @throws IllegalArgumentException Si la receta no existe
     */
    public Receta buscarPorId(Long id) {
        return recetaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Receta no encontrada con ID: " + id));
    }
}