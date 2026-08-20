package com.example.employeetimetracking.service;

import com.example.employeetimetracking.exception.LeaveApprovalException;
import com.example.employeetimetracking.model.entities.CompanyMembership;
import com.example.employeetimetracking.model.entities.LeaveRequest;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.Status;
import com.example.employeetimetracking.model.enums.UserRole;
import com.example.employeetimetracking.security.CustomUserDetails;
import com.example.employeetimetracking.tenant.MembershipAccess;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class LeaveApprovalService {
    private final LeaveRequestService leaveRequestService;
    private final LeaveBalanceService leaveBalanceService;
    private final NotificationService notificationService;
    private final MembershipAccess membershipAccess;

    @Autowired
    public LeaveApprovalService(LeaveRequestService leaveRequestService,
                                LeaveBalanceService leaveBalanceService,
                                NotificationService notificationService,
                                MembershipAccess membershipAccess) {
        this.leaveRequestService = leaveRequestService;
        this.leaveBalanceService = leaveBalanceService;
        this.notificationService = notificationService;
        this.membershipAccess = membershipAccess;
    }

    @Transactional
    public void approve(Long lrId, CustomUserDetails authenticatedUser, String approverNotes) {
        LeaveRequest lr = leaveRequestService.getById(lrId);
        User owner = requireCanDecide(lr, authenticatedUser,
                "You can't approve your own leave request",
                "You can't approve this user");
        if (lr.getStatus() != Status.PENDING || lr.getManagerApprovalStatus() != Status.PENDING) {
            throw new LeaveApprovalException("Leave request cannot be approved");
        }
        leaveRequestService.approve(lr, authenticatedUser.getId(), approverNotes);
        leaveBalanceService.deductLeaveBalance(lr, owner);
        notificationService.notifyLeaveApproved(lr);
    }

    @Transactional
    public void deny(Long lrId, CustomUserDetails authenticatedUser, String denialReason) {
        LeaveRequest lr = leaveRequestService.getById(lrId);
        requireCanDecide(lr, authenticatedUser,
                "You can't deny your own leave request",
                "You can't deny this user");
        if (lr.getStatus() != Status.PENDING || lr.getManagerApprovalStatus() != Status.PENDING) {
            throw new LeaveApprovalException("Leave request cannot be denied");
        }
        leaveRequestService.deny(lr, authenticatedUser.getId(), denialReason);
        notificationService.notifyLeaveDenied(lr);
    }

    @Transactional
    public void cancel(Long lrId, CustomUserDetails authenticatedUser, String reason) {
        LeaveRequest lr = leaveRequestService.getById(lrId);
        User owner = lr.getUser();
        if (owner == null || owner.getId() == null) {
            throw new LeaveApprovalException("Leave request has no owner");
        }
        if (!owner.getId().equals(authenticatedUser.getId())) {
            throw new AccessDeniedException("You can't cancel this leave request");
        }

        if (lr.getStatus() == Status.PENDING) {
            if (reason != null && !reason.isBlank()) {
                lr.setCancellationReason(reason);
            }
            leaveRequestService.cancel(lr);
            notificationService.notifyLeaveCancelled(lr);
            return;
        }

        if (lr.getStatus() == Status.APPROVED) {
            lr.setStatus(Status.CANCELLATION_PENDING);
            if (reason != null && !reason.isBlank()) {
                lr.setCancellationReason(reason);
            }
            notificationService.notifyCancellationRequested(lr);
            return;
        }

        throw new LeaveApprovalException("Leave request cannot be cancelled");
    }

    @Transactional
    public void approveCancellation(Long lrId, CustomUserDetails authenticatedUser, String notes) {
        LeaveRequest lr = leaveRequestService.getById(lrId);
        User owner = requireCanDecide(lr, authenticatedUser,
                "You can't cancel your own leave request",
                "You can't cancel this user's leave request");
        if (lr.getStatus() != Status.CANCELLATION_PENDING) {
            throw new LeaveApprovalException("Only approved leave with a pending cancellation can be cancelled");
        }
        if (notes != null && !notes.isBlank()) {
            lr.setManagerNotes(notes);
        }
        leaveRequestService.cancel(lr);
        leaveBalanceService.restoreLeaveBalance(lr, owner);
        notificationService.notifyLeaveCancelled(lr);
    }

    @Transactional
    public void denyCancellation(Long lrId, CustomUserDetails authenticatedUser, String reason) {
        LeaveRequest lr = leaveRequestService.getById(lrId);
        requireCanDecide(lr, authenticatedUser,
                "You can't deny cancellation of your own leave request",
                "You can't deny this cancellation request");
        if (lr.getStatus() != Status.CANCELLATION_PENDING) {
            throw new LeaveApprovalException("Only a pending cancellation can be denied");
        }
        lr.setStatus(Status.APPROVED);
        if (reason != null && !reason.isBlank()) {
            lr.setManagerNotes(reason);
        }
        notificationService.notifyCancellationDenied(lr);
    }

    private User requireCanDecide(LeaveRequest lr, CustomUserDetails authenticatedUser,
                                  String selfMessage, String otherMessage) {
        User owner = lr.getUser();
        if (owner == null) {
            throw new LeaveApprovalException("Leave request has no owner");
        }
        if (owner.getId() != null && owner.getId().equals(authenticatedUser.getId())) {
            throw new AccessDeniedException(selfMessage);
        }
        CompanyMembership ownerMembership = membershipAccess
                .findFor(authenticatedUser, owner.getId())
                .orElseThrow(() -> new AccessDeniedException(otherMessage));
        boolean isAssignedManager = ownerMembership.getManagerMembership() != null
                && ownerMembership.getManagerMembership().getUser() != null
                && ownerMembership.getManagerMembership().getUser().getId() != null
                && ownerMembership.getManagerMembership().getUser().getId().equals(authenticatedUser.getId());
        boolean peerHr = ownerMembership.getRole() == UserRole.HR_ADMIN
                && authenticatedUser.hasRole("HR_ADMIN");
        if (isAssignedManager || peerHr) {
            return owner;
        }
        throw new AccessDeniedException(otherMessage);
    }
}
