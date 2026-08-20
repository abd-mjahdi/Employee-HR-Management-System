package com.example.employeetimetracking.tenant;

import com.example.employeetimetracking.exception.InactiveTenantException;
import com.example.employeetimetracking.exception.InvalidTenantException;
import com.example.employeetimetracking.model.entities.Company;
import com.example.employeetimetracking.service.TenantService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

/**
 * Login sequence:
 * Host → resolve Company → authenticate User → require ACTIVE membership in that Company
 * → issue JWT with tenant claims → later requests re-resolve Host and compare to JWT.
 *
 * Runs before {@code JwtAuthenticationFilter}. {@code /auth/login} is never skipped.
 */
@Component
public class TenantResolutionFilter extends OncePerRequestFilter {

    private final TenantResolver tenantResolver;
    private final TenantService tenantService;
    private final HandlerExceptionResolver exceptionResolver;

    public TenantResolutionFilter(TenantResolver tenantResolver,
                                  TenantService tenantService,
                                  @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
        this.tenantResolver = tenantResolver;
        this.tenantService = tenantService;
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = pathWithinApplication(request);
        return isSwaggerOrDocs(path) || isActuator(path) || isInternalBootstrap(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String slug = tenantResolver.resolveSlug(request);
            Company company = tenantService.requireActiveBySlug(slug);
            TenantContext.set(new TenantContext.TenantInfo(
                    company.getId(),
                    company.getSlug(),
                    company.getStatus()
            ));
            filterChain.doFilter(request, response);
        } catch (InvalidTenantException | InactiveTenantException ex) {
            exceptionResolver.resolveException(request, response, null, ex);
        } finally {
            TenantContext.clear();
        }
    }

    private static String pathWithinApplication(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String context = request.getContextPath();
        if (context != null && !context.isEmpty() && uri.startsWith(context)) {
            return uri.substring(context.length());
        }
        return uri;
    }

    private static boolean isSwaggerOrDocs(String path) {
        return path.equals("/swagger-ui.html")
                || path.equals("/swagger-ui")
                || path.startsWith("/swagger-ui/")
                || path.equals("/v3/api-docs")
                || path.startsWith("/v3/api-docs/");
    }

    private static boolean isActuator(String path) {
        return path.equals("/actuator") || path.startsWith("/actuator/");
    }

    /**
     * First-company bootstrap runs before any tenant exists; Host cannot be resolved.
     * Protection is {@code X-Bootstrap-Key}, not JWT or tenant membership.
     */
    private static boolean isInternalBootstrap(String path) {
        return path.equals("/internal/bootstrap/company");
    }
}
