package com.example.employeetimetracking.security;

import com.example.employeetimetracking.model.enums.UserRole;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;

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
        SecretKey secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        Date expirationDate = Date.from(Instant.now().plusMillis(expirationDuration));

        return Jwts.builder()
                .setHeaderParam("alg","HS256")
                .setHeaderParam("typ","JWT")
                .setSubject(sub)
                .claim("user_id", userId)
                .claim("role", role)
                .setExpiration(expirationDate)
                .signWith(secretKey)
                .compact();
    }
}



