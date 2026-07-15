package com.ares.backend.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expirationMs;

    public JwtUtil(
            @Value("${JWT_SECRET_CODE}") String secret,
            @Value("${app.jwt.expiration-ms:86400000}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationMs = expirationMs;
    }

    /**
     * Genera un token JWT para el usuario dado, incluyendo la claim firmada
     * {@code negocioId} (multi-tenancy). Esta claim se deriva siempre en el
     * servidor a partir del negocio real del usuario; nunca se acepta un
     * negocioId propuesto por el cliente.
     */
    public String generarToken(Long id, String username, boolean esJefeCocina, Long negocioId) {
        var builder = Jwts.builder()
                .claim("id", id)
                .subject(username)
                .claim("esJefeCocina", esJefeCocina);

        if (negocioId != null) {
            builder.claim("negocioId", negocioId);
        }

        return builder
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key)
                .compact();
    }

    /**
     * Sobrecarga que genera un token sin claim {@code negocioId}.
     *
     * @deprecated Solo debe usarse para simular tokens legacy (emitidos antes
     * de la multi-tenancy) en tests. El código de producción SIEMPRE debe
     * llamar a la sobrecarga con {@code negocioId}.
     */
    @Deprecated
    public String generarToken(Long id, String username, boolean esJefeCocina) {
        return generarToken(id, username, esJefeCocina, null);
    }

    /** Extrae el id de usuario del token*/
    public Long extraerUsuarioId(String token) {
        return ((Number) parsear(token).getPayload().get("id")).longValue();
    }

    /**
     * Extrae la claim {@code negocioId} del token.
     *
     * @return el negocioId firmado, o {@code null} si el token no lleva la
     * claim (token legacy emitido antes de la multi-tenancy).
     */
    public Long extraerNegocioId(String token) {
        Object claim = parsear(token).getPayload().get("negocioId");
        return claim == null ? null : ((Number) claim).longValue();
    }

    /** Extrae el username del token. */
    public String extraerUsername(String token) {
        return parsear(token).getPayload().getSubject();
    }

    /** Extrae el rol del token. */
    public boolean extraerEsJefeCocina(String token) {
        return (Boolean) parsear(token).getPayload().get("esJefeCocina");
    }

    /** Valida que el token sea correcto y no haya expirado. */
    public boolean validarToken(String token) {
        try {
            parsear(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Jws<Claims> parsear(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
    }
}