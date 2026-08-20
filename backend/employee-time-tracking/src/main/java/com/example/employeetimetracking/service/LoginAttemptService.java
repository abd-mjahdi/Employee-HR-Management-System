package com.example.employeetimetracking.service;

import com.example.employeetimetracking.config.LoginProperties;
import com.example.employeetimetracking.exception.LoginRateLimitedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Failed-login tracking keyed by company + client IP. Logs company id, never passwords.
 */
@Service
public class LoginAttemptService {
    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);
    private static final String RATE_LIMITED = "Too many login attempts";

    private final LoginProperties loginProperties;
    private final ConcurrentHashMap<String, Window> attempts = new ConcurrentHashMap<>();

    public LoginAttemptService(LoginProperties loginProperties) {
        this.loginProperties = loginProperties;
    }

    public void assertNotLimited(Long companyId, String clientIp) {
        if (loginProperties.getMaxFailedAttempts() <= 0) {
            return;
        }
        Window window = attempts.get(key(companyId, clientIp));
        if (window == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - window.startedAtMs >= loginProperties.getWindowMs()) {
            attempts.remove(key(companyId, clientIp), window);
            return;
        }
        if (window.failures >= loginProperties.getMaxFailedAttempts()) {
            log.warn("Login rate-limited companyId={} ip={}", companyId, sanitizeIp(clientIp));
            throw new LoginRateLimitedException(RATE_LIMITED);
        }
    }

    public void recordFailure(Long companyId, String clientIp, String reason) {
        log.warn("Login failed companyId={} ip={} reason={}", companyId, sanitizeIp(clientIp), reason);
        if (loginProperties.getMaxFailedAttempts() <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        attempts.compute(key(companyId, clientIp), (ignored, window) -> {
            if (window == null || now - window.startedAtMs >= loginProperties.getWindowMs()) {
                return new Window(now, 1);
            }
            return new Window(window.startedAtMs, window.failures + 1);
        });
    }

    public void recordSuccess(Long companyId, String clientIp) {
        attempts.remove(key(companyId, clientIp));
    }

    private static String key(Long companyId, String clientIp) {
        return String.valueOf(companyId) + "|" + sanitizeIp(clientIp);
    }

    static String sanitizeIp(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return "unknown";
        }
        return clientIp.trim();
    }

    private record Window(long startedAtMs, int failures) {
    }
}
