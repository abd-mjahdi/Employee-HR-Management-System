package com.example.employeetimetracking.repository;

import com.example.employeetimetracking.model.entities.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {
    Optional<LeaveBalance> findByIdAndCompanyId(Long id, Long companyId);

    List<LeaveBalance> findAllByCompanyId(Long companyId);

    List<LeaveBalance> findByCompanyIdAndUserId(Long companyId, Long userId);

    List<LeaveBalance> findByCompanyIdAndUserIdAndYear(Long companyId, Long userId, int year);

    Optional<LeaveBalance> findByCompanyIdAndUserIdAndLeaveTypeIdAndYear(
            Long companyId, Long userId, Long leaveTypeId, int year);

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

    @Query("""
    SELECT DISTINCT lb
    FROM LeaveBalance lb
    JOIN FETCH lb.user u
    JOIN FETCH u.department d
    JOIN FETCH lb.leaveType lt
    JOIN FETCH lt.leavePolicy
    WHERE lb.year = :year AND lb.company.id = :companyId
    """)
    List<LeaveBalance> findAllLeaveBalancesForYearAndCompany(
            @Param("year") int year, @Param("companyId") Long companyId);

    @Query("""
    SELECT DISTINCT lb
    FROM LeaveBalance lb
    JOIN FETCH lb.user u
    JOIN FETCH u.department d
    JOIN FETCH lb.leaveType lt
    JOIN FETCH lt.leavePolicy
    WHERE lb.year = :year AND d.id = :departmentId AND lb.company.id = :companyId
    """)
    List<LeaveBalance> findLeaveBalancesForYearAndDepartmentAndCompany(
            @Param("year") int year,
            @Param("departmentId") Long departmentId,
            @Param("companyId") Long companyId);
}