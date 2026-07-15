package com.ares.backend.service;

import com.ares.backend.config.SecurityUtils;
import com.ares.backend.dto.AlertaCountResponse;
import com.ares.backend.dto.AlertaResponse;
import com.ares.backend.dto.IngredienteFaltanteDTO;
import com.ares.backend.dto.IngredienteResponse;
import com.ares.backend.entity.Alerta;
import com.ares.backend.entity.Ingrediente;
import com.ares.backend.entity.Negocio;
import com.ares.backend.entity.Receta;
import com.ares.backend.entity.Usuario;
import com.ares.backend.exception.RecursoNoEncontradoException;
import com.ares.backend.repository.AlertaRepository;
import com.ares.backend.repository.NegocioRepository;
import com.ares.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertaServiceTest {

    @Mock private AlertaRepository alertaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EmailService emailService;
    @Mock private NegocioRepository negocioRepository;

    @InjectMocks
    private AlertaService alertaService;

    // ─── Helpers ────────────────────────────────────────────────────────────

    private Ingrediente ingrediente(Long id) {
        Ingrediente i = new Ingrediente();
        i.setId(id);
        i.setNombre("ingrediente-" + id);
        i.setCantidad(2.0);
        i.setUnidadMedida("ud");
        i.setStockMinimo(5.0);
        i.setFechaActualizacion(LocalDateTime.now());
        return i;
    }

    private Usuario jefe(Long id) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setUsername("jefe-" + id);
        u.setPassword("hashed");
        u.setEsJefeCocina(true);
        u.setEmail("jefe" + id + "@test.com");
        u.setFechaRegistro(LocalDateTime.now());
        return u;
    }

    private Negocio negocio(Long id) {
        Negocio n = new Negocio("negocio-" + id, "negocio" + id + "@test.com");
        n.setId(id);
        return n;
    }

    private Alerta alertaExistente(Long id, String tipo) {
        Alerta a = new Alerta();
        a.setId(id);
        a.setTipo(tipo);
        a.setMensaje("mensaje original");
        a.setLeida(false);
        a.setFechaCreacion(LocalDateTime.now());
        a.setDestinatario(jefe(1L));
        return a;
    }

    private Receta receta(Long id) {
        Receta r = new Receta();
        r.setId(id);
        r.setNombre("receta-" + id);
        r.setDescripcion("desc");
        r.setFechaCreacion(LocalDateTime.now());
        r.setCreadaPor(jefe(1L));
        r.setPasos(new ArrayList<>());
        r.setIngredientes(new ArrayList<>());
        return r;
    }

    // ─── crearAlertaStockBajo() ─────────────────────────────────────────────

    @Nested
    @DisplayName("crearAlertaStockBajo()")
    class CrearAlertaStockBajo {

        @Test
        @DisplayName("actualiza alerta existente no leída en vez de crear una nueva")
        void actualizaAlertaExistente() {
            Ingrediente i = ingrediente(1L);
            List<Alerta> existentes = List.of(alertaExistente(1L, "STOCK_BAJO"));
            when(alertaRepository.findByIngredienteIdAndLeidaFalse(1L)).thenReturn(existentes);

            alertaService.crearAlertaStockBajo(i);

            verify(alertaRepository).saveAll(existentes);
            verify(usuarioRepository, never()).findByEsJefeCocinaTrueAndNegocioId(any());
            verify(emailService, never()).enviarNotificacionAlerta(any());
        }

        @Test
        @DisplayName("crea alerta nueva para cada jefe DEL NEGOCIO DEL CALLER si no existe alerta previa")
        void creaAlertaNuevaPorJefe() {
            Ingrediente i = ingrediente(1L);
            Negocio negocio = negocio(10L);

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getNegocioId).thenReturn(10L);
                when(alertaRepository.findByIngredienteIdAndLeidaFalse(1L)).thenReturn(List.of());
                when(negocioRepository.findById(10L)).thenReturn(Optional.of(negocio));
                when(usuarioRepository.findByEsJefeCocinaTrueAndNegocioId(10L)).thenReturn(List.of(jefe(1L), jefe(2L)));
                when(alertaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                alertaService.crearAlertaStockBajo(i);

                verify(alertaRepository, times(2)).save(any());
                verify(emailService, times(2)).enviarNotificacionAlerta(any());
                verify(usuarioRepository, never()).findByEsJefeCocinaTrue();
            }
        }

        @Test
        @DisplayName("CROSS-TENANT: no notifica a jefes de otros negocios")
        void noNotificaAJefesDeOtroNegocio() {
            Ingrediente i = ingrediente(1L);

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getNegocioId).thenReturn(10L);
                when(alertaRepository.findByIngredienteIdAndLeidaFalse(1L)).thenReturn(List.of());
                // Ningún jefe en el negocio 10 (solo hay jefes en otros negocios) -> lista vacía
                when(usuarioRepository.findByEsJefeCocinaTrueAndNegocioId(10L)).thenReturn(List.of());

                alertaService.crearAlertaStockBajo(i);

                verify(alertaRepository, never()).save(any());
                verify(emailService, never()).enviarNotificacionAlerta(any());
                verify(usuarioRepository, never()).findByEsJefeCocinaTrue();
                verify(negocioRepository, never()).findById(any());
            }
        }

        @Test
        @DisplayName("no crea alertas si no hay jefes de cocina en el negocio del caller")
        void noCreaAlertasSinJefes() {
            Ingrediente i = ingrediente(1L);

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getNegocioId).thenReturn(10L);
                when(alertaRepository.findByIngredienteIdAndLeidaFalse(1L)).thenReturn(List.of());
                when(usuarioRepository.findByEsJefeCocinaTrueAndNegocioId(10L)).thenReturn(List.of());

                alertaService.crearAlertaStockBajo(i);

                verify(alertaRepository, never()).save(any());
                verify(emailService, never()).enviarNotificacionAlerta(any());
                verify(negocioRepository, never()).findById(any());
            }
        }
    }

    // ─── crearAlertaEscaldaio() ─────────────────────────────────────────────

    @Nested
    @DisplayName("crearAlertaMerma()")
    class CrearAlertaMerma {

        @Test
        @DisplayName("actualiza alerta MERMA existente sin crear nueva")
        void actualizaAlertaExistente() {
            Ingrediente i = ingrediente(1L);
            List<Alerta> existentes = List.of(alertaExistente(1L, "MERMA"));
            when(alertaRepository.findByIngredienteIdAndLeidaFalse(1L)).thenReturn(existentes);

            alertaService.crearAlertaMerma(i, 10.0, 4.0);

            verify(alertaRepository).saveAll(any());
            verify(usuarioRepository, never()).findByEsJefeCocinaTrueAndNegocioId(any());
        }

        @Test
        @DisplayName("crea alerta MERMA nueva para cada jefe del negocio del caller si no existe previa")
        void creaAlertaNueva() {
            Ingrediente i = ingrediente(1L);
            Negocio negocio = negocio(10L);

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getNegocioId).thenReturn(10L);
                when(alertaRepository.findByIngredienteIdAndLeidaFalse(1L)).thenReturn(List.of());
                when(negocioRepository.findById(10L)).thenReturn(Optional.of(negocio));
                when(usuarioRepository.findByEsJefeCocinaTrueAndNegocioId(10L)).thenReturn(List.of(jefe(1L)));
                when(alertaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                alertaService.crearAlertaMerma(i, 10.0, 4.0);

                verify(alertaRepository).save(any());
                verify(emailService).enviarNotificacionAlerta(any());
            }
        }
    }

    // ─── crearAlertaRecetaNoDisponible() ────────────────────────────────────

    @Nested
    @DisplayName("crearAlertaRecetaNoDisponible()")
    class CrearAlertaRecetaNoDisponible {

        @Test
        @DisplayName("no crea alerta si ya existe una no leída para esa receta")
        void noCreaAlertaSiYaExiste() {
            Receta r = receta(1L);
            when(alertaRepository.existsByRecetaIdAndLeidaFalse(1L)).thenReturn(true);

            alertaService.crearAlertaRecetaNoDisponible(r, List.of());

            verify(alertaRepository, never()).save(any());
            verify(emailService, never()).enviarNotificacionAlerta(any());
        }

        @Test
        @DisplayName("crea alerta para cada jefe del negocio del caller si no existe previa (incl. REQUIRES_NEW)")
        void creaAlertaNuevaPorJefe() {
            Receta r = receta(1L);
            Negocio negocio = negocio(10L);

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getNegocioId).thenReturn(10L);
                when(alertaRepository.existsByRecetaIdAndLeidaFalse(1L)).thenReturn(false);
                when(negocioRepository.findById(10L)).thenReturn(Optional.of(negocio));
                when(usuarioRepository.findByEsJefeCocinaTrueAndNegocioId(10L)).thenReturn(List.of(jefe(1L), jefe(2L)));
                when(alertaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                Ingrediente ing = ingrediente(1L);
                IngredienteResponse ingResponse = new IngredienteResponse(ing);
                List<IngredienteFaltanteDTO> faltantes = List.of(new IngredienteFaltanteDTO(ingResponse, 5.0, 2.0));

                alertaService.crearAlertaRecetaNoDisponible(r, faltantes);

                verify(alertaRepository, times(2)).save(any());
                verify(emailService, times(2)).enviarNotificacionAlerta(any());
                verify(usuarioRepository, never()).findByEsJefeCocinaTrue();
            }
        }
    }

    // ─── marcarComoLeida() ──────────────────────────────────────────────────

    @Nested
    @DisplayName("marcarComoLeida()")
    class MarcarComoLeida {

        @Test
        @DisplayName("marca la alerta como leída y la devuelve, scoped al negocio del caller")
        void marcaAlertaComoLeida() {
            Alerta alerta = alertaExistente(1L, "STOCK_BAJO");

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getNegocioId).thenReturn(10L);
                when(alertaRepository.findByIdAndNegocioId(1L, 10L)).thenReturn(Optional.of(alerta));
                when(alertaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                AlertaResponse response = alertaService.marcarComoLeida(1L);

                assertThat(response).isNotNull();
                assertThat(alerta.getLeida()).isTrue();
                verify(alertaRepository).save(alerta);
            }
        }

        @Test
        @DisplayName("lanza RecursoNoEncontradoException si la alerta no existe en el negocio del caller")
        void lanzaExcepcionAlertaNoExiste() {
            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getNegocioId).thenReturn(10L);
                when(alertaRepository.findByIdAndNegocioId(99L, 10L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> alertaService.marcarComoLeida(99L))
                        .isInstanceOf(RecursoNoEncontradoException.class);
            }
        }

        @Test
        @DisplayName("CROSS-TENANT: una alerta de otro negocio es indistinguible de una inexistente")
        void alertaDeOtroNegocioEsIndistinguibleDeInexistente() {
            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getNegocioId).thenReturn(1L);
                // La alerta 1L existe pero pertenece al negocio 2 -> el finder scoped no la ve
                when(alertaRepository.findByIdAndNegocioId(1L, 1L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> alertaService.marcarComoLeida(1L))
                        .isInstanceOf(RecursoNoEncontradoException.class);

                verify(alertaRepository, never()).findById(any());
            }
        }
    }

    // ─── marcarTodasComoLeidas() ────────────────────────────────────────────

    @Nested
    @DisplayName("marcarTodasComoLeidas()")
    class MarcarTodasComoLeidas {

        @Test
        @DisplayName("marca todas las alertas no leídas del usuario como leídas")
        void marcaTodasComoLeidas() {
            List<Alerta> alertas = List.of(
                    alertaExistente(1L, "STOCK_BAJO"),
                    alertaExistente(2L, "RECETA_NO_DISPONIBLE")
            );

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(1L);
                when(alertaRepository.findByDestinatarioIdAndLeidaFalse(1L)).thenReturn(alertas);

                alertaService.marcarTodasComoLeidas();

                alertas.forEach(a -> assertThat(a.getLeida()).isTrue());
                verify(alertaRepository).saveAll(alertas);
            }
        }
    }

    // ─── contarNoLeidas() ───────────────────────────────────────────────────

    @Nested
    @DisplayName("contarNoLeidas()")
    class ContarNoLeidas {

        @Test
        @DisplayName("devuelve el conteo correcto de alertas no leídas")
        void devuelveConteo() {
            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(1L);
                when(alertaRepository.countByDestinatarioIdAndLeidaFalse(1L)).thenReturn(3L);

                AlertaCountResponse response = alertaService.contarNoLeidas();

                assertThat(response.getPendientes()).isEqualTo(3L);
            }
        }
    }
}
