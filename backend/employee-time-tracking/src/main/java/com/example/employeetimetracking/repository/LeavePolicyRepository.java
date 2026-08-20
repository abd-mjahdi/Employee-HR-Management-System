package com.example.employeetimetracking.repository;

import com.example.employeetimetracking.model.entities.LeavePolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeavePolicyRepository extends JpaRepository<LeavePolicy, Long> {
    Optional<LeavePolicy> findByIdAndCompanyId(Long id, Long companyId);

    List<LeavePolicy> findAllByCompanyId(Long companyId);

    Optional<LeavePolicy> findByCompanyIdAndLeaveTypeId(Long companyId, Long leaveTypeId);

    Optional<LeavePolicy> findByLeaveTypeId(Long leaveTypeId);
}
