package com.example.employeetimetracking.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.tenant")
public class TenantProperties {

    private String baseDomain = "myhr.com";
    private String localDevSuffix = "localhost";
    private boolean allowApex = false;
    private boolean allowWww = false;
    /**
     * When true, {@code X-Forwarded-Host} may supply the tenant host (first value if comma-separated).
     * Must stay false in production unless a trusted reverse proxy overwrites that header.
     */
    private boolean trustForwardedHost = false;
    /**
     * Used only when the request host is bare {@code localhost}/{@code 127.0.0.1}.
     * Empty means disabled (required in production). Tests set this to {@code acme}.
     */
    private String devDefaultSlug = "";
    private List<String> reservedSlugs = new ArrayList<>(List.of(
            "www", "api", "app", "admin", "mail", "localhost"
    ));

    public Set<String> reservedSlugSet() {
        return reservedSlugs.stream()
                .map(slug -> slug.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }
}
