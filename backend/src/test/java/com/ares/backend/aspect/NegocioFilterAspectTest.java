package com.ares.backend.aspect;

import com.ares.backend.config.SecurityUtils;
import jakarta.persistence.EntityManager;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * Prueba unitaria de la lógica del aspecto (Fase 6, capa 1 del modelo de
 * dos capas de aislamiento). No ejercita el pointcut real de AspectJ —
 * eso lo cubre la prueba de integración en NegocioFilterAspectIntegrationTest,
 * que sí prueba el proxy de Spring interceptando llamadas de servicio reales.
 */
@ExtendWith(MockitoExtension.class)
class NegocioFilterAspectTest {

    @Mock private EntityManager entityManager;
    @Mock private Session session;
    @Mock private Filter filter;

    private NegocioFilterAspect aspect;

    @BeforeEach
    void construirAspecto() {
        aspect = new NegocioFilterAspect(entityManager);
    }

    @Nested
    @DisplayName("habilitarFiltroNegocio()")
    class HabilitarFiltroNegocio {

        @Test
        @DisplayName("habilita el filtro negocioFilter con el negocioId del caller autenticado")
        void habilitaFiltroConNegocioIdDelCaller() {
            doReturn(session).when(entityManager).unwrap(Session.class);
            when(session.enableFilter("negocioFilter")).thenReturn(filter);

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getNegocioId).thenReturn(10L);

                aspect.habilitarFiltroNegocio();

                verify(session).enableFilter("negocioFilter");
                verify(filter).setParameter("negocioId", 10L);
            }
        }

        @Test
        @DisplayName("CROSS-TENANT: usa el negocioId del negocio 2 cuando el caller pertenece al negocio 2, no al 1")
        void usaElNegocioIdCorrectoPorCaller() {
            doReturn(session).when(entityManager).unwrap(Session.class);
            when(session.enableFilter("negocioFilter")).thenReturn(filter);

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getNegocioId).thenReturn(2L);

                aspect.habilitarFiltroNegocio();

                verify(filter).setParameter("negocioId", 2L);
                verify(filter, never()).setParameter("negocioId", 1L);
            }
        }

        @Test
        @DisplayName("BUGFIX: re-setea el parámetro al negocioId actual aunque el filtro ya estuviera habilitado " +
                "(evita arrastrar un negocioId obsoleto de una llamada anterior en la misma sesión)")
        void reseteaParametroAunqueYaEstuvieraHabilitado() {
            doReturn(session).when(entityManager).unwrap(Session.class);
            when(session.enableFilter("negocioFilter")).thenReturn(filter);

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                // Primera llamada: negocio 1 (simula una llamada anterior en la misma sesión).
                mocked.when(SecurityUtils::getNegocioId).thenReturn(1L);
                aspect.habilitarFiltroNegocio();

                // Segunda llamada: negocio 10. Debe re-setear el parámetro al valor ACTUAL,
                // nunca confiar en que el filtro ya estaba habilitado con el valor anterior.
                mocked.when(SecurityUtils::getNegocioId).thenReturn(10L);
                aspect.habilitarFiltroNegocio();

                verify(filter).setParameter("negocioId", 1L);
                verify(filter).setParameter("negocioId", 10L);
            }
        }

        @Test
        @DisplayName("no hace nada si no hay autenticación en el contexto (login/registro)")
        void noHaceNadaSinAutenticacion() {
            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getNegocioId).thenThrow(new NullPointerException("no hay autenticación"));

                aspect.habilitarFiltroNegocio();

                verifyNoInteractions(entityManager);
            }
        }
    }
}
