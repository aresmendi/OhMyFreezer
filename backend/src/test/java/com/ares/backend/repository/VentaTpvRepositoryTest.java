package com.ares.backend.repository;

import com.ares.backend.entity.EstadoVentaTpv;
import com.ares.backend.entity.Ingrediente;
import com.ares.backend.entity.Negocio;
import com.ares.backend.entity.TipoEventoTpv;
import com.ares.backend.entity.VentaTpv;
import com.ares.backend.entity.VentaTpvLinea;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba, contra una BD real (H2, {@code ddl-auto=create-drop}), el mapeo
 * de {@link VentaTpv}/{@link VentaTpvLinea} y los finders de idempotencia
 * usados por el orquestador de ingesta (D-A del diseño):
 * {@code findByNegocioIdAndExternalId} (replay de una venta/anulación) y
 * {@code findByNegocioIdAndExternalIdOriginal} (resolución del original al
 * procesar una anulación, y bloqueo de doble anulación).
 */
@DataJpaTest
@Transactional
class VentaTpvRepositoryTest {

    @Autowired private NegocioRepository negocioRepository;
    @Autowired private IngredienteRepository ingredienteRepository;
    @Autowired private VentaTpvRepository ventaTpvRepository;

    private Negocio negocioA;
    private Negocio negocioB;

    @BeforeEach
    void crearNegocios() {
        negocioA = negocioRepository.save(new Negocio("Negocio A", "a@ares.dev"));
        negocioB = negocioRepository.save(new Negocio("Negocio B", "b@ares.dev"));
    }

    private VentaTpv venta(Negocio negocio, TipoEventoTpv tipo, String externalId, String externalIdOriginal, EstadoVentaTpv estado) {
        VentaTpv v = new VentaTpv(negocio, tipo, externalId, externalIdOriginal, "101", 1, estado);
        return ventaTpvRepository.save(v);
    }

    @Test
    @DisplayName("findByNegocioIdAndExternalId encuentra la venta del mismo negocio")
    void findByNegocioIdAndExternalIdEncuentraLaVenta() {
        venta(negocioA, TipoEventoTpv.VENTA, "ext-1", null, EstadoVentaTpv.PROCESADA);

        Optional<VentaTpv> encontrada = ventaTpvRepository.findByNegocioIdAndExternalId(negocioA.getId(), "ext-1");

        assertThat(encontrada).isPresent();
        assertThat(encontrada.get().getEstado()).isEqualTo(EstadoVentaTpv.PROCESADA);
    }

    @Test
    @DisplayName("findByNegocioIdAndExternalId no cruza tenants: el mismo externalId en otro negocio no aparece")
    void findByNegocioIdAndExternalIdNoCruzaTenants() {
        venta(negocioA, TipoEventoTpv.VENTA, "ext-cross", null, EstadoVentaTpv.PROCESADA);

        Optional<VentaTpv> deB = ventaTpvRepository.findByNegocioIdAndExternalId(negocioB.getId(), "ext-cross");

        assertThat(deB).isEmpty();
    }

    @Test
    @DisplayName("findByNegocioIdAndExternalIdOriginal resuelve la venta original al procesar una anulacion")
    void findByNegocioIdAndExternalIdOriginalResuelveLaVentaOriginal() {
        venta(negocioA, TipoEventoTpv.VENTA, "ext-orig", null, EstadoVentaTpv.PROCESADA);
        venta(negocioA, TipoEventoTpv.ANULACION, "ext-anul", "ext-orig", EstadoVentaTpv.ANULACION_APLICADA);

        Optional<VentaTpv> anulacion = ventaTpvRepository.findByNegocioIdAndExternalIdOriginal(negocioA.getId(), "ext-orig");

        assertThat(anulacion).isPresent();
        assertThat(anulacion.get().getTipo()).isEqualTo(TipoEventoTpv.ANULACION);
        assertThat(anulacion.get().getExternalId()).isEqualTo("ext-anul");
    }

    @Test
    @DisplayName("findByNegocioIdAndExternalIdOriginal devuelve vacio si esa venta nunca fue anulada (huerfano detectable)")
    void findByNegocioIdAndExternalIdOriginalDevuelveVacioSiNoHayAnulacion() {
        venta(negocioA, TipoEventoTpv.VENTA, "ext-sin-anular", null, EstadoVentaTpv.PROCESADA);

        Optional<VentaTpv> anulacion = ventaTpvRepository.findByNegocioIdAndExternalIdOriginal(negocioA.getId(), "ext-sin-anular");

        assertThat(anulacion).isEmpty();
    }

    @Test
    @DisplayName("las lineas de una venta persisten el delta de cada ingrediente descontado (D-C del diseño)")
    void lasLineasDeUnaVentaPersistenElDeltaDeIngrediente() {
        Ingrediente harina = new Ingrediente("Harina TPV", 10.0, "kg", 1.0);
        harina.setNegocio(negocioA);
        ingredienteRepository.save(harina);

        VentaTpv v = venta(negocioA, TipoEventoTpv.VENTA, "ext-lineas", null, EstadoVentaTpv.PROCESADA);
        v.getLineas().add(new VentaTpvLinea(v, harina, 2.5));
        VentaTpv guardada = ventaTpvRepository.saveAndFlush(v);

        assertThat(guardada.getLineas()).hasSize(1);
        assertThat(guardada.getLineas().get(0).getCantidadDescontada()).isEqualTo(2.5);
        assertThat(guardada.getLineas().get(0).getIngrediente().getNombre()).isEqualTo("Harina TPV");
    }
}
