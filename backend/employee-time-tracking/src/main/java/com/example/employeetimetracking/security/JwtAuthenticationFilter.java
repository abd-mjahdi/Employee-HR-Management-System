package com.example.employeetimetracking.security;

import com.example.employeetimetracking.exception.AccountDeactivatedException;
import com.example.employeetimetracking.exception.MembershipInactiveException;
import com.example.employeetimetracking.exception.TenantMismatchException;
import com.example.employeetimetracking.exception.UserNotFoundException;
import com.example.employeetimetracking.model.enums.MembershipStatus;
import com.example.employeetimetracking.service.CustomUserDetailsService;
import com.example.employeetimetracking.tenant.TenantContext;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.Objects;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService customUserDetailsService;
    private final HandlerExceptionResolver exceptionResolver;

    public JwtAuthenticationFilter(JwtUtil jwtUtil,
                                   CustomUserDetailsService customUserDetailsService,
                                   @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
        this.jwtUtil = jwtUtil;
        this.customUserDetailsService = customUserDetailsService;
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwtToken = authHeader.substring(7);

        try {
            String email = jwtUtil.extractEmail(jwtToken);
            Long jwtUserId = jwtUtil.extractUserId(jwtToken);
            Long jwtCompanyId = jwtUtil.extractCompanyId(jwtToken);
            Long jwtMembershipId = jwtUtil.extractMembershipId(jwtToken);
            jwtUtil.extractRole(jwtToken);

            Long tenantCompanyId = TenantContext.getCompanyId();
            if (tenantCompanyId == null || !tenantCompanyId.equals(jwtCompanyId)) {
                throw new TenantMismatchException("Unauthorized");
            }

            CustomUserDetails userDetails = customUserDetailsService.loadForTenant(email, tenantCompanyId);
            if (!Objects.equals(userDetails.getId(), jwtUserId)
                    || !Objects.equals(userDetails.getCompanyId(), jwtCompanyId)
                    || !Objects.equals(userDetails.getMembershipId(), jwtMembershipId)) {
                throw new TenantMismatchException("Unauthorized");
            }

            if (userDetails.getMembershipStatus() != MembershipStatus.ACTIVE) {
                throw new MembershipInactiveException("Account deactivated");
            }
            if (!userDetails.isEnabled()) {
                throw new AccountDeactivatedException("Account deactivated");
            }

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (ExpiredJwtException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Token expired");
            return;
        } catch (JwtException | IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid token");
            return;
        } catch (UserNotFoundException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid token");
            return;
        } catch (TenantMismatchException | AccountDeactivatedException | MembershipInactiveException e) {
            exceptionResolver.resolveException(request, response, null, e);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
