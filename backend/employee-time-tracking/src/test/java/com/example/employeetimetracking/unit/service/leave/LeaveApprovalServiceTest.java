package com.example.employeetimetracking.unit.service.leave;

import com.example.employeetimetracking.exception.LeaveApprovalException;
import com.example.employeetimetracking.model.entities.LeaveRequest;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.Status;
import com.example.employeetimetracking.model.enums.UserRole;
import com.example.employeetimetracking.security.CustomUserDetails;
import com.example.employeetimetracking.service.LeaveApprovalService;
import com.example.employeetimetracking.service.LeaveBalanceService;
import com.example.employeetimetracking.service.LeaveRequestService;
import com.example.employeetimetracking.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LeaveApprovalServiceTest {

    @Mock
    private LeaveRequestService leaveRequestService;

    @Mock
    private LeaveBalanceService leaveBalanceService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private LeaveApprovalService leaveApprovalService;

    private User owner;
    private User manager;
    private User hrOwner;
    private User peerHr;
    private CustomUserDetails ownerDetails;
    private CustomUserDetails managerDetails;
    private CustomUserDetails hrOwnerDetails;
    private CustomUserDetails peerHrDetails;
    private LeaveRequest leaveRequest;

    @BeforeEach
    void setUp() {
        manager = new User();
        manager.setId(10L);
        manager.setEmail("manager@test.com");
        manager.setPasswordHash("password");
        manager.setUserRole(UserRole.MANAGER);

        owner = new User();
        owner.setId(1L);
        owner.setEmail("owner@test.com");
        owner.setPasswordHash("password");
        owner.setUserRole(UserRole.EMPLOYEE);
        owner.setManager(manager);

        ownerDetails = new CustomUserDetails(owner);
        managerDetails = new CustomUserDetails(manager);

        hrOwner = new User();
        hrOwner.setId(2L);
        hrOwner.setEmail("hr1@test.com");
        hrOwner.setPasswordHash("password");
        hrOwner.setUserRole(UserRole.HR_ADMIN);
        hrOwner.setManager(null);

        peerHr = new User();
        peerHr.setId(3L);
        peerHr.setEmail("hr2@test.com");
        peerHr.setPasswordHash("password");
        peerHr.setUserRole(UserRole.HR_ADMIN);

        hrOwnerDetails = new CustomUserDetails(hrOwner);
        peerHrDetails = new CustomUserDetails(peerHr);

        leaveRequest = new LeaveRequest();
        leaveRequest.setId(100L);
        leaveRequest.setUser(owner);
        leaveRequest.setStatus(Status.APPROVED);
    }

    @Test
    void testCancelPendingLeaveCancelsImmediately() {
        leaveRequest.setStatus(Status.PENDING);
        when(leaveRequestService.getById(100L)).thenReturn(leaveRequest);

        leaveApprovalService.cancel(100L, ownerDetails, "Changed plans");

        verify(leaveRequestService).cancel(leaveRequest);
        verify(leaveBalanceService, never()).restoreLeaveBalance(any(), any());
        verify(notificationService).notifyLeaveCancelled(leaveRequest);
        assertEquals("Changed plans", leaveRequest.getCancellationReason());
    }

    @Test
    void testCancelApprovedLeaveSetsCancellationPending() {
        when(leaveRequestService.getById(100L)).thenReturn(leaveRequest);

        leaveApprovalService.cancel(100L, ownerDetails, "Need to cancel");

        assertEquals(Status.CANCELLATION_PENDING, leaveRequest.getStatus());
        assertEquals("Need to cancel", leaveRequest.getCancellationReason());
        verify(leaveRequestService, never()).cancel(leaveRequest);
        verify(leaveBalanceService, never()).restoreLeaveBalance(leaveRequest, owner);
        verify(notificationService).notifyCancellationRequested(leaveRequest);
    }

    @Test
    void testApproveCancellationSuccess() {
        leaveRequest.setStatus(Status.CANCELLATION_PENDING);
        when(leaveRequestService.getById(100L)).thenReturn(leaveRequest);

        leaveApprovalService.approveCancellation(100L, managerDetails, "Cancellation approved");

        verify(leaveRequestService).cancel(leaveRequest);
        verify(leaveBalanceService).restoreLeaveBalance(leaveRequest, owner);
        verify(notificationService).notifyLeaveCancelled(leaveRequest);
    }

    @Test
    void testApproveCancellationFailsIfNotCancellationPending() {
        leaveRequest.setStatus(Status.APPROVED);
        when(leaveRequestService.getById(100L)).thenReturn(leaveRequest);

        assertThrows(LeaveApprovalException.class, () ->
                leaveApprovalService.approveCancellation(100L, managerDetails, "Notes"));
    }

    @Test
    void testDenyCancellationSuccessResetsStatusToApproved() {
        leaveRequest.setStatus(Status.CANCELLATION_PENDING);
        when(leaveRequestService.getById(100L)).thenReturn(leaveRequest);

        leaveApprovalService.denyCancellation(100L, managerDetails, "Cancellation denied");

        assertEquals(Status.APPROVED, leaveRequest.getStatus());
        assertEquals("Cancellation denied", leaveRequest.getManagerNotes());
        verify(notificationService).notifyCancellationDenied(leaveRequest);
    }

    @Test
    void testDenyCancellationFailsIfNotCancellationPending() {
        leaveRequest.setStatus(Status.APPROVED);
        when(leaveRequestService.getById(100L)).thenReturn(leaveRequest);

        assertThrows(LeaveApprovalException.class, () ->
                leaveApprovalService.denyCancellation(100L, managerDetails, "Reason"));
    }

    @Test
    void testPeerHrCanApproveAnotherHrLeave() {
        leaveRequest.setUser(hrOwner);
        leaveRequest.setStatus(Status.PENDING);
        leaveRequest.setManagerApprovalStatus(Status.PENDING);
        when(leaveRequestService.getById(100L)).thenReturn(leaveRequest);

        leaveApprovalService.approve(100L, peerHrDetails, "ok");

        verify(leaveRequestService).approve(leaveRequest, peerHr.getId(), "ok");
        verify(leaveBalanceService).deductLeaveBalance(leaveRequest, hrOwner);
    }

    @Test
    void testHrCannotApproveOwnLeave() {
        leaveRequest.setUser(hrOwner);
        leaveRequest.setStatus(Status.PENDING);
        leaveRequest.setManagerApprovalStatus(Status.PENDING);
        when(leaveRequestService.getById(100L)).thenReturn(leaveRequest);

        assertThrows(org.springframework.security.access.AccessDeniedException.class, () ->
                leaveApprovalService.approve(100L, hrOwnerDetails, "ok"));
    }

    @Test
    void testManagerCannotApproveHrLeave() {
        leaveRequest.setUser(hrOwner);
        leaveRequest.setStatus(Status.PENDING);
        leaveRequest.setManagerApprovalStatus(Status.PENDING);
        when(leaveRequestService.getById(100L)).thenReturn(leaveRequest);

        assertThrows(org.springframework.security.access.AccessDeniedException.class, () ->
                leaveApprovalService.approve(100L, managerDetails, "ok"));
    }
}
