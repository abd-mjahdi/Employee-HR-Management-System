package com.example.employeetimetracking.security;

import com.example.employeetimetracking.model.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${spring.jwt.expiration-ms}")
    private Long expirationDuration;
    @Value("${spring.jwt.secret}")
    private String jwtSecret;

    public String generateJwtToken(String sub, Long userId, Long companyId, Long membershipId, UserRole role) {
        Date expirationDate = Date.from(Instant.now().plusMillis(expirationDuration));

        return Jwts.builder()
                .setSubject(sub)
                .claim("user_id", userId)
                .claim("company_id", companyId)
                .claim("membership_id", membershipId)
                .claim("role", role.name())
                .setExpiration(expirationDate)
                .signWith(getSigningKey())
                .compact();
    }

    public SecretKey getSigningKey() {
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
        String email = extractClaims(token).getSubject();
        if (email == null || email.isBlank()) {
            throw new MalformedJwtException("Invalid token");
        }
        return email;
    }

    public Long extractUserId(String token) {
        return requireLongClaim(extractClaims(token), "user_id");
    }

    public Long extractCompanyId(String token) {
        return requireLongClaim(extractClaims(token), "company_id");
    }

    public Long extractMembershipId(String token) {
        return requireLongClaim(extractClaims(token), "membership_id");
    }

    public String extractRole(String token) {
        String role = extractClaims(token).get("role", String.class);
        if (role == null || role.isBlank()) {
            throw new MalformedJwtException("Invalid token");
        }
        return role;
    }

    private static Long requireLongClaim(Claims claims, String name) {
        Object value = claims.get(name);
        if (!(value instanceof Number number)) {
            throw new MalformedJwtException("Invalid token");
        }
        return number.longValue();
    }
}
