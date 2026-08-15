package com.ares.backend.dto;

import com.ares.backend.entity.Negocio;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para un Negocio, expuesto SOLO en la API del superadmin de
 * plataforma ({@code /api/admin/**}). Representa el propio Negocio; no
 * incluye ningún dato tenant-owned (usuarios, ingredientes, recetas, etc.).
 *
 * @author Ares
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NegocioAdminResponse {

    /**
     * Identificador único del negocio.
     */
    private Long id;

    /**
     * Nombre comercial del negocio. No es único (duplicados permitidos).
     */
    private String nombre;

    /**
     * Plan de suscripción del negocio (por ahora siempre "FREE").
     */
    private String plan;

    /**
     * Fecha y hora de alta del negocio en el sistema.
     */
    private LocalDateTime fechaAlta;

    /**
     * Email de contacto del negocio (opcional).
     */
    private String emailContacto;

    /**
     * Constructor que convierte una entidad Negocio a NegocioAdminResponse.
     *
     * @param negocio Entidad Negocio a convertir
     */
    public NegocioAdminResponse(Negocio negocio) {
        this.id = negocio.getId();
        this.nombre = negocio.getNombre();
        this.plan = negocio.getPlan();
        this.fechaAlta = negocio.getFechaAlta();
        this.emailContacto = negocio.getEmailContacto();
    }
}
