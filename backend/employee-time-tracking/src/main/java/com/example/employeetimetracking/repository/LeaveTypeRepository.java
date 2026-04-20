package com.example.employeetimetracking.repository;

import com.example.employeetimetracking.model.entities.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LeaveTypeRepository extends JpaRepository<LeaveType, Long> {
    Optional<LeaveType> findByTypeName(String typeName);
    List<LeaveType> findByIsActive(Boolean isActive);
    Optional<LeaveType> findByIdAndIsActive(Long Id, Boolean isActive);
    @Query("""
    SELECT DISTINCT lt
    FROM LeaveType lt
    JOIN FETCH lt.leavePolicy
    """)
    List<LeaveType> findAllWithPolicy();
}