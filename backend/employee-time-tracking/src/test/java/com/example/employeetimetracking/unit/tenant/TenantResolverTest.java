package com.example.employeetimetracking.unit.tenant;

import com.example.employeetimetracking.config.TenantProperties;
import com.example.employeetimetracking.exception.InvalidTenantException;
import com.example.employeetimetracking.tenant.TenantResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TenantResolverTest {

    private TenantProperties properties;
    private TenantResolver resolver;

    @BeforeEach
    void setUp() {
        properties = new TenantProperties();
        properties.setBaseDomain("myhr.com");
        properties.setLocalDevSuffix("localhost");
        properties.setAllowApex(false);
        properties.setAllowWww(false);
        properties.setTrustForwardedHost(false);
        properties.setDevDefaultSlug("");
        properties.setReservedSlugs(List.of("www", "api", "app", "admin", "mail", "localhost"));
        resolver = new TenantResolver(properties);
    }

    @Test
    void acmeDotMyhrCom_resolvesAcme() {
        assertEquals("acme", resolver.resolveSlug("acme.myhr.com"));
    }

    @Test
    void acmeLocalhost_resolvesAcme() {
        assertEquals("acme", resolver.resolveSlug("acme.localhost"));
    }

    @Test
    void globexLocalhost_resolvesGlobex() {
        assertEquals("globex", resolver.resolveSlug("globex.localhost"));
    }

    @Test
    void hostWithPort_stripsPort() {
        assertEquals("acme", resolver.resolveSlug("acme.localhost:4200"));
    }

    @Test
    void mixedCaseHost_normalizes() {
        assertEquals("acme", resolver.resolveSlug("Acme.MyHR.com"));
    }

    @Test
    void localhost_isInvalidWithoutDevFallback() {
        assertThrows(InvalidTenantException.class, () -> resolver.resolveSlug("localhost"));
        assertThrows(InvalidTenantException.class, () -> resolver.resolveSlug("127.0.0.1"));
    }

    @Test
    void localhost_usesDevDefaultSlugWhenConfigured() {
        properties.setDevDefaultSlug("acme");
        assertEquals("acme", resolver.resolveSlug("localhost"));
        assertEquals("acme", resolver.resolveSlug("127.0.0.1"));
    }

    @Test
    void apexAndWww_areInvalid() {
        assertThrows(InvalidTenantException.class, () -> resolver.resolveSlug("myhr.com"));
        assertThrows(InvalidTenantException.class, () -> resolver.resolveSlug("www.myhr.com"));
    }

    @Test
    void reservedApiSubdomain_isInvalid() {
        assertThrows(InvalidTenantException.class, () -> resolver.resolveSlug("api.myhr.com"));
    }

    @Test
    void unknownHost_isInvalid() {
        assertThrows(InvalidTenantException.class, () -> resolver.resolveSlug("example.com"));
    }

    @Test
    void request_usesServerName_notCompanyIdHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName("globex.myhr.com");
        request.addHeader("X-Company-Id", "1");
        assertEquals("globex", resolver.resolveSlug(request));
    }

    @Test
    void request_ignoresTenantCookies() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName("acme.myhr.com");
        request.setCookies(
                new jakarta.servlet.http.Cookie("tenant", "globex"),
                new jakarta.servlet.http.Cookie("slug", "globex"),
                new jakarta.servlet.http.Cookie("companyId", "99")
        );
        request.addHeader("Cookie", "tenant=globex; slug=globex");
        assertEquals("acme", resolver.resolveSlug(request));
    }

    @Test
    void forwardedHost_ignoredWhenNotTrusted() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName("acme.myhr.com");
        request.addHeader("X-Forwarded-Host", "globex.myhr.com");
        assertEquals("acme", resolver.resolveSlug(request));
    }

    @Test
    void forwardedHost_usedWhenTrusted() {
        properties.setTrustForwardedHost(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName("localhost");
        request.addHeader("X-Forwarded-Host", "globex.localhost:8080");
        assertEquals("globex", resolver.resolveSlug(request));
    }
}
