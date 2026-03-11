package com.ares.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO para representar un dato de estadística (fecha y número de usos).
 *
 * @author Ares
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatoEstadisticaDTO {

    /**
     * Fecha del dato estadístico.
     */
    private LocalDate fecha;

    /**
     * Número de usos en esa fecha.
     */
    private Integer usos;
}