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

    /** Genera un token JWT para el usuario dado. */
    public String generarToken(Long id,String username, boolean esJefeCocina) {
        return Jwts.builder()
                .claim("id", id)
                .subject(username)
                .claim("esJefeCocina", esJefeCocina)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key)
                .compact();
    }

    /** Extrae el id de usuario del token*/
    public Long extraerUsuarioId(String token) {
        return ((Number) parsear(token).getPayload().get("id")).longValue();
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