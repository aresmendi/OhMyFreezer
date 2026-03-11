package com.ares.backend.service;

import com.ares.backend.dto.IngredienteRequest;
import com.ares.backend.dto.IngredienteResponse;
import com.ares.backend.dto.IngredienteUpdateRequest;
import com.ares.backend.entity.Ingrediente;
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

        Ingrediente ingrediente = new Ingrediente();
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
        Ingrediente ingrediente = ingredienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ingrediente no encontrado"));

        ingrediente.setCantidad(request.getCantidad());
        ingrediente.setFechaActualizacion(LocalDateTime.now());

        Ingrediente ingredienteGuardado = ingredienteRepository.save(ingrediente);

        // Verificar si hay stock bajo y crear alerta
        if (ingredienteGuardado.tieneStockBajo()) {
            alertaService.crearAlertaStockBajo(ingredienteGuardado);
        }

        return new IngredienteResponse(ingredienteGuardado);
    }

    /**
     * Elimina un ingrediente.
     *
     * @param id ID del ingrediente
     * @throws IllegalArgumentException Si el ingrediente no existe
     */
    @Transactional
    public void eliminar(Long id) {
        if (!ingredienteRepository.existsById(id)) {
            throw new IllegalArgumentException("Ingrediente no encontrado");
        }
        ingredienteRepository.deleteById(id);
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
        ingrediente.setCantidad(ingrediente.getCantidad() - cantidad);
        ingrediente.setFechaActualizacion(LocalDateTime.now());

        Ingrediente ingredienteGuardado = ingredienteRepository.save(ingrediente);

        // Verificar si hay stock bajo y crear alerta
        if (ingredienteGuardado.tieneStockBajo()) {
            alertaService.crearAlertaStockBajo(ingredienteGuardado);
        }
    }
}