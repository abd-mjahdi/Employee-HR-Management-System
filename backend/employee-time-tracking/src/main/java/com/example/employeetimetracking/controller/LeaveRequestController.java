package com.example.employeetimetracking.controller;

import com.example.employeetimetracking.dto.request.CreateLeaveRequestDto;
import com.example.employeetimetracking.dto.request.LeaveApprovalNotesDto;
import com.example.employeetimetracking.dto.request.LeaveCancelRequestDto;
import com.example.employeetimetracking.dto.request.LeaveDenyRequestDto;
import com.example.employeetimetracking.dto.response.LeaveRequestDto;
import com.example.employeetimetracking.dto.response.LeaveRequestReviewDto;
import com.example.employeetimetracking.model.enums.Status;
import com.example.employeetimetracking.security.CustomUserDetails;
import com.example.employeetimetracking.service.LeaveApprovalService;
import com.example.employeetimetracking.service.LeaveRequestService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    public LeaveRequestController(LeaveRequestService leaveRequestService, LeaveApprovalService leaveApprovalService) {
        this.leaveRequestService = leaveRequestService;
        this.leaveApprovalService = leaveApprovalService;
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','HR_ADMIN')")
    @PostMapping
    public ResponseEntity<LeaveRequestDto> createLeaveRequest(
            @Valid @RequestBody CreateLeaveRequestDto request,
            @AuthenticationPrincipal CustomUserDetails authenticatedUser
    ) {
        LeaveRequestDto lr = leaveRequestService.create(request, authenticatedUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(lr);
    }

    @GetMapping("/me")
    public ResponseEntity<List<LeaveRequestDto>> getAuthenticatedUserLeaveRequests(
            @AuthenticationPrincipal CustomUserDetails authenticatedUser
    ) {
        return ResponseEntity.ok(leaveRequestService.getByUserIdOrderByCreatedAtDesc(authenticatedUser.getId()));
    }

    @PreAuthorize("hasRole('HR_ADMIN')")
    @GetMapping
    public ResponseEntity<Page<LeaveRequestReviewDto>> listAllLeaveRequests(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            Pageable pageable
    ) {
        return ResponseEntity.ok(leaveRequestService.searchAll(userId, status, startDate, endDate, pageable));
    }

    @PreAuthorize("hasAnyRole('MANAGER','HR_ADMIN')")
    @GetMapping("/pending")
    public ResponseEntity<List<LeaveRequestReviewDto>> getDirectReportPendingRequests(
            @AuthenticationPrincipal CustomUserDetails authenticatedUser
    ) {
        boolean hr = authenticatedUser.hasRole("HR_ADMIN");
        return ResponseEntity.ok(leaveRequestService.getPendingForReviewer(authenticatedUser.getId(), hr));
    }

    @PreAuthorize("hasAnyRole('MANAGER','HR_ADMIN')")
    @GetMapping("/cancellation-pending")
    public ResponseEntity<List<LeaveRequestReviewDto>> getDirectReportCancellationPendingRequests(
            @AuthenticationPrincipal CustomUserDetails authenticatedUser
    ) {
        boolean hr = authenticatedUser.hasRole("HR_ADMIN");
        return ResponseEntity.ok(leaveRequestService.getCancellationPendingForReviewer(authenticatedUser.getId(), hr));
    }

    @PreAuthorize("hasAnyRole('MANAGER','HR_ADMIN')")
    @GetMapping("/team")
    public ResponseEntity<List<LeaveRequestReviewDto>> getTeamLeaveRequests(
            @AuthenticationPrincipal CustomUserDetails authenticatedUser,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    ) {
        return ResponseEntity.ok(leaveRequestService.getTeamLeaveRequests(
                authenticatedUser.getId(), status, startDate, endDate));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeaveRequestDto> getLeaveRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails authenticatedUser
    ) {
        return ResponseEntity.ok(leaveRequestService.getIfAllowed(id, authenticatedUser));
    }

    @PreAuthorize("hasAnyRole('MANAGER','HR_ADMIN')")
    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approve(
            @PathVariable Long id,
            @RequestBody(required = false) LeaveApprovalNotesDto request,
            @AuthenticationPrincipal CustomUserDetails authenticatedUser
    ) {
        leaveApprovalService.approve(id, authenticatedUser, request != null ? request.getNotes() : null);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('MANAGER','HR_ADMIN')")
    @PostMapping("/{id}/deny")
    public ResponseEntity<Void> deny(
            @PathVariable Long id,
            @Valid @RequestBody LeaveDenyRequestDto request,
            @AuthenticationPrincipal CustomUserDetails authenticatedUser
    ) {
        leaveApprovalService.deny(id, authenticatedUser, request.getReason());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(
            @PathVariable Long id,
            @RequestBody(required = false) LeaveCancelRequestDto request,
            @AuthenticationPrincipal CustomUserDetails authenticatedUser
    ) {
        leaveApprovalService.cancel(id, authenticatedUser, request != null ? request.getReason() : null);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('MANAGER','HR_ADMIN')")
    @PostMapping("/{id}/cancel-approve")
    public ResponseEntity<Void> approveCancellation(
            @PathVariable Long id,
            @RequestBody(required = false) LeaveApprovalNotesDto request,
            @AuthenticationPrincipal CustomUserDetails authenticatedUser
    ) {
        leaveApprovalService.approveCancellation(id, authenticatedUser, request != null ? request.getNotes() : null);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('MANAGER','HR_ADMIN')")
    @PostMapping("/{id}/cancel-deny")
    public ResponseEntity<Void> denyCancellation(
            @PathVariable Long id,
            @Valid @RequestBody LeaveDenyRequestDto request,
            @AuthenticationPrincipal CustomUserDetails authenticatedUser
    ) {
        leaveApprovalService.denyCancellation(id, authenticatedUser, request.getReason());
        return ResponseEntity.noContent().build();
    }
}
