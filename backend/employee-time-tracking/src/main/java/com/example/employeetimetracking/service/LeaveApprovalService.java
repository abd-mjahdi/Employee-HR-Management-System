package com.example.employeetimetracking.service;

import com.example.employeetimetracking.exception.LeaveApprovalException;
import com.example.employeetimetracking.mapper.LeaveRequestMapper;
import com.example.employeetimetracking.model.entities.LeavePolicy;
import com.example.employeetimetracking.model.entities.LeaveRequest;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.Status;
import com.example.employeetimetracking.repository.LeaveRequestRepository;
import com.example.employeetimetracking.security.CustomUserDetails;
import com.example.employeetimetracking.util.WorkingDaysCalculator;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class LeaveApprovalService {
    private final LeaveTypeService leaveTypeService;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveRequestService leaveRequestService;
    private final LeaveRequestMapper leaveRequestMapper;
    private final LeavePolicyService leavePolicyService;
    private final LeaveBalanceService leaveBalanceService;
    private final UserService userService;
    private final WorkingDaysCalculator workingDaysCalculator;

    @Autowired
    public LeaveApprovalService(LeaveRequestRepository leaveRequestRepository,
                                LeaveTypeService leaveTypeService,
                                LeaveRequestMapper leaveRequestMapper,
                                LeavePolicyService leavePolicyService,
                                LeaveBalanceService leaveBalanceService,
                                UserService userService,
                                LeaveRequestService leaveRequestService,
                                WorkingDaysCalculator workingDaysCalculator) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveTypeService = leaveTypeService;
        this.leaveRequestMapper = leaveRequestMapper;
        this.leavePolicyService = leavePolicyService;
        this.leaveBalanceService = leaveBalanceService;
        this.userService = userService;
        this.leaveRequestService = leaveRequestService;
        this.workingDaysCalculator = workingDaysCalculator;
    }

    @Transactional
    public void approve(Long lrId, CustomUserDetails authenticatedUser) {
        LeaveRequest lr = leaveRequestService.getById(lrId);
        // No lazy loading risk
        User ownerOfRequest = lr.getUser();
        boolean isDirectReport = ownerOfRequest.getManager().getId().equals(authenticatedUser.getId());
        LeavePolicy policy = lr.getLeaveType().getLeavePolicy();


        if (!isDirectReport) throw new AccessDeniedException("You can't approve this user");

        if (lr.getStatus() == Status.APPROVED ||
                lr.getManagerApprovalStatus() == Status.APPROVED ||
                lr.getStatus() == Status.DENIED ||
                lr.getStatus() == Status.CANCELLED) {
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

