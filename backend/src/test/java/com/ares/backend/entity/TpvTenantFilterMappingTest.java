package com.ares.backend.entity;

import com.ares.backend.repository.NegocioRepository;
import com.ares.backend.repository.TpvApiKeyRepository;
import com.ares.backend.repository.TpvSkuMappingRepository;
import com.ares.backend.repository.UsuarioRepository;
import com.ares.backend.repository.VentaTpvRepository;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que las 3 entidades tenant-owned nuevas de la Fase 6
 * ({@link TpvApiKey}, {@link TpvSkuMapping}, {@link VentaTpv}) tienen su FK
 * {@code negocio} mapeada y que el filtro Hibernate {@code negocioFilter}
 * (mismo {@code @FilterDef} compartido, ver {@code package-info.java})
 * realmente restringe los resultados cuando se habilita manualmente —
 * mismo patrón que {@link TenantFilterMappingTest} para las 6 entidades
 * originales. {@link VentaTpvLinea} queda fuera a propósito: se scopea
 * transitivamente a través de su padre {@link VentaTpv}, sin @Filter propio
 * (igual que {@code RecetaIngrediente} a través de {@code Receta}).
 */
@DataJpaTest
@Transactional
class TpvTenantFilterMappingTest {

    @Autowired private EntityManager entityManager;
    @Autowired private NegocioRepository negocioRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private TpvApiKeyRepository tpvApiKeyRepository;
    @Autowired private TpvSkuMappingRepository tpvSkuMappingRepository;
    @Autowired private VentaTpvRepository ventaTpvRepository;

    private Negocio negocioA;
    private Negocio negocioB;

    @BeforeEach
    void crearNegocios() {
        negocioA = negocioRepository.save(new Negocio("Negocio A", "a@ares.dev"));
        negocioB = negocioRepository.save(new Negocio("Negocio B", "b@ares.dev"));
    }

    private void habilitarFiltroPara(Long negocioId) {
        entityManager.unwrap(Session.class)
                .enableFilter("negocioFilter")
                .setParameter("negocioId", negocioId);
    }

    private Usuario usuarioSistema(String username, Negocio negocio) {
        Usuario u = new Usuario(username, "sentinel", false);
        u.setEmail(username + "@tpv.ohmyfreezer.invalid");
        u.setNegocio(negocio);
        return usuarioRepository.save(u);
    }

    @Test
    void tpvApiKeyQuedaAisladaPorNegocioAlHabilitarElFiltro() {
        Usuario sistemaA = usuarioSistema("tpv-a", negocioA);
        Usuario sistemaB = usuarioSistema("tpv-b", negocioB);

        tpvApiKeyRepository.save(new TpvApiKey(negocioA, "pfx-a", "hash-a", sistemaA));
        tpvApiKeyRepository.save(new TpvApiKey(negocioB, "pfx-b", "hash-b", sistemaB));

        assertThat(tpvApiKeyRepository.findAll()).hasSize(2);

        habilitarFiltroPara(negocioA.getId());
        List<TpvApiKey> soloA = tpvApiKeyRepository.findAll();

        assertThat(soloA).hasSize(1);
        assertThat(soloA.get(0).getPrefijo()).isEqualTo("pfx-a");
    }

    @Test
    void tpvSkuMappingQuedaAisladoPorNegocioAlHabilitarElFiltro() {
        tpvSkuMappingRepository.save(new TpvSkuMapping(negocioA, "101", "Producto A", null));
        tpvSkuMappingRepository.save(new TpvSkuMapping(negocioB, "202", "Producto B", null));

        assertThat(tpvSkuMappingRepository.findAll()).hasSize(2);

        habilitarFiltroPara(negocioB.getId());
        List<TpvSkuMapping> soloB = tpvSkuMappingRepository.findAll();

        assertThat(soloB).hasSize(1);
        assertThat(soloB.get(0).getSkuTpv()).isEqualTo("202");
    }

    @Test
    void ventaTpvQuedaAisladaPorNegocioAlHabilitarElFiltro() {
        ventaTpvRepository.save(new VentaTpv(negocioA, TipoEventoTpv.VENTA, "ext-a", null, "101", 1, EstadoVentaTpv.PROCESADA));
        ventaTpvRepository.save(new VentaTpv(negocioB, TipoEventoTpv.VENTA, "ext-b", null, "202", 1, EstadoVentaTpv.PROCESADA));

        assertThat(ventaTpvRepository.findAll()).hasSize(2);

        habilitarFiltroPara(negocioA.getId());
        List<VentaTpv> soloA = ventaTpvRepository.findAll();

        assertThat(soloA).hasSize(1);
        assertThat(soloA.get(0).getExternalId()).isEqualTo("ext-a");
    }
}
