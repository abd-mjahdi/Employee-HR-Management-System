package com.example.employeetimetracking.unit.service.auth;

import com.example.employeetimetracking.config.LoginProperties;
import com.example.employeetimetracking.exception.LoginRateLimitedException;
import com.example.employeetimetracking.service.LoginAttemptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginAttemptServiceTest {

    private LoginAttemptService loginAttemptService;

    @BeforeEach
    void setUp() {
        LoginProperties properties = new LoginProperties();
        properties.setMaxFailedAttempts(3);
        properties.setWindowMs(60_000L);
        loginAttemptService = new LoginAttemptService(properties);
    }

    @Test
    void blocksAfterMaxFailuresForSameIpAndCompany() {
        loginAttemptService.recordFailure(1L, "10.0.0.1", "bad_password");
        loginAttemptService.recordFailure(1L, "10.0.0.1", "bad_password");
        loginAttemptService.recordFailure(1L, "10.0.0.1", "bad_password");

        assertThrows(LoginRateLimitedException.class,
                () -> loginAttemptService.assertNotLimited(1L, "10.0.0.1"));
    }

    @Test
    void otherCompanyOrIp_isNotBlocked() {
        loginAttemptService.recordFailure(1L, "10.0.0.1", "bad_password");
        loginAttemptService.recordFailure(1L, "10.0.0.1", "bad_password");
        loginAttemptService.recordFailure(1L, "10.0.0.1", "bad_password");

        assertDoesNotThrow(() -> loginAttemptService.assertNotLimited(2L, "10.0.0.1"));
        assertDoesNotThrow(() -> loginAttemptService.assertNotLimited(1L, "10.0.0.2"));
    }

    @Test
    void success_clearsFailures() {
        loginAttemptService.recordFailure(1L, "10.0.0.1", "bad_password");
        loginAttemptService.recordFailure(1L, "10.0.0.1", "bad_password");
        loginAttemptService.recordSuccess(1L, "10.0.0.1");
        loginAttemptService.recordFailure(1L, "10.0.0.1", "bad_password");

        assertDoesNotThrow(() -> loginAttemptService.assertNotLimited(1L, "10.0.0.1"));
    }
}
