package com.ares.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de petición para asignar una receta a un mapeo SKU de TPV.
 *
 * @author Ares
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TpvSkuMappingRequest {

    /**
     * ID de la receta a asignar al mapeo.
     */
    @NotNull
    private Long recetaId;
}
