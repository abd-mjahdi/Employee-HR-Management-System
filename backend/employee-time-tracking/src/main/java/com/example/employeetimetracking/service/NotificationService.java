package com.example.employeetimetracking.service;

import com.example.employeetimetracking.model.entities.LeaveRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public void notifyLeaveDenied(LeaveRequest leaveRequest) {
        if (leaveRequest == null || leaveRequest.getUser() == null) return;
        log.info("Leave request {} denied for user {}", leaveRequest.getId(), leaveRequest.getUser().getId());
    }

    public void notifyLeaveApproved(LeaveRequest leaveRequest) {
        if (leaveRequest == null || leaveRequest.getUser() == null) return;
        log.info("Leave request {} approved for user {}", leaveRequest.getId(), leaveRequest.getUser().getId());
    }

    public void notifyLeaveCancelled(LeaveRequest leaveRequest) {
        if (leaveRequest == null || leaveRequest.getUser() == null) return;
        log.info("Leave request {} cancelled for user {}", leaveRequest.getId(), leaveRequest.getUser().getId());
    }

    public void notifyCancellationRequested(LeaveRequest leaveRequest) {
        if (leaveRequest == null || leaveRequest.getUser() == null) return;
        log.info("Cancellation requested for leave request {} by user {}", leaveRequest.getId(), leaveRequest.getUser().getId());
    }

    public void notifyCancellationDenied(LeaveRequest leaveRequest) {
        if (leaveRequest == null || leaveRequest.getUser() == null) return;
        log.info("Cancellation denied for leave request {} of user {}", leaveRequest.getId(), leaveRequest.getUser().getId());
    }
}

