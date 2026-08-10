package com.ares.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la solicitud de creación de un Negocio ({@code POST
 * /api/admin/negocios}), disponible solo para el superadmin de plataforma.
 * No exige unicidad de {@code nombre} entre Negocios (confirmed assumption
 * #2).
 *
 * @author Ares
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrearNegocioRequest {

    /**
     * Nombre comercial del negocio.
     */
    private String nombre;

    /**
     * Email de contacto del negocio (opcional).
     */
    private String emailContacto;
}
