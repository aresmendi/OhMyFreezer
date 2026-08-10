package com.ares.backend.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Genera códigos de alta (signup codes) para nuevos Negocios. Un único
 * {@link SecureRandom} estático se comparte entre todas las llamadas: es
 * seguro para hilos concurrentes y evita el coste de reinicializar la
 * fuente de entropía en cada generación.
 * <p>
 * Alfabeto de 32 símbolos, en mayúsculas, sin los caracteres clásicamente
 * confundibles al dictar o retipear un código a mano: {@code I}/{@code 1}
 * y {@code O}/{@code 0}. Longitud 10 → 32^10 ≈ 2^50 combinaciones, no
 * enumerable a través de {@code POST /api/usuarios/register}. Sin
 * separadores ni agrupación visual: el código se compara por coincidencia
 * literal exacta en {@code UsuarioService.registrar()}, así que cualquier
 * carácter cosmético añadido solo aumenta la probabilidad de fallo al
 * retipearlo.
 *
 * @author Ares
 * @version 1.0
 */
@Component
public class SignupCodeGenerator {

    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LONGITUD = 10;

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Genera un código de alta aleatorio de {@value #LONGITUD} caracteres.
     * No comprueba unicidad contra la base de datos: esa responsabilidad es
     * de quien lo invoca (ver {@code NegocioAdminService.crearNegocio()}).
     *
     * @return Código de alta generado
     */
    public String generar() {
        StringBuilder sb = new StringBuilder(LONGITUD);
        for (int i = 0; i < LONGITUD; i++) {
            // ALFABETO.length() es potencia de dos (32), así que nextInt no
            // introduce sesgo de módulo entre los símbolos posibles.
            sb.append(ALFABETO.charAt(RANDOM.nextInt(ALFABETO.length())));
        }
        return sb.toString();
    }
}
