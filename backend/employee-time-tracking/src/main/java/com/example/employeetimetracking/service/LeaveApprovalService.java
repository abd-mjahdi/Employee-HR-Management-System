package com.example.employeetimetracking.service;

import com.example.employeetimetracking.exception.LeaveApprovalException;
import com.example.employeetimetracking.model.entities.LeavePolicy;
import com.example.employeetimetracking.model.entities.LeaveRequest;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.Status;
import com.example.employeetimetracking.security.CustomUserDetails;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class LeaveApprovalService {
    private final LeaveRequestService leaveRequestService;
    private final LeaveBalanceService leaveBalanceService;

    @Autowired
    public LeaveApprovalService(LeaveRequestService leaveRequestService,
                                LeaveBalanceService leaveBalanceService) {
        this.leaveRequestService = leaveRequestService;
        this.leaveBalanceService = leaveBalanceService;
    }

    @Transactional
    public void approve(Long lrId, CustomUserDetails authenticatedUser) {
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
            leaveRequestService.approveDirectly(lr, authenticatedUser.getId());
            leaveBalanceService.deductLeaveBalance(lr, ownerOfRequest);
        } else {
            leaveRequestService.approvePendingHr(lr, authenticatedUser.getId());
        }
    }

}

