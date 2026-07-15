package com.ares.backend.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios puros (sin contexto de Spring) para JwtUtil.
 * Verifican la generación/validación de tokens y, en particular, que la
 * expiración es configurable vía constructor y que el valor por defecto
 * (cuando no se inyecta ninguna propiedad) sigue siendo 24h.
 */
class JwtUtilTest {

    private static final String SECRET = "clave-de-test-para-jwt-con-mas-de-treinta-y-dos-bytes-1234567890";
    private static final long DEFAULT_EXPIRATION_MS = 86400000L;

    @Test
    @DisplayName("Genera un token válido y lo valida correctamente")
    void generarYValidarToken_ok() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, DEFAULT_EXPIRATION_MS);

        String token = jwtUtil.generarToken(1L, "usuario", true);

        assertThat(jwtUtil.validarToken(token)).isTrue();
    }

    @Test
    @DisplayName("Un token manipulado se considera inválido")
    void tokenManipulado_invalido() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, DEFAULT_EXPIRATION_MS);

        String token = jwtUtil.generarToken(1L, "usuario", true);
        // Se altera un carácter del payload (no el último, que en base64url
        // puede no afectar los bytes decodificados por el padding de bits).
        int mid = token.length() / 2;
        char alterado = token.charAt(mid) == 'a' ? 'b' : 'a';
        String tokenManipulado = token.substring(0, mid) + alterado + token.substring(mid + 1);

        assertThat(jwtUtil.validarToken(tokenManipulado)).isFalse();
    }

    @Test
    @DisplayName("Extrae id, username y rol correctamente del token")
    void extraeClaims_id_username_esJefeCocina() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, DEFAULT_EXPIRATION_MS);

        String token = jwtUtil.generarToken(42L, "jefe", true);

        assertThat(jwtUtil.extraerUsuarioId(token)).isEqualTo(42L);
        assertThat(jwtUtil.extraerUsername(token)).isEqualTo("jefe");
        assertThat(jwtUtil.extraerEsJefeCocina(token)).isTrue();
    }

    @Test
    @DisplayName("Un token ya expirado se considera inválido")
    void tokenExpirado_invalido() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, -1000L);

        String token = jwtUtil.generarToken(1L, "usuario", false);

        assertThat(jwtUtil.validarToken(token)).isFalse();
    }

    @Test
    @DisplayName("Con el valor por defecto, el token expira exactamente a las 24h")
    void expiracionPorDefecto_24h() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, DEFAULT_EXPIRATION_MS);

        String token = jwtUtil.generarToken(1L, "usuario", false);

        SecretKey key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(SECRET.getBytes());
        Jws<Claims> parsed = Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
        Date issuedAt = parsed.getPayload().getIssuedAt();
        Date expiration = parsed.getPayload().getExpiration();

        assertThat(expiration.getTime() - issuedAt.getTime()).isEqualTo(DEFAULT_EXPIRATION_MS);
    }

    @Test
    @DisplayName("El token generado con negocioId lleva la claim firmada y extraerNegocioId la recupera")
    void generarToken_conNegocioId_extraeClaim() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, DEFAULT_EXPIRATION_MS);

        String token = jwtUtil.generarToken(7L, "jefe", true, 3L);

        assertThat(jwtUtil.extraerNegocioId(token)).isEqualTo(3L);
    }

    @Test
    @DisplayName("Un token generado sin negocioId (legacy, pre-multitenancy) no lleva la claim")
    void generarToken_legacySinNegocioId_extraerNegocioIdEsNull() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, DEFAULT_EXPIRATION_MS);

        // Sobrecarga de 3 argumentos: simula un token emitido antes de que
        // existiera la claim negocioId.
        String tokenLegacy = jwtUtil.generarToken(1L, "usuario", false);

        assertThat(jwtUtil.extraerNegocioId(tokenLegacy)).isNull();
    }

    @Test
    @DisplayName("Dos negocios distintos producen valores distintos en la claim (triangulación)")
    void generarToken_conNegocioIdDistinto_extraeValorDistinto() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, DEFAULT_EXPIRATION_MS);

        String tokenNegocioA = jwtUtil.generarToken(1L, "usuarioA", false, 1L);
        String tokenNegocioB = jwtUtil.generarToken(2L, "usuarioB", false, 99L);

        assertThat(jwtUtil.extraerNegocioId(tokenNegocioA)).isEqualTo(1L);
        assertThat(jwtUtil.extraerNegocioId(tokenNegocioB)).isEqualTo(99L);
    }
}
