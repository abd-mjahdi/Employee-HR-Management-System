package com.example.employeetimetracking.repository;

import com.example.employeetimetracking.model.entities.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {
    List<LeaveBalance> findByUserId(Long userId);
    List<LeaveBalance> findByUserIdAndYear(Long userId, int year);
    Optional<LeaveBalance> findByUserIdAndLeaveTypeIdAndYear(Long userId, Long leaveTypeId, int year);
    List<LeaveBalance> findByLeaveTypeId(Long leaveTypeId);
    Optional<LeaveBalance> findByUserIdAndLeaveTypeId(Long userId , Long leaveTypeId);
    @Query("""
    SELECT DISTINCT lb
    FROM LeaveBalance lb
    JOIN FETCH lb.leaveType lt
    JOIN FETCH lt.leavePolicy
    """)
    List<LeaveBalance> findAllLeaveBalances();
}