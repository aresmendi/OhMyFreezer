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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para {@link VentaTpvIngestaService}: verifica el flujo
 * completo de ingesta (venta y anulación) con todos los estados de respuesta.
 */
@ExtendWith(MockitoExtension.class)
class VentaTpvIngestaServiceTest {

    @Mock VentaTpvRepository ventaTpvRepository;
    @Mock VentaTpvLineaRepository ventaTpvLineaRepository;
    @Mock TpvSkuMappingRepository tpvSkuMappingRepository;
    @Mock RecetaService recetaService;
    @Mock IngredienteService ingredienteService;
    @Mock NegocioRepository negocioRepository;
    @Mock TpvProveedorRegistry tpvProveedorRegistry;
    @Mock TpvProveedorAdapter adaptador;

    @InjectMocks
    VentaTpvIngestaService service;

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static final long NEGOCIO_ID = 1L;

    private Negocio negocio() {
        Negocio n = new Negocio();
        n.setId(NEGOCIO_ID);
        n.setNombre("negocio-test");
        return n;
    }

    private Ingrediente ingrediente(Long id, double cantidad) {
        Ingrediente i = new Ingrediente();
        i.setId(id);
        i.setNombre("ing-" + id);
        i.setCantidad(cantidad);
        i.setUnidadMedida("kg");
        return i;
    }

    private Receta recetaConIngredientes(Ingrediente... ingredientes) {
        Receta receta = new Receta();
        receta.setId(10L);
        receta.setNombre("Receta-Test");
        List<RecetaIngrediente> ris = new ArrayList<>();
        for (Ingrediente ing : ingredientes) {
            RecetaIngrediente ri = new RecetaIngrediente();
            ri.setIngrediente(ing);
            ri.setCantidadNecesaria(1.0);
            ris.add(ri);
        }
        receta.setIngredientes(ris);
        return receta;
    }

    private VentaTpvDto dtoVenta(String externalId, String skuTpv, int cantidad) {
        return new VentaTpvDto(TipoEventoTpv.VENTA, externalId, null, skuTpv, cantidad, LocalDateTime.now());
    }

    private VentaTpvDto dtoAnulacion(String externalId, String externalIdOriginal) {
        return new VentaTpvDto(TipoEventoTpv.ANULACION, externalId, externalIdOriginal, "SKU-X", 1, LocalDateTime.now());
    }

    private VentaTpv ventaGuardada(Long id, EstadoVentaTpv estado) {
        VentaTpv v = new VentaTpv();
        v.setId(id);
        v.setEstado(estado);
        v.setExternalId("ext-" + id);
        return v;
    }

    private void stubAdaptador(VentaTpvDto dto) {
        when(tpvProveedorRegistry.resolverAdaptador(any())).thenReturn(adaptador);
        when(adaptador.parsear(any())).thenReturn(dto);
    }

    private void stubNegocioYSecurityContext(MockedStatic<SecurityUtils> mocked) {
        mocked.when(SecurityUtils::getNegocioId).thenReturn(NEGOCIO_ID);
        when(negocioRepository.findById(NEGOCIO_ID)).thenReturn(Optional.of(negocio()));
    }

    // ─── Caso 1: Venta OK → PROCESADA ────────────────────────────────────────

    @Nested
    @DisplayName("Venta OK")
    class VentaProcesada {

        @Test
        @DisplayName("venta con stock suficiente devuelve PROCESADA y guarda líneas")
        void ventaOk_devuelveProcesadaYGuardaLineas() {
            VentaTpvDto dto = dtoVenta("ext-001", "SKU-A", 1);
            Ingrediente ing = ingrediente(1L, 5.0);
            Receta receta = recetaConIngredientes(ing);
            TpvSkuMapping mapping = new TpvSkuMapping(negocio(), "SKU-A", null, receta);

            VentaTpv ventaCreada = ventaGuardada(100L, EstadoVentaTpv.RECIBIDA);

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                stubNegocioYSecurityContext(mocked);
                stubAdaptador(dto);
                when(ventaTpvRepository.saveAndFlush(any())).thenReturn(ventaCreada);
                when(tpvSkuMappingRepository.findByNegocioIdAndSkuTpv(NEGOCIO_ID, "SKU-A"))
                        .thenReturn(Optional.of(mapping));
                when(recetaService.calcularDeltasPorIngrediente(receta, 1))
                        .thenReturn(List.of(new DeltaIngrediente(ing, 1.0)));

                ResultadoIngestaResponse resp = service.ingestar("{}", "generico");

                assertThat(resp.estado()).isEqualTo(EstadoVentaTpv.PROCESADA);
                assertThat(resp.ventaTpvId()).isEqualTo(100L);
                verify(ingredienteService).reducirCantidad(ing, 1.0);
                verify(ventaTpvLineaRepository).save(any(VentaTpvLinea.class));
                verify(ventaTpvRepository).save(ventaCreada);
            }
        }
    }

    // ─── Caso 2: SKU sin mapeo → SIN_MAPEO ───────────────────────────────────

    @Nested
    @DisplayName("Venta SIN_MAPEO")
    class VentaSinMapeo {

        @Test
        @DisplayName("SKU desconocido crea fila pendiente y devuelve SIN_MAPEO")
        void skuDesconocido_devuelveSinMapeoYCreaFilaPendiente() {
            VentaTpvDto dto = dtoVenta("ext-002", "SKU-NUEVO", 2);
            VentaTpv ventaCreada = ventaGuardada(101L, EstadoVentaTpv.RECIBIDA);

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                stubNegocioYSecurityContext(mocked);
                stubAdaptador(dto);
                when(ventaTpvRepository.saveAndFlush(any())).thenReturn(ventaCreada);
                when(tpvSkuMappingRepository.findByNegocioIdAndSkuTpv(NEGOCIO_ID, "SKU-NUEVO"))
                        .thenReturn(Optional.empty());

                ResultadoIngestaResponse resp = service.ingestar("{}", "generico");

                assertThat(resp.estado()).isEqualTo(EstadoVentaTpv.SIN_MAPEO);
                verify(tpvSkuMappingRepository).save(any(TpvSkuMapping.class));
                verify(ingredienteService, never()).reducirCantidad(any(), any());
            }
        }

        @Test
        @DisplayName("SKU con fila pendiente (receta null) devuelve SIN_MAPEO sin crear fila nueva")
        void skuFilaPendiente_devuelveSinMapeoSinCrearFilaNueva() {
            VentaTpvDto dto = dtoVenta("ext-002b", "SKU-PENDIENTE", 1);
            TpvSkuMapping pendiente = new TpvSkuMapping(negocio(), "SKU-PENDIENTE", null, null);
            VentaTpv ventaCreada = ventaGuardada(102L, EstadoVentaTpv.RECIBIDA);

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                stubNegocioYSecurityContext(mocked);
                stubAdaptador(dto);
                when(ventaTpvRepository.saveAndFlush(any())).thenReturn(ventaCreada);
                when(tpvSkuMappingRepository.findByNegocioIdAndSkuTpv(NEGOCIO_ID, "SKU-PENDIENTE"))
                        .thenReturn(Optional.of(pendiente));

                ResultadoIngestaResponse resp = service.ingestar("{}", "generico");

                assertThat(resp.estado()).isEqualTo(EstadoVentaTpv.SIN_MAPEO);
                // No debe crear nueva fila porque la pendiente ya existe
                verify(tpvSkuMappingRepository, never()).save(any());
            }
        }
    }

    // ─── Caso 3: Venta sin stock → SIN_STOCK ─────────────────────────────────

    @Nested
    @DisplayName("Venta SIN_STOCK")
    class VentaSinStock {

        @Test
        @DisplayName("ingrediente con stock insuficiente devuelve SIN_STOCK sin descontar")
        void sinStock_devuelveSinStockSinDescontar() {
            VentaTpvDto dto = dtoVenta("ext-003", "SKU-B", 1);
            Ingrediente ing = ingrediente(2L, 0.5); // stock < 1.0 requerido
            Receta receta = recetaConIngredientes(ing);
            TpvSkuMapping mapping = new TpvSkuMapping(negocio(), "SKU-B", null, receta);
            VentaTpv ventaCreada = ventaGuardada(103L, EstadoVentaTpv.RECIBIDA);

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                stubNegocioYSecurityContext(mocked);
                stubAdaptador(dto);
                when(ventaTpvRepository.saveAndFlush(any())).thenReturn(ventaCreada);
                when(tpvSkuMappingRepository.findByNegocioIdAndSkuTpv(NEGOCIO_ID, "SKU-B"))
                        .thenReturn(Optional.of(mapping));
                when(recetaService.calcularDeltasPorIngrediente(receta, 1))
                        .thenReturn(List.of(new DeltaIngrediente(ing, 2.0))); // necesita 2, hay 0.5

                ResultadoIngestaResponse resp = service.ingestar("{}", "generico");

                assertThat(resp.estado()).isEqualTo(EstadoVentaTpv.SIN_STOCK);
                verify(ingredienteService, never()).reducirCantidad(any(), any());
                verify(ventaTpvLineaRepository, never()).save(any());
            }
        }
    }

    // ─── Caso 4: Duplicada → DUPLICADA ───────────────────────────────────────

    @Nested
    @DisplayName("Venta DUPLICADA")
    class VentaDuplicada {

        @Test
        @DisplayName("DataIntegrityViolationException en saveAndFlush devuelve DUPLICADA")
        void duplicada_devuelveDuplicada() {
            VentaTpvDto dto = dtoVenta("ext-dup", "SKU-C", 1);

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                stubNegocioYSecurityContext(mocked);
                stubAdaptador(dto);
                when(ventaTpvRepository.saveAndFlush(any()))
                        .thenThrow(new DataIntegrityViolationException("uk_ventas_tpv_external"));

                ResultadoIngestaResponse resp = service.ingestar("{}", "generico");

                assertThat(resp.estado()).isEqualTo(EstadoVentaTpv.DUPLICADA);
                assertThat(resp.ventaTpvId()).isNull();
                assertThat(resp.externalId()).isEqualTo("ext-dup");
                verify(ingredienteService, never()).reducirCantidad(any(), any());
            }
        }
    }

    // ─── Caso 5: Anulación OK → ANULACION_APLICADA ───────────────────────────

    @Nested
    @DisplayName("Anulación OK")
    class AnulacionAplicada {

        @Test
        @DisplayName("anulación de venta PROCESADA restaura stock y devuelve ANULACION_APLICADA")
        void anulacionOk_restauraStockYDevuelveAnulacionAplicada() {
            VentaTpvDto dto = dtoAnulacion("ext-anul-001", "ext-001");
            VentaTpv anulacion = ventaGuardada(200L, EstadoVentaTpv.RECIBIDA);
            anulacion.setExternalId("ext-anul-001");

            Ingrediente ing = ingrediente(1L, 3.0);
            VentaTpv ventaOriginal = ventaGuardada(100L, EstadoVentaTpv.PROCESADA);
            VentaTpvLinea linea = new VentaTpvLinea(ventaOriginal, ing, 1.5);

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                stubNegocioYSecurityContext(mocked);
                stubAdaptador(dto);
                when(ventaTpvRepository.saveAndFlush(any())).thenReturn(anulacion);
                when(ventaTpvRepository.findByNegocioIdAndExternalId(NEGOCIO_ID, "ext-001"))
                        .thenReturn(Optional.of(ventaOriginal));
                when(ventaTpvLineaRepository.findByVentaTpvId(100L))
                        .thenReturn(List.of(linea));

                ResultadoIngestaResponse resp = service.ingestar("{}", "generico");

                assertThat(resp.estado()).isEqualTo(EstadoVentaTpv.ANULACION_APLICADA);
                verify(ingredienteService).restaurarCantidad(ing, 1.5);
                verify(ventaTpvRepository, atLeast(2)).save(any());
            }
        }
    }

    // ─── Caso 6: Anulación huérfana → ANULACION_HUERFANA ─────────────────────

    @Nested
    @DisplayName("Anulación huérfana")
    class AnulacionHuerfana {

        @Test
        @DisplayName("venta original no encontrada devuelve ANULACION_HUERFANA")
        void anulacionHuerfana_devuelveAnulacionHuerfana() {
            VentaTpvDto dto = dtoAnulacion("ext-anul-002", "ext-inexistente");
            VentaTpv anulacion = ventaGuardada(201L, EstadoVentaTpv.RECIBIDA);

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                stubNegocioYSecurityContext(mocked);
                stubAdaptador(dto);
                when(ventaTpvRepository.saveAndFlush(any())).thenReturn(anulacion);
                when(ventaTpvRepository.findByNegocioIdAndExternalId(NEGOCIO_ID, "ext-inexistente"))
                        .thenReturn(Optional.empty());

                ResultadoIngestaResponse resp = service.ingestar("{}", "generico");

                assertThat(resp.estado()).isEqualTo(EstadoVentaTpv.ANULACION_HUERFANA);
                verify(ingredienteService, never()).restaurarCantidad(any(), any());
            }
        }
    }

    // ─── Caso 7: Anulación de venta SIN_STOCK → ANULADA_SIN_EFECTO ───────────

    @Nested
    @DisplayName("Anulación de venta SIN_STOCK")
    class AnulacionSinEfecto {

        @Test
        @DisplayName("anulación de venta SIN_STOCK devuelve ANULADA_SIN_EFECTO sin tocar stock")
        void anulacionDeVentaSinStock_devuelveAnuladaSinEfecto() {
            VentaTpvDto dto = dtoAnulacion("ext-anul-003", "ext-sin-stock");
            VentaTpv anulacion = ventaGuardada(202L, EstadoVentaTpv.RECIBIDA);
            VentaTpv ventaSinStock = ventaGuardada(104L, EstadoVentaTpv.SIN_STOCK);

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                stubNegocioYSecurityContext(mocked);
                stubAdaptador(dto);
                when(ventaTpvRepository.saveAndFlush(any())).thenReturn(anulacion);
                when(ventaTpvRepository.findByNegocioIdAndExternalId(NEGOCIO_ID, "ext-sin-stock"))
                        .thenReturn(Optional.of(ventaSinStock));

                ResultadoIngestaResponse resp = service.ingestar("{}", "generico");

                assertThat(resp.estado()).isEqualTo(EstadoVentaTpv.ANULADA_SIN_EFECTO);
                verify(ingredienteService, never()).restaurarCantidad(any(), any());
                verify(ventaTpvLineaRepository, never()).findByVentaTpvId(any());
            }
        }

        @Test
        @DisplayName("anulación de venta SIN_MAPEO devuelve ANULADA_SIN_EFECTO sin tocar stock")
        void anulacionDeVentaSinMapeo_devuelveAnuladaSinEfecto() {
            VentaTpvDto dto = dtoAnulacion("ext-anul-004", "ext-sin-mapeo");
            VentaTpv anulacion = ventaGuardada(203L, EstadoVentaTpv.RECIBIDA);
            VentaTpv ventaSinMapeo = ventaGuardada(105L, EstadoVentaTpv.SIN_MAPEO);

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                stubNegocioYSecurityContext(mocked);
                stubAdaptador(dto);
                when(ventaTpvRepository.saveAndFlush(any())).thenReturn(anulacion);
                when(ventaTpvRepository.findByNegocioIdAndExternalId(NEGOCIO_ID, "ext-sin-mapeo"))
                        .thenReturn(Optional.of(ventaSinMapeo));

                ResultadoIngestaResponse resp = service.ingestar("{}", "generico");

                assertThat(resp.estado()).isEqualTo(EstadoVentaTpv.ANULADA_SIN_EFECTO);
                verify(ingredienteService, never()).restaurarCantidad(any(), any());
            }
        }
    }
}
