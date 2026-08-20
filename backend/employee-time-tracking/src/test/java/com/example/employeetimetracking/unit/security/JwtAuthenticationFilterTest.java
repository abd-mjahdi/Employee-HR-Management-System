package com.example.employeetimetracking.unit.security;

import com.example.employeetimetracking.exception.MembershipInactiveException;
import com.example.employeetimetracking.exception.TenantMismatchException;
import com.example.employeetimetracking.model.entities.Company;
import com.example.employeetimetracking.model.entities.CompanyMembership;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.CompanyStatus;
import com.example.employeetimetracking.model.enums.MembershipStatus;
import com.example.employeetimetracking.model.enums.UserRole;
import com.example.employeetimetracking.security.CustomUserDetails;
import com.example.employeetimetracking.security.JwtAuthenticationFilter;
import com.example.employeetimetracking.security.JwtUtil;
import com.example.employeetimetracking.service.CustomUserDetailsService;
import com.example.employeetimetracking.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.HandlerExceptionResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock CustomUserDetailsService customUserDetailsService;
    @Mock HandlerExceptionResolver exceptionResolver;
    @Mock FilterChain filterChain;

    private JwtUtil jwtUtil;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "expirationDuration", 3_600_000L);
        ReflectionTestUtils.setField(
                jwtUtil,
                "jwtSecret",
                "0ef2d553cf17c6a144e82d71ca3d5d1f931f4b42d637a6d0b3ae645be2e1e67a"
        );
        filter = new JwtAuthenticationFilter(jwtUtil, customUserDetailsService, exceptionResolver);
        TenantContext.set(new TenantContext.TenantInfo(1L, "acme", CompanyStatus.ACTIVE));
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void noBearer_continuesWithoutAuthentication() throws Exception {
        MockHttpServletRequest request = request("/users");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void tokenForOtherCompany_isUnauthorizedEvenIfUserHasMembershipThere() throws Exception {
        String token = jwtUtil.generateJwtToken("a@x.com", 10L, 1L, 10L, UserRole.HR_ADMIN);
        TenantContext.set(new TenantContext.TenantInfo(2L, "globex", CompanyStatus.ACTIVE));
        MockHttpServletRequest request = request("/users");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(customUserDetailsService, never()).loadForTenant(anyLong(), anyLong(), anyLong());
        verify(exceptionResolver).resolveException(eq(request), eq(response), isNull(), any(TenantMismatchException.class));
        verify(filterChain, never()).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void validToken_loadsMembershipByIdAndCompanyAndUser() throws Exception {
        String token = jwtUtil.generateJwtToken("a@x.com", 10L, 1L, 99L, UserRole.HR_ADMIN);
        when(customUserDetailsService.loadForTenant(99L, 1L, 10L)).thenReturn(details(UserRole.EMPLOYEE, MembershipStatus.ACTIVE));

        MockHttpServletRequest request = request("/users");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(customUserDetailsService).loadForTenant(99L, 1L, 10L);
        verify(filterChain).doFilter(request, response);
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE")));
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals("ROLE_HR_ADMIN")));
    }

    @Test
    void deactivatedMembership_isForbidden() throws Exception {
        String token = jwtUtil.generateJwtToken("a@x.com", 10L, 1L, 99L, UserRole.EMPLOYEE);
        when(customUserDetailsService.loadForTenant(99L, 1L, 10L))
                .thenReturn(details(UserRole.EMPLOYEE, MembershipStatus.DEACTIVATED));

        MockHttpServletRequest request = request("/users");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(exceptionResolver).resolveException(
                eq(request), eq(response), isNull(), any(MembershipInactiveException.class));
        verify(filterChain, never()).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void swaggerDocs_skipJwtEvenWithBearerToken() throws Exception {
        String token = jwtUtil.generateJwtToken("a@x.com", 10L, 1L, 99L, UserRole.HR_ADMIN);
        MockHttpServletRequest request = request("/v3/api-docs");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(customUserDetailsService, never()).loadForTenant(anyLong(), anyLong(), anyLong());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void expiredToken_returnsPlainUnauthorizedBody() throws Exception {
        ReflectionTestUtils.setField(jwtUtil, "expirationDuration", -3_600_000L);
        String token = jwtUtil.generateJwtToken("a@x.com", 10L, 1L, 99L, UserRole.EMPLOYEE);
        MockHttpServletRequest request = request("/users");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
        assertEquals("Token expired", response.getContentAsString());
        verify(filterChain, never()).doFilter(request, response);
    }

    private static MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRequestURI(path);
        return request;
    }

    private static CustomUserDetails details(UserRole dbRole, MembershipStatus status) {
        User user = new User();
        user.setId(10L);
        user.setEmail("a@x.com");
        user.setPasswordHash("hash");
        user.setIsActive(true);

        Company company = new Company();
        company.setId(1L);

        CompanyMembership membership = new CompanyMembership();
        membership.setId(99L);
        membership.setUser(user);
        membership.setCompany(company);
        membership.setRole(dbRole);
        membership.setStatus(status);
        return new CustomUserDetails(membership);
    }
}
