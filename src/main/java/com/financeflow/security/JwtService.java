package com.financeflow.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;
    private final long refreshExpirationMs;

    public JwtService(
            @Value("${financeflow.jwt.secret}") String secret,
            @Value("${financeflow.jwt.expiration-ms}") long expirationMs,
            @Value("${financeflow.jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String generateToken(String email) {
        return buildToken(email, expirationMs, "access");
    }

    public String generateRefreshToken(String email) {
        return buildToken(email, refreshExpirationMs, "refresh");
    }

    private String buildToken(String email, long ttlMs, String type) {
        var now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(email)
                .claim("type", type)
                .issuedAt(new Date(now))
                .expiration(new Date(now + ttlMs))
                .signWith(key)
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, String expectedType) {
        try {
            var claims = parseClaims(token);
            var type = claims.get("type", String.class);
            return expectedType.equals(type) && claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}