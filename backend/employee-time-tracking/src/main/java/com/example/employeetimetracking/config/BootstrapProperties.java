package com.example.employeetimetracking.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.bootstrap")
public class BootstrapProperties {

    public static final String HEADER = "X-Bootstrap-Key";

    /**
     * Shared secret for {@code POST /internal/bootstrap/company}. Blank disables the endpoint.
     */
    private String key = "";

    public boolean isEnabled() {
        return key != null && !key.isBlank();
    }

    public boolean matches(String provided) {
        if (!isEnabled()) {
            return false;
        }
        String given = provided == null ? "" : provided;
        return MessageDigest.isEqual(sha256(key), sha256(given));
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
