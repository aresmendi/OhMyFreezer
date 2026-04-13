package com.ares.backend.service;

import com.ares.backend.entity.Alerta;
import com.ares.backend.entity.Ingrediente;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void enviarNotificacionAlerta(Alerta alerta) {
        String email = alerta.getDestinatario().getEmail();
        if (email == null || email.isBlank()) {
            log.warn("Usuario {} no tiene email configurado, se omite envío",
                    alerta.getDestinatario().getUsername());
            return;
        }

        Ingrediente ingrediente = alerta.getIngrediente();
        String tipoAlerta = formatearTipo(alerta.getTipo());
        String ingredienteNombre = ingrediente != null ? ingrediente.getNombre() : "N/A";
        String cantidadActual = ingrediente != null
                ? String.format("%.2f %s", ingrediente.getCantidad(), ingrediente.getUnidadMedida())
                : "N/A";

        String asunto = String.format("[OhMyFreezer] Alerta: %s - %s", tipoAlerta, ingredienteNombre);

        String cuerpo = String.format(
                "Se ha generado una nueva alerta de stock en OhMyFreezer.\n\n" +
                "Detalles:\n" +
                "--------\n" +
                "Ingrediente: %s\n" +
                "Tipo de alerta: %s\n" +
                "Cantidad actual: %s\n" +
                "Fecha: %s\n" +
                "\nMensaje: %s\n",
                ingredienteNombre,
                tipoAlerta,
                cantidadActual,
                alerta.getFechaCreacion().toString(),
                alerta.getMensaje()
        );

        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(email);
        mensaje.setSubject(asunto);
        mensaje.setText(cuerpo);

        try {
            mailSender.send(mensaje);
            log.info("Email enviado a {} para alerta id={}", email, alerta.getId());
        } catch (Exception e) {
            log.error("Error al enviar email a {}: {}", email, e.getMessage());
        }
    }

    private String formatearTipo(String tipo) {
        return switch (tipo) {
            case "STOCK_BAJO" -> "Stock bajo";
            case "ESCALDAIO" -> "Escaldado";
            case "RECETA_NO_DISPONIBLE" -> "Receta no disponible";
            default -> tipo;
        };
    }
}
