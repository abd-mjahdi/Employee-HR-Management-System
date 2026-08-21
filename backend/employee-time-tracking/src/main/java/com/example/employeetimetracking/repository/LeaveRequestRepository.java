package com.example.employeetimetracking.repository;

import com.example.employeetimetracking.model.entities.LeaveRequest;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.Status;
import com.example.employeetimetracking.model.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> , JpaSpecificationExecutor<LeaveRequest> {

    Optional<LeaveRequest> findByIdAndCompanyId(Long id, Long companyId);

    List<LeaveRequest> findByCompanyIdAndUserIdOrderByCreatedAtDesc(Long companyId, Long userId);

    List<LeaveRequest> findByCompanyIdAndUserIdOrderByCreatedAtDesc(Long companyId, Long userId, Pageable pageable);

    Integer countByCompanyIdAndUserIdAndStatus(Long companyId, Long userId, Status status);

    List<LeaveRequest> findByCompanyIdAndUserIdAndStatusInAndStartDateAfterOrderByStartDateAsc(
            Long companyId, Long userId, List<Status> statuses, LocalDate startDate, Pageable pageable);

    List<LeaveRequest> findByUserId(Long userId);
    List<LeaveRequest> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<LeaveRequest> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Optional<LeaveRequest> findByIdAndStatus(Long id, Status status);
    List<LeaveRequest> findByUserIdAndStatus(Long userId, Status status);
    List<LeaveRequest> findByStatus(Status status);
    List<LeaveRequest> findByManagerApprovedById(Long managerId);
    List<LeaveRequest> findByHrApprovedById(Long hrId);
    List<LeaveRequest> findByManagerApprovalStatus(Status status);
    List<LeaveRequest> findByHrApprovalStatus(Status status);
    List<LeaveRequest> findByLeaveTypeId(Long leaveTypeId);
    List<LeaveRequest> findByStartDateBetween(LocalDate startDate, LocalDate endDate);
    List<LeaveRequest> findByUserIdAndStatusAndStartDateAfterOrderByStartDateAsc(Long userId, Status status, LocalDate startDate, Pageable pageable);
    List<LeaveRequest> findByUserIdAndStatusInAndStartDateAfterOrderByStartDateAsc(Long userId, List<Status> statuses, LocalDate startDate, Pageable pageable);
    Integer countByUserIdAndStatus(Long userId, Status status);

    @Query("""
    SELECT COUNT(lr) FROM LeaveRequest lr
    JOIN lr.user u
    JOIN u.memberships om
    JOIN om.managerMembership mm
    JOIN mm.user managerUser
    WHERE managerUser.id = :managerId
      AND om.company = lr.company
      AND lr.status = :status
    """)
    Integer countByManagerIdAndStatus(@Param("managerId") Long managerId, @Param("status") Status status);

    @Query("""
    SELECT COUNT(lr) FROM LeaveRequest lr
    JOIN lr.user u
    JOIN u.memberships om
    JOIN om.managerMembership mm
    JOIN mm.user managerUser
    WHERE managerUser.id = :managerId
      AND om.company = lr.company
      AND lr.status = :status
      AND :date BETWEEN lr.startDate AND lr.endDate
    """)
    Integer teamMembersOnLeaveToday(@Param("managerId") Long managerId, @Param("status") Status status, @Param("date") LocalDate date);

    @Query("""
    SELECT COUNT(lr) FROM LeaveRequest lr
    JOIN lr.user u
    JOIN u.memberships om
    JOIN om.managerMembership mm
    JOIN mm.user managerUser
    WHERE managerUser.id = :managerId
      AND om.company = lr.company
      AND lr.status IN :statuses
      AND :date BETWEEN lr.startDate AND lr.endDate
    """)
    Integer teamMembersOnLeaveTodayInStatuses(@Param("managerId") Long managerId, @Param("statuses") List<Status> statuses, @Param("date") LocalDate date);

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
    JOIN lr.user u
    JOIN u.memberships om
    JOIN om.managerMembership mm
    JOIN mm.user managerUser
    WHERE managerUser.id = :managerId
      AND om.company = lr.company
      AND lr.status = :status
    """)
    List<LeaveRequest> findByUserManagerIdAndStatus(@Param("managerId") Long managerId, @Param("status") Status status);

    @Query("""
    SELECT lr FROM LeaveRequest lr
    JOIN lr.user u
    JOIN u.memberships om
    WHERE om.role = :userRole
      AND om.status = com.example.employeetimetracking.model.enums.MembershipStatus.ACTIVE
      AND om.company = lr.company
      AND lr.status = :status
      AND u.id <> :userId
    """)
    List<LeaveRequest> findByUserUserRoleAndStatusAndUserIdNot(
            @Param("userRole") UserRole userRole,
            @Param("status") Status status,
            @Param("userId") Long userId);

    @Query("""
    SELECT COUNT(lr) FROM LeaveRequest lr
    JOIN lr.user u
    JOIN u.memberships om
    WHERE om.role = :userRole
      AND om.status = com.example.employeetimetracking.model.enums.MembershipStatus.ACTIVE
      AND om.company = lr.company
      AND lr.status = :status
      AND u.id <> :userId
    """)
    Integer countByUserUserRoleAndStatusAndUserIdNot(
            @Param("userRole") UserRole userRole,
            @Param("status") Status status,
            @Param("userId") Long userId);

    @Query("""
    SELECT lr FROM LeaveRequest lr
    JOIN lr.user u
    JOIN u.memberships om
    JOIN om.managerMembership mm
    JOIN mm.user managerUser
    WHERE managerUser.id = :managerId
      AND om.company = lr.company
      AND lr.status = :status
      AND lr.startDate > :startDate
    ORDER BY lr.startDate ASC
    """)
    List<LeaveRequest> findByUserManagerIdAndStatusAndStartDateAfterOrderByStartDateAsc(
            @Param("managerId") Long managerId,
            @Param("status") Status status,
            @Param("startDate") LocalDate startDate
    );

    @Query("""
    SELECT lr FROM LeaveRequest lr
    JOIN lr.user u
    JOIN u.memberships om
    JOIN om.managerMembership mm
    JOIN mm.user managerUser
    WHERE managerUser.id = :managerId
      AND om.company = lr.company
      AND lr.status IN :statuses
      AND lr.startDate > :startDate
    ORDER BY lr.startDate ASC
    """)
    List<LeaveRequest> findByUserManagerIdAndStatusInAndStartDateAfterOrderByStartDateAsc(
            @Param("managerId") Long managerId,
            @Param("statuses") List<Status> statuses,
            @Param("startDate") LocalDate startDate
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

    @Query("""
    SELECT COUNT(lr)
    FROM LeaveRequest lr
    WHERE lr.user = :user
    AND lr.company.id = :companyId
    AND lr.status IN :statuses
    AND :entryDate BETWEEN lr.startDate AND lr.endDate
    """)
    long countInRangeAndStatusForUserAndCompany(@Param("user") User user,
                                                @Param("companyId") Long companyId,
                                                @Param("entryDate") LocalDate entryDate,
                                                @Param("statuses") List<Status> statuses);

    @Query("""
    SELECT COUNT(lr) FROM LeaveRequest lr
    JOIN lr.user u
    JOIN u.memberships om
    JOIN om.managerMembership mm
    JOIN mm.user managerUser
    WHERE managerUser.id = :managerId
      AND om.company.id = :companyId
      AND lr.company.id = :companyId
      AND lr.status = :status
    """)
    Integer countByManagerIdAndStatusForCompany(
            @Param("managerId") Long managerId,
            @Param("status") Status status,
            @Param("companyId") Long companyId);

    @Query("""
    SELECT COUNT(lr) FROM LeaveRequest lr
    JOIN lr.user u
    JOIN u.memberships om
    JOIN om.managerMembership mm
    JOIN mm.user managerUser
    WHERE managerUser.id = :managerId
      AND om.company.id = :companyId
      AND lr.company.id = :companyId
      AND lr.status IN :statuses
      AND :date BETWEEN lr.startDate AND lr.endDate
    """)
    Integer teamMembersOnLeaveTodayInStatusesForCompany(
            @Param("managerId") Long managerId,
            @Param("statuses") List<Status> statuses,
            @Param("date") LocalDate date,
            @Param("companyId") Long companyId);

    @Query("""
    SELECT lr FROM LeaveRequest lr
    JOIN lr.user u
    JOIN u.memberships om
    JOIN om.managerMembership mm
    JOIN mm.user managerUser
    WHERE managerUser.id = :managerId
      AND om.company.id = :companyId
      AND lr.company.id = :companyId
      AND lr.status = :status
    """)
    List<LeaveRequest> findByUserManagerIdAndStatusForCompany(
            @Param("managerId") Long managerId,
            @Param("status") Status status,
            @Param("companyId") Long companyId);

    @Query("""
    SELECT lr FROM LeaveRequest lr
    JOIN lr.user u
    JOIN u.memberships om
    WHERE om.role = :userRole
      AND om.status = com.example.employeetimetracking.model.enums.MembershipStatus.ACTIVE
      AND om.company.id = :companyId
      AND lr.company.id = :companyId
      AND lr.status = :status
      AND u.id <> :userId
    """)
    List<LeaveRequest> findByUserUserRoleAndStatusAndUserIdNotForCompany(
            @Param("userRole") UserRole userRole,
            @Param("status") Status status,
            @Param("userId") Long userId,
            @Param("companyId") Long companyId);

    @Query("""
    SELECT COUNT(lr) FROM LeaveRequest lr
    JOIN lr.user u
    JOIN u.memberships om
    WHERE om.role = :userRole
      AND om.status = com.example.employeetimetracking.model.enums.MembershipStatus.ACTIVE
      AND om.company.id = :companyId
      AND lr.company.id = :companyId
      AND lr.status = :status
      AND u.id <> :userId
    """)
    Integer countByUserUserRoleAndStatusAndUserIdNotForCompany(
            @Param("userRole") UserRole userRole,
            @Param("status") Status status,
            @Param("userId") Long userId,
            @Param("companyId") Long companyId);

    @Query("""
    SELECT lr FROM LeaveRequest lr
    WHERE lr.user.id = :userId
    AND lr.company.id = :companyId
    AND lr.status IN :statuses
    AND :startDate <= lr.endDate
    AND :endDate >= lr.startDate
    """)
    List<LeaveRequest> findOverlappingRequestsForCompany(
            @Param("userId") Long userId,
            @Param("companyId") Long companyId,
            @Param("statuses") List<Status> statuses,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
    SELECT lr FROM LeaveRequest lr
    JOIN lr.user u
    JOIN u.memberships om
    JOIN om.managerMembership mm
    JOIN mm.user managerUser
    WHERE managerUser.id = :managerId
      AND om.company.id = :companyId
      AND lr.company.id = :companyId
      AND lr.status IN :statuses
      AND lr.startDate <= :endDate
      AND lr.endDate >= :startDate
    """)
    List<LeaveRequest> findByStatusInAndDateRangeOverlapForCompany(
            @Param("managerId") Long managerId,
            @Param("statuses") List<Status> statuses,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("companyId") Long companyId);

    @Query("""
    SELECT lr FROM LeaveRequest lr
    JOIN lr.user u
    JOIN u.memberships om
    JOIN om.managerMembership mm
    JOIN mm.user managerUser
    WHERE managerUser.id = :managerId
      AND om.company.id = :companyId
      AND lr.company.id = :companyId
      AND lr.status IN :statuses
      AND lr.startDate > :startDate
    ORDER BY lr.startDate ASC
    """)
    List<LeaveRequest> findByUserManagerIdAndStatusInAndStartDateAfterOrderByStartDateAscForCompany(
            @Param("managerId") Long managerId,
            @Param("statuses") List<Status> statuses,
            @Param("startDate") LocalDate startDate,
            @Param("companyId") Long companyId);

    @Query("""
    SELECT DISTINCT lr
    FROM LeaveRequest lr
    JOIN FETCH lr.user u
    JOIN FETCH lr.leaveType lt
    WHERE lr.company.id = :companyId
      AND lr.status IN :statuses
      AND lr.startDate <= :endDate
      AND lr.endDate >= :startDate
    """)
    List<LeaveRequest> findByStatusInAndDateRangeOverlapAllForCompany(
            @Param("statuses") List<Status> statuses,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("companyId") Long companyId);
}