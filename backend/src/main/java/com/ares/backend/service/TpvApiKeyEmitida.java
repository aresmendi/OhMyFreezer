package com.ares.backend.service;

import com.ares.backend.entity.TpvApiKey;

/**
 * Resultado de {@code TpvApiKeyAdminService.emitir()}: la entidad
 * persistida (con el hash bcrypt, prefijo, etc.) más la key completa EN
 * CLARO ({@code omf_tpv_<prefijo>_<secreto>}), que solo existe en memoria
 * en este instante — nunca se persiste ni se puede volver a recuperar
 * después. Es responsabilidad del llamador (controller, PR posterior)
 * devolverla al superadmin exactamente una vez.
 *
 * @param clave         Entidad {@link TpvApiKey} ya persistida
 * @param claveEnClaro  Key completa en claro, formato {@code omf_tpv_<prefijo>_<secreto>}
 * @author Ares
 * @version 1.0
 */
public record TpvApiKeyEmitida(TpvApiKey clave, String claveEnClaro) {
}
