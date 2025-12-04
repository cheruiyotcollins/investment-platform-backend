package com.investment.backend.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;  // <-- Use this import for Spring Boot 3+
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtTokenUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenUtil.class);

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.token-validity}")
    private Long expiration;

    private Key signingKey;

    @PostConstruct
    public void init() {
        logger.info("Initializing JwtTokenUtil...");
        logger.info("JWT Secret length: {}", secret != null ? secret.length() : "NULL");
        logger.info("JWT Expiration: {} ms ({} minutes)", expiration, expiration / 60000);

        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException("JWT secret is not configured. Set app.jwt.secret in application.yml or as environment variable JWT_SECRET");
        }

        if (secret.length() < 32) {
            logger.warn("JWT secret is only {} characters. For production, use at least 32 characters (256 bits).", secret.length());
        }

        try {
            this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
            logger.info("JWT signing key initialized successfully");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize JWT signing key: " + e.getMessage(), e);
        }
    }

    private Key getSigningKey() {
        if (signingKey == null) {
            throw new IllegalStateException("JWT signing key not initialized. Check JWT secret configuration.");
        }
        return signingKey;
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, userDetails.getUsername());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        try {
            return Jwts.builder()
                    .setClaims(claims)
                    .setSubject(subject)
                    .setIssuedAt(new Date(System.currentTimeMillis()))
                    .setExpiration(new Date(System.currentTimeMillis() + expiration))
                    .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                    .compact();
        } catch (Exception e) {
            logger.error("Failed to create JWT token", e);
            throw new RuntimeException("Failed to create JWT token", e);
        }
    }

    public String getUsernameFromToken(String token) {
        try {
            return getClaimFromToken(token, Claims::getSubject);
        } catch (ExpiredJwtException e) {
            logger.warn("JWT token expired: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Failed to get username from token", e);
            throw new RuntimeException("Invalid JWT token", e);
        }
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    private Claims getAllClaimsFromToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            logger.warn("JWT token expired: {}", e.getMessage());
            throw e;
        } catch (SecurityException e) {
            logger.error("JWT security exception: {}", e.getMessage());
            throw new RuntimeException("Invalid JWT signature", e);
        } catch (MalformedJwtException e) {
            logger.error("Malformed JWT token: {}", e.getMessage());
            throw new RuntimeException("Malformed JWT token", e);
        } catch (Exception e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
            throw new RuntimeException("Invalid JWT token", e);
        }
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = getUsernameFromToken(token);
            return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (Exception e) {
            logger.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    private Boolean isTokenExpired(String token) {
        try {
            final Date expiration = getExpirationDateFromToken(token);
            return expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            logger.error("Failed to check token expiration", e);
            return true; // If we can't parse, treat as expired
        }
    }

    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }
}