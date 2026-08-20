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
import java.util.Optional;

public interface TimeEntryRepository extends JpaRepository<TimeEntry, Long>, JpaSpecificationExecutor<TimeEntry> {

    Optional<TimeEntry> findByIdAndCompanyId(Long id, Long companyId);

    List<TimeEntry> findByCompanyIdAndUserIdOrderByEntryDateDesc(Long companyId, Long userId, Pageable limit);

    List<TimeEntry> findByCompanyIdAndUserIdAndEntryDate(Long companyId, Long userId, LocalDate entryDate);

    List<TimeEntry> findByCompanyIdAndUserIdAndEntryDateBetweenAndStatus(
            Long companyId, Long userId, LocalDate startDate, LocalDate endDate, Status status);

    List<TimeEntry> findByUserId(Long userId);
    List<TimeEntry> findByUserIdAndStatus(Long userId, Status status);

    List<TimeEntry> findByStatus(Status status);
    List<TimeEntry> findByApprovedById(Long approverId);
    List<TimeEntry> findByProjectId(Long projectId);
    List<TimeEntry> findByUserIdAndEntryDateBetweenAndStatus(Long userId, LocalDate startDate, LocalDate endDate ,Status status);
    List<TimeEntry> findByUserIdOrderByEntryDateDesc(Long userId , Pageable limit);
    List<TimeEntry> findByUserIdAndEntryDate(Long userId, LocalDate entryDate);

    @Query("""
    SELECT te FROM TimeEntry te
    JOIN te.user u
    JOIN u.memberships om
    JOIN om.managerMembership mm
    JOIN mm.user managerUser
    WHERE managerUser.id = :managerId
      AND om.company = te.company
      AND te.status = :status
    ORDER BY te.createdAt ASC
    """)
    List<TimeEntry> findByUserManagerIdAndStatusOrderByCreatedAtAsc(
            @Param("managerId") Long managerId,
            @Param("status") Status status);

    Integer countByUserIdAndStatus(Long userId, Status status);

    @Query("""
    SELECT COUNT(te) FROM TimeEntry te
    JOIN te.user u
    JOIN u.memberships om
    JOIN om.managerMembership mm
    JOIN mm.user managerUser
    WHERE managerUser.id = :managerId
      AND om.company = te.company
      AND te.status = :status
    """)
    Integer countByUserManagerIdAndStatus(@Param("managerId") Long managerId, @Param("status") Status status);

    @Query("""
    SELECT DISTINCT te
    FROM TimeEntry te
    JOIN FETCH te.user u
    JOIN FETCH te.project p
    WHERE te.status = :status
      AND te.entryDate BETWEEN :startDate AND :endDate
    """)
    List<TimeEntry> findForDepartmentUtilization(
            @Param("status") Status status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
    SELECT DISTINCT te
    FROM TimeEntry te
    JOIN FETCH te.user u
    JOIN FETCH te.project p
    WHERE te.status = :status
      AND te.entryDate BETWEEN :startDate AND :endDate
    """)
    List<TimeEntry> findForProjectHours(
            @Param("status") Status status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
    SELECT DISTINCT te
    FROM TimeEntry te
    JOIN FETCH te.user u
    JOIN FETCH te.project p
    WHERE te.company.id = :companyId
      AND te.status = :status
      AND te.entryDate BETWEEN :startDate AND :endDate
    """)
    List<TimeEntry> findForDepartmentUtilizationByCompany(
            @Param("companyId") Long companyId,
            @Param("status") Status status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
    SELECT DISTINCT te
    FROM TimeEntry te
    JOIN FETCH te.user u
    JOIN FETCH te.project p
    WHERE te.company.id = :companyId
      AND te.status = :status
      AND te.entryDate BETWEEN :startDate AND :endDate
    """)
    List<TimeEntry> findForProjectHoursByCompany(
            @Param("companyId") Long companyId,
            @Param("status") Status status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}