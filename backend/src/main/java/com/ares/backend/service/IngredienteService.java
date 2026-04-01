package com.ares.backend.service;

import com.ares.backend.config.SecurityUtils;
import com.ares.backend.dto.IngredienteRequest;
import com.ares.backend.dto.IngredienteResponse;
import com.ares.backend.dto.IngredienteUpdateRequest;
import com.ares.backend.entity.Ingrediente;
import com.ares.backend.entity.Usuario;
import com.ares.backend.repository.IngredienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para la gestión de ingredientes.
 *
 * @author Ares
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class IngredienteService {

    private final IngredienteRepository ingredienteRepository;
    private final AlertaService alertaService;
    private final UsuarioService usuarioService;
    private final MovimientoStockService movimientoStockService;

    /**
     * Obtiene todos los ingredientes del sistema.
     *
     * @return Lista de ingredientes
     */
    public List<IngredienteResponse> obtenerTodos() {
        return ingredienteRepository.findAll().stream()
                .map(IngredienteResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene un ingrediente por su ID.
     *
     * @param id ID del ingrediente
     * @return Ingrediente encontrado
     * @throws IllegalArgumentException Si el ingrediente no existe
     */
    public IngredienteResponse obtenerPorId(Long id) {
        Ingrediente ingrediente = ingredienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ingrediente no encontrado"));
        return new IngredienteResponse(ingrediente);
    }

    /**
     * Crea un nuevo ingrediente.
     *
     * @param request Datos del ingrediente
     * @return Ingrediente creado
     */
    @Transactional
    public IngredienteResponse crear(IngredienteRequest request) {
        // Validar que no exista un ingrediente con el mismo nombre
        if (ingredienteRepository.existsByNombreIgnoreCase(request.getNombre())) {
            throw new IllegalArgumentException("Ya existe un ingrediente con ese nombre");
        }
        Long usuarioId = SecurityUtils.getUsuarioId();

        Ingrediente ingrediente = new Ingrediente();
        ingrediente.setNombre(request.getNombre());
        ingrediente.setCantidad(request.getCantidad());
        ingrediente.setUnidadMedida(request.getUnidadMedida());
        ingrediente.setStockMinimo(request.getStockMinimo());
        ingrediente.setFechaActualizacion(LocalDateTime.now());

        Ingrediente ingredienteGuardado = ingredienteRepository.save(ingrediente);

        //Registrar el movimiento del stock
        movimientoStockService.registrarMovimiento(ingrediente, 0.0, request.getCantidad(), "ENTRADA", "Creación de ingrediente", usuarioId);

        // Verificar si hay stock bajo y crear alerta
        if (ingredienteGuardado.tieneStockBajo()) {
            alertaService.crearAlertaStockBajo(ingredienteGuardado);
        }

        return new IngredienteResponse(ingredienteGuardado);
    }

    /**
     * Actualiza un ingrediente existente.
     *
     * @param id ID del ingrediente
     * @param request Nuevos datos del ingrediente
     * @return Ingrediente actualizado
     * @throws IllegalArgumentException Si el ingrediente no existe
     */
    @Transactional
    public IngredienteResponse actualizar(Long id, IngredienteRequest request) {
        Ingrediente ingrediente = ingredienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ingrediente no encontrado"));

        // Guardar cantidad anterior para detectar descenso y generar escaldaio si aplica
        Double anterior = ingrediente.getCantidad();

        // Validar que no exista otro ingrediente con el mismo nombre
        if (!ingrediente.getNombre().equalsIgnoreCase(request.getNombre()) &&
                ingredienteRepository.existsByNombreIgnoreCase(request.getNombre())) {
            throw new IllegalArgumentException("Ya existe un ingrediente con ese nombre");
        }

        ingrediente.setNombre(request.getNombre());
        ingrediente.setCantidad(request.getCantidad());
        ingrediente.setUnidadMedida(request.getUnidadMedida());
        ingrediente.setStockMinimo(request.getStockMinimo());
        ingrediente.setFechaActualizacion(LocalDateTime.now());

        Ingrediente ingredienteGuardado = ingredienteRepository.save(ingrediente);

        // Verificar si hay stock bajo y crear alerta
        if (ingredienteGuardado.tieneStockBajo()) {
            alertaService.crearAlertaStockBajo(ingredienteGuardado);
        }

        // Nueva alerta escaldaio si el stock desciende respecto al anterior
        if (ingredienteGuardado.getCantidad() < anterior) {
            alertaService.crearAlertaEscaldaio(ingredienteGuardado, anterior, ingredienteGuardado.getCantidad());
        }

        return new IngredienteResponse(ingredienteGuardado);
    }

    /**
     * Actualiza solo la cantidad de un ingrediente.
     *
     * @param id ID del ingrediente
     * @param request Nueva cantidad
     * @return Ingrediente actualizado
     * @throws IllegalArgumentException Si el ingrediente no existe
     */
    @Transactional
    public IngredienteResponse actualizarCantidad(Long id, IngredienteUpdateRequest request) {
        Long usuarioId = SecurityUtils.getUsuarioId();
        Ingrediente ingrediente = ingredienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ingrediente no encontrado"));
        Double anterior = ingrediente.getCantidad();
        String tipo = request.getCantidad() > anterior ? "ENTRADA" : "SALIDA";

        ingrediente.setCantidad(request.getCantidad());
        ingrediente.setFechaActualizacion(LocalDateTime.now());

        movimientoStockService.registrarMovimiento(ingrediente,anterior,request.getCantidad(), tipo, "Actualización manual", usuarioId);

        Ingrediente ingredienteGuardado = ingredienteRepository.save(ingrediente);

        // Nueva alerta escaldaio si el stock desciende respecto al anterior
        if (ingredienteGuardado.getCantidad() < anterior) {
            alertaService.crearAlertaEscaldaio(ingredienteGuardado, anterior, ingredienteGuardado.getCantidad());
        }

        // Verificar si hay stock bajo y crear alerta
        if (ingredienteGuardado.tieneStockBajo()) {
            alertaService.crearAlertaStockBajo(ingredienteGuardado);
        }

        return new IngredienteResponse(ingredienteGuardado);
    }

    /**
     * Elimina un ingrediente.
     * Solo los jefes de cocina pueden eliminar ingredientes.
     *
     * @param id ID del ingrediente
     * @throws IllegalArgumentException Si el ingrediente no existe o el usuario no es jefe de cocina
     */
    @Transactional
    public void eliminar(Long id) {
        // Validar que el usuario es Jefe de Cocina
        if (!usuarioService.esJefeCocina()) {
            throw new IllegalArgumentException("Solo los jefes de cocina pueden eliminar ingredientes");
        }

        Ingrediente ingrediente = ingredienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ingrediente no encontrado"));

        // Limpiar alertas relacionadas
        alertaService.eliminarPorIngrediente(ingrediente);

        ingredienteRepository.delete(ingrediente);
    }

    /**
     * Obtiene todos los ingredientes con stock bajo.
     *
     * @return Lista de ingredientes con stock bajo
     */
    public List<IngredienteResponse> obtenerConStockBajo() {
        return ingredienteRepository.findAll().stream()
                .filter(Ingrediente::tieneStockBajo)
                .map(IngredienteResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Busca un ingrediente por su ID (método interno).
     *
     * @param id ID del ingrediente
     * @return Ingrediente encontrado
     * @throws IllegalArgumentException Si el ingrediente no existe
     */
    public Ingrediente buscarPorId(Long id) {
        return ingredienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ingrediente no encontrado con ID: " + id));
    }

    /**
     * Reduce la cantidad de un ingrediente.
     *
     * @param ingrediente Ingrediente a reducir
     * @param cantidad Cantidad a reducir
     */
    @Transactional
    public void reducirCantidad(Ingrediente ingrediente, Double cantidad) {
        Long usuarioId = SecurityUtils.getUsuarioId();
        Double anterior = ingrediente.getCantidad();
        ingrediente.setCantidad(anterior - cantidad);
        ingrediente.setFechaActualizacion(LocalDateTime.now());

        Ingrediente ingredienteGuardado = ingredienteRepository.save(ingrediente);

        movimientoStockService.registrarMovimiento(ingrediente, anterior, ingrediente.getCantidad(), "SALIDA", "Elaboración de receta", usuarioId);

        // Verificar si hay stock bajo y crear alerta
        if (ingredienteGuardado.tieneStockBajo()) {
            alertaService.crearAlertaStockBajo(ingredienteGuardado);
        }
    }
}
