package com.ares.backend.service;

import com.ares.backend.config.TpvConstantes;
import com.ares.backend.entity.TpvApiKey;
import com.ares.backend.entity.Usuario;
import com.ares.backend.repository.TpvApiKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Resuelve la autenticación de una API key TPV para {@code TpvApiKeyFilter}
 * (D-D del diseño). Formato en claro: {@code omf_tpv_<prefijo>_<secreto>}.
 * Solo declara como colaboradores {@link TpvApiKeyRepository} y {@link
 * PasswordEncoder} — resolución de key, ninguna lógica de negocio tenant.
 * <p>
 * El coste está intencionadamente concentrado en un único
 * {@code passwordEncoder.matches()} TRAS un lookup indexado por prefijo en
 * claro: bcrypt es deliberadamente lento (~100 ms), así que comparar contra
 * TODAS las credenciales activas del sistema en cada petición sería
 * inviable (ver diseño D-D).
 *
 * @author Ares
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class TpvAutenticacionService {

    private final TpvApiKeyRepository tpvApiKeyRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Autentica una API key TPV completa. Formato inválido, prefijo
     * desconocido, credencial revocada o secreto incorrecto: en todos los
     * casos devuelve {@link Optional#empty()} — nunca revela cuál de esas
     * causas fue (misma filosofía que {@code UsuarioService.login()}).
     *
     * @param apiKeyCompleta Valor literal de la cabecera {@code X-Tpv-Api-Key}
     * @return El usuario sistema (sintético) de la credencial, si la key es válida
     */
    public Optional<Usuario> autenticar(String apiKeyCompleta) {
        if (apiKeyCompleta == null || !apiKeyCompleta.startsWith(TpvConstantes.PREFIJO_API_KEY)) {
            return Optional.empty();
        }

        String resto = apiKeyCompleta.substring(TpvConstantes.PREFIJO_API_KEY.length());
        int separador = resto.indexOf('_');
        if (separador <= 0 || separador == resto.length() - 1) {
            // Sin separador, o prefijo/secreto vacío a cualquiera de los dos lados.
            return Optional.empty();
        }

        String prefijo = resto.substring(0, separador);
        String secreto = resto.substring(separador + 1);

        // Lookup O(1) por prefijo indexado en claro: si no hay fila activa
        // (prefijo desconocido O credencial revocada), ni siquiera se llega
        // a invocar bcrypt — ver Javadoc de clase.
        return tpvApiKeyRepository.findByPrefijoAndActivaTrue(prefijo)
                .filter(clave -> passwordEncoder.matches(secreto, clave.getSecretoHash()))
                .map(TpvApiKey::getUsuarioSistema);
    }
}
