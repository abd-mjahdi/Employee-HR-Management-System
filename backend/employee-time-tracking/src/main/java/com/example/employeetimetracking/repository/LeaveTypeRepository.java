package com.example.employeetimetracking.repository;

import com.example.employeetimetracking.model.entities.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LeaveTypeRepository extends JpaRepository<LeaveType, Long> {
    Optional<LeaveType> findByIdAndCompanyId(Long id, Long companyId);

    List<LeaveType> findAllByCompanyId(Long companyId);

    Optional<LeaveType> findByCompanyIdAndTypeName(Long companyId, String typeName);

    List<LeaveType> findByCompanyIdAndIsActive(Long companyId, Boolean isActive);

    Optional<LeaveType> findByIdAndCompanyIdAndIsActive(Long id, Long companyId, Boolean isActive);

    Optional<LeaveType> findByTypeName(String typeName);

    List<LeaveType> findByIsActive(Boolean isActive);

    Optional<LeaveType> findByIdAndIsActive(Long Id, Boolean isActive);

    @Query("""
    SELECT DISTINCT lt
    FROM LeaveType lt
    JOIN FETCH lt.leavePolicy
    """)
    List<LeaveType> findAllWithPolicy();

    @Query("""
    SELECT DISTINCT lt
    FROM LeaveType lt
    JOIN FETCH lt.leavePolicy
    WHERE lt.company.id = :companyId
    """)
    List<LeaveType> findAllWithPolicyByCompanyId(@Param("companyId") Long companyId);
}
