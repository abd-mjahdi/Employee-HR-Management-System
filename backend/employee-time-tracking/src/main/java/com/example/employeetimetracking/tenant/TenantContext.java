package com.example.employeetimetracking.tenant;

import com.example.employeetimetracking.exception.InvalidTenantException;
import com.example.employeetimetracking.model.enums.CompanyStatus;

public final class TenantContext {

    private static final ThreadLocal<TenantInfo> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(TenantInfo tenant) {
        CURRENT.set(tenant);
    }

    public static TenantInfo get() {
        return CURRENT.get();
    }

    public static TenantInfo require() {
        TenantInfo tenant = CURRENT.get();
        if (tenant == null) {
            throw new InvalidTenantException("Tenant not found");
        }
        return tenant;
    }

    public static Long getCompanyId() {
        TenantInfo tenant = CURRENT.get();
        return tenant == null ? null : tenant.companyId();
    }

    public static void clear() {
        CURRENT.remove();
    }

    public record TenantInfo(Long companyId, String slug, CompanyStatus status) {
    }
}
