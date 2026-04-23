package com.example.employeetimetracking.specification;


import com.example.employeetimetracking.model.entities.LeaveRequest;
import com.example.employeetimetracking.model.entities.TimeEntry;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.Status;
import org.springframework.data.jpa.domain.Specification;

import java.sql.Time;
import java.time.LocalDate;

public class TimeEntrySpecification {
    public static Specification<TimeEntry> hasStatus(Status status){
        return (root,query,cb)-> status==null ? cb.conjunction(): cb.equal(root.get("status"), status);
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
        return (root, query, cb) -> cb.equal(root.get("user").get("manager").get("id"), id);
    }

    public static Specification<TimeEntry> hasName(String name){
        return (root, query, cb) -> name == null ? cb.conjunction() : cb.or(
                cb.like(cb.lower(root.get("user").get("firstName")), "%" + name.toLowerCase() + "%"),
                cb.like(cb.lower(root.get("user").get("lastName")), "%" + name.toLowerCase() + "%")
        );
    }
}
