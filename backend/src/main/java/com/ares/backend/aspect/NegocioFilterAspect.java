package com.ares.backend.aspect;

import com.ares.backend.config.SecurityUtils;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

/**
 * Habilita el filtro Hibernate {@code negocioFilter} (Fase 6 de
 * multi-tenancy) antes de cada llamada externa a un método de la capa de
 * servicio, scoped al negocioId del caller autenticado.
 *
 * Este aspecto es la CAPA 1 (red de defensa en profundidad) del modelo de
 * dos capas descrito en el diseño: scoping automático de lecturas
 * {@code findAll}-style. NUNCA es la capa autoritativa — esa sigue siendo
 * siempre el finder explícito scoped por negocioId (Fase 4/5,
 * {@code findByIdAndNegocioId} y equivalentes), que sigue aplicándose en
 * todo punto de lectura/mutación de un solo recurso independientemente de
 * si este filtro llegó a habilitarse.
 *
 * Caso conocido en el que este filtro NO llega a aplicarse: código que abre
 * una sesión de Hibernate nueva vía {@code REQUIRES_NEW} (p. ej.
 * {@code AlertaService.crearAlertaRecetaNoDisponible}) — esa sesión nueva
 * no pasa por este {@code @Before} de nuevo dentro de la misma invocación
 * de aspecto, por lo que esos flujos dependen exclusivamente de la capa
 * autoritativa (scoping explícito), nunca de este filtro.
 *
 * @author Ares
 * @version 1.0
 */
@Aspect
@Component
@RequiredArgsConstructor
public class NegocioFilterAspect {

    private static final String FILTRO = "negocioFilter";
    private static final String PARAMETRO = "negocioId";

    private final EntityManager entityManager;

    /**
     * Se ejecuta antes de cualquier método público invocado externamente
     * (a través del proxy de Spring) sobre una clase del paquete
     * {@code com.ares.backend.service} o sus subpaquetes.
     */
    @Before("execution(* com.ares.backend.service..*.*(..))")
    public void habilitarFiltroNegocio() {
        Long negocioId = obtenerNegocioIdSiAutenticado();
        if (negocioId == null) {
            return;
        }

        // Siempre re-setea el parámetro, incluso si el filtro ya estaba
        // habilitado: una misma sesión de Hibernate de larga vida (p. ej.
        // en tests @Transactional que simulan varias llamadas) NO debe
        // arrastrar un negocioId obsoleto de una llamada anterior. El coste
        // de volver a llamar enableFilter()/setParameter() es trivial.
        Session session = entityManager.unwrap(Session.class);
        session.enableFilter(FILTRO).setParameter(PARAMETRO, negocioId);
    }

    /**
     * Intenta resolver el negocioId del contexto de seguridad actual. Si no
     * hay autenticación (login, registro, o cualquier llamada interna sin
     * JWT) el aspecto NUNCA debe romper ese flujo — simplemente no habilita
     * el filtro y el método de servicio continúa normalmente sin scoping
     * automático (la capa autoritativa sigue intacta para cualquier acceso
     * a un recurso concreto).
     *
     * @return negocioId del caller autenticado, o null si no hay contexto de seguridad
     */
    private Long obtenerNegocioIdSiAutenticado() {
        try {
            return SecurityUtils.getNegocioId();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
