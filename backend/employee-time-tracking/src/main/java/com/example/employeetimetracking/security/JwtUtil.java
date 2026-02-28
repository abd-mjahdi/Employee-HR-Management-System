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

        JwtBuilder jwtToken = Jwts.builder();
        jwtToken.setHeaderParam("alg","HS256");
        jwtToken.setHeaderParam("typ","JWT");


        Instant expirationInstant = Instant.now().plusMillis(this.expirationDuration);
        Date expirationDate = Date.from(expirationInstant);

        jwtToken.setSubject(sub);
        jwtToken.claim("user_id",userId);
        jwtToken.claim("role",role);
        jwtToken.setExpiration(expirationDate);

        String secretString = this.jwtSecret;
        SecretKey secretKey = Keys.hmacShaKeyFor(secretString.getBytes());
        jwtToken.signWith((secretKey));

        return jwtToken.compact();


    }
}



