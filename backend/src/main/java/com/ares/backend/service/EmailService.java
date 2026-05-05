package com.ares.backend.service;

import com.ares.backend.entity.Alerta;
import com.ares.backend.entity.Ingrediente;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Slf4j
public class EmailService {

    private final SendGrid sendGrid;
    private final String fromEmail;

    public EmailService(@Value("${sendgrid.api-key}") String apiKey,
                        @Value("${sendgrid.from-email}") String fromEmail) {
        this.sendGrid = new SendGrid(apiKey);
        this.fromEmail = fromEmail;
    }

    @Async
    public void enviarNotificacionAlerta(Alerta alerta) {
        String destinatario = alerta.getDestinatario().getEmail();
        if (destinatario == null || destinatario.isBlank()) {
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
                ingredienteNombre, tipoAlerta, cantidadActual,
                alerta.getFechaCreacion().toString(), alerta.getMensaje()
        );

        Mail mail = new Mail(new Email(fromEmail, "OhMyFreezer"),
                asunto,
                new Email(destinatario),
                new Content("text/plain", cuerpo));

        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            var response = sendGrid.api(request);
            if (response.getStatusCode() >= 400) {
                log.error("Error al enviar email a {} (status {}): {}", destinatario, response.getStatusCode(), response.getBody());
            } else {
                log.info("Email enviado a {} para alerta id={}", destinatario, alerta.getId());
            }
        } catch (IOException e) {
            log.error("Error al enviar email a {}: {}", destinatario, e.getMessage());
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
