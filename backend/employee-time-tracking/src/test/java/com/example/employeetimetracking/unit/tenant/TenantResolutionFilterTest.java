package com.example.employeetimetracking.unit.tenant;

import com.example.employeetimetracking.service.TenantService;
import com.example.employeetimetracking.tenant.TenantResolutionFilter;
import com.example.employeetimetracking.tenant.TenantResolver;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerExceptionResolver;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TenantResolutionFilterTest {

    @Mock
    TenantResolver tenantResolver;
    @Mock
    TenantService tenantService;
    @Mock
    HandlerExceptionResolver exceptionResolver;
    @Mock
    FilterChain filterChain;

    @Test
    void bootstrapPath_skipsTenantResolution() throws Exception {
        TenantResolutionFilter filter = new TenantResolutionFilter(
                tenantResolver, tenantService, exceptionResolver);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/internal/bootstrap/company");
        request.setRequestURI("/internal/bootstrap/company");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verifyNoInteractions(tenantResolver, tenantService, exceptionResolver);
        verify(filterChain).doFilter(request, response);
    }
}
