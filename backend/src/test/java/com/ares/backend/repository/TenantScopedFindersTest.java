package com.ares.backend.repository;

import com.ares.backend.entity.Ingrediente;
import com.ares.backend.entity.Negocio;
import com.ares.backend.entity.PasoReceta;
import com.ares.backend.entity.Receta;
import com.ares.backend.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba, contra una BD real (H2), que los finders scoped por negocio
 * (Fase 4 de multi-tenancy) realmente aíslan cross-tenant a nivel de
 * repositorio — sin depender de mocks:
 *
 *  (a) un id perteneciente a otro negocio nunca se puede cargar por la vía
 *      scoped (Optional vacío == 404 en la capa de servicio), y
 *  (b) dos negocios distintos pueden tener recursos con el mismo nombre
 *      (p. ej. un ingrediente "Tomate" cada uno) sin colisionar, porque la
 *      unicidad de nombre es per-negocio.
 */
@DataJpaTest
@Transactional
class TenantScopedFindersTest {

    @Autowired private NegocioRepository negocioRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private IngredienteRepository ingredienteRepository;
    @Autowired private RecetaRepository recetaRepository;

    private Negocio negocioA;
    private Negocio negocioB;

    @BeforeEach
    void crearNegocios() {
        negocioA = negocioRepository.save(new Negocio("Negocio A", "a@ares.dev"));
        negocioB = negocioRepository.save(new Negocio("Negocio B", "b@ares.dev"));
    }

    @Nested
    @DisplayName("Ingrediente — findByIdAndNegocioId / existsByNombreIgnoreCaseAndNegocioId")
    class IngredienteScoped {

        @Test
        @DisplayName("un id de negocio A no se puede cargar con el negocioId de B")
        void idDeOtroNegocioNoSeCarga() {
            Ingrediente tomateA = new Ingrediente("Tomate", 5.0, "ud", 1.0);
            tomateA.setNegocio(negocioA);
            Long id = ingredienteRepository.save(tomateA).getId();

            Optional<Ingrediente> propio = ingredienteRepository.findByIdAndNegocioId(id, negocioA.getId());
            Optional<Ingrediente> ajeno = ingredienteRepository.findByIdAndNegocioId(id, negocioB.getId());

            assertThat(propio).isPresent();
            assertThat(ajeno).isEmpty();
        }

        @Test
        @DisplayName("un id inexistente y un id foráneo son indistinguibles (ambos Optional vacío)")
        void idInexistenteEIdForaneoIndistinguibles() {
            Ingrediente tomateA = new Ingrediente("Tomate", 5.0, "ud", 1.0);
            tomateA.setNegocio(negocioA);
            Long idReal = ingredienteRepository.save(tomateA).getId();

            Optional<Ingrediente> foraneo = ingredienteRepository.findByIdAndNegocioId(idReal, negocioB.getId());
            Optional<Ingrediente> inexistente = ingredienteRepository.findByIdAndNegocioId(999999L, negocioB.getId());

            assertThat(foraneo).isEmpty();
            assertThat(inexistente).isEmpty();
        }

        @Test
        @DisplayName("negocio A y negocio B pueden tener cada uno un ingrediente 'Tomate' sin colisionar")
        void mismoNombreEnDosNegociosNoColisiona() {
            Ingrediente tomateA = new Ingrediente("Tomate", 5.0, "ud", 1.0);
            tomateA.setNegocio(negocioA);
            ingredienteRepository.save(tomateA);

            Ingrediente tomateB = new Ingrediente("Tomate", 3.0, "ud", 1.0);
            tomateB.setNegocio(negocioB);
            ingredienteRepository.save(tomateB);

            assertThat(ingredienteRepository.existsByNombreIgnoreCaseAndNegocioId("tomate", negocioA.getId())).isTrue();
            assertThat(ingredienteRepository.existsByNombreIgnoreCaseAndNegocioId("tomate", negocioB.getId())).isTrue();

            // Un tercer negocio sin ingredientes propios no ve el nombre como ocupado.
            Negocio negocioC = negocioRepository.save(new Negocio("Negocio C", "c@ares.dev"));
            assertThat(ingredienteRepository.existsByNombreIgnoreCaseAndNegocioId("tomate", negocioC.getId())).isFalse();
        }
    }

    @Nested
    @DisplayName("Receta — findByIdAndNegocioId / findByIdWithIngredientesAndNegocioId / findByIdWithPasosAndNegocioId")
    class RecetaScoped {

        private Usuario jefe(String username, Negocio negocio) {
            Usuario u = new Usuario(username, "hash", true);
            u.setNegocio(negocio);
            return usuarioRepository.save(u);
        }

        @Test
        @DisplayName("un id de negocio A no se puede cargar con el negocioId de B (findByIdAndNegocioId)")
        void idDeOtroNegocioNoSeCarga() {
            Usuario jefeA = jefe("jefeA", negocioA);
            Receta pastaA = new Receta("Pasta", "desc", jefeA);
            pastaA.setNegocio(negocioA);
            Long id = recetaRepository.save(pastaA).getId();

            assertThat(recetaRepository.findByIdAndNegocioId(id, negocioA.getId())).isPresent();
            assertThat(recetaRepository.findByIdAndNegocioId(id, negocioB.getId())).isEmpty();
        }

        @Test
        @DisplayName("findByIdWithIngredientesAndNegocioId no filtra recetas de otro negocio")
        void findByIdWithIngredientesNoFiltraCrossTenant() {
            Usuario jefeA = jefe("jefeA2", negocioA);
            Receta pastaA = new Receta("Pasta con Queso", "desc", jefeA);
            pastaA.setNegocio(negocioA);
            Long id = recetaRepository.save(pastaA).getId();

            assertThat(recetaRepository.findByIdWithIngredientesAndNegocioId(id, negocioA.getId())).isPresent();
            assertThat(recetaRepository.findByIdWithIngredientesAndNegocioId(id, negocioB.getId())).isEmpty();
        }

        @Test
        @DisplayName("findByIdWithPasosAndNegocioId no filtra recetas de otro negocio")
        void findByIdWithPasosNoFiltraCrossTenant() {
            Usuario jefeB = jefe("jefeB2", negocioB);
            Receta pizzaB = new Receta("Pizza", "desc", jefeB);
            pizzaB.setNegocio(negocioB);
            List<PasoReceta> pasos = new ArrayList<>();
            PasoReceta paso = new PasoReceta();
            paso.setOrden(1);
            paso.setDescripcion("Precalentar horno");
            paso.setReceta(pizzaB);
            pasos.add(paso);
            pizzaB.setPasos(pasos);
            Long id = recetaRepository.save(pizzaB).getId();

            assertThat(recetaRepository.findByIdWithPasosAndNegocioId(id, negocioB.getId())).isPresent();
            assertThat(recetaRepository.findByIdWithPasosAndNegocioId(id, negocioA.getId())).isEmpty();
        }

        @Test
        @DisplayName("negocio A y negocio B pueden tener cada uno una receta con el mismo nombre sin colisionar")
        void mismoNombreEnDosNegociosNoColisiona() {
            Usuario jefeA = jefe("jefeA3", negocioA);
            Usuario jefeB = jefe("jefeB3", negocioB);

            Receta ensaladaA = new Receta("Ensalada", "desc", jefeA);
            ensaladaA.setNegocio(negocioA);
            Long idA = recetaRepository.save(ensaladaA).getId();

            Receta ensaladaB = new Receta("Ensalada", "desc", jefeB);
            ensaladaB.setNegocio(negocioB);
            Long idB = recetaRepository.save(ensaladaB).getId();

            assertThat(idA).isNotEqualTo(idB);
            assertThat(recetaRepository.findByIdAndNegocioId(idA, negocioA.getId())).isPresent();
            assertThat(recetaRepository.findByIdAndNegocioId(idB, negocioB.getId())).isPresent();
            assertThat(recetaRepository.findByIdAndNegocioId(idA, negocioB.getId())).isEmpty();
            assertThat(recetaRepository.findByIdAndNegocioId(idB, negocioA.getId())).isEmpty();
        }
    }
}
