package com.example.employeetimetracking.specification;

import com.example.employeetimetracking.model.entities.LeaveRequest;
import com.example.employeetimetracking.model.enums.Status;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class LeaveRequestSpecifications {
    public static Specification<LeaveRequest> hasManagerId(Long id){
        return (root, query, cb) -> cb.equal(root.get("user").get("manager").get("id"), id);
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
