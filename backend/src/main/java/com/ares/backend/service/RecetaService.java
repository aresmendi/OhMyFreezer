package com.ares.backend.service;

import com.ares.backend.config.SecurityUtils;
import com.ares.backend.dto.*;
import com.ares.backend.entity.*;
import com.ares.backend.exception.RecursoNoEncontradoException;
import com.ares.backend.repository.NegocioRepository;
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
    private final NegocioRepository negocioRepository;

    /**
     * Obtiene todas las recetas del sistema.
     * Usa dos queries separadas para evitar MultipleBagFetchException.
     *
     * @return Lista de recetas
     */
    @Transactional(readOnly = true)
    public List<RecetaDetailResponse> obtenerTodas() {
        List<Receta> recetas = recetaRepository.findAllWithIngredientes();
        recetaRepository.findAllWithPasos(); // carga pasos en el persistence context
        return recetas.stream()
                .map(receta -> new RecetaDetailResponse(receta, tieneStockSuficiente(receta)))
                .collect(Collectors.toList());
    }

    /**
     * Obtiene una receta por su ID con todos los detalles, scoped al negocio del caller.
     * Usa dos queries separadas para evitar MultipleBagFetchException.
     *
     * @param id ID de la receta
     * @return Receta detallada
     * @throws RecursoNoEncontradoException Si la receta no existe o pertenece a otro negocio
     */
    @Transactional(readOnly = true)
    public RecetaDetailResponse obtenerPorId(Long id) {
        Long negocioId = SecurityUtils.getNegocioId();
        Receta receta = recetaRepository.findByIdWithIngredientesAndNegocioId(id, negocioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Receta no encontrada"));
        recetaRepository.findByIdWithPasosAndNegocioId(id, negocioId); // carga pasos en el persistence context
        return new RecetaDetailResponse(receta, tieneStockSuficiente(receta));
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
        Long usuarioId = SecurityUtils.getUsuarioId();
        Usuario usuario = usuarioService.buscarPorId(usuarioId);
        if (!Boolean.TRUE.equals(usuario.getEsJefeCocina())) {
            throw new IllegalArgumentException("Solo los jefes de cocina pueden crear recetas");
        }

        // Crear receta — el negocio (tenant) se deriva siempre del caller autenticado
        Negocio negocio = negocioRepository.findById(SecurityUtils.getNegocioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Negocio no encontrado"));

        Receta receta = new Receta();
        receta.setNombre(request.getNombre());
        receta.setDescripcion(request.getDescripcion());
        receta.setCreadaPor(usuario);
        receta.setFechaCreacion(LocalDateTime.now());
        receta.setNegocio(negocio);

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
        return new RecetaDetailResponse(recetaGuardada, tieneStockSuficiente(recetaGuardada));
    }

    /**
     * Actualiza una receta existente.
     * Solo los jefes de cocina pueden actualizar recetas.
     *
     * @param id ID de la receta
     * @param request Nuevos datos de la receta
     * @return Receta actualizada
     * @throws IllegalArgumentException Si el usuario no es jefe de cocina
     * @throws RecursoNoEncontradoException Si la receta no existe o pertenece a otro negocio
     */
    @Transactional
    public RecetaDetailResponse actualizar(Long id, RecetaRequest request) {
        // Validar que el usuario sea jefe de cocina
        Long usuarioId = SecurityUtils.getUsuarioId();
        Usuario usuario = usuarioService.buscarPorId(usuarioId);
        if (!Boolean.TRUE.equals(usuario.getEsJefeCocina())) {
            throw new IllegalArgumentException("Solo los jefes de cocina pueden actualizar recetas");
        }

        Receta receta = recetaRepository.findByIdAndNegocioId(id, SecurityUtils.getNegocioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Receta no encontrada"));

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
        return new RecetaDetailResponse(recetaGuardada, tieneStockSuficiente(recetaGuardada));
    }

    /**
     * Elimina una receta.
     * Solo los jefes de cocina pueden eliminar recetas.
     *
     * @param id ID de la receta
     * @throws IllegalArgumentException Si el usuario no es jefe de cocina
     * @throws RecursoNoEncontradoException Si la receta no existe o pertenece a otro negocio
     */
    @Transactional
    public void eliminar(Long id) {
        // Validar que el usuario sea jefe de cocina
        if (!usuarioService.esJefeCocina()) {
            throw new IllegalArgumentException("Solo los jefes de cocina pueden eliminar recetas");
        }

        Receta receta = recetaRepository.findByIdAndNegocioId(id, SecurityUtils.getNegocioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Receta no encontrada"));

        // Limpiar registros relacionados para evitar fallos por FK
        alertaService.eliminarPorReceta(receta);
        registroUsoService.eliminarPorReceta(receta);

        recetaRepository.delete(receta);
    }

    /**
     * Verifica si una receta está disponible y envía alerta si no hay stock suficiente.
     *
     * @param id ID de la receta
     * @return Resultado de la verificación con lista de ingredientes faltantes
     * @throws RecursoNoEncontradoException Si la receta no existe o pertenece a otro negocio
     */
    public VerificarRecetaResponse verificarDisponibilidadYNotificar(Long id) {
        Receta receta = recetaRepository.findByIdAndNegocioId(id, SecurityUtils.getNegocioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Receta no encontrada"));

        List<IngredienteFaltanteDTO> ingredientesFaltantes = obtenerIngredientesFaltantes(receta);

        if (!ingredientesFaltantes.isEmpty()) {
            alertaService.crearAlertaRecetaNoDisponible(receta, ingredientesFaltantes);
        }

        return new VerificarRecetaResponse(id, ingredientesFaltantes.isEmpty(), ingredientesFaltantes);
    }

    /**
     * Elabora una receta (reduce el stock de ingredientes).
     *
     * @param id ID de la receta
     * @param request Datos de elaboración
     * @return Registro de uso creado
     * @throws IllegalArgumentException Si no hay stock suficiente
     * @throws RecursoNoEncontradoException Si la receta no existe o pertenece a otro negocio
     */
    @Transactional
    public RegistroUsoResponse elaborar(Long id, ElaborarRecetaRequest request) {
        Receta receta = recetaRepository.findByIdAndNegocioId(id, SecurityUtils.getNegocioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Receta no encontrada"));

        Long usuarioId = SecurityUtils.getUsuarioId();
        Usuario usuario = usuarioService.buscarPorId(usuarioId);

        // Si el usuario indica explícitamente que fue fallido
        if (Boolean.FALSE.equals(request.getCompletada())) {
            return registroUsoService.crear(receta, false);
        }

        // Si completada es null o true, verificar stock antes de descontar
        VerificarRecetaResponse verificacion = verificarDisponibilidadYNotificar(id);
        if (!verificacion.getDisponible()) {
            //Guardar registro fallido y lanzar excepción
            registroUsoService.crear(receta, false);
            alertaService.crearAlertaRecetaNoDisponible(receta, verificacion.getIngredientesFaltantes());
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
        return registroUsoService.crear(receta, true);
    }

    /**
     * Obtiene los pasos de una receta.
     *
     * @param id ID de la receta
     * @return Lista de pasos ordenados
     * @throws RecursoNoEncontradoException Si la receta no existe o pertenece a otro negocio
     */
    public List<PasoRecetaDTO> obtenerPasos(Long id) {
        Receta receta = recetaRepository.findByIdAndNegocioId(id, SecurityUtils.getNegocioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Receta no encontrada"));

        return receta.getPasos().stream()
                .sorted((p1, p2) -> p1.getOrden().compareTo(p2.getOrden()))
                .map(PasoRecetaDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Indica si la receta tiene stock suficiente de todos sus ingredientes (método interno).
     *
     * @param receta Receta a verificar
     * @return true si tiene stock, false si falta alguno
     */
    private boolean tieneStockSuficiente(Receta receta) {
        return obtenerIngredientesFaltantes(receta).isEmpty();
    }

    /**
     * Obtiene la lista de ingredientes faltantes para una receta (método interno compartido).
     *
     * @param receta Receta a verificar
     * @return Lista de ingredientes con cantidad insuficiente
     */
    private List<IngredienteFaltanteDTO> obtenerIngredientesFaltantes(Receta receta) {
        List<IngredienteFaltanteDTO> faltantes = new ArrayList<>();
        for (RecetaIngrediente recetaIngrediente : receta.getIngredientes()) {
            Ingrediente ingrediente = recetaIngrediente.getIngrediente();
            Double cantidadNecesaria = recetaIngrediente.getCantidadNecesaria();
            Double cantidadDisponible = ingrediente.getCantidad();
            if (cantidadDisponible < cantidadNecesaria) {
                faltantes.add(new IngredienteFaltanteDTO(
                        new IngredienteResponse(ingrediente),
                        cantidadNecesaria,
                        cantidadDisponible
                ));
            }
        }
        return faltantes;
    }

    /**
     * Busca una receta por su ID (método interno).
     * Carga ingredientes y pasos en queries separadas para evitar MultipleBagFetchException.
     *
     * @param id ID de la receta
     * @return Receta encontrada
     * @throws RecursoNoEncontradoException Si la receta no existe o pertenece a otro negocio
     */
    @Transactional(readOnly = true)
    public Receta buscarPorId(Long id) {
        Long negocioId = SecurityUtils.getNegocioId();
        Receta receta = recetaRepository.findByIdWithIngredientesAndNegocioId(id, negocioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Receta no encontrada con ID: " + id));
        recetaRepository.findByIdWithPasosAndNegocioId(id, negocioId); // carga pasos en el persistence context
        return receta;
    }
}