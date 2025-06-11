package com.buildmaster.projecttracker.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

// Ensure this import is present, though it's often implicit
// import io.jsonwebtoken.Jwts; // Already present in your file

@Component
@Slf4j
public class JwtUtil {

    @Value("${app.jwt.secret:mySecretKey}")
    private String jwtSecret;

    @Value("${app.jwt.expiration:1800000}") // 30 minutes
    private int jwtExpirationMs;

    @Value("${app.jwt.refresh-expiration:86400000}") // 24 hours
    private int refreshExpirationMs;

    /**
     * Retrieves the signing key used for JWT operations.
     * The key is derived from the jwtSecret string.
     * @return A SecretKey for HMAC-SHA algorithms.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generates a JWT token for an authenticated user.
     * @param authentication The Spring Security Authentication object containing user details.
     * @return The generated JWT token string.
     */
    public String generateToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        return generateToken(userPrincipal.getUsername());
    }

    /**
     * Generates a standard JWT token for a given username.
     * @param username The username to include in the token's subject.
     * @return The generated JWT token string.
     */
    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, username, jwtExpirationMs);
    }

    /**
     * Generates a refresh JWT token for a given username.
     * @param username The username to include in the token's subject.
     * @return The generated refresh JWT token string.
     */
    public String generateRefreshToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, username, refreshExpirationMs);
    }

    /**
     * Creates a JWT token with specified claims, subject, and expiration.
     * This method uses the `signWith` method for signing, which is correct for jjwt 0.12.x.
     * @param claims A map of claims to include in the token.
     * @param subject The subject (usually username) of the token.
     * @param expiration The expiration time in milliseconds from now.
     * @return The compact JWT token string.
     */
    private String createToken(Map<String, Object> claims, String subject, int expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512) // Correct: Use signWith() for building/signing the JWT
                .compact();
    }

    /**
     * Extracts the username (subject) from a JWT token.
     * @param token The JWT token string.
     * @return The username extracted from the token.
     */
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    /**
     * Extracts the expiration date from a JWT token.
     * @param token The JWT token string.
     * @return The expiration Date of the token.
     */
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    /**
     * Generic method to extract a specific claim from a JWT token.
     * @param token The JWT token string.
     * @param claimsResolver A function to resolve the desired claim from the Claims object.
     * @param <T> The type of the claim to be extracted.
     * @return The extracted claim.
     */
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parses the JWT token to retrieve all its claims.
     * This method uses `Jwts.parserBuilder()` which is the correct approach for jjwt 0.12.x for parsing.
     * @param token The JWT token string.
     * @return The Claims body of the JWT.
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    /**
     * Checks if a JWT token has expired.
     * @param token The JWT token string.
     * @return True if the token is expired, false otherwise.
     */
    public Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    /**
     * Validates a JWT token against user details.
     * @param token The JWT token string.
     * @param userDetails The UserDetails object to validate against.
     * @return True if the token is valid for the user and not expired, false otherwise.
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = getUsernameFromToken(token);
            return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (JwtException | IllegalArgumentException e) {
            log.error("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validates a JWT token's signature and structure.
     * This method uses `Jwts.parserBuilder()` which is the correct approach for jjwt 0.12.x for parsing.
     * @param token The JWT token string.
     * @return True if the token is valid (signature and structure), false otherwise.
     */
    public Boolean validateToken(String token) {
        try {
            Jwts.parser()
                .setSigningKey(getSigningKey())
                .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }
}
