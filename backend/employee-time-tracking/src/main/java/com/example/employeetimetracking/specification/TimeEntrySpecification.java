package com.example.employeetimetracking.specification;


import com.example.employeetimetracking.model.entities.LeaveRequest;
import com.example.employeetimetracking.model.entities.TimeEntry;
import com.example.employeetimetracking.model.enums.Status;
import org.springframework.data.jpa.domain.Specification;

import java.sql.Time;
import java.time.LocalDate;

public class TimeEntrySpecification {
    public static Specification<TimeEntry> hasStatus(Status status){
        return (root,query,cb)-> status==null ? cb.conjunction(): cb.equal(root.get("status"), status);
    }

    public static Specification<TimeEntry> afterDate(LocalDate date){
        return (root, query, cb) -> date==null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("startDate"), date);
    }

    public static Specification<TimeEntry> beforeDate(LocalDate date){
        return (root, query, cb) -> date==null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("endDate"), date);
    }

    public static Specification<TimeEntry> hasUserId(Long userId){
        return (root, query,cb)-> cb.equal(root.get("user").get("id"), userId);
    }
}
