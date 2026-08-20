package com.example.employeetimetracking.unit.security;

import com.example.employeetimetracking.model.enums.UserRole;
import com.example.employeetimetracking.security.JwtUtil;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "expirationDuration", 3_600_000L);
        ReflectionTestUtils.setField(
                jwtUtil,
                "jwtSecret",
                "0ef2d553cf17c6a144e82d71ca3d5d1f931f4b42d637a6d0b3ae645be2e1e67a"
        );
    }

    @Test
    void generateJwtToken_includesTenantClaims() {
        String token = jwtUtil.generateJwtToken("a@x.com", 10L, 1L, 99L, UserRole.MANAGER);
        assertEquals("a@x.com", jwtUtil.extractEmail(token));
        assertEquals(10L, jwtUtil.extractUserId(token));
        assertEquals(1L, jwtUtil.extractCompanyId(token));
        assertEquals(99L, jwtUtil.extractMembershipId(token));
        assertEquals("MANAGER", jwtUtil.extractRole(token));
    }

    @Test
    void extractCompanyId_rejectsLegacyTokenWithoutTenantClaims() {
        String legacy = io.jsonwebtoken.Jwts.builder()
                .setSubject("a@x.com")
                .claim("user_id", 10L)
                .claim("role", "HR_ADMIN")
                .signWith(jwtUtil.getSigningKey())
                .compact();
        assertThrows(MalformedJwtException.class, () -> jwtUtil.extractCompanyId(legacy));
        assertThrows(MalformedJwtException.class, () -> jwtUtil.extractMembershipId(legacy));
    }
}
