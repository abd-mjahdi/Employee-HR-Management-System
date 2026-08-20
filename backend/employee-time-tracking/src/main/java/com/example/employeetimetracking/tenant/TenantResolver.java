package com.example.employeetimetracking.tenant;

import com.example.employeetimetracking.config.TenantProperties;
import com.example.employeetimetracking.exception.InvalidTenantException;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class TenantResolver {

    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?$");

    private static final String UNKNOWN_TENANT = "Tenant not found";

    private final TenantProperties tenantProperties;

    public TenantResolver(TenantProperties tenantProperties) {
        this.tenantProperties = tenantProperties;
    }

    /**
     * Extracts and validates the tenant slug from a hostname. Does not load {@code Company} from the database;
     * that lookup is Phase 4.
     */
    public String resolveSlug(String host) {
        if (host == null || host.isBlank()) {
            throw new InvalidTenantException(UNKNOWN_TENANT);
        }

        String hostname = stripPort(host.trim().toLowerCase(Locale.ROOT));
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

    private static boolean isBareNonTenantHost(String hostname) {
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
