package com.example.employeetimetracking.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Login sequence:
 * Host → resolve Company → authenticate User → require ACTIVE membership in that Company
 * → issue JWT with tenant claims → later requests re-resolve Host and compare to JWT.
 *
 * Not registered in {@code SecurityConfig} yet. Phase 4 wires this filter before
 * {@code JwtAuthenticationFilter} and populates {@link TenantContext} from the Host slug.
 */
public class TenantResolutionFilter extends OncePerRequestFilter {

    @SuppressWarnings("unused")
    private final TenantResolver tenantResolver;

    public TenantResolutionFilter(TenantResolver tenantResolver) {
        this.tenantResolver = tenantResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
