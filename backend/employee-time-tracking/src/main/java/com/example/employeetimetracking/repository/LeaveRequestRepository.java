package com.example.employeetimetracking.repository;

import com.example.employeetimetracking.model.entities.LeaveRequest;
import com.example.employeetimetracking.model.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findByUserId(Long userId);
    List<LeaveRequest> findByUserIdAndStatus(Long userId, Status status);
    List<LeaveRequest> findByStatus(Status status);
    List<LeaveRequest> findByManagerApprovedById(Long managerId);
    List<LeaveRequest> findByHrApprovedById(Long hrId);
    List<LeaveRequest> findByManagerApprovalStatus(Status status);
    List<LeaveRequest> findByHrApprovalStatus(Status status);
    List<LeaveRequest> findByLeaveTypeId(Long leaveTypeId);
    List<LeaveRequest> findByStartDateBetween(LocalDate startDate, LocalDate endDate);

}