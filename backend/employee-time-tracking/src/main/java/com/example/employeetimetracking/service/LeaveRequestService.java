package com.example.employeetimetracking.service;

import com.example.employeetimetracking.dto.request.CreateLeaveRequestDto;
import com.example.employeetimetracking.dto.response.LeaveRequestDto;
import com.example.employeetimetracking.dto.response.LeaveRequestReviewDto;
import com.example.employeetimetracking.exception.*;
import com.example.employeetimetracking.mapper.LeaveRequestMapper;
import com.example.employeetimetracking.model.entities.*;
import com.example.employeetimetracking.model.enums.Status;
import com.example.employeetimetracking.repository.LeaveRequestRepository;
import com.example.employeetimetracking.specification.LeaveRequestSpecifications;
import com.example.employeetimetracking.util.WorkingDaysCalculator;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class LeaveRequestService {
    private final LeaveTypeService leaveTypeService;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveRequestMapper leaveRequestMapper;
    private final LeavePolicyService leavePolicyService;
    private final LeaveBalanceService leaveBalanceService;
    private final UserService userService;
    private final WorkingDaysCalculator workingDaysCalculator;

    @Autowired
    public LeaveRequestService(LeaveRequestRepository leaveRequestRepository,
                               LeaveTypeService leaveTypeService,
                               LeaveRequestMapper leaveRequestMapper,
                               LeavePolicyService leavePolicyService,
                               LeaveBalanceService leaveBalanceService,
                               UserService userService,
                               WorkingDaysCalculator workingDaysCalculator) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveTypeService = leaveTypeService;
        this.leaveRequestMapper = leaveRequestMapper;
        this.leavePolicyService = leavePolicyService;
        this.leaveBalanceService = leaveBalanceService;
        this.userService = userService;
        this.workingDaysCalculator = workingDaysCalculator;
    }
    public List<LeaveRequestDto> getByUserIdOrderByCreatedAtDesc(Long userId){
        return leaveRequestRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(leaveRequestMapper::toDto).toList();
    }

    public LeaveRequest getById(Long id){
        return leaveRequestRepository.findById(id).orElseThrow(()-> new LeaveRequestNotFoundException("Leave request not found with the id :"+id));
    }

    // Self approved leave requests
    public List<LeaveRequestDto> getUpcomingLeave(User user){
        List<LeaveRequest> allLeaveRequests = user.getLeaveRequestList();
        List<LeaveRequest> upcomingLeave = allLeaveRequests.stream().filter(leaveRequest -> leaveRequest.getStatus().equals(Status.APPROVED) && leaveRequest.getStartDate().isAfter(LocalDate.now())).toList();
        return upcomingLeave.stream().map(leaveRequestMapper::toDto).toList();
    }

    // Count of self pending leave requests
    public Integer getUserPendingCount(Long userId){
        return leaveRequestRepository.countByUserIdAndStatus(userId ,Status.PENDING);
    }
    // Number of leave requests from their direct reports with manager_approval_status=PENDING waiting for the manager to approve
    public Integer getPendingLeaveApprovalsCount(Long managerId){
        return leaveRequestRepository.countByManagerIdAndStatus(managerId , Status.PENDING);
    }
    // Number of their direct reports who have approved leave for today's date
    public Integer getTeamMembersOnLeaveToday(Long managerId){
        LocalDate today = LocalDate.now();
        return leaveRequestRepository.teamMembersOnLeaveToday(managerId , Status.APPROVED , today);
    }
    // Number of leave requests across the whole company with manager_approval_status=APPROVED
    // but hr_approval_status=PENDING (leave requests waiting for HR's final approval after manager already approved)
    public Integer getPendingHrApprovalsCount(){
        return leaveRequestRepository.countByManagerApprovalStatusAndHrApprovalStatus(Status.APPROVED , Status.PENDING);
    }

    public void validateLeaveRequest(LeaveRequest lr, LeavePolicy policy, LeaveBalance balance){
        // leavetype and user are fine to access via getters because they are loaded by setters
        if(lr.getStartDate().isAfter(lr.getEndDate())){
            throw new InvalidDateRangeException("Start date cannot be after end date");
        }

        long daysUntilStart = ChronoUnit.DAYS.between(LocalDate.now(), lr.getStartDate());
        if (daysUntilStart < policy.getMinNoticeDays()) {
            throw new InsufficientNoticePeriodException("Leave request does not meet minimum notice period requirement");
        }

        List<LeaveRequest> existingLeaveRequests = leaveRequestRepository.findOverlappingRequests(lr.getUser().getId(), List.of(Status.PENDING, Status.APPROVED), lr.getStartDate(), lr.getEndDate());
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
        LeaveBalance balance = leaveBalanceService.getByUserIdAndLeaveTypeIdAndYear(id,leaveType.getId(),LocalDate.now().getYear());

        LeaveRequest lr = new LeaveRequest();
        lr.setUser(user);
        lr.setLeaveType(leaveType);
        lr.setStartDate(request.getStartDate());
        lr.setEndDate(request.getEndDate());
        lr.setTotalDays(workingDaysCalculator.calculate(request.getStartDate(),request.getEndDate()));
        lr.setReason(request.getReason());
        lr.setStatus(Status.PENDING);
        lr.setManagerApprovalStatus(Status.PENDING);
        lr.setHrApprovalStatus(Status.PENDING);

        validateLeaveRequest(lr,policy,balance);

        return leaveRequestMapper.toDto(leaveRequestRepository.save(lr));

    }

    public List<LeaveRequestReviewDto> getDirectReportPendingRequests(Long managerId){
        return leaveRequestRepository.findByUserManagerIdAndStatus(managerId,Status.PENDING)
                .stream().map(leaveRequestMapper::toLeaveRequestReviewDto).toList();
    }

    public List<LeaveRequestReviewDto> getTeamLeaveRequests(
            Long managerId,
            Status status,
            LocalDate startDate,
            LocalDate endDate
    ){
        Specification<LeaveRequest> spec = Specification
                .where(LeaveRequestSpecifications.hasManagerId(managerId))
                .and(LeaveRequestSpecifications.hasStatus(status))
                .and(LeaveRequestSpecifications.afterDate(startDate))
                .and(LeaveRequestSpecifications.beforeDate(endDate));

        return leaveRequestRepository.findAll(spec)
                .stream().map(leaveRequestMapper::toLeaveRequestReviewDto).toList();
    }

    @Transactional
    public void approveDirectly(LeaveRequest lr, Long approverId, String approverNotes){
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
    public void approvePendingHr(LeaveRequest lr, Long approverId, String approverNotes){
        LocalDateTime now = LocalDateTime.now();
        User approver = userService.getById(approverId);
        lr.setManagerApprovalStatus(Status.APPROVED);
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
    public void hrApprove(LeaveRequest lr, Long hrApproverId, String hrNotes) {
        LocalDateTime now = LocalDateTime.now();
        User approver = userService.getById(hrApproverId);
        if (lr.getManagerApprovalStatus() == Status.PENDING) {
            lr.setManagerApprovalStatus(Status.APPROVED);
        }
        lr.setHrApprovalStatus(Status.APPROVED);
        lr.setHrApprovedBy(approver);
        lr.setHrApprovedAt(now);
        lr.setHrNotes(hrNotes);
        lr.setStatus(Status.APPROVED);
    }

    @Transactional
    public void hrDeny(LeaveRequest lr, Long hrApproverId, String denialReason) {
        LocalDateTime now = LocalDateTime.now();
        User approver = userService.getById(hrApproverId);
        if (lr.getManagerApprovalStatus() == Status.PENDING) {
            lr.setManagerApprovalStatus(Status.DENIED);
        }
        lr.setStatus(Status.DENIED);
        lr.setHrApprovalStatus(Status.DENIED);
        lr.setHrApprovedBy(approver);
        lr.setHrApprovedAt(now);
        lr.setHrNotes(denialReason);
    }

    @Transactional
    public void cancel(LeaveRequest lr) {
        lr.setStatus(Status.CANCELLED);
        lr.setManagerApprovalStatus(Status.CANCELLED);
        lr.setHrApprovalStatus(Status.CANCELLED);
    }

}
