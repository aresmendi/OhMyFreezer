package com.ares.backend.tpv;

import com.ares.backend.entity.Ingrediente;

/**
 * Proyección interna que asocia un ingrediente con la cantidad que debe
 * descontarse (en su unidad base) al procesar una venta TPV.
 *
 * @param ingrediente ingrediente afectado
 * @param cantidad    cantidad a descontar, expresada en la unidad base del ingrediente
 *
 * @author Ares
 * @version 1.0
 */
public record DeltaIngrediente(Ingrediente ingrediente, Double cantidad) {}
