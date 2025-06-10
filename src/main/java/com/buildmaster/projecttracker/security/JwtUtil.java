package com.buildmaster.projecttracker.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
public class JwtUtil {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret:MxZxrt9rpDlaFZcLrvXlwQh/P3K3uUvd2USpwlKrxxKFPFahoYO5/3F/RMjheId46xb092EZgtfqIzijKBCHiw==}")
    private String jwtSecret;

    @Value("${jwt.expiration.ms:3600000}")
    private long jwtExpirationMs;

    private SecretKey secretKey;
    private JwtParser jwtParser; // Reusable parser instance

    @PostConstruct
    public void init() {
        // Validate secret key length for HS512
        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < 64) {
            logger.warn("JWT secret key should be at least 512 bits (64 bytes) for HMAC-SHA512");
        }

        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        // Create reusable parser instance
        this.jwtParser = Jwts.parser()
                .verifyWith(secretKey)
                .build();
    }

    /**
     * Generate JWT token for a given username
     * @param username the username to encode in the token
     * @return JWT token string
     */
    public String generateToken(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        Instant now = Instant.now();
        Instant expiration = now.plus(jwtExpirationMs, ChronoUnit.MILLIS);

        return Jwts.builder()
                .subject(username.trim())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(secretKey, Jwts.SIG.HS512)
                .compact();
    }

    /**
     * Extract username from JWT token
     * @param token JWT token
     * @return username from token subject
     * @throws JwtException if token is invalid
     */
    public String getUsernameFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    /**
     * Validate JWT token
     * @param token JWT token to validate
     * @return true if token is valid, false otherwise
     */
    public boolean validateJwtToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        try {
            jwtParser.parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            logger.debug("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.debug("JWT token is unsupported: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.debug("JWT token is malformed: {}", e.getMessage());
        } catch (SecurityException e) {
            logger.debug("JWT signature validation failed: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.debug("JWT token compact of handler are invalid: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Extract all claims from JWT token
     * @param token JWT token
     * @return Claims object containing all token claims
     * @throws JwtException if token is invalid
     */
    public Claims getAllClaimsFromToken(String token) {
        return getClaimsFromToken(token);
    }

    /**
     * Check if token is expired
     * @param token JWT token
     * @return true if token is expired, false otherwise
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getClaimsFromToken(token).getExpiration();
            return expiration.before(new Date());
        } catch (JwtException e) {
            return true; // Consider invalid tokens as expired
        }
    }

    /**
     * Extract expiration date from token
     * @param token JWT token
     * @return expiration date
     */
    public Date getExpirationDateFromToken(String token) {
        return getClaimsFromToken(token).getExpiration();
    }

    /**
     * Get remaining time until token expires in milliseconds
     * @param token JWT token
     * @return remaining time in milliseconds, or 0 if expired/invalid
     */
    public long getRemainingTimeMs(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            long remaining = expiration.getTime() - System.currentTimeMillis();
            return Math.max(0, remaining);
        } catch (JwtException e) {
            return 0;
        }
    }

    /**
     * Private helper method to extract claims from token
     * @param token JWT token
     * @return Claims object
     * @throws JwtException if token is invalid
     */
    private Claims getClaimsFromToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }

        return jwtParser.parseSignedClaims(token).getPayload();
    }
}