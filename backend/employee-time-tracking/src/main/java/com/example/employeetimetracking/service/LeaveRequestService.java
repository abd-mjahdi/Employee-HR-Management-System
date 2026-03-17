package com.example.employeetimetracking.service;

import com.example.employeetimetracking.dto.response.LeaveRequestDto;
import com.example.employeetimetracking.dto.response.LeaveTypeDto;
import com.example.employeetimetracking.dto.response.UserResponseDto;
import com.example.employeetimetracking.model.entities.LeaveRequest;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.Status;
import com.example.employeetimetracking.repository.LeaveBalanceRepository;
import com.example.employeetimetracking.repository.LeaveRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class LeaveRequestService {

    private final UserService userService;
    private final LeaveTypeService leaveTypeService;
    private final LeaveRequestRepository leaveRequestRepository;
    @Autowired
    public LeaveRequestService( UserService userService ,
                                LeaveTypeService leaveTypeService,
                                LeaveRequestRepository leaveRequestRepository){
        this.userService = userService;
        this.leaveTypeService = leaveTypeService;
        this.leaveRequestRepository = leaveRequestRepository;
    }

    // Self approved leave requests
    public List<LeaveRequestDto> getUpcomingLeave(User user){
        List<LeaveRequest> allLeaveRequests = user.getLeaveRequestList();
        List<LeaveRequest> upcomingLeave = allLeaveRequests.stream().filter(leaveRequest -> leaveRequest.getStatus().equals(Status.APPROVED) && leaveRequest.getStartDate().isAfter(LocalDate.now())).toList();
        return upcomingLeave.stream().map(this::convertToDto).toList();
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

    public LeaveRequestDto convertToDto(LeaveRequest lr){

        UserResponseDto userResponseDto = userService.convertToDto(lr.getUser());
        LeaveTypeDto leaveTypeDto = leaveTypeService.convertToDto(lr.getLeaveType());

        return new LeaveRequestDto(lr.getId(),
                userResponseDto,
                leaveTypeDto,
                lr.getStartDate(),
                lr.getEndDate(),
                lr.getTotalDays(),
                lr.getReason(),
                lr.getStatus());
    }
}
