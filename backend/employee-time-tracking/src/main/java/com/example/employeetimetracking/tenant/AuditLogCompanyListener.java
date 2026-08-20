package com.example.employeetimetracking.tenant;

import com.example.employeetimetracking.model.entities.AuditLog;
import com.example.employeetimetracking.model.entities.Company;
import jakarta.persistence.PrePersist;

/**
 * Audit rows always belong to the Host-resolved tenant. Ignore any company set on the entity
 * (including values copied from a request payload).
 */
public class AuditLogCompanyListener {

    @PrePersist
    public void assignCompanyFromContext(AuditLog auditLog) {
        Company company = new Company();
        company.setId(TenantContext.require().companyId());
        auditLog.setCompany(company);
    }
}
