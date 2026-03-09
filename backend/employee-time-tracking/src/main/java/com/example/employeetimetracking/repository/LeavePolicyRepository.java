package com.example.employeetimetracking.repository;

import com.example.employeetimetracking.model.entities.LeavePolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LeavePolicyRepository extends JpaRepository<LeavePolicy, Long> {
    Optional<LeavePolicy> findByLeaveTypeId(Long leaveTypeId);
}
