package com.example.employeetimetracking.unit.tenant;

import com.example.employeetimetracking.model.entities.AuditLog;
import com.example.employeetimetracking.model.entities.Company;
import com.example.employeetimetracking.model.enums.CompanyStatus;
import com.example.employeetimetracking.tenant.AuditLogCompanyListener;
import com.example.employeetimetracking.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class AuditLogCompanyListenerTest {

    AuditLogCompanyListener listener = new AuditLogCompanyListener();

    @BeforeEach
    void setUp() {
        TenantContext.set(new TenantContext.TenantInfo(1L, "acme", CompanyStatus.ACTIVE));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void prePersist_overwritesCompanyFromPayloadWithTenantContext() {
        Company payloadCompany = new Company();
        payloadCompany.setId(99L);

        AuditLog log = new AuditLog();
        log.setCompany(payloadCompany);

        listener.assignCompanyFromContext(log);

        assertEquals(1L, log.getCompany().getId());
        assertNotSame(payloadCompany, log.getCompany());
        assertEquals(99L, payloadCompany.getId());
    }
}
