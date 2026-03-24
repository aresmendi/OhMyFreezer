package com.ares.backend.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    // TODO: Clave secreta — cambiar en producción por una variable de entorno
    private static final String SECRET = "ohmyfreezer-clave-secreta-muy-larga-2024-segura";
    private static final long EXPIRATION_MS = 86400000L; // 24 horas

    private final SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());

    /** Genera un token JWT para el usuario dado. */
    public String generarToken(String username, boolean esJefeCocina) {
        return Jwts.builder()
                .subject(username)
                .claim("esJefeCocina", esJefeCocina)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(key)
                .compact();
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