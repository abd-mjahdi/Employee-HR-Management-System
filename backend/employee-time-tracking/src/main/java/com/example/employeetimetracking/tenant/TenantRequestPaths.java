package com.example.employeetimetracking.tenant;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Paths that skip Host tenant resolution. Swagger/OpenAPI and actuator only —
 * never {@code /auth/login} or business APIs.
 */
public final class TenantRequestPaths {

    private TenantRequestPaths() {
    }

    public static String withinApplication(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String context = request.getContextPath();
        if (context != null && !context.isEmpty() && uri.startsWith(context)) {
            return uri.substring(context.length());
        }
        return uri;
    }

    public static boolean skipTenantResolution(HttpServletRequest request) {
        return skipTenantResolution(withinApplication(request));
    }

    public static boolean skipTenantResolution(String path) {
        return isSwaggerOrDocs(path) || isActuator(path) || isInternalBootstrap(path);
    }

    public static boolean isSwaggerOrDocs(String path) {
        return path.equals("/swagger-ui.html")
                || path.equals("/swagger-ui")
                || path.startsWith("/swagger-ui/")
                || path.equals("/v3/api-docs")
                || path.startsWith("/v3/api-docs/");
    }

    public static boolean isActuator(String path) {
        return path.equals("/actuator") || path.startsWith("/actuator/");
    }

    public static boolean isInternalBootstrap(String path) {
        return path.equals("/internal/bootstrap/company");
    }
}
