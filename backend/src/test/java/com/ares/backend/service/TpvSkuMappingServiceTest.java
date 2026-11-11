package com.ares.backend.service;

import com.ares.backend.config.SecurityUtils;
import com.ares.backend.entity.Negocio;
import com.ares.backend.entity.Receta;
import com.ares.backend.entity.TpvSkuMapping;
import com.ares.backend.exception.RecursoNoEncontradoException;
import com.ares.backend.repository.RecetaRepository;
import com.ares.backend.repository.TpvSkuMappingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios para {@link TpvSkuMappingService}: listar, asignar receta
 * y desmapear, incluyendo los IDOR checks (mapeo de otro negocio → 404).
 */
@ExtendWith(MockitoExtension.class)
class TpvSkuMappingServiceTest {

    @Mock
    private TpvSkuMappingRepository tpvSkuMappingRepository;

    @Mock
    private RecetaRepository recetaRepository;

    @InjectMocks
    private TpvSkuMappingService tpvSkuMappingService;

    // ─── Helpers ────────────────────────────────────────────────────────────

    private Negocio negocio(Long id) {
        Negocio n = new Negocio();
        n.setId(id);
        return n;
    }

    private TpvSkuMapping mapping(Long id, Negocio negocio, Receta receta) {
        TpvSkuMapping m = new TpvSkuMapping(negocio, "SKU-" + id, "Producto " + id, receta);
        m.setId(id);
        return m;
    }

    private Receta receta(Long id, Negocio negocio) {
        Receta r = new Receta();
        r.setId(id);
        r.setNombre("Receta " + id);
        r.setNegocio(negocio);
        return r;
    }

    // ─── listar() ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("listar()")
    class Listar {

        @Test
        @DisplayName("devuelve los mapeos del negocio autenticado, más recientes primero")
        void devuelveMapeosDelNegocio() {
            Negocio negocio = negocio(1L);
            TpvSkuMapping m1 = mapping(10L, negocio, null);
            TpvSkuMapping m2 = mapping(11L, negocio, null);

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getNegocioId).thenReturn(1L);
                when(tpvSkuMappingRepository.findByNegocioIdOrderByFechaCreacionDesc(1L))
                        .thenReturn(List.of(m2, m1));

                List<TpvSkuMapping> resultado = tpvSkuMappingService.listar();

                assertThat(resultado).containsExactly(m2, m1);
                verify(tpvSkuMappingRepository).findByNegocioIdOrderByFechaCreacionDesc(1L);
            }
        }

        @Test
        @DisplayName("devuelve lista vacía si el negocio no tiene mapeos (triangulación)")
        void devuelveListaVaciaSiNoHayMapeos() {
            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getNegocioId).thenReturn(2L);
                when(tpvSkuMappingRepository.findByNegocioIdOrderByFechaCreacionDesc(2L))
                        .thenReturn(List.of());

                List<TpvSkuMapping> resultado = tpvSkuMappingService.listar();

                assertThat(resultado).isEmpty();
            }
        }
    }

    // ─── asignarReceta() ────────────────────────────────────────────────────

    @Nested
    @DisplayName("asignarReceta()")
    class AsignarReceta {

        @Test
        @DisplayName("asigna la receta al mapeo y actualiza fechaActualizacion")
        void asignaRecetaYActualizaFecha() {
            Negocio negocio = negocio(1L);
            TpvSkuMapping mapping = mapping(10L, negocio, null);
            Receta receta = receta(20L, negocio);

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getNegocioId).thenReturn(1L);
                when(tpvSkuMappingRepository.findByIdAndNegocioId(10L, 1L))
                        .thenReturn(Optional.of(mapping));
                when(recetaRepository.findByIdAndNegocioId(20L, 1L))
                        .thenReturn(Optional.of(receta));
                when(tpvSkuMappingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                TpvSkuMapping resultado = tpvSkuMappingService.asignarReceta(10L, 20L);

                assertThat(resultado.getReceta()).isEqualTo(receta);
                assertThat(resultado.getFechaActualizacion()).isNotNull();
                verify(tpvSkuMappingRepository).save(mapping);
            }
        }

        @Test
        @DisplayName("lanza excepción si el mapeo no existe")
        void lanzaExcepcionSiMappingNoExiste() {
            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getNegocioId).thenReturn(1L);
                when(tpvSkuMappingRepository.findByIdAndNegocioId(99L, 1L))
                        .thenReturn(Optional.empty());

                assertThatThrownBy(() -> tpvSkuMappingService.asignarReceta(99L, 20L))
                        .isInstanceOf(RecursoNoEncontradoException.class);

                verify(tpvSkuMappingRepository, never()).save(any());
            }
        }

        @Test
        @DisplayName("lanza excepción si el mapeo pertenece a otro negocio (IDOR)")
        void lanzaExcepcionSiMappingDeOtroNegocio() {
            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getNegocioId).thenReturn(1L);
                // El mapping existe pero no en el negocio 1 → findByIdAndNegocioId devuelve empty
                when(tpvSkuMappingRepository.findByIdAndNegocioId(10L, 1L))
                        .thenReturn(Optional.empty());

                assertThatThrownBy(() -> tpvSkuMappingService.asignarReceta(10L, 20L))
                        .isInstanceOf(RecursoNoEncontradoException.class);

                verify(recetaRepository, never()).findByIdAndNegocioId(any(), any());
            }
        }

        @Test
        @DisplayName("lanza excepción si la receta no existe en el negocio")
        void lanzaExcepcionSiRecetaNoExiste() {
            Negocio negocio = negocio(1L);
            TpvSkuMapping mapping = mapping(10L, negocio, null);

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getNegocioId).thenReturn(1L);
                when(tpvSkuMappingRepository.findByIdAndNegocioId(10L, 1L))
                        .thenReturn(Optional.of(mapping));
                when(recetaRepository.findByIdAndNegocioId(99L, 1L))
                        .thenReturn(Optional.empty());

                assertThatThrownBy(() -> tpvSkuMappingService.asignarReceta(10L, 99L))
                        .isInstanceOf(RecursoNoEncontradoException.class);

                verify(tpvSkuMappingRepository, never()).save(any());
            }
        }
    }

    // ─── desmapear() ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("desmapear()")
    class Desmapear {

        @Test
        @DisplayName("pone receta a null y actualiza fechaActualizacion")
        void poneRecetaANullYActualizaFecha() {
            Negocio negocio = negocio(1L);
            Receta receta = receta(20L, negocio);
            TpvSkuMapping mapping = mapping(10L, negocio, receta);

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getNegocioId).thenReturn(1L);
                when(tpvSkuMappingRepository.findByIdAndNegocioId(10L, 1L))
                        .thenReturn(Optional.of(mapping));
                when(tpvSkuMappingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                TpvSkuMapping resultado = tpvSkuMappingService.desmapear(10L);

                assertThat(resultado.getReceta()).isNull();
                assertThat(resultado.getFechaActualizacion()).isNotNull();
                verify(tpvSkuMappingRepository).save(mapping);
            }
        }

        @Test
        @DisplayName("lanza excepción si el mapeo pertenece a otro negocio (IDOR)")
        void lanzaExcepcionSiMappingDeOtroNegocio() {
            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getNegocioId).thenReturn(1L);
                when(tpvSkuMappingRepository.findByIdAndNegocioId(10L, 1L))
                        .thenReturn(Optional.empty());

                assertThatThrownBy(() -> tpvSkuMappingService.desmapear(10L))
                        .isInstanceOf(RecursoNoEncontradoException.class);

                verify(tpvSkuMappingRepository, never()).save(any());
            }
        }
    }
}
