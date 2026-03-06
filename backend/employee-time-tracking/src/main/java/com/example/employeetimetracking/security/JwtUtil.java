package com.example.employeetimetracking.security;

import com.example.employeetimetracking.model.enums.UserRole;
import io.jsonwebtoken.*;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil{

    @Value("${spring.jwt.expiration-ms}")
    private Long expirationDuration;
    @Value("${spring.jwt.secret}")
    private String jwtSecret;

    public String generateJwtToken(String sub, Long userId, UserRole role){

        Date expirationDate = Date.from(Instant.now().plusMillis(expirationDuration));

        return Jwts.builder()
                .setSubject(sub)
                .claim("user_id", userId)
                .claim("role", role)
                .setExpiration(expirationDate)
                .signWith(getSigningKey())
                .compact();
    }

    public SecretKey getSigningKey(){
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    private Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return extractClaims(token).get("role", String.class);

    }
}



