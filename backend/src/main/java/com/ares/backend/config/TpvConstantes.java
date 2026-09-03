package com.ares.backend.config;

/**
 * Constantes compartidas del dominio de integración TPV. Única fuente de
 * verdad para el dominio de email reservado del usuario sintético (usado
 * simultáneamente en el provisioning, el rechazo de login y la exclusión
 * de listados — ver diseño, sección "Synthetic-User Containment"), el
 * formato del prefijo de API key (D-D del diseño) y los nombres de
 * cabecera HTTP del endpoint de ingesta.
 *
 * @author Ares
 * @version 1.0
 */
public final class TpvConstantes {

    private TpvConstantes() {
        // Clase de constantes: no instanciable.
    }

    /**
     * Dominio de email reservado para los usuarios sintéticos TPV
     * ({@code tpv+negocio-{id}@tpv.ohmyfreezer.invalid}). Este dominio
     * NUNCA puede autenticarse vía {@code /api/usuarios/login}, sea cual
     * sea la contraseña — ver {@code UsuarioService.login()}.
     */
    public static final String DOMINIO_EMAIL_TPV = "tpv.ohmyfreezer.invalid";

    /**
     * Prefijo fijo del formato de API key {@code omf_tpv_<prefijo>_<secreto>}
     * (D-D del diseño).
     */
    public static final String PREFIJO_API_KEY = "omf_tpv_";

    /**
     * Cabecera HTTP que porta la API key TPV en cada petición a
     * {@code /api/tpv/**}.
     */
    public static final String HEADER_API_KEY = "X-Tpv-Api-Key";

    /**
     * Cabecera HTTP que identifica el proveedor/adaptador a usar en
     * {@code POST /api/tpv/ventas}. Por defecto {@code "generico"} si se
     * omite; un valor desconocido es un 400 (petición malformada, no un
     * resultado de negocio).
     */
    public static final String HEADER_PROVEEDOR = "X-Tpv-Proveedor";
}
