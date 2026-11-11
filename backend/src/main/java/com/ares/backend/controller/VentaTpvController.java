package com.ares.backend.controller;

import com.ares.backend.config.TpvConstantes;
import com.ares.backend.dto.ResultadoIngestaResponse;
import com.ares.backend.tpv.VentaTpvIngestaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoint de ingesta de ventas desde terminales TPV.
 * Solo delega en {@link VentaTpvIngestaService}; no contiene lógica de negocio.
 * Siempre responde 200: el estado del procesamiento viaja en el body.
 *
 * @author Ares
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/tpv/ventas")
@RequiredArgsConstructor
public class VentaTpvController {

    private final VentaTpvIngestaService ingestaService;

    /**
     * Recibe un evento de venta o anulación desde un TPV.
     *
     * @param rawPayload cuerpo HTTP con el payload crudo del proveedor
     * @param proveedor  valor de la cabecera {@code X-Tpv-Proveedor}; por
     *                   defecto {@code "generico"} si se omite
     * @return resultado del procesamiento (200 siempre)
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResultadoIngestaResponse> ingestar(
            @RequestBody String rawPayload,
            @RequestHeader(value = TpvConstantes.HEADER_PROVEEDOR, defaultValue = "generico") String proveedor) {
        return ResponseEntity.ok(ingestaService.ingestar(rawPayload, proveedor));
    }
}
