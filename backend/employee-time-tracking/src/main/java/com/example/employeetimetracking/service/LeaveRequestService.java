package com.example.employeetimetracking.service;

import com.example.employeetimetracking.dto.response.LeaveRequestDto;
import com.example.employeetimetracking.mapper.LeaveRequestMapper;
import com.example.employeetimetracking.model.entities.LeaveRequest;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.Status;
import com.example.employeetimetracking.repository.LeaveRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveRequestMapper leaveRequestMapper;
    @Autowired
    public LeaveRequestService(LeaveRequestRepository leaveRequestRepository , LeaveRequestMapper leaveRequestMapper){
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveRequestMapper = leaveRequestMapper;
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


}
