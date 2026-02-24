package com.example.employeetimetracking.repository;

import com.example.employeetimetracking.model.entities.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {
    List<LeaveBalance> findByUserId(Long userId);
    List<LeaveBalance> findByUserIdAndYear(Long userId, int year);
    LeaveBalance findByUserIdAndLeaveTypeIdAndYear(Long userId, Long leaveTypeId, int year);
    List<LeaveBalance> findByLeaveTypeId(Long leaveTypeId);
}