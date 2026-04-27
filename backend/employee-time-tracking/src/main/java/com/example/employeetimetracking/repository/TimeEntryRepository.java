package com.example.employeetimetracking.repository;

import com.example.employeetimetracking.model.entities.TimeEntry;
import com.example.employeetimetracking.model.enums.Status;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TimeEntryRepository extends JpaRepository<TimeEntry, Long>, JpaSpecificationExecutor<TimeEntry> {

    List<TimeEntry> findByUserId(Long userId);
    List<TimeEntry> findByUserIdAndStatus(Long userId, Status status);

    List<TimeEntry> findByStatus(Status status);
    List<TimeEntry> findByApprovedById(Long approverId);
    List<TimeEntry> findByProjectId(Long projectId);
    List<TimeEntry> findByUserIdAndEntryDateBetweenAndStatus(Long userId, LocalDate startDate, LocalDate endDate ,Status status);
    List<TimeEntry> findByUserIdOrderByEntryDateDesc(Long userId , Pageable limit);
    List<TimeEntry> findByUserIdAndEntryDate(Long userId, LocalDate entryDate);
    List<TimeEntry> findByUserManagerIdAndStatusOrderByCreatedAtAsc(Long managerId, Status status);
    Integer countByUserIdAndStatus(Long userId, Status status);
    Integer countByUserManagerIdAndStatus(Long managerId , Status status);

    @Query("""
    SELECT te
    FROM TimeEntry te
    JOIN FETCH te.user u
    JOIN FETCH u.department d
    JOIN FETCH te.project p
    WHERE te.status = :status
      AND te.entryDate BETWEEN :startDate AND :endDate
    """)
    List<TimeEntry> findForDepartmentUtilization(
            @Param("status") Status status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

}