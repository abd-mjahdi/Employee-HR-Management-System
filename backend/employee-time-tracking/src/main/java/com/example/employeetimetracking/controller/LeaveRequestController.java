package com.example.employeetimetracking.controller;

import com.example.employeetimetracking.dto.request.CreateLeaveRequestDto;
import com.example.employeetimetracking.dto.response.LeaveRequestDto;
import com.example.employeetimetracking.dto.response.LeaveRequestReviewDto;
import com.example.employeetimetracking.model.enums.Status;
import com.example.employeetimetracking.repository.LeaveRequestRepository;
import com.example.employeetimetracking.security.CustomUserDetails;
import com.example.employeetimetracking.service.LeaveApprovalService;
import com.example.employeetimetracking.service.LeaveRequestService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/leave-requests")
public class LeaveRequestController {
    private final LeaveRequestService leaveRequestService;
    private final LeaveApprovalService leaveApprovalService;
    @Autowired
    public LeaveRequestController(LeaveRequestService leaveRequestService, LeaveApprovalService leaveApprovalService){
        this.leaveRequestService = leaveRequestService;
        this.leaveApprovalService = leaveApprovalService;
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
    public ResponseEntity<List<LeaveRequestReviewDto>> getDirectReportPendingRequests(@AuthenticationPrincipal CustomUserDetails authenticatedUser){
        List<LeaveRequestReviewDto> leaveRequests = leaveRequestService.getDirectReportPendingRequests(authenticatedUser.getId());
        return ResponseEntity.ok(leaveRequests);
    }

    @PreAuthorize("hasAnyRole('MANAGER','HR_ADMIN')")
    @GetMapping("/team")
    public ResponseEntity<List<LeaveRequestReviewDto>> getTeamLeaveRequests(
            @AuthenticationPrincipal CustomUserDetails authenticatedUser,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    ) {
        List<LeaveRequestReviewDto> leaveRequests =
                leaveRequestService.getTeamLeaveRequests(
                        authenticatedUser.getId(),
                        status,
                        startDate,
                        endDate
                );

        return ResponseEntity.ok(leaveRequests);
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approve(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails authenticatedUser){
        leaveApprovalService.approve(id, authenticatedUser);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
    }


}
