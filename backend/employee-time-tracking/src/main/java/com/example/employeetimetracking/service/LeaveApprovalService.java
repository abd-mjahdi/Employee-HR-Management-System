package com.example.employeetimetracking.service;

import com.example.employeetimetracking.exception.LeaveApprovalException;
import com.example.employeetimetracking.model.entities.LeavePolicy;
import com.example.employeetimetracking.model.entities.LeaveRequest;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.Status;
import com.example.employeetimetracking.model.enums.UserRole;
import com.example.employeetimetracking.security.CustomUserDetails;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class LeaveApprovalService {
    private final LeaveRequestService leaveRequestService;
    private final LeaveBalanceService leaveBalanceService;
    private final NotificationService notificationService;

    @Autowired
    public LeaveApprovalService(LeaveRequestService leaveRequestService,
                                LeaveBalanceService leaveBalanceService,
                                NotificationService notificationService) {
        this.leaveRequestService = leaveRequestService;
        this.leaveBalanceService = leaveBalanceService;
        this.notificationService = notificationService;
    }

    @Transactional
    public void approve(Long lrId, CustomUserDetails authenticatedUser, String approverNotes) {
        LeaveRequest lr = leaveRequestService.getById(lrId);
        User ownerOfRequest = lr.getUser();
        if (ownerOfRequest == null) {
            throw new LeaveApprovalException("Leave request has no owner");
        }
        if (ownerOfRequest.getId() != null && ownerOfRequest.getId().equals(authenticatedUser.getId())) {
            throw new AccessDeniedException("You can't approve your own leave request");
        }
        if (ownerOfRequest.getManager() == null || ownerOfRequest.getManager().getId() == null) {
            throw new LeaveApprovalException("Leave request owner has no manager assigned");
        }

        boolean isDirectReport = ownerOfRequest.getManager().getId().equals(authenticatedUser.getId());
        LeavePolicy policy = lr.getLeaveType().getLeavePolicy();

        if (!isDirectReport) {
            throw new AccessDeniedException("You can't approve this user");
        }

        if (lr.getStatus() != Status.PENDING) {
            throw new LeaveApprovalException("Leave request cannot be approved");
        }
        if (lr.getManagerApprovalStatus() != Status.PENDING) {
            throw new LeaveApprovalException("Leave request cannot be approved");
        }

        if (!policy.getRequiresHrApproval()) {
            leaveRequestService.approveDirectly(lr, authenticatedUser.getId(), approverNotes);
            leaveBalanceService.deductLeaveBalance(lr, ownerOfRequest);
            notificationService.notifyLeaveApproved(lr);
        } else {
            leaveRequestService.approvePendingHr(lr, authenticatedUser.getId(), approverNotes);
        }
    }

    @Transactional
    public void deny(Long lrId, CustomUserDetails authenticatedUser, String denialReason) {
        LeaveRequest lr = leaveRequestService.getById(lrId);
        User ownerOfRequest = lr.getUser();
        if (ownerOfRequest == null) {
            throw new LeaveApprovalException("Leave request has no owner");
        }
        if (ownerOfRequest.getId() != null && ownerOfRequest.getId().equals(authenticatedUser.getId())) {
            throw new AccessDeniedException("You can't deny your own leave request");
        }
        if (ownerOfRequest.getManager() == null || ownerOfRequest.getManager().getId() == null) {
            throw new LeaveApprovalException("Leave request owner has no manager assigned");
        }

        boolean isDirectReport = ownerOfRequest.getManager().getId().equals(authenticatedUser.getId());
        if (!isDirectReport) {
            throw new AccessDeniedException("You can't deny this user");
        }

        if (lr.getStatus() != Status.PENDING) {
            throw new LeaveApprovalException("Leave request cannot be denied");
        }
        if (lr.getManagerApprovalStatus() != Status.PENDING) {
            throw new LeaveApprovalException("Leave request cannot be denied");
        }

        leaveRequestService.deny(lr, authenticatedUser.getId(), denialReason);
        notificationService.notifyLeaveDenied(lr);
    }

    @Transactional
    public void hrApprove(Long lrId, CustomUserDetails authenticatedUser, String hrNotes) {
        LeaveRequest lr = leaveRequestService.getById(lrId);
        User ownerOfRequest = lr.getUser();
        if (ownerOfRequest == null) {
            throw new LeaveApprovalException("Leave request has no owner");
        }
        if (ownerOfRequest.getId().equals(authenticatedUser.getId())) {
            throw new AccessDeniedException("You can't approve your own leave request");
        }

        LeavePolicy policy = lr.getLeaveType().getLeavePolicy();
        if (lr.getStatus() != Status.PENDING || lr.getHrApprovalStatus() != Status.PENDING) {
            throw new LeaveApprovalException("Leave request cannot be approved");
        }

        boolean ownerIsEmployee = ownerOfRequest.getUserRole() == UserRole.EMPLOYEE;
        if (ownerIsEmployee) {
            if (!policy.getRequiresHrApproval()) {
                throw new LeaveApprovalException("Leave request does not require HR approval");
            }
            if (lr.getManagerApprovalStatus() != Status.APPROVED) {
                throw new LeaveApprovalException("Leave request must be manager-approved first");
            }
        }

        leaveRequestService.hrApprove(lr, authenticatedUser.getId(), hrNotes);
        leaveBalanceService.deductLeaveBalance(lr, ownerOfRequest);
        notificationService.notifyLeaveApproved(lr);
    }

    @Transactional
    public void hrDeny(Long lrId, CustomUserDetails authenticatedUser, String denialReason) {
        LeaveRequest lr = leaveRequestService.getById(lrId);
        User ownerOfRequest = lr.getUser();
        if (ownerOfRequest == null) {
            throw new LeaveApprovalException("Leave request has no owner");
        }
        if (ownerOfRequest.getId() != null && ownerOfRequest.getId().equals(authenticatedUser.getId())) {
            throw new AccessDeniedException("You can't deny your own leave request");
        }

        LeavePolicy policy = lr.getLeaveType().getLeavePolicy();
        if (lr.getStatus() != Status.PENDING || lr.getHrApprovalStatus() != Status.PENDING) {
            throw new LeaveApprovalException("Leave request cannot be denied");
        }

        boolean ownerIsEmployee = ownerOfRequest.getUserRole() == UserRole.EMPLOYEE;
        if (ownerIsEmployee) {
            if (!policy.getRequiresHrApproval()) {
                throw new LeaveApprovalException("Leave request does not require HR denial");
            }
            if (lr.getManagerApprovalStatus() != Status.APPROVED) {
                throw new LeaveApprovalException("Leave request must be manager-reviewed first");
            }
        }

        leaveRequestService.hrDeny(lr, authenticatedUser.getId(), denialReason);
        notificationService.notifyLeaveDenied(lr);
    }

    @Transactional
    public void cancel(Long lrId, CustomUserDetails authenticatedUser, String reason) {
        LeaveRequest lr = leaveRequestService.getById(lrId);
        User ownerOfRequest = lr.getUser();
        if (ownerOfRequest == null || ownerOfRequest.getId() == null) {
            throw new LeaveApprovalException("Leave request has no owner");
        }
        if (!ownerOfRequest.getId().equals(authenticatedUser.getId())) {
            throw new AccessDeniedException("You can't cancel this leave request");
        }

        if (lr.getStatus() == Status.PENDING) {
            if (reason != null && !reason.isBlank()) {
                lr.setManagerNotes(reason);
            }
            leaveRequestService.cancel(lr);
            notificationService.notifyLeaveCancelled(lr);
            return;
        }

        if (lr.getStatus() == Status.APPROVED) {
            if (reason != null && !reason.isBlank()) {
                lr.setManagerNotes(reason);
            }
            notificationService.notifyCancellationRequested(lr);
            return;
        }

        throw new LeaveApprovalException("Leave request cannot be cancelled");
    }

    @Transactional
    public void approveCancellation(Long lrId, CustomUserDetails authenticatedUser, String notes) {
        LeaveRequest lr = leaveRequestService.getById(lrId);
        User ownerOfRequest = lr.getUser();
        if (ownerOfRequest == null) {
            throw new LeaveApprovalException("Leave request has no owner");
        }
        if (ownerOfRequest.getId() != null && ownerOfRequest.getId().equals(authenticatedUser.getId())) {
            throw new AccessDeniedException("You can't cancel your own leave request");
        }
        if (ownerOfRequest.getManager() == null || ownerOfRequest.getManager().getId() == null) {
            throw new LeaveApprovalException("Leave request owner has no manager assigned");
        }

        boolean isDirectReport = ownerOfRequest.getManager().getId().equals(authenticatedUser.getId());
        if (!isDirectReport) {
            throw new AccessDeniedException("You can't cancel this user's leave request");
        }

        if (lr.getStatus() != Status.APPROVED) {
            throw new LeaveApprovalException("Only approved leave requests require cancellation approval");
        }

        if (notes != null && !notes.isBlank()) {
            lr.setManagerNotes(notes);
        }
        leaveRequestService.cancel(lr);
        leaveBalanceService.restoreLeaveBalance(lr, ownerOfRequest);
        notificationService.notifyLeaveCancelled(lr);
    }

    @Transactional
    public void denyCancellation(Long lrId, CustomUserDetails authenticatedUser, String reason) {
        LeaveRequest lr = leaveRequestService.getById(lrId);
        User ownerOfRequest = lr.getUser();
        if (ownerOfRequest == null) {
            throw new LeaveApprovalException("Leave request has no owner");
        }
        if (ownerOfRequest.getManager() == null || ownerOfRequest.getManager().getId() == null) {
            throw new LeaveApprovalException("Leave request owner has no manager assigned");
        }

        boolean isDirectReport = ownerOfRequest.getManager().getId().equals(authenticatedUser.getId());
        if (!isDirectReport) {
            throw new AccessDeniedException("You can't deny this cancellation request");
        }
        if (lr.getStatus() != Status.APPROVED) {
            throw new LeaveApprovalException("Only approved leave requests can have cancellation denied");
        }

        lr.setManagerNotes(reason);
        notificationService.notifyCancellationDenied(lr);
    }

}

