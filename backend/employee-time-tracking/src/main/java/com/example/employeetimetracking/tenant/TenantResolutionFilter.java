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
 * Tenant bypass is only for Swagger/OpenAPI, actuator, and first-company bootstrap.
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
        return TenantRequestPaths.skipTenantResolution(request);
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
}
