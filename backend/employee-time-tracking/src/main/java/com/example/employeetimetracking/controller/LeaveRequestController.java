package com.example.employeetimetracking.controller;

import com.example.employeetimetracking.dto.request.CreateLeaveRequestDto;
import com.example.employeetimetracking.dto.response.LeaveRequestDto;
import com.example.employeetimetracking.dto.response.LeaveRequestReviewDto;
import com.example.employeetimetracking.repository.LeaveRequestRepository;
import com.example.employeetimetracking.security.CustomUserDetails;
import com.example.employeetimetracking.service.LeaveRequestService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/leave-requests")
public class LeaveRequestController {
    private final LeaveRequestService leaveRequestService;
    @Autowired
    public LeaveRequestController(LeaveRequestService leaveRequestService){
        this.leaveRequestService = leaveRequestService;
    }

    @PostMapping
    public ResponseEntity<LeaveRequestDto> createLeaveRequest(@Valid @RequestBody CreateLeaveRequestDto request , @AuthenticationPrincipal CustomUserDetails authenticatedUser){
        LeaveRequestDto lr = leaveRequestService.create(request ,authenticatedUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(lr);
    }

    @GetMapping("/me")
    public ResponseEntity<List<LeaveRequestDto>> getAuthenticatedUserLeaveRequests(@AuthenticationPrincipal CustomUserDetails authenticatedUser){
        List<LeaveRequestDto> leaveRequests = leaveRequestService.getByUserIdOrderByCreatedAtDesc(authenticatedUser.getId());
        return ResponseEntity.ok(leaveRequests);
    }

    @PreAuthorize("hasAnyRole('MANAGER','HR_ADMIN')")
    @GetMapping("/pending")
    public ResponseEntity<List<LeaveRequestDto>> getDirectReportPendingRequests(@AuthenticationPrincipal CustomUserDetails authenticatedUser){
        List<LeaveRequestDto> leaveRequests = leaveRequestService.getDirectReportPendingRequests(authenticatedUser.getId());
        return ResponseEntity.ok(leaveRequests);
    }




}
