package com.example.employeetimetracking.specification;

import com.example.employeetimetracking.model.entities.CompanyMembership;
import com.example.employeetimetracking.model.entities.LeaveRequest;
import com.example.employeetimetracking.model.enums.Status;
import com.example.employeetimetracking.tenant.TenantContext;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class LeaveRequestSpecifications {
    public static Specification<LeaveRequest> hasManagerId(Long id){
        return (root, query, cb) -> {
            if (id == null) {
                return cb.conjunction();
            }
            Subquery<Long> sq = query.subquery(Long.class);
            Root<CompanyMembership> membership = sq.from(CompanyMembership.class);
            var companyPred = TenantContext.getCompanyId() == null
                    ? cb.equal(membership.get("company"), root.get("company"))
                    : cb.equal(membership.get("company").get("id"), TenantContext.getCompanyId());
            sq.select(membership.get("id")).where(
                    cb.equal(membership.get("user"), root.get("user")),
                    cb.equal(membership.get("managerMembership").get("user").get("id"), id),
                    companyPred
            );
            return cb.exists(sq);
        };
    }

    public static Specification<LeaveRequest> hasUserId(Long userId){
        return (root, query, cb) -> userId == null ? cb.conjunction() : cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<LeaveRequest> hasStatus(Status status){
        return (root, query, cb) -> status==null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    public static Specification<LeaveRequest> afterDate(LocalDate date){
        return (root, query, cb) -> date==null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("startDate"), date);
    }

    public static Specification<LeaveRequest> beforeDate(LocalDate date){
        return (root, query, cb) -> date==null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("endDate"), date);
    }


}
