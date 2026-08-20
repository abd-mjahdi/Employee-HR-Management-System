package com.example.employeetimetracking.specification;

import com.example.employeetimetracking.model.entities.CompanyMembership;
import com.example.employeetimetracking.model.enums.MembershipStatus;
import com.example.employeetimetracking.model.enums.UserRole;
import com.example.employeetimetracking.tenant.TenantContext;
import org.springframework.data.jpa.domain.Specification;

public class MembershipSpecifications {

    public static Specification<CompanyMembership> belongsToCurrentCompany() {
        return (root, query, cb) ->
                cb.equal(root.get("company").get("id"), TenantContext.require().companyId());
    }

    public static Specification<CompanyMembership> hasDepartmentId(Long id) {
        return (root, query, cb) ->
                id == null ? cb.conjunction() : cb.equal(root.get("department").get("id"), id);
    }

    public static Specification<CompanyMembership> hasRole(UserRole role) {
        return (root, query, cb) ->
                role == null ? cb.conjunction() : cb.equal(root.get("role"), role);
    }

    public static Specification<CompanyMembership> isActive(Boolean active) {
        return (root, query, cb) -> {
            if (active == null) {
                return cb.conjunction();
            }
            if (active) {
                return cb.equal(root.get("status"), MembershipStatus.ACTIVE);
            }
            return cb.notEqual(root.get("status"), MembershipStatus.ACTIVE);
        };
    }

    public static Specification<CompanyMembership> hasName(String name) {
        return (root, query, cb) -> {
            if (name == null) {
                return cb.conjunction();
            }
            var user = root.join("user");
            String pattern = "%" + name.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(user.get("firstName")), pattern),
                    cb.like(cb.lower(user.get("lastName")), pattern)
            );
        };
    }
}
