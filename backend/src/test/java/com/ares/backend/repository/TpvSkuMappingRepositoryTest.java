package com.ares.backend.repository;

import com.ares.backend.entity.Negocio;
import com.ares.backend.entity.Receta;
import com.ares.backend.entity.TpvSkuMapping;
import com.ares.backend.entity.Usuario;
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
 * de {@link TpvSkuMapping} y el finder {@code findByNegocioIdAndSkuTpv}:
 * la fila "pendiente" (receta null, D-B del diseño), la resolución cuando
 * ya está mapeada, y que el mismo sku_tpv no colisiona entre dos negocios.
 */
@DataJpaTest
@Transactional
class TpvSkuMappingRepositoryTest {

    @Autowired private NegocioRepository negocioRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RecetaRepository recetaRepository;
    @Autowired private TpvSkuMappingRepository tpvSkuMappingRepository;

    private Negocio negocioA;
    private Negocio negocioB;

    @BeforeEach
    void crearNegocios() {
        negocioA = negocioRepository.save(new Negocio("Negocio A", "a@ares.dev"));
        negocioB = negocioRepository.save(new Negocio("Negocio B", "b@ares.dev"));
    }

    private Receta receta(String nombre, Negocio negocio) {
        Usuario jefe = new Usuario("jefe-" + nombre, "hash", true);
        jefe.setEmail(nombre + "@test.com");
        jefe.setNegocio(negocio);
        usuarioRepository.save(jefe);

        Receta r = new Receta(nombre, "desc", jefe);
        r.setNegocio(negocio);
        return recetaRepository.save(r);
    }

    @Test
    @DisplayName("una fila con receta null es la fila pendiente de mapear (D-B del diseño)")
    void filaConRecetaNullEsLaFilaPendiente() {
        TpvSkuMapping pendiente = new TpvSkuMapping(negocioA, "101", "Menu del dia", null);
        tpvSkuMappingRepository.save(pendiente);

        Optional<TpvSkuMapping> encontrado = tpvSkuMappingRepository.findByNegocioIdAndSkuTpv(negocioA.getId(), "101");

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getReceta()).isNull();
        assertThat(encontrado.get().getNombreTpv()).isEqualTo("Menu del dia");
    }

    @Test
    @DisplayName("findByNegocioIdAndSkuTpv resuelve la receta mapeada")
    void findByNegocioIdAndSkuTpvResuelveRecetaMapeada() {
        Receta pizza = receta("Pizza", negocioA);
        TpvSkuMapping mapeo = new TpvSkuMapping(negocioA, "202", "Pizza Grande", pizza);
        tpvSkuMappingRepository.save(mapeo);

        Optional<TpvSkuMapping> encontrado = tpvSkuMappingRepository.findByNegocioIdAndSkuTpv(negocioA.getId(), "202");

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getReceta().getNombre()).isEqualTo("Pizza");
    }

    @Test
    @DisplayName("dos negocios pueden mapear el mismo sku_tpv sin colisionar entre si")
    void dosNegociosMapeanElMismoSkuSinColisionar() {
        Receta pastaA = receta("Pasta A", negocioA);
        Receta pastaB = receta("Pasta B", negocioB);

        tpvSkuMappingRepository.save(new TpvSkuMapping(negocioA, "101", "Pasta", pastaA));
        tpvSkuMappingRepository.save(new TpvSkuMapping(negocioB, "101", "Pasta", pastaB));

        Optional<TpvSkuMapping> deA = tpvSkuMappingRepository.findByNegocioIdAndSkuTpv(negocioA.getId(), "101");
        Optional<TpvSkuMapping> deB = tpvSkuMappingRepository.findByNegocioIdAndSkuTpv(negocioB.getId(), "101");

        assertThat(deA).isPresent();
        assertThat(deB).isPresent();
        assertThat(deA.get().getReceta().getNombre()).isEqualTo("Pasta A");
        assertThat(deB.get().getReceta().getNombre()).isEqualTo("Pasta B");
    }

    @Test
    @DisplayName("findByNegocioIdAndSkuTpv devuelve vacio si el negocio no tiene ese sku mapeado")
    void findByNegocioIdAndSkuTpvDevuelveVacioSiNoExiste() {
        Optional<TpvSkuMapping> encontrado = tpvSkuMappingRepository.findByNegocioIdAndSkuTpv(negocioA.getId(), "999");

        assertThat(encontrado).isEmpty();
    }
}
