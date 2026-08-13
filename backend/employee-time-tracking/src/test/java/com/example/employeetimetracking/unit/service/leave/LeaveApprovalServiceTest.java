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
    private CustomUserDetails ownerDetails;
    private CustomUserDetails managerDetails;
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

        leaveRequest = new LeaveRequest();
        leaveRequest.setId(100L);
        leaveRequest.setUser(owner);
        leaveRequest.setStatus(Status.APPROVED);
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
}
