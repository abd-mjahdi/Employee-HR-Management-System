package com.example.employeetimetracking.specification;


import com.example.employeetimetracking.model.entities.CompanyMembership;
import com.example.employeetimetracking.model.entities.TimeEntry;
import com.example.employeetimetracking.model.enums.Status;
import com.example.employeetimetracking.tenant.TenantContext;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class TimeEntrySpecification {
    public static Specification<TimeEntry> hasStatus(Status status){
        return (root,query,cb)-> status==null ? cb.conjunction(): cb.equal(root.get("status"), status);
    }

    public static Specification<TimeEntry> hasStatusIn(java.util.Collection<Status> statuses){
        return (root, query, cb) ->
                statuses == null || statuses.isEmpty()
                        ? cb.conjunction()
                        : root.get("status").in(statuses);
    }

    public static Specification<TimeEntry> afterDate(LocalDate date){
        return (root, query, cb) -> date==null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("entryDate"), date);
    }

    public static Specification<TimeEntry> beforeDate(LocalDate date){
        return (root, query, cb) -> date==null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("entryDate"), date);
    }

    public static Specification<TimeEntry> hasUserId(Long userId){
        return (root, query,cb)-> cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<TimeEntry> hasManagerId(Long id){
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

    public static Specification<TimeEntry> hasName(String name){
        return (root, query, cb) -> name == null ? cb.conjunction() : cb.or(
                cb.like(cb.lower(root.get("user").get("firstName")), "%" + name.toLowerCase() + "%"),
                cb.like(cb.lower(root.get("user").get("lastName")), "%" + name.toLowerCase() + "%")
        );
    }
}
