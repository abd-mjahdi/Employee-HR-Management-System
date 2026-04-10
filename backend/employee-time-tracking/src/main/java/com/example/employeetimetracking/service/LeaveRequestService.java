package com.example.employeetimetracking.service;

import com.example.employeetimetracking.dto.response.LeaveRequestDto;
import com.example.employeetimetracking.exception.*;
import com.example.employeetimetracking.mapper.LeaveRequestMapper;
import com.example.employeetimetracking.model.entities.*;
import com.example.employeetimetracking.model.enums.Status;
import com.example.employeetimetracking.repository.LeaveBalanceRepository;
import com.example.employeetimetracking.repository.LeavePolicyRepository;
import com.example.employeetimetracking.repository.LeaveRequestRepository;
import com.example.employeetimetracking.repository.LeaveTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class LeaveRequestService {
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveRequestMapper leaveRequestMapper;
    private final LeavePolicyRepository leavePolicyRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    @Autowired
    public LeaveRequestService(LeaveRequestRepository leaveRequestRepository ,
                               LeaveTypeRepository leaveTypeRepository,
                               LeaveRequestMapper leaveRequestMapper,
                               LeavePolicyRepository leavePolicyRepository,
                               LeaveBalanceRepository leaveBalanceRepository){
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveRequestMapper = leaveRequestMapper;
        this.leavePolicyRepository = leavePolicyRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.leaveTypeRepository = leaveTypeRepository;
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

    public boolean isRequestWithinPolicy(LeaveRequest request, User user) {
        LeavePolicy policy = leavePolicyRepository.findByLeaveTypeId(request.getLeaveType().getId())
                .orElseThrow(() -> new LeavePolicyNotFoundException("Policy not found for leave type"));

        // Check minimum notice period
        long daysUntilStart = ChronoUnit.DAYS.between(LocalDate.now(), request.getStartDate());
        if (daysUntilStart < policy.getMinNoticeDays()) {
            return false;
        }

        // Check if user has enough balance
        LeaveBalance balance = leaveBalanceRepository.findByUserIdAndLeaveTypeIdAndYear(
                user.getId(),
                request.getLeaveType().getId(),
                request.getStartDate().getYear()
        ).orElse(null);

        if (balance == null) {
            return false;
        }

        BigDecimal balanceAfterRequest = balance.getCurrentBalance().subtract(request.getTotalDays());

        // Check if negative balance is allowed
        if (balanceAfterRequest.compareTo(BigDecimal.ZERO) < 0 && !policy.getAllowsNegativeBalance()) {
            return false;
        }

        // Check if request days don't exceed annual allocation
        if (request.getTotalDays().compareTo(policy.getAnnualAllocation()) > 0) {
            return false;
        }

        return true;
    }

    public void validateLeaveRequest(LeaveRequest lr, User user){
        LeaveType lt = leaveTypeRepository.findById(lr.getLeaveType().getId())
                .orElseThrow(() -> new LeaveTypeNotFoundException("Leave type not found"));

        if(!lt.getIsActive()){
            throw new InactiveLeaveTypeException("Leave type is inactive");
        }

        LeavePolicy policy = leavePolicyRepository.findByLeaveTypeId(lt.getId())
                .orElseThrow(() -> new LeavePolicyNotFoundException("Policy not found for leave type"));

        if(lr.getStartDate().isAfter(lr.getEndDate())){
            throw new InvalidDateRangeException("Start date cannot be after end date");
        }

        long daysUntilStart = ChronoUnit.DAYS.between(LocalDate.now(), lr.getStartDate());
        if (daysUntilStart < policy.getMinNoticeDays()) {
            throw new InsufficientNoticePeriodException("Leave request does not meet minimum notice period requirement");
        }

        List<LeaveRequest> existingLeaveRequests = leaveRequestRepository.findOverlappingRequests(user.getId(), List.of(Status.PENDING, Status.APPROVED), lr.getStartDate(), lr.getEndDate());
        if(!existingLeaveRequests.isEmpty()){
            throw new OverlappingLeaveRequestException("Leave request overlaps with an existing request");
        }

        LeaveBalance balance = leaveBalanceRepository.findByUserIdAndLeaveTypeIdAndYear(
                user.getId(),
                lr.getLeaveType().getId(),
                lr.getStartDate().getYear()
        ).orElseThrow(()-> new NullBalanceException("Leave balance not initialized"));

        BigDecimal balanceAfterRequest = balance.getCurrentBalance().subtract(lr.getTotalDays());

        // Check if negative balance is allowed
        if (balanceAfterRequest.compareTo(BigDecimal.ZERO) < 0 && !policy.getAllowsNegativeBalance()) {
            throw new InsufficientLeaveBalanceException("Insufficient leave balance for this request");
        }




    }


}
