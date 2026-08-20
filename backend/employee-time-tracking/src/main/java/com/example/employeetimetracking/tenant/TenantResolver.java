package com.example.employeetimetracking.tenant;

import com.example.employeetimetracking.config.TenantProperties;
import com.example.employeetimetracking.exception.InvalidTenantException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Tenant comes from Host (or X-Forwarded-Host only when trust-forwarded-host is enabled).
 * Never reads {@code X-Company-Id}, {@code companyId} query params, or JSON bodies.
 */
@Component
public class TenantResolver {

    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?$");

    static final String UNKNOWN_TENANT = "Tenant not found";

    private static final String FORWARDED_HOST = "X-Forwarded-Host";

    private final TenantProperties tenantProperties;

    public TenantResolver(TenantProperties tenantProperties) {
        this.tenantProperties = tenantProperties;
    }

    public String resolveSlug(HttpServletRequest request) {
        return resolveSlug(resolveHost(request));
    }

    /**
     * Production: {@link HttpServletRequest#getServerName()} only.
     * Dev/proxy: {@code X-Forwarded-Host} when {@code app.tenant.trust-forwarded-host} is true.
     */
    public String resolveHost(HttpServletRequest request) {
        if (tenantProperties.isTrustForwardedHost()) {
            String forwarded = request.getHeader(FORWARDED_HOST);
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }
        return request.getServerName();
    }

    /**
     * Extracts and validates the tenant slug from a hostname.
     * Bare localhost may fall back to {@code app.tenant.dev-default-slug} when that property is set.
     */
    public String resolveSlug(String host) {
        if (host == null || host.isBlank()) {
            throw new InvalidTenantException(UNKNOWN_TENANT);
        }

        String hostname = stripPort(host.trim().toLowerCase(Locale.ROOT));

        try {
            return parseSlug(hostname);
        } catch (InvalidTenantException ex) {
            String fallback = tenantProperties.getDevDefaultSlug();
            if (fallback != null && !fallback.isBlank() && isBareNonTenantHost(hostname)) {
                return validateSlug(fallback.toLowerCase(Locale.ROOT));
            }
            throw ex;
        }
    }

    private String parseSlug(String hostname) {
        String baseDomain = tenantProperties.getBaseDomain().toLowerCase(Locale.ROOT);
        String localDevSuffix = tenantProperties.getLocalDevSuffix().toLowerCase(Locale.ROOT);

        if (isBareNonTenantHost(hostname)) {
            throw new InvalidTenantException(UNKNOWN_TENANT);
        }

        if (hostname.equals(baseDomain) && !tenantProperties.isAllowApex()) {
            throw new InvalidTenantException(UNKNOWN_TENANT);
        }

        if (hostname.equals("www." + baseDomain) && !tenantProperties.isAllowWww()) {
            throw new InvalidTenantException(UNKNOWN_TENANT);
        }

        String slug = extractSlug(hostname, baseDomain, localDevSuffix);
        if (slug == null || slug.contains(".")) {
            throw new InvalidTenantException(UNKNOWN_TENANT);
        }

        return validateSlug(slug);
    }

    private String validateSlug(String slug) {
        if (tenantProperties.reservedSlugSet().contains(slug)) {
            throw new InvalidTenantException(UNKNOWN_TENANT);
        }
        if (!SLUG_PATTERN.matcher(slug).matches()) {
            throw new InvalidTenantException(UNKNOWN_TENANT);
        }
        return slug;
    }

    private static String stripPort(String hostname) {
        int colon = hostname.indexOf(':');
        if (colon < 0) {
            return hostname;
        }
        if (hostname.startsWith("[")) {
            int end = hostname.indexOf(']');
            return end > 0 ? hostname.substring(0, end + 1) : hostname;
        }
        return hostname.substring(0, colon);
    }

    static boolean isBareNonTenantHost(String hostname) {
        return "localhost".equals(hostname)
                || "127.0.0.1".equals(hostname)
                || "::1".equals(hostname)
                || "[::1]".equals(hostname);
    }

    private static String extractSlug(String hostname, String baseDomain, String localDevSuffix) {
        String baseSuffix = "." + baseDomain;
        if (hostname.endsWith(baseSuffix)) {
            return hostname.substring(0, hostname.length() - baseSuffix.length());
        }
        String localSuffix = "." + localDevSuffix;
        if (hostname.endsWith(localSuffix)) {
            return hostname.substring(0, hostname.length() - localSuffix.length());
        }
        return null;
    }
}
