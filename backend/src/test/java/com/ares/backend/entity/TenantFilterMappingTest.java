package com.ares.backend.entity;

import com.ares.backend.repository.*;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que las 6 entidades tenant-owned tienen su FK `negocio` mapeada
 * y que el filtro Hibernate `negocioFilter` (@FilterDef/@Filter) definido en
 * cada una realmente restringe los resultados de una consulta cuando se
 * habilita manualmente. Habilitar el filtro automáticamente por request es
 * responsabilidad de NegocioFilterAspect (fase posterior); aquí solo se
 * prueba que el mapeo del filtro es correcto a nivel de entidad.
 */
@DataJpaTest
@Transactional
class TenantFilterMappingTest {

    @Autowired private EntityManager entityManager;
    @Autowired private NegocioRepository negocioRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private IngredienteRepository ingredienteRepository;
    @Autowired private RecetaRepository recetaRepository;
    @Autowired private AlertaRepository alertaRepository;
    @Autowired private MovimientoStockRepository movimientoStockRepository;
    @Autowired private RegistroUsoRecetaRepository registroUsoRecetaRepository;

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

    private Usuario usuario(String username, Negocio negocio) {
        Usuario u = new Usuario(username, "hash", false);
        u.setEmail(username + "@test.com");
        u.setNegocio(negocio);
        return usuarioRepository.save(u);
    }

    @Test
    void usuarioQuedaAisladoPorNegocioAlHabilitarElFiltro() {
        usuario("ana", negocioA);
        usuario("beto", negocioB);

        assertThat(usuarioRepository.findAll()).hasSize(2);

        habilitarFiltroPara(negocioA.getId());
        List<Usuario> soloA = usuarioRepository.findAll();

        assertThat(soloA).hasSize(1);
        assertThat(soloA.get(0).getUsername()).isEqualTo("ana");
    }

    @Test
    void ingredienteQuedaAisladoPorNegocioAlHabilitarElFiltro() {
        Ingrediente ia = new Ingrediente("Tomate", 5.0, "ud", 1.0);
        ia.setNegocio(negocioA);
        ingredienteRepository.save(ia);

        Ingrediente ib = new Ingrediente("Tomate", 3.0, "ud", 1.0);
        ib.setNegocio(negocioB);
        ingredienteRepository.save(ib);

        assertThat(ingredienteRepository.findAll()).hasSize(2);

        habilitarFiltroPara(negocioB.getId());
        List<Ingrediente> soloB = ingredienteRepository.findAll();

        assertThat(soloB).hasSize(1);
        assertThat(soloB.get(0).getNegocio().getId()).isEqualTo(negocioB.getId());
    }

    @Test
    void recetaQuedaAisladaPorNegocioAlHabilitarElFiltro() {
        Usuario jefeA = usuario("jefeA", negocioA);
        Usuario jefeB = usuario("jefeB", negocioB);

        Receta ra = new Receta("Pasta", "desc", jefeA);
        ra.setNegocio(negocioA);
        recetaRepository.save(ra);

        Receta rb = new Receta("Pizza", "desc", jefeB);
        rb.setNegocio(negocioB);
        recetaRepository.save(rb);

        assertThat(recetaRepository.findAll()).hasSize(2);

        habilitarFiltroPara(negocioA.getId());
        List<Receta> soloA = recetaRepository.findAll();

        assertThat(soloA).hasSize(1);
        assertThat(soloA.get(0).getNombre()).isEqualTo("Pasta");
    }

    @Test
    void alertaQuedaAisladaPorNegocioAlHabilitarElFiltro() {
        Usuario jefeA = usuario("jefeA2", negocioA);
        Usuario jefeB = usuario("jefeB2", negocioB);

        Alerta aa = new Alerta("STOCK_BAJO", "mensaje A", jefeA);
        aa.setNegocio(negocioA);
        alertaRepository.save(aa);

        Alerta ab = new Alerta("STOCK_BAJO", "mensaje B", jefeB);
        ab.setNegocio(negocioB);
        alertaRepository.save(ab);

        assertThat(alertaRepository.findAll()).hasSize(2);

        habilitarFiltroPara(negocioB.getId());
        List<Alerta> soloB = alertaRepository.findAll();

        assertThat(soloB).hasSize(1);
        assertThat(soloB.get(0).getMensaje()).isEqualTo("mensaje B");
    }

    @Test
    void movimientoStockQuedaAisladoPorSuPropiaColumnaNegocioIdAlHabilitarElFiltro() {
        Ingrediente ia = new Ingrediente("Harina", 10.0, "kg", 1.0);
        ia.setNegocio(negocioA);
        ingredienteRepository.save(ia);

        Ingrediente ib = new Ingrediente("Harina", 10.0, "kg", 1.0);
        ib.setNegocio(negocioB);
        ingredienteRepository.save(ib);

        MovimientoStock ma = new MovimientoStock(ia, 10.0, 8.0, "SALIDA", "consumo", 1L);
        ma.setNegocio(negocioA);
        movimientoStockRepository.save(ma);

        MovimientoStock mb = new MovimientoStock(ib, 10.0, 6.0, "SALIDA", "consumo", 2L);
        mb.setNegocio(negocioB);
        movimientoStockRepository.save(mb);

        assertThat(movimientoStockRepository.findAll()).hasSize(2);

        habilitarFiltroPara(negocioA.getId());
        List<MovimientoStock> soloA = movimientoStockRepository.findAll();

        assertThat(soloA).hasSize(1);
        assertThat(soloA.get(0).getNegocio().getId()).isEqualTo(negocioA.getId());
    }

    @Test
    void registroUsoRecetaQuedaAisladoPorNegocioAlHabilitarElFiltro() {
        Usuario jefeA = usuario("jefeA3", negocioA);
        Usuario jefeB = usuario("jefeB3", negocioB);

        Receta ra = new Receta("Sopa", "desc", jefeA);
        ra.setNegocio(negocioA);
        recetaRepository.save(ra);

        Receta rb = new Receta("Ensalada", "desc", jefeB);
        rb.setNegocio(negocioB);
        recetaRepository.save(rb);

        RegistroUsoReceta usoA = new RegistroUsoReceta(ra, jefeA, true);
        usoA.setNegocio(negocioA);
        registroUsoRecetaRepository.save(usoA);

        RegistroUsoReceta usoB = new RegistroUsoReceta(rb, jefeB, true);
        usoB.setNegocio(negocioB);
        registroUsoRecetaRepository.save(usoB);

        assertThat(registroUsoRecetaRepository.findAll()).hasSize(2);

        habilitarFiltroPara(negocioB.getId());
        List<RegistroUsoReceta> soloB = registroUsoRecetaRepository.findAll();

        assertThat(soloB).hasSize(1);
        assertThat(soloB.get(0).getReceta().getNombre()).isEqualTo("Ensalada");
    }
}
