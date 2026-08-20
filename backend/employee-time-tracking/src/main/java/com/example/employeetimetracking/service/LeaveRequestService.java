package com.example.employeetimetracking.service;

import com.example.employeetimetracking.dto.request.CreateLeaveRequestDto;
import com.example.employeetimetracking.dto.response.LeaveRequestDto;
import com.example.employeetimetracking.dto.response.LeaveRequestReviewDto;
import com.example.employeetimetracking.exception.*;
import com.example.employeetimetracking.mapper.LeaveRequestMapper;
import com.example.employeetimetracking.model.entities.*;
import com.example.employeetimetracking.model.enums.Status;
import com.example.employeetimetracking.model.enums.UserRole;
import com.example.employeetimetracking.repository.CompanyRepository;
import com.example.employeetimetracking.repository.LeaveRequestRepository;
import com.example.employeetimetracking.security.CustomUserDetails;
import com.example.employeetimetracking.specification.LeaveRequestSpecifications;
import com.example.employeetimetracking.tenant.MembershipAccess;
import com.example.employeetimetracking.tenant.TenantContext;
import com.example.employeetimetracking.util.WorkingDaysCalculator;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class LeaveRequestService {
    private final LeaveTypeService leaveTypeService;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveRequestMapper leaveRequestMapper;
    private final LeavePolicyService leavePolicyService;
    private final LeaveBalanceService leaveBalanceService;
    private final UserService userService;
    private final WorkingDaysCalculator workingDaysCalculator;
    private final MembershipAccess membershipAccess;
    private final CompanyRepository companyRepository;

    @Autowired
    public LeaveRequestService(LeaveRequestRepository leaveRequestRepository,
                               LeaveTypeService leaveTypeService,
                               LeaveRequestMapper leaveRequestMapper,
                               LeavePolicyService leavePolicyService,
                               LeaveBalanceService leaveBalanceService,
                               UserService userService,
                               WorkingDaysCalculator workingDaysCalculator,
                               MembershipAccess membershipAccess,
                               CompanyRepository companyRepository) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveTypeService = leaveTypeService;
        this.leaveRequestMapper = leaveRequestMapper;
        this.leavePolicyService = leavePolicyService;
        this.leaveBalanceService = leaveBalanceService;
        this.userService = userService;
        this.workingDaysCalculator = workingDaysCalculator;
        this.membershipAccess = membershipAccess;
        this.companyRepository = companyRepository;
    }
    public List<LeaveRequestDto> getByUserIdOrderByCreatedAtDesc(Long userId){
        return leaveRequestRepository.findByCompanyIdAndUserIdOrderByCreatedAtDesc(currentCompanyId(), userId)
                .stream().map(leaveRequestMapper::toDto).toList();
    }

    public LeaveRequest getById(Long id){
        return leaveRequestRepository.findByIdAndCompanyId(id, currentCompanyId())
                .orElseThrow(()-> new LeaveRequestNotFoundException("Leave request not found with the id :"+id));
    }

    public LeaveRequestDto getIfAllowed(Long id, CustomUserDetails authenticatedUser) {
        LeaveRequest lr = getById(id);
        User owner = lr.getUser();
        Long ownerId = owner != null ? owner.getId() : null;
        Long managerId = owner != null ? membershipAccess.managerUserId(ownerId) : null;

        boolean isOwner = Objects.equals(authenticatedUser.getId(), ownerId);
        boolean isManager = Objects.equals(authenticatedUser.getId(), managerId);
        boolean isHrAdmin = authenticatedUser.hasRole("HR_ADMIN");
        if (isOwner || isManager || isHrAdmin) {
            return leaveRequestMapper.toDto(lr);
        }
        throw new AccessDeniedException("You cannot access this resource");
    }

    public Page<LeaveRequestReviewDto> searchAll(
            Long userId,
            Status status,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {
        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "createdAt")
            );
        }
        Specification<LeaveRequest> spec = Specification
                .where(LeaveRequestSpecifications.belongsToCurrentCompany())
                .and(LeaveRequestSpecifications.hasUserId(userId))
                .and(LeaveRequestSpecifications.hasStatus(status))
                .and(LeaveRequestSpecifications.afterDate(startDate))
                .and(LeaveRequestSpecifications.beforeDate(endDate));
        return leaveRequestRepository.findAll(spec, pageable)
                .map(leaveRequestMapper::toLeaveRequestReviewDto);
    }

    // Self approved leave requests
    public List<LeaveRequestDto> getUpcomingLeave(User user){
        return getUpcomingLeave(user.getId(), 10);
    }

    public List<LeaveRequestDto> getUpcomingLeave(Long userId, int limit){
        int safeLimit = Math.max(1, Math.min(limit, 50));
        Pageable pageable = PageRequest.of(0, safeLimit);
        return leaveRequestRepository
                .findByCompanyIdAndUserIdAndStatusInAndStartDateAfterOrderByStartDateAsc(
                        currentCompanyId(),
                        userId,
                        List.of(Status.APPROVED, Status.CANCELLATION_PENDING),
                        LocalDate.now(),
                        pageable)
                .stream()
                .map(leaveRequestMapper::toDto)
                .toList();
    }

    public List<LeaveRequestDto> getRecentLeaveRequests(Long userId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        Pageable pageable = PageRequest.of(0, safeLimit);
        return leaveRequestRepository.findByCompanyIdAndUserIdOrderByCreatedAtDesc(currentCompanyId(), userId, pageable)
                .stream()
                .map(leaveRequestMapper::toDto)
                .toList();
    }

    // Count of self pending leave requests
    public Integer getUserPendingCount(Long userId){
        return leaveRequestRepository.countByCompanyIdAndUserIdAndStatus(currentCompanyId(), userId, Status.PENDING);
    }
    // Number of leave requests from their direct reports with manager_approval_status=PENDING waiting for the manager to approve
    public Integer getPendingLeaveApprovalsCount(Long actorId){
        return getPendingLeaveApprovalsCount(actorId, false);
    }

    public Integer getPendingLeaveApprovalsCount(Long actorId, boolean hrAdmin){
        Long companyId = currentCompanyId();
        int reports = nz(leaveRequestRepository.countByManagerIdAndStatusForCompany(actorId, Status.PENDING, companyId));
        if (!hrAdmin) {
            return reports;
        }
        return reports + nz(leaveRequestRepository.countByUserUserRoleAndStatusAndUserIdNotForCompany(
                UserRole.HR_ADMIN, Status.PENDING, actorId, companyId));
    }

    public List<LeaveRequestReviewDto> getDirectReportPendingRequests(Long actorId){
        return getPendingForReviewer(actorId, false);
    }

    public List<LeaveRequestReviewDto> getPendingForReviewer(Long actorId, boolean hrAdmin) {
        return requestsForReviewer(actorId, hrAdmin, Status.PENDING);
    }

    public List<LeaveRequestReviewDto> getDirectReportCancellationPendingRequests(Long actorId){
        return getCancellationPendingForReviewer(actorId, false);
    }

    public List<LeaveRequestReviewDto> getCancellationPendingForReviewer(Long actorId, boolean hrAdmin) {
        return requestsForReviewer(actorId, hrAdmin, Status.CANCELLATION_PENDING);
    }

    private List<LeaveRequestReviewDto> requestsForReviewer(Long actorId, boolean hrAdmin, Status status) {
        Long companyId = currentCompanyId();
        List<LeaveRequest> requests = new ArrayList<>(
                leaveRequestRepository.findByUserManagerIdAndStatusForCompany(actorId, status, companyId)
        );
        if (hrAdmin) {
            requests.addAll(leaveRequestRepository.findByUserUserRoleAndStatusAndUserIdNotForCompany(
                    UserRole.HR_ADMIN, status, actorId, companyId));
        }
        return requests.stream().map(leaveRequestMapper::toLeaveRequestReviewDto).toList();
    }

    private int nz(Integer value) {
        return value == null ? 0 : value;
    }
    // Number of their direct reports who have approved leave for today's date
    public Integer getTeamMembersOnLeaveToday(Long managerId){
        LocalDate today = LocalDate.now();
        return leaveRequestRepository.teamMembersOnLeaveTodayInStatusesForCompany(
                managerId,
                List.of(Status.APPROVED, Status.CANCELLATION_PENDING),
                today,
                currentCompanyId());
    }

    public void validateLeaveRequest(LeaveRequest lr, LeavePolicy policy, LeaveBalance balance){
        // leavetype and user are fine to access via getters because they are loaded by setters
        if(lr.getStartDate().isAfter(lr.getEndDate())){
            throw new InvalidDateRangeException("Start date cannot be after end date");
        }

        if (lr.getStartDate().getYear() != lr.getEndDate().getYear()) {
            throw new InvalidLeaveRequestException(
                    "Leave requests cannot span across years. Please submit two separate requests."
            );
        }

        long daysUntilStart = ChronoUnit.DAYS.between(LocalDate.now(), lr.getStartDate());
        if (daysUntilStart < policy.getMinNoticeDays()) {
            throw new InsufficientNoticePeriodException("Leave request does not meet minimum notice period requirement");
        }

        List<LeaveRequest> existingLeaveRequests = leaveRequestRepository.findOverlappingRequestsForCompany(
                lr.getUser().getId(),
                currentCompanyId(),
                List.of(Status.PENDING, Status.APPROVED, Status.CANCELLATION_PENDING),
                lr.getStartDate(),
                lr.getEndDate());
        if(!existingLeaveRequests.isEmpty()){
            throw new OverlappingLeaveRequestException("Leave request overlaps with an existing request");
        }

        BigDecimal balanceAfterRequest = balance.getCurrentBalance().subtract(lr.getTotalDays());

        // Check if negative balance is allowed
        if (balanceAfterRequest.compareTo(BigDecimal.ZERO) < 0 && !policy.getAllowsNegativeBalance()) {
            throw new InsufficientLeaveBalanceException("Insufficient leave balance for this request");
        }
    }

    @Transactional
    public LeaveRequestDto create(CreateLeaveRequestDto request ,Long id){
        User user = userService.getById(id);
        LeaveType leaveType = leaveTypeService.getById(request.getLeaveTypeId());
        LeavePolicy policy = leavePolicyService.getPolicyByLeaveTypeId(request.getLeaveTypeId());
        LeaveBalance balance = leaveBalanceService.getByUserIdAndLeaveTypeIdAndYear(id, leaveType.getId(), request.getStartDate().getYear());

        Company company = companyRepository.findById(currentCompanyId())
                .orElseThrow(() -> new InvalidTenantException("Tenant not found"));

        LeaveRequest lr = new LeaveRequest();
        lr.setCompany(company);
        lr.setUser(user);
        lr.setLeaveType(leaveType);
        lr.setStartDate(request.getStartDate());
        lr.setEndDate(request.getEndDate());
        lr.setTotalDays(workingDaysCalculator.calculate(request.getStartDate(),request.getEndDate()));
        lr.setReason(request.getReason());
        lr.setStatus(Status.PENDING);
        lr.setManagerApprovalStatus(Status.PENDING);
        lr.setHrApprovalStatus(Status.PENDING);

        var membership = membershipAccess.findInCurrentCompany(id)
                .orElseThrow(() -> new InvalidLeaveRequestException("You must have a manager assigned before requesting leave"));
        if (membership.getRole() != UserRole.HR_ADMIN
                && (membership.getManagerMembership() == null
                || membership.getManagerMembership().getUser() == null
                || membership.getManagerMembership().getUser().getId() == null)) {
            throw new InvalidLeaveRequestException("You must have a manager assigned before requesting leave");
        }
        if (lr.getTotalDays() == null || lr.getTotalDays().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidLeaveRequestException("Leave request must include at least one working day");
        }

        validateLeaveRequest(lr,policy,balance);

        return leaveRequestMapper.toDto(leaveRequestRepository.save(lr));

    }

    public List<LeaveRequestReviewDto> getTeamLeaveRequests(
            Long managerId,
            Status status,
            LocalDate startDate,
            LocalDate endDate
    ){
        Specification<LeaveRequest> spec = Specification
                .where(LeaveRequestSpecifications.belongsToCurrentCompany())
                .and(LeaveRequestSpecifications.hasManagerId(managerId))
                .and(LeaveRequestSpecifications.hasStatus(status))
                .and(LeaveRequestSpecifications.afterDate(startDate))
                .and(LeaveRequestSpecifications.beforeDate(endDate));

        return leaveRequestRepository.findAll(spec)
                .stream().map(leaveRequestMapper::toLeaveRequestReviewDto).toList();
    }

    @Transactional
    public void approve(LeaveRequest lr, Long approverId, String approverNotes){
        LocalDateTime now = LocalDateTime.now();
        User approver = userService.getById(approverId);
        lr.setManagerApprovalStatus(Status.APPROVED);
        lr.setHrApprovalStatus(Status.APPROVED);
        lr.setStatus(Status.APPROVED);
        lr.setManagerApprovedBy(approver);
        lr.setManagerApprovedAt(now);
        lr.setManagerNotes(approverNotes);
    }

    @Transactional
    public void deny(LeaveRequest lr, Long approverId, String denialReason) {
        LocalDateTime now = LocalDateTime.now();
        User approver = userService.getById(approverId);
        lr.setStatus(Status.DENIED);
        lr.setManagerApprovalStatus(Status.DENIED);
        lr.setHrApprovalStatus(Status.DENIED);
        lr.setManagerNotes(denialReason);
        lr.setManagerApprovedBy(approver);
        lr.setManagerApprovedAt(now);
    }

    @Transactional
    public void cancel(LeaveRequest lr) {
        lr.setStatus(Status.CANCELLED);
        lr.setManagerApprovalStatus(Status.CANCELLED);
        lr.setHrApprovalStatus(Status.CANCELLED);
    }

    public boolean hasActiveLeaveRequestOnDate(User user, LocalDate entryDate, List<Status> statuses){
        return leaveRequestRepository.countInRangeAndStatusForUserAndCompany(
                user, currentCompanyId(), entryDate, statuses) != 0;
    }

    private static Long currentCompanyId() {
        return TenantContext.require().companyId();
    }

}
