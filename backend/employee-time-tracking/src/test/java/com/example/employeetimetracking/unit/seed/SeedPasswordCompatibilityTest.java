package com.example.employeetimetracking.unit.seed;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SeedPasswordCompatibilityTest {

    static final String SEED_HASH = "$2a$10$ZjukDbYm4QPlvbcFHqpOMeeltGjCBNj7xBDj0n6rMSiCrBpuaFR1W";

    @Test
    void seedBcryptHash_matchesPlaintextPassword() {
        assertTrue(new BCryptPasswordEncoder().matches("password", SEED_HASH));
    }
}
