package com.ares.backend.tpv;

import com.ares.backend.dto.VentaTpvDto;
import com.ares.backend.entity.TipoEventoTpv;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Adaptador de proveedor genérico. Espera un JSON plano con los mismos
 * campos que {@link VentaTpvDto}:
 * <pre>{@code
 * {
 *   "tipo":               "VENTA" | "ANULACION",
 *   "externalId":         "tx-001",
 *   "externalIdOriginal": null,         // opcional
 *   "skuTpv":             "CAFE-001",
 *   "cantidad":           2,
 *   "fechaTpv":           "2026-11-11T14:30:00"  // opcional
 * }
 * }</pre>
 * <p>
 * Es el adaptador por defecto cuando el cliente omite la cabecera
 * {@code X-Tpv-Proveedor} (ver {@link TpvProveedorRegistry}).
 *
 * @author Ares
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GenericoTpvAdapter implements TpvProveedorAdapter {

    private final ObjectMapper objectMapper;

    @Override
    public String getNombre() {
        return "generico";
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException si el JSON es inválido o faltan
     *                                  campos obligatorios (tipo, externalId,
     *                                  skuTpv, cantidad)
     */
    @Override
    public VentaTpvDto parsear(String rawPayload) {
        JsonNode nodo;
        try {
            nodo = objectMapper.readTree(rawPayload);
        } catch (Exception e) {
            throw new IllegalArgumentException("Payload TPV con JSON inválido: " + e.getMessage(), e);
        }

        String tipoStr = campo(nodo, "tipo");
        TipoEventoTpv tipo;
        try {
            tipo = TipoEventoTpv.valueOf(tipoStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Valor de 'tipo' no reconocido: " + tipoStr);
        }

        String externalId = campo(nodo, "externalId");
        String skuTpv = campo(nodo, "skuTpv");

        JsonNode cantidadNodo = nodo.get("cantidad");
        if (cantidadNodo == null || cantidadNodo.isNull()) {
            throw new IllegalArgumentException("Campo obligatorio ausente: 'cantidad'");
        }
        Integer cantidad = cantidadNodo.asInt();

        String externalIdOriginal = nodoOpcionalTexto(nodo, "externalIdOriginal");

        LocalDateTime fechaTpv = null;
        JsonNode fechaNodo = nodo.get("fechaTpv");
        if (fechaNodo != null && !fechaNodo.isNull()) {
            try {
                fechaTpv = LocalDateTime.parse(fechaNodo.asText());
            } catch (Exception e) {
                throw new IllegalArgumentException("Formato de 'fechaTpv' inválido: " + fechaNodo.asText(), e);
            }
        }

        return new VentaTpvDto(tipo, externalId, externalIdOriginal, skuTpv, cantidad, fechaTpv);
    }

    /**
     * Extrae un campo String obligatorio; lanza si está ausente o es null.
     */
    private String campo(JsonNode nodo, String nombre) {
        JsonNode valor = nodo.get(nombre);
        if (valor == null || valor.isNull()) {
            throw new IllegalArgumentException("Campo obligatorio ausente: '" + nombre + "'");
        }
        String texto = valor.asText();
        if (texto.isBlank()) {
            throw new IllegalArgumentException("Campo obligatorio vacío: '" + nombre + "'");
        }
        return texto;
    }

    /**
     * Extrae un campo String opcional; devuelve null si está ausente o es null.
     */
    private String nodoOpcionalTexto(JsonNode nodo, String nombre) {
        JsonNode valor = nodo.get(nombre);
        if (valor == null || valor.isNull()) {
            return null;
        }
        return valor.asText();
    }
}
