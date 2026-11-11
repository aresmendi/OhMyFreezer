package com.ares.backend.tpv;

import com.ares.backend.config.SecurityUtils;
import com.ares.backend.dto.ResultadoIngestaResponse;
import com.ares.backend.dto.VentaTpvDto;
import com.ares.backend.entity.*;
import com.ares.backend.repository.NegocioRepository;
import com.ares.backend.repository.TpvSkuMappingRepository;
import com.ares.backend.repository.VentaTpvLineaRepository;
import com.ares.backend.repository.VentaTpvRepository;
import com.ares.backend.service.IngredienteService;
import com.ares.backend.service.RecetaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Servicio principal de ingesta de ventas TPV. Orquesta el flujo completo:
 * parseo del payload, reserva de idempotencia, resolución del mapeo SKU→receta,
 * comprobación y descuento de stock, y reversión en caso de anulación.
 * <p>
 * Siempre devuelve {@link ResultadoIngestaResponse}; nunca lanza excepciones
 * de negocio al controller — los estados de error van en el body (200).
 *
 * @author Ares
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VentaTpvIngestaService {

    private final VentaTpvRepository ventaTpvRepository;
    private final VentaTpvLineaRepository ventaTpvLineaRepository;
    private final TpvSkuMappingRepository tpvSkuMappingRepository;
    private final RecetaService recetaService;
    private final IngredienteService ingredienteService;
    private final NegocioRepository negocioRepository;
    private final TpvProveedorRegistry tpvProveedorRegistry;

    /**
     * Punto de entrada único para la ingesta de un evento TPV (venta o anulación).
     * <p>
     * El flujo es:
     * <ol>
     *   <li>Parsear el payload crudo con el adaptador del proveedor.</li>
     *   <li>Reservar fila de idempotencia; si ya existe (constraint viola),
     *       responder DUPLICADA sin modificar stock.</li>
     *   <li>Si es VENTA: resolver mapeo SKU→receta, comprobar stock y descontar.</li>
     *   <li>Si es ANULACION: buscar venta original y restaurar su snapshot de stock.</li>
     * </ol>
     *
     * @param rawPayload      cuerpo HTTP del TPV, tal como llegó
     * @param nombreProveedor valor de la cabecera {@code X-Tpv-Proveedor}
     * @return resultado del procesamiento (nunca null)
     */
    @Transactional
    public ResultadoIngestaResponse ingestar(String rawPayload, String nombreProveedor) {
        // 1. Parsear
        VentaTpvDto dto = tpvProveedorRegistry.resolverAdaptador(nombreProveedor).parsear(rawPayload);
        log.debug("Ingesta TPV: tipo={} externalId={} proveedor={}", dto.tipo(), dto.externalId(), nombreProveedor);

        // 2. Negocio del caller autenticado
        long negocioId = SecurityUtils.getNegocioId();
        Negocio negocio = negocioRepository.findById(negocioId)
                .orElseThrow(() -> new IllegalStateException("Negocio no encontrado para id=" + negocioId));

        // 3. Reservar fila de idempotencia (D-A del diseño)
        VentaTpv venta;
        try {
            venta = ventaTpvRepository.saveAndFlush(new VentaTpv(
                    negocio,
                    dto.tipo(),
                    dto.externalId(),
                    dto.externalIdOriginal(),
                    dto.skuTpv(),
                    dto.cantidad(),
                    EstadoVentaTpv.RECIBIDA));
        } catch (DataIntegrityViolationException ex) {
            log.warn("Evento TPV duplicado: externalId={} negocioId={}", dto.externalId(), negocioId);
            return new ResultadoIngestaResponse(null, dto.externalId(), EstadoVentaTpv.DUPLICADA, null);
        }

        // 4. Ramificar por tipo
        if (dto.tipo() == TipoEventoTpv.VENTA) {
            return procesarVenta(venta, dto, negocioId, negocio);
        } else {
            return procesarAnulacion(venta, dto, negocioId);
        }
    }

    // ─── Venta ───────────────────────────────────────────────────────────────

    private ResultadoIngestaResponse procesarVenta(VentaTpv venta, VentaTpvDto dto,
                                                    long negocioId, Negocio negocio) {
        // Resolver mapeo SKU→receta
        Optional<TpvSkuMapping> mappingOpt =
                tpvSkuMappingRepository.findByNegocioIdAndSkuTpv(negocioId, dto.skuTpv());

        if (mappingOpt.isEmpty() || mappingOpt.get().getReceta() == null) {
            // Crear fila pendiente si todavía no existe
            if (mappingOpt.isEmpty()) {
                tpvSkuMappingRepository.save(new TpvSkuMapping(negocio, dto.skuTpv(), null, null));
                log.info("SKU desconocido '{}' — fila pendiente creada en tpv_sku_mapping", dto.skuTpv());
            }
            venta.setEstado(EstadoVentaTpv.SIN_MAPEO);
            ventaTpvRepository.save(venta);
            return new ResultadoIngestaResponse(venta.getId(), dto.externalId(), EstadoVentaTpv.SIN_MAPEO, null);
        }

        Receta receta = mappingOpt.get().getReceta();
        List<DeltaIngrediente> deltas = recetaService.calcularDeltasPorIngrediente(receta, dto.cantidad());

        // Comprobar stock
        for (DeltaIngrediente delta : deltas) {
            if (delta.ingrediente().getCantidad() < delta.cantidad()) {
                venta.setEstado(EstadoVentaTpv.SIN_STOCK);
                ventaTpvRepository.save(venta);
                log.info("Stock insuficiente para venta externalId={}", dto.externalId());
                return new ResultadoIngestaResponse(venta.getId(), dto.externalId(), EstadoVentaTpv.SIN_STOCK, null);
            }
        }

        // Descontar stock y guardar snapshot
        for (DeltaIngrediente delta : deltas) {
            ingredienteService.reducirCantidad(delta.ingrediente(), delta.cantidad());
            ventaTpvLineaRepository.save(new VentaTpvLinea(venta, delta.ingrediente(), delta.cantidad()));
        }

        venta.setReceta(receta);
        venta.setEstado(EstadoVentaTpv.PROCESADA);
        ventaTpvRepository.save(venta);
        log.info("Venta TPV procesada: externalId={} receta={}", dto.externalId(), receta.getNombre());
        return new ResultadoIngestaResponse(venta.getId(), dto.externalId(), EstadoVentaTpv.PROCESADA, null);
    }

    // ─── Anulación ───────────────────────────────────────────────────────────

    private ResultadoIngestaResponse procesarAnulacion(VentaTpv anulacion, VentaTpvDto dto, long negocioId) {
        // Buscar venta original por su propio externalId
        Optional<VentaTpv> originalOpt =
                ventaTpvRepository.findByNegocioIdAndExternalId(negocioId, dto.externalIdOriginal());

        if (originalOpt.isEmpty()) {
            anulacion.setEstado(EstadoVentaTpv.ANULACION_HUERFANA);
            ventaTpvRepository.save(anulacion);
            log.warn("Anulación huérfana: externalIdOriginal={} negocioId={}", dto.externalIdOriginal(), negocioId);
            return new ResultadoIngestaResponse(anulacion.getId(), dto.externalId(), EstadoVentaTpv.ANULACION_HUERFANA, null);
        }

        VentaTpv original = originalOpt.get();

        // Si la venta original no llegó a descontar stock, nada que restaurar
        if (original.getEstado() == EstadoVentaTpv.SIN_STOCK
                || original.getEstado() == EstadoVentaTpv.SIN_MAPEO) {
            original.setEstado(EstadoVentaTpv.ANULADA);
            ventaTpvRepository.save(original);
            anulacion.setEstado(EstadoVentaTpv.ANULADA_SIN_EFECTO);
            ventaTpvRepository.save(anulacion);
            log.info("Anulación sin efecto en stock: externalIdOriginal={}", dto.externalIdOriginal());
            return new ResultadoIngestaResponse(anulacion.getId(), dto.externalId(), EstadoVentaTpv.ANULADA_SIN_EFECTO, null);
        }

        // Restaurar stock a partir del snapshot
        List<VentaTpvLinea> lineas = ventaTpvLineaRepository.findByVentaTpvId(original.getId());
        for (VentaTpvLinea linea : lineas) {
            ingredienteService.restaurarCantidad(linea.getIngrediente(), linea.getCantidadDescontada());
        }

        original.setEstado(EstadoVentaTpv.ANULADA);
        ventaTpvRepository.save(original);
        anulacion.setEstado(EstadoVentaTpv.ANULACION_APLICADA);
        ventaTpvRepository.save(anulacion);
        log.info("Anulación aplicada: externalIdOriginal={} lineas={}", dto.externalIdOriginal(), lineas.size());
        return new ResultadoIngestaResponse(anulacion.getId(), dto.externalId(), EstadoVentaTpv.ANULACION_APLICADA, null);
    }
}
