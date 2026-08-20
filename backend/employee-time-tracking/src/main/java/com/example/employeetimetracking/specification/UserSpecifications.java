package com.example.employeetimetracking.specification;

import com.example.employeetimetracking.model.entities.CompanyMembership;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.UserRole;
import com.example.employeetimetracking.tenant.TenantContext;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecifications {

    public static Specification<User> hasDepartmentId(Long id){
        return (root, query, cb) -> {
            if (id == null) {
                return cb.conjunction();
            }
            query.distinct(true);
            Join<User, CompanyMembership> membership = root.join("memberships");
            var deptPred = cb.equal(membership.get("department").get("id"), id);
            Long companyId = TenantContext.getCompanyId();
            if (companyId == null) {
                return deptPred;
            }
            return cb.and(deptPred, cb.equal(membership.get("company").get("id"), companyId));
        };
    }

    public static Specification<User> hasRole(UserRole role){
        return (root, query, cb) -> {
            if (role == null) {
                return cb.conjunction();
            }
            query.distinct(true);
            Join<User, CompanyMembership> membership = root.join("memberships");
            var rolePred = cb.equal(membership.get("role"), role);
            Long companyId = TenantContext.getCompanyId();
            if (companyId == null) {
                return rolePred;
            }
            return cb.and(rolePred, cb.equal(membership.get("company").get("id"), companyId));
        };
    }

    public static Specification<User> isActive(Boolean isActive){
        return (root, query, cb) -> isActive == null ? cb.conjunction() : cb.equal(root.get("isActive") , isActive);
    }

    public static Specification<User> hasName(String name){
        return (root, query, cb) -> name == null ? cb.conjunction() : cb.or(
                cb.like(cb.lower(root.get("firstName")), "%" + name.toLowerCase() + "%"),
                cb.like(cb.lower(root.get("lastName")), "%" + name.toLowerCase() + "%")
        );
    }


}
