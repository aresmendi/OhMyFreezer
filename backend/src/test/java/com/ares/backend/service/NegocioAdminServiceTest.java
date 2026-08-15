package com.ares.backend.service;

import com.ares.backend.entity.Negocio;
import com.ares.backend.entity.NegocioSignupCode;
import com.ares.backend.exception.CodigoGeneracionException;
import com.ares.backend.exception.ConflictoEstadoException;
import com.ares.backend.exception.RecursoNoEncontradoException;
import com.ares.backend.repository.NegocioRepository;
import com.ares.backend.repository.NegocioSignupCodeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de {@link NegocioAdminService}. Usa Mockito para aislar
 * el servicio de sus 3 colaboradores permitidos (ver
 * NegocioAdminServiceDependencyTest), siguiendo la convención de
 * UsuarioServiceTest (MockitoExtension + AssertJ + @Nested por método).
 */
@ExtendWith(MockitoExtension.class)
class NegocioAdminServiceTest {

    @Mock
    private NegocioRepository negocioRepository;

    @Mock
    private NegocioSignupCodeRepository negocioSignupCodeRepository;

    @Mock
    private SignupCodeGenerator signupCodeGenerator;

    @InjectMocks
    private NegocioAdminService negocioAdminService;

    // ─── Helpers ────────────────────────────────────────────────────────────

    private Negocio negocio(Long id, String nombre) {
        Negocio n = new Negocio(nombre, "contacto@ares.dev");
        n.setId(id);
        return n;
    }

    private void stubGuardarNegocioConId(Long id) {
        when(negocioRepository.save(any(Negocio.class))).thenAnswer(inv -> {
            Negocio n = inv.getArgument(0);
            n.setId(id);
            return n;
        });
    }

    private void stubGuardarCodigoDevolviendoElMismo() {
        when(negocioSignupCodeRepository.save(any(NegocioSignupCode.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ─── crearNegocio() ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("crearNegocio()")
    class CrearNegocio {

        @Test
        @DisplayName("crea el Negocio y su primer código de alta de forma atómica")
        void creaNegocioYPrimerCodigoAtomicamente() {
            stubGuardarNegocioConId(10L);
            when(signupCodeGenerator.generar()).thenReturn("ABCDEFGHJK");
            when(negocioSignupCodeRepository.findByCodigo("ABCDEFGHJK")).thenReturn(Optional.empty());
            stubGuardarCodigoDevolviendoElMismo();

            NegocioSignupCode resultado = negocioAdminService.crearNegocio("Cocina Central", "contacto@ares.dev");

            assertThat(resultado.getCodigo()).isEqualTo("ABCDEFGHJK");
            assertThat(resultado.getNegocio().getId()).isEqualTo(10L);
            assertThat(resultado.getUsado()).isFalse();
            assertThat(resultado.getActivo()).isTrue();

            ArgumentCaptor<Negocio> negocioCaptor = ArgumentCaptor.forClass(Negocio.class);
            verify(negocioRepository).save(negocioCaptor.capture());
            assertThat(negocioCaptor.getValue().getNombre()).isEqualTo("Cocina Central");

            ArgumentCaptor<NegocioSignupCode> codigoCaptor = ArgumentCaptor.forClass(NegocioSignupCode.class);
            verify(negocioSignupCodeRepository).save(codigoCaptor.capture());
            assertThat(codigoCaptor.getValue().getCodigo()).isEqualTo("ABCDEFGHJK");
        }

        @Test
        @DisplayName("permite crear dos negocios con el mismo nombre (sin restricción de unicidad)")
        void permiteNombresDuplicados() {
            stubGuardarNegocioConId(10L);
            when(signupCodeGenerator.generar()).thenReturn("CODIGO0001");
            when(negocioSignupCodeRepository.findByCodigo(anyString())).thenReturn(Optional.empty());
            stubGuardarCodigoDevolviendoElMismo();

            negocioAdminService.crearNegocio("Cocina Central", "a@ares.dev");
            negocioAdminService.crearNegocio("Cocina Central", "b@ares.dev");

            verify(negocioRepository, times(2)).save(any(Negocio.class));
        }

        @Test
        @DisplayName("reintenta la generación del código si el primer candidato ya existe, hasta obtener uno libre")
        void reintentaGeneracionSiHayColision() {
            stubGuardarNegocioConId(10L);
            // Primer y segundo candidato colisionan; el tercero está libre.
            when(signupCodeGenerator.generar()).thenReturn("COLISION01", "COLISION02", "CODIGOLIBRE");
            when(negocioSignupCodeRepository.findByCodigo("COLISION01"))
                    .thenReturn(Optional.of(new NegocioSignupCode(negocio(1L, "Otro"), "COLISION01")));
            when(negocioSignupCodeRepository.findByCodigo("COLISION02"))
                    .thenReturn(Optional.of(new NegocioSignupCode(negocio(1L, "Otro"), "COLISION02")));
            when(negocioSignupCodeRepository.findByCodigo("CODIGOLIBRE")).thenReturn(Optional.empty());
            stubGuardarCodigoDevolviendoElMismo();

            NegocioSignupCode resultado = negocioAdminService.crearNegocio("Cocina Central", "a@ares.dev");

            assertThat(resultado.getCodigo()).isEqualTo("CODIGOLIBRE");
            verify(signupCodeGenerator, times(3)).generar();
        }

        @Test
        @DisplayName("lanza CodigoGeneracionException tras agotar 5 intentos, todos colisionando")
        void lanzaExcepcionTrasAgotarCincoIntentos() {
            stubGuardarNegocioConId(10L);
            when(signupCodeGenerator.generar()).thenReturn("SIEMPRECOL");
            when(negocioSignupCodeRepository.findByCodigo("SIEMPRECOL"))
                    .thenReturn(Optional.of(new NegocioSignupCode(negocio(1L, "Otro"), "SIEMPRECOL")));

            assertThatThrownBy(() -> negocioAdminService.crearNegocio("Cocina Central", "a@ares.dev"))
                    .isInstanceOf(CodigoGeneracionException.class);

            verify(signupCodeGenerator, times(5)).generar();
            verify(negocioSignupCodeRepository, never()).save(any(NegocioSignupCode.class));
        }
    }

    // ─── generarCodigo() ────────────────────────────────────────────────────

    @Nested
    @DisplayName("generarCodigo()")
    class GenerarCodigo {

        @Test
        @DisplayName("emite un código adicional para un negocio existente")
        void emiteCodigoAdicionalParaNegocioExistente() {
            Negocio negocio = negocio(10L, "Cocina Central");
            when(negocioRepository.findById(10L)).thenReturn(Optional.of(negocio));
            when(signupCodeGenerator.generar()).thenReturn("NUEVOCODIGO");
            when(negocioSignupCodeRepository.findByCodigo("NUEVOCODIGO")).thenReturn(Optional.empty());
            stubGuardarCodigoDevolviendoElMismo();

            NegocioSignupCode resultado = negocioAdminService.generarCodigo(10L);

            assertThat(resultado.getCodigo()).isEqualTo("NUEVOCODIGO");
            assertThat(resultado.getNegocio()).isEqualTo(negocio);
        }

        @Test
        @DisplayName("no impone límite: emite un tercer código aunque el negocio ya tenga dos válidos")
        void noImponeLimiteDeCodigos() {
            Negocio negocio = negocio(10L, "Cocina Central");
            when(negocioRepository.findById(10L)).thenReturn(Optional.of(negocio));
            when(signupCodeGenerator.generar()).thenReturn("TERCERCODIGO");
            when(negocioSignupCodeRepository.findByCodigo("TERCERCODIGO")).thenReturn(Optional.empty());
            stubGuardarCodigoDevolviendoElMismo();

            NegocioSignupCode resultado = negocioAdminService.generarCodigo(10L);

            assertThat(resultado).isNotNull();
            verify(negocioSignupCodeRepository).save(any(NegocioSignupCode.class));
        }

        @Test
        @DisplayName("lanza RecursoNoEncontradoException si el negocio no existe")
        void lanzaExcepcionSiNegocioNoExiste() {
            when(negocioRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> negocioAdminService.generarCodigo(99L))
                    .isInstanceOf(RecursoNoEncontradoException.class);

            verify(negocioSignupCodeRepository, never()).save(any());
        }
    }

    // ─── revocarCodigo() ────────────────────────────────────────────────────

    @Nested
    @DisplayName("revocarCodigo()")
    class RevocarCodigo {

        @Test
        @DisplayName("revoca un código activo y sin usar (200)")
        void revocaCodigoActivoYSinUsar() {
            NegocioSignupCode codigo = new NegocioSignupCode(negocio(10L, "Cocina Central"), "A_REVOCAR");
            codigo.setId(1L);

            when(negocioSignupCodeRepository.revocarAtomico(1L)).thenReturn(1);
            when(negocioSignupCodeRepository.findById(1L)).thenReturn(Optional.of(codigo));

            NegocioSignupCode resultado = negocioAdminService.revocarCodigo(1L);

            assertThat(resultado.getId()).isEqualTo(1L);
            verify(negocioSignupCodeRepository).revocarAtomico(1L);
        }

        @Test
        @DisplayName("lanza ConflictoEstadoException (409) si el código ya fue usado")
        void lanzaConflictoSiCodigoYaUsado() {
            NegocioSignupCode codigo = new NegocioSignupCode(negocio(10L, "Cocina Central"), "YA_USADO");
            codigo.setId(2L);
            codigo.marcarUsado(5L);

            when(negocioSignupCodeRepository.revocarAtomico(2L)).thenReturn(0);
            when(negocioSignupCodeRepository.findById(2L)).thenReturn(Optional.of(codigo));

            assertThatThrownBy(() -> negocioAdminService.revocarCodigo(2L))
                    .isInstanceOf(ConflictoEstadoException.class);
        }

        @Test
        @DisplayName("es idempotente (200, no error) si el código ya estaba revocado y sin usar")
        void esIdempotenteSiYaEstabaRevocado() {
            NegocioSignupCode codigo = new NegocioSignupCode(negocio(10L, "Cocina Central"), "YA_REVOCADO");
            codigo.setId(3L);
            codigo.setActivo(false);

            when(negocioSignupCodeRepository.revocarAtomico(3L)).thenReturn(0);
            when(negocioSignupCodeRepository.findById(3L)).thenReturn(Optional.of(codigo));

            NegocioSignupCode resultado = negocioAdminService.revocarCodigo(3L);

            assertThat(resultado.getActivo()).isFalse();
            assertThat(resultado.getUsado()).isFalse();
        }

        @Test
        @DisplayName("lanza RecursoNoEncontradoException (404) si el código no existe")
        void lanzaExcepcionSiCodigoNoExiste() {
            when(negocioSignupCodeRepository.revocarAtomico(99L)).thenReturn(0);
            when(negocioSignupCodeRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> negocioAdminService.revocarCodigo(99L))
                    .isInstanceOf(RecursoNoEncontradoException.class);
        }
    }

    // ─── listarNegocios() ───────────────────────────────────────────────────

    @Nested
    @DisplayName("listarNegocios()")
    class ListarNegocios {

        @Test
        @DisplayName("devuelve todos los negocios ordenados por fecha de alta descendente")
        void devuelveNegociosOrdenadosPorFechaAltaDescendente() {
            List<Negocio> negocios = List.of(negocio(2L, "Reciente"), negocio(1L, "Antiguo"));
            when(negocioRepository.findAll(any(Sort.class))).thenReturn(negocios);

            List<Negocio> resultado = negocioAdminService.listarNegocios();

            assertThat(resultado).containsExactly(negocios.get(0), negocios.get(1));
            verify(negocioRepository).findAll(eq(Sort.by(Sort.Direction.DESC, "fechaAlta")));
        }

        @Test
        @DisplayName("devuelve lista vacía si no hay negocios")
        void devuelveListaVaciaSiNoHayNegocios() {
            when(negocioRepository.findAll(any(Sort.class))).thenReturn(List.of());

            assertThat(negocioAdminService.listarNegocios()).isEmpty();
        }
    }

    // ─── listarCodigos() ────────────────────────────────────────────────────

    @Nested
    @DisplayName("listarCodigos()")
    class ListarCodigos {

        @Test
        @DisplayName("devuelve todos los códigos del negocio (usados, sin usar y revocados)")
        void devuelveTodosLosCodigosDelNegocio() {
            Negocio negocio = negocio(10L, "Cocina Central");
            NegocioSignupCode usado = new NegocioSignupCode(negocio, "USADO");
            usado.marcarUsado(1L);
            NegocioSignupCode sinUsar = new NegocioSignupCode(negocio, "SIN_USAR");
            NegocioSignupCode revocado = new NegocioSignupCode(negocio, "REVOCADO");
            revocado.setActivo(false);

            when(negocioRepository.existsById(10L)).thenReturn(true);
            when(negocioSignupCodeRepository.findByNegocioIdOrderByFechaCreacionDesc(10L))
                    .thenReturn(List.of(revocado, sinUsar, usado));

            List<NegocioSignupCode> resultado = negocioAdminService.listarCodigos(10L);

            assertThat(resultado).hasSize(3);
            assertThat(resultado).containsExactly(revocado, sinUsar, usado);
        }

        @Test
        @DisplayName("lanza RecursoNoEncontradoException (404) si el negocio no existe")
        void lanzaExcepcionSiNegocioNoExiste() {
            when(negocioRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> negocioAdminService.listarCodigos(99L))
                    .isInstanceOf(RecursoNoEncontradoException.class);

            verify(negocioSignupCodeRepository, never()).findByNegocioIdOrderByFechaCreacionDesc(any());
        }
    }
}
