package com.ares.backend.service;

import com.ares.backend.config.SecurityUtils;
import com.ares.backend.dto.AlertaCountResponse;
import com.ares.backend.dto.AlertaResponse;
import com.ares.backend.dto.IngredienteFaltanteDTO;
import com.ares.backend.entity.Alerta;
import com.ares.backend.entity.Ingrediente;
import com.ares.backend.entity.Receta;
import com.ares.backend.entity.Usuario;
import com.ares.backend.repository.AlertaRepository;
import com.ares.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para la gestión de alertas.
 *
 * @author Ares
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class AlertaService {

    private final AlertaRepository alertaRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Crea una alerta de stock bajo para un ingrediente.
     * Si ya existe una alerta no leída para ese ingrediente, actualiza su fecha y mensaje.
     *
     * @param ingrediente Ingrediente con stock bajo
     */
    @Transactional
    public void crearAlertaStockBajo(Ingrediente ingrediente) {
        List<Alerta> alertasExistentes = alertaRepository.findByIngredienteIdAndLeidaFalse(ingrediente.getId());

        String mensaje = String.format("Stock bajo: %s (%.2f %s disponibles, mínimo: %.2f %s)",
                ingrediente.getNombre(),
                ingrediente.getCantidad(),
                ingrediente.getUnidadMedida(),
                ingrediente.getStockMinimo(),
                ingrediente.getUnidadMedida());

        if (!alertasExistentes.isEmpty()) {
            for (Alerta alerta : alertasExistentes) {
                alerta.setMensaje(mensaje);
                alerta.setFechaCreacion(LocalDateTime.now());
            }
            alertaRepository.saveAll(alertasExistentes);
            return;
        }

        List<Usuario> jefes = usuarioRepository.findByEsJefeCocinaTrue();

        for (Usuario jefe : jefes) {
            Alerta alerta = new Alerta();
            alerta.setTipo("STOCK_BAJO");
            alerta.setMensaje(mensaje);
            alerta.setIngrediente(ingrediente);
            alerta.setDestinatario(jefe);
            alerta.setFechaCreacion(LocalDateTime.now());
            alerta.setLeida(false);

            alertaRepository.save(alerta);
        }
    }

    /**
     * Crea una alerta de Escaldaio cuando el stock de un ingrediente se actualiza
     * y desciende respecto al valor anterior (stock lower-down respecto al previo).
     * Si ya existe una alerta de Escaldaio no leída para ese ingrediente, se actualiza
     * su mensaje y fecha; de lo contrario se crean alertas para los jefes.
     *
     * @param ingrediente Ingrediente afectado
     * @param anterior Stock anterior
     * @param nuevo Stock nuevo
     */
    @Transactional
    public void crearAlertaEscaldaio(Ingrediente ingrediente, Double anterior, Double nuevo) {
        String mensaje = String.format("Escaldaio: stock de %s actualizado de %.2f %s a %.2f %s",
                ingrediente.getNombre(), anterior, ingrediente.getUnidadMedida(), nuevo, ingrediente.getUnidadMedida());

        List<Alerta> alertasExistentes = alertaRepository.findByIngredienteIdAndLeidaFalse(ingrediente.getId())
                .stream()
                .filter(a -> "ESCALDAIO".equals(a.getTipo()))
                .collect(Collectors.toList());

        if (!alertasExistentes.isEmpty()) {
            for (Alerta a : alertasExistentes) {
                a.setMensaje(mensaje);
                a.setFechaCreacion(LocalDateTime.now());
            }
            alertaRepository.saveAll(alertasExistentes);
            return;
        }

        List<Usuario> jefes = usuarioRepository.findByEsJefeCocinaTrue();

        for (Usuario jefe : jefes) {
            Alerta alerta = new Alerta();
            alerta.setTipo("ESCALDAIO");
            alerta.setMensaje(mensaje);
            alerta.setIngrediente(ingrediente);
            alerta.setDestinatario(jefe);
            alerta.setFechaCreacion(LocalDateTime.now());
            alerta.setLeida(false);

            alertaRepository.save(alerta);
        }
    }

    /**
     * Crea una alerta de receta no disponible.
     * Usa REQUIRES_NEW para garantizar que la alerta se persiste aunque el llamador haga rollback.
     *
     * @param receta Receta no disponible
     * @param ingredientesFaltantes Lista de ingredientes faltantes
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void crearAlertaRecetaNoDisponible(Receta receta, List<IngredienteFaltanteDTO> ingredientesFaltantes) {
        boolean existeAlerta = alertaRepository.existsByRecetaIdAndLeidaFalse(receta.getId());
        if (existeAlerta) {
            return;
        }

        List<Usuario> jefes = usuarioRepository.findByEsJefeCocinaTrue();

        String ingredientesFaltantesStr = ingredientesFaltantes.stream()
                .map(i -> String.format("%s (necesita: %.2f, disponible: %.2f)",
                        i.getIngrediente().getNombre(),
                        i.getCantidadNecesaria(),
                        i.getCantidadDisponible()))
                .collect(Collectors.joining(", "));

        for (Usuario jefe : jefes) {
            Alerta alerta = new Alerta();
            alerta.setTipo("RECETA_NO_DISPONIBLE");
            alerta.setMensaje(String.format("Receta '%s' no disponible. Ingredientes faltantes: %s",
                    receta.getNombre(),
                    ingredientesFaltantesStr));
            alerta.setReceta(receta);
            alerta.setDestinatario(jefe);
            alerta.setFechaCreacion(LocalDateTime.now());
            alerta.setLeida(false);

            alertaRepository.save(alerta);
        }
    }


    /**
     * Obtiene todas las alertas del usuario autenticado.
     *
     * @return Lista de alertas
     */
    public List<AlertaResponse> obtenerPorUsuario() {
        Long usuarioId = SecurityUtils.getUsuarioId();
        return alertaRepository.findByDestinatarioIdOrderByFechaCreacionDesc(usuarioId).stream()
                .map(AlertaResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene las alertas no leídas del usuario autenticado.
     *
     * @return Lista de alertas no leídas
     */
    public List<AlertaResponse> obtenerNoLeidasPorUsuario() {
        Long usuarioId = SecurityUtils.getUsuarioId();
        return alertaRepository.findByDestinatarioIdAndLeidaFalseOrderByFechaCreacionDesc(usuarioId).stream()
                .map(AlertaResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Marca una alerta como leída.
     *
     * @param id ID de la alerta
     * @return Alerta actualizada
     * @throws IllegalArgumentException Si la alerta no existe
     */
    @Transactional
    public AlertaResponse marcarComoLeida(Long id) {
        Alerta alerta = alertaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alerta no encontrada"));

        alerta.setLeida(true);
        Alerta alertaGuardada = alertaRepository.save(alerta);
        return new AlertaResponse(alertaGuardada);
    }

    /**
     * Marca todas las alertas del usuario autenticado como leídas.
     */
    @Transactional
    public void marcarTodasComoLeidas() {
        Long usuarioId = SecurityUtils.getUsuarioId();
        List<Alerta> alertas = alertaRepository.findByDestinatarioIdAndLeidaFalse(usuarioId);
        for (Alerta alerta : alertas) {
            alerta.setLeida(true);
        }
        alertaRepository.saveAll(alertas);
    }

    /**
     * Obtiene el conteo de alertas no leídas del usuario autenticado.
     *
     * @return Conteo de alertas pendientes
     */
    public AlertaCountResponse contarNoLeidas() {
        Long usuarioId = SecurityUtils.getUsuarioId();
        Long count = alertaRepository.countByDestinatarioIdAndLeidaFalse(usuarioId);
        return new AlertaCountResponse(count);
    }
    /**
     * Elimina todas las alertas relacionadas con una receta.
     *
     * @param receta Receta cuyas alertas se eliminarán
     */
    @Transactional
    public void eliminarPorReceta(Receta receta) {
        alertaRepository.deleteByReceta(receta);
    }
    /**
     * Elimina todas las alertas relacionadas con un ingrediente.
     *
     * @param ingrediente Ingrediente cuyas alertas se eliminarán
     */
    @Transactional
    public void eliminarPorIngrediente(Ingrediente ingrediente) {
        alertaRepository.deleteByIngrediente(ingrediente);
    }
}
