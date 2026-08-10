package com.ares.backend.service;

import com.ares.backend.config.SecurityUtils;
import com.ares.backend.dto.IngredienteRequest;
import com.ares.backend.dto.IngredienteResponse;
import com.ares.backend.dto.IngredienteUpdateRequest;
import com.ares.backend.entity.Ingrediente;
import com.ares.backend.entity.Negocio;
import com.ares.backend.entity.UnidadMedida;
import com.ares.backend.exception.RecursoNoEncontradoException;
import com.ares.backend.repository.IngredienteRepository;
import com.ares.backend.repository.NegocioRepository;
import com.ares.backend.repository.UnidadMedidaRepository;
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
    private final NegocioRepository negocioRepository;
    private final UnidadMedidaRepository unidadMedidaRepository;

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
     * Obtiene un ingrediente por su ID, scoped al negocio del caller.
     *
     * @param id ID del ingrediente
     * @return Ingrediente encontrado
     * @throws RecursoNoEncontradoException Si el ingrediente no existe o pertenece a otro negocio
     */
    public IngredienteResponse obtenerPorId(Long id) {
        Ingrediente ingrediente = ingredienteRepository.findByIdAndNegocioId(id, SecurityUtils.getNegocioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Ingrediente no encontrado"));
        return new IngredienteResponse(ingrediente);
    }

    /**
     * Crea un nuevo ingrediente. El negocio (tenant) del ingrediente se
     * deriva siempre del caller autenticado, nunca del request.
     *
     * @param request Datos del ingrediente
     * @return Ingrediente creado
     */
    @Transactional
    public IngredienteResponse crear(IngredienteRequest request) {
        Long negocioId = SecurityUtils.getNegocioId();

        // Validar que no exista un ingrediente con el mismo nombre EN ESTE NEGOCIO
        if (ingredienteRepository.existsByNombreIgnoreCaseAndNegocioId(request.getNombre(), negocioId)) {
            throw new IllegalArgumentException("Ya existe un ingrediente con ese nombre");
        }
        Long usuarioId = SecurityUtils.getUsuarioId();
        Negocio negocio = negocioRepository.findById(negocioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Negocio no encontrado"));
        UnidadMedida unidad = resolverUnidadBase(request.getUnidadBaseId(), request.getUnidadMedida());

        Ingrediente ingrediente = new Ingrediente();
        ingrediente.setNombre(request.getNombre());
        ingrediente.setCantidad(request.getCantidad());
        ingrediente.setUnidadBase(unidad);
        ingrediente.setUnidadMedida(unidad.getCodigo());
        ingrediente.setStockMinimo(request.getStockMinimo());
        ingrediente.setFechaActualizacion(LocalDateTime.now());
        ingrediente.setNegocio(negocio);

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
     * Actualiza un ingrediente existente, scoped al negocio del caller.
     *
     * @param id ID del ingrediente
     * @param request Nuevos datos del ingrediente
     * @return Ingrediente actualizado
     * @throws RecursoNoEncontradoException Si el ingrediente no existe o pertenece a otro negocio
     */
    @Transactional
    public IngredienteResponse actualizar(Long id, IngredienteRequest request) {
        Long negocioId = SecurityUtils.getNegocioId();
        Ingrediente ingrediente = ingredienteRepository.findByIdAndNegocioId(id, negocioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Ingrediente no encontrado"));

        // Guardar cantidad anterior para detectar descenso y generar escaldaio si aplica
        Double anterior = ingrediente.getCantidad();

        // Validar que no exista otro ingrediente con el mismo nombre EN ESTE NEGOCIO
        if (!ingrediente.getNombre().equalsIgnoreCase(request.getNombre()) &&
                ingredienteRepository.existsByNombreIgnoreCaseAndNegocioId(request.getNombre(), negocioId)) {
            throw new IllegalArgumentException("Ya existe un ingrediente con ese nombre");
        }

        UnidadMedida unidad = resolverUnidadBase(request.getUnidadBaseId(), request.getUnidadMedida());

        ingrediente.setNombre(request.getNombre());
        ingrediente.setCantidad(request.getCantidad());
        ingrediente.setUnidadBase(unidad);
        ingrediente.setUnidadMedida(unidad.getCodigo());
        ingrediente.setStockMinimo(request.getStockMinimo());
        ingrediente.setFechaActualizacion(LocalDateTime.now());

        Ingrediente ingredienteGuardado = ingredienteRepository.save(ingrediente);

        // Verificar si hay stock bajo y crear alerta
        if (ingredienteGuardado.tieneStockBajo()) {
            alertaService.crearAlertaStockBajo(ingredienteGuardado);
        }

        // Nueva alerta escaldaio si el stock desciende respecto al anterior
        if (ingredienteGuardado.getCantidad() < anterior) {
            alertaService.crearAlertaMerma(ingredienteGuardado, anterior, ingredienteGuardado.getCantidad());
        }

        return new IngredienteResponse(ingredienteGuardado);
    }

    /**
     * Actualiza solo la cantidad de un ingrediente, scoped al negocio del caller.
     *
     * @param id ID del ingrediente
     * @param request Nueva cantidad
     * @return Ingrediente actualizado
     * @throws RecursoNoEncontradoException Si el ingrediente no existe o pertenece a otro negocio
     */
    @Transactional
    public IngredienteResponse actualizarCantidad(Long id, IngredienteUpdateRequest request) {
        Long usuarioId = SecurityUtils.getUsuarioId();
        Ingrediente ingrediente = ingredienteRepository.findByIdAndNegocioId(id, SecurityUtils.getNegocioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Ingrediente no encontrado"));
        Double anterior = ingrediente.getCantidad();
        String tipo = request.getCantidad() > anterior ? "ENTRADA" : "SALIDA";

        ingrediente.setCantidad(request.getCantidad());
        ingrediente.setFechaActualizacion(LocalDateTime.now());

        movimientoStockService.registrarMovimiento(ingrediente,anterior,request.getCantidad(), tipo, "Actualización manual", usuarioId);

        Ingrediente ingredienteGuardado = ingredienteRepository.save(ingrediente);

        // Nueva alerta escaldaio si el stock desciende respecto al anterior
        if (ingredienteGuardado.getCantidad() < anterior) {
            alertaService.crearAlertaMerma(ingredienteGuardado, anterior, ingredienteGuardado.getCantidad());
        }

        // Verificar si hay stock bajo y crear alerta
        if (ingredienteGuardado.tieneStockBajo()) {
            alertaService.crearAlertaStockBajo(ingredienteGuardado);
        }

        return new IngredienteResponse(ingredienteGuardado);
    }

    /**
     * Elimina un ingrediente, scoped al negocio del caller.
     * Solo los jefes de cocina pueden eliminar ingredientes.
     *
     * @param id ID del ingrediente
     * @throws IllegalArgumentException Si el usuario no es jefe de cocina
     * @throws RecursoNoEncontradoException Si el ingrediente no existe o pertenece a otro negocio
     */
    @Transactional
    public void eliminar(Long id) {
        // Validar que el usuario es Jefe de Cocina
        if (!usuarioService.esJefeCocina()) {
            throw new IllegalArgumentException("Solo los jefes de cocina pueden eliminar ingredientes");
        }

        Ingrediente ingrediente = ingredienteRepository.findByIdAndNegocioId(id, SecurityUtils.getNegocioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Ingrediente no encontrado"));

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
     * Busca un ingrediente por su ID (método interno), scoped al negocio del caller.
     *
     * @param id ID del ingrediente
     * @return Ingrediente encontrado
     * @throws RecursoNoEncontradoException Si el ingrediente no existe o pertenece a otro negocio
     */
    public Ingrediente buscarPorId(Long id) {
        return ingredienteRepository.findByIdAndNegocioId(id, SecurityUtils.getNegocioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Ingrediente no encontrado con ID: " + id));
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

    /**
     * Resuelve la unidad de medida base de un ingrediente a partir del
     * request (Fase 2 "unidades-medida"). Orden de resolución:
     * 1. {@code unidadBaseId}, si viene informado, gana siempre.
     * 2. Si no, se resuelve {@code unidadMedida} (código legado) por
     *    coincidencia EXACTA de {@code codigo} en el catálogo global.
     * 3. Si ninguno resuelve, falla con 400 — nunca se asume una unidad por
     *    defecto.
     *
     * @param unidadBaseId  id preferido de la unidad (catálogo global)
     * @param unidadMedida  código legado de texto libre (ej. "kg")
     * @return la unidad de medida resuelta
     * @throws RecursoNoEncontradoException si {@code unidadBaseId} no existe en el catálogo
     * @throws IllegalArgumentException si no se informa ningún campo, o el código legado es desconocido
     */
    private UnidadMedida resolverUnidadBase(Long unidadBaseId, String unidadMedida) {
        if (unidadBaseId != null) {
            return unidadMedidaRepository.findById(unidadBaseId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Unidad de medida no encontrada"));
        }
        if (unidadMedida != null) {
            return unidadMedidaRepository.findByCodigo(unidadMedida)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Unidad de medida desconocida: '" + unidadMedida + "'"));
        }
        throw new IllegalArgumentException("Debe indicarse unidadBaseId o unidadMedida");
    }
}
