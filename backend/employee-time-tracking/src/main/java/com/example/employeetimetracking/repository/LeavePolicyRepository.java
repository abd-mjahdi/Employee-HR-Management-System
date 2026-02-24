package com.example.employeetimetracking.repository;

import com.example.employeetimetracking.model.entities.LeavePolicy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeavePolicyRepository extends JpaRepository<LeavePolicy, Long> {
    LeavePolicy findByLeaveTypeId(Long leaveTypeId);
}
