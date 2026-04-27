package com.example.employeetimetracking.repository;

import com.example.employeetimetracking.model.entities.LeaveRequest;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> , JpaSpecificationExecutor<LeaveRequest> {

    List<LeaveRequest> findByUserId(Long userId);
    List<LeaveRequest> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<LeaveRequest> findByIdAndStatus(Long id, Status status);
    List<LeaveRequest> findByUserIdAndStatus(Long userId, Status status);
    List<LeaveRequest> findByStatus(Status status);
    List<LeaveRequest> findByManagerApprovedById(Long managerId);
    List<LeaveRequest> findByHrApprovedById(Long hrId);
    List<LeaveRequest> findByManagerApprovalStatus(Status status);
    List<LeaveRequest> findByHrApprovalStatus(Status status);
    List<LeaveRequest> findByLeaveTypeId(Long leaveTypeId);
    List<LeaveRequest> findByStartDateBetween(LocalDate startDate, LocalDate endDate);
    Integer countByUserIdAndStatus(Long userId, Status status);

    @Query("SELECT COUNT(lr) FROM LeaveRequest lr WHERE lr.user.manager.id = :managerId AND lr.status = :status")
    Integer countByManagerIdAndStatus(@Param("managerId") Long managerId, @Param("status") Status status);

    @Query("SELECT COUNT(lr) FROM LeaveRequest lr WHERE lr.user.manager.id = :managerId AND lr.status = :status AND :date BETWEEN lr.startDate AND lr.endDate")
    Integer teamMembersOnLeaveToday(@Param("managerId") Long managerId, @Param("status") Status status, @Param("date") LocalDate date);

    Integer countByManagerApprovalStatusAndHrApprovalStatus(Status managerApprovalStatus , Status hrAdminApprovalStatus);

    @Query("""
    SELECT lr FROM LeaveRequest lr
    WHERE lr.user.id = :userId
    AND lr.status IN :statuses
    AND :startDate <= lr.endDate
    AND :endDate >= lr.startDate
    """)
    List<LeaveRequest> findOverlappingRequests(
            Long userId,
            List<Status> statuses,
            LocalDate startDate,
            LocalDate endDate
    );

    @Query("""
    SELECT lr FROM LeaveRequest lr
    WHERE lr.user.manager.id = :managerId
    AND lr.status = :status
    AND lr.startDate <= :endDate
    AND lr.endDate >= :startDate
    """)
    List<LeaveRequest> findByStatusAndDateRangeOverlap(
            @Param(("managerId"))  Long managerId,
            @Param("status") Status status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    List<LeaveRequest> findByUserManagerIdAndStatus(Long managerId ,Status status);
    List<LeaveRequest> findByUserManagerIdAndStatusAndStartDateAfterOrderByStartDateAsc(
            Long managerId,
            Status status,
            LocalDate startDate
    );
    List<LeaveRequest> findByStatusAndManagerApprovalStatusAndHrApprovalStatus(
            Status status,
            Status managerApprovalStatus,
            Status hrApprovalStatus
    );

    @Query("""
    SELECT COUNT(lr)
    FROM LeaveRequest lr
    WHERE lr.user = :user
    AND lr.status IN :statuses
    AND :entryDate BETWEEN lr.startDate AND lr.endDate
    """)
    long countInRangeAndStatusForUser(@Param("user") User user,
                                      @Param("entryDate") LocalDate entryDate,
                                      @Param("statuses") List<Status> statuses);


}