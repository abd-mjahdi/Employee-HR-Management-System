package com.example.employeetimetracking.repository;

import com.example.employeetimetracking.model.entities.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {
    List<LeaveBalance> findByUserId(Long userId);
    List<LeaveBalance> findByUserIdAndYear(Long userId, int year);
    List<LeaveBalance> findByYear(int year);
    Optional<LeaveBalance> findByUserIdAndLeaveTypeIdAndYear(Long userId, Long leaveTypeId, int year);
    List<LeaveBalance> findByLeaveTypeId(Long leaveTypeId);
    @Query("""
    SELECT DISTINCT lb
    FROM LeaveBalance lb
    JOIN FETCH lb.leaveType lt
    JOIN FETCH lt.leavePolicy
    """)
    List<LeaveBalance> findAllLeaveBalances();

    @Query("""
    SELECT DISTINCT lb
    FROM LeaveBalance lb
    JOIN FETCH lb.user u
    JOIN FETCH u.department d
    JOIN FETCH lb.leaveType lt
    JOIN FETCH lt.leavePolicy
    WHERE lb.year = :year
    """)
    List<LeaveBalance> findAllLeaveBalancesForYear(@Param("year") int year);

    @Query("""
    SELECT DISTINCT lb
    FROM LeaveBalance lb
    JOIN FETCH lb.user u
    JOIN FETCH u.department d
    JOIN FETCH lb.leaveType lt
    JOIN FETCH lt.leavePolicy
    WHERE lb.year = :year AND d.id = :departmentId
    """)
    List<LeaveBalance> findLeaveBalancesForYearAndDepartment(@Param("year") int year, @Param("departmentId") Long departmentId);
}