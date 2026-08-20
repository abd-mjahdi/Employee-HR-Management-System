package com.example.employeetimetracking.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.login")
public class LoginProperties {
    /**
     * Failed attempts per IP + company within {@link #windowMs} before login is blocked.
     * {@code 0} disables rate limiting.
     */
    private int maxFailedAttempts = 10;

    /** Sliding window for failed-attempt counting. */
    private long windowMs = 900_000L;
}
