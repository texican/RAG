package com.enterprise.rag.auth.security;

import com.enterprise.rag.shared.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration:3600}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration:604800}") long refreshTokenExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("tenantId", user.getTenant().getId().toString());
        claims.put("role", user.getRole().toString());
        claims.put("email", user.getEmail());
        claims.put("firstName", user.getFirstName());
        claims.put("lastName", user.getLastName());
        
        return createToken(claims, user.getEmail(), accessTokenExpiration);
    }

    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("tokenType", "refresh");
        
        return createToken(claims, user.getEmail(), refreshTokenExpiration);
    }

    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        Instant now = Instant.now();
        Instant expiryDate = now.plus(expiration, ChronoUnit.SECONDS);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiryDate))
                .signWith(secretKey)
                .compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public UUID extractUserId(String token) {
        String userIdStr = extractClaims(token).get("userId", String.class);
        return UUID.fromString(userIdStr);
    }

    public UUID extractTenantId(String token) {
        String tenantIdStr = extractClaims(token).get("tenantId", String.class);
        return tenantIdStr != null ? UUID.fromString(tenantIdStr) : null;
    }

    public String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }

    public boolean isTokenValid(String token, String username) {
        try {
            Claims claims = extractClaims(token);
            return claims.getSubject().equals(username) && !isTokenExpired(claims);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isRefreshToken(String token) {
        try {
            Claims claims = extractClaims(token);
            return "refresh".equals(claims.get("tokenType", String.class));
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }

    public Date getExpirationDate(String token) {
        return extractClaims(token).getExpiration();
    }

    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }
}