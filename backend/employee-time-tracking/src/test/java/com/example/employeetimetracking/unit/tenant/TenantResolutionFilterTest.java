package com.example.employeetimetracking.unit.tenant;

import com.example.employeetimetracking.exception.InactiveTenantException;
import com.example.employeetimetracking.model.entities.Company;
import com.example.employeetimetracking.model.enums.CompanyStatus;
import com.example.employeetimetracking.service.TenantService;
import com.example.employeetimetracking.tenant.TenantContext;
import com.example.employeetimetracking.tenant.TenantResolutionFilter;
import com.example.employeetimetracking.tenant.TenantResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerExceptionResolver;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantResolutionFilterTest {

    @Mock TenantResolver tenantResolver;
    @Mock TenantService tenantService;
    @Mock HandlerExceptionResolver exceptionResolver;
    @Mock FilterChain filterChain;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void bootstrapPath_skipsTenantResolution() throws Exception {
        TenantResolutionFilter filter = filter();
        MockHttpServletRequest request = request("POST", "/internal/bootstrap/company");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verifyNoInteractions(tenantResolver, tenantService, exceptionResolver);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void swaggerUi_skipsTenantResolution() throws Exception {
        TenantResolutionFilter filter = filter();
        MockHttpServletRequest request = request("GET", "/swagger-ui/index.html");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verifyNoInteractions(tenantResolver, tenantService);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void apiDocs_skipsTenantResolution() throws Exception {
        TenantResolutionFilter filter = filter();
        MockHttpServletRequest request = request("GET", "/v3/api-docs");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verifyNoInteractions(tenantResolver, tenantService);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void login_resolvesActiveTenantBeforeContinuing() throws Exception {
        Company acme = company(1L, "acme", CompanyStatus.ACTIVE);
        when(tenantResolver.resolveSlug(any(HttpServletRequest.class))).thenReturn("acme");
        when(tenantService.requireActiveBySlug("acme")).thenReturn(acme);

        TenantResolutionFilter filter = filter();
        MockHttpServletRequest request = request("POST", "/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(tenantService).requireActiveBySlug("acme");
        verify(filterChain).doFilter(request, response);
        assertNull(TenantContext.get());
    }

    @Test
    void usersApi_doesNotSkipTenantResolution() throws Exception {
        Company acme = company(1L, "acme", CompanyStatus.ACTIVE);
        when(tenantResolver.resolveSlug(any(HttpServletRequest.class))).thenReturn("acme");
        when(tenantService.requireActiveBySlug("acme")).thenReturn(acme);

        TenantResolutionFilter filter = filter();
        MockHttpServletRequest request = request("GET", "/users");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(tenantResolver).resolveSlug(request);
        verify(tenantService).requireActiveBySlug("acme");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void inactiveCompany_rejectsBeforeJwtAndDoesNotContinue() throws Exception {
        when(tenantResolver.resolveSlug(any(HttpServletRequest.class))).thenReturn("acme");
        when(tenantService.requireActiveBySlug("acme"))
                .thenThrow(new InactiveTenantException("Tenant not found"));

        TenantResolutionFilter filter = filter();
        MockHttpServletRequest request = request("GET", "/users");
        request.addHeader("Authorization", "Bearer not-checked");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(exceptionResolver).resolveException(
                eq(request), eq(response), isNull(), any(InactiveTenantException.class));
        verify(filterChain, never()).doFilter(request, response);
        assertNull(TenantContext.get());
    }

    private TenantResolutionFilter filter() {
        return new TenantResolutionFilter(tenantResolver, tenantService, exceptionResolver);
    }

    private static MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRequestURI(path);
        return request;
    }

    private static Company company(Long id, String slug, CompanyStatus status) {
        Company company = new Company();
        company.setId(id);
        company.setSlug(slug);
        company.setStatus(status);
        return company;
    }
}
