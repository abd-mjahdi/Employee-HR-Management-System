package com.example.employeetimetracking.unit.service.leave;

import com.example.employeetimetracking.exception.LeaveApprovalException;
import com.example.employeetimetracking.model.entities.Company;
import com.example.employeetimetracking.model.entities.CompanyMembership;
import com.example.employeetimetracking.model.entities.LeaveRequest;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.CompanyStatus;
import com.example.employeetimetracking.model.enums.MembershipStatus;
import com.example.employeetimetracking.model.enums.Status;
import com.example.employeetimetracking.model.enums.UserRole;
import com.example.employeetimetracking.security.CustomUserDetails;
import com.example.employeetimetracking.service.LeaveApprovalService;
import com.example.employeetimetracking.service.LeaveBalanceService;
import com.example.employeetimetracking.service.LeaveRequestService;
import com.example.employeetimetracking.service.NotificationService;
import com.example.employeetimetracking.tenant.MembershipAccess;
import com.example.employeetimetracking.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

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

    @Mock
    private MembershipAccess membershipAccess;

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
    private CompanyMembership ownerMembership;
    private CompanyMembership hrOwnerMembership;
    private Company company;

    @BeforeEach
    void setUp() {
        TenantContext.set(new TenantContext.TenantInfo(1L, "acme", CompanyStatus.ACTIVE));
        company = new Company();
        company.setId(1L);

        manager = user(10L, "manager@test.com");
        owner = user(1L, "owner@test.com");
        hrOwner = user(2L, "hr1@test.com");
        peerHr = user(3L, "hr2@test.com");

        CompanyMembership managerMembership = membership(10L, manager, company, UserRole.MANAGER, null);
        ownerMembership = membership(1L, owner, company, UserRole.EMPLOYEE, managerMembership);
        hrOwnerMembership = membership(2L, hrOwner, company, UserRole.HR_ADMIN, null);
        CompanyMembership peerHrMembership = membership(3L, peerHr, company, UserRole.HR_ADMIN, null);

        ownerDetails = new CustomUserDetails(ownerMembership);
        managerDetails = new CustomUserDetails(managerMembership);
        hrOwnerDetails = new CustomUserDetails(hrOwnerMembership);
        peerHrDetails = new CustomUserDetails(peerHrMembership);

        leaveRequest = new LeaveRequest();
        leaveRequest.setId(100L);
        leaveRequest.setCompany(company);
        leaveRequest.setUser(owner);
        leaveRequest.setStatus(Status.APPROVED);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
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
        when(membershipAccess.findFor(managerDetails, owner.getId())).thenReturn(Optional.of(ownerMembership));

        leaveApprovalService.approveCancellation(100L, managerDetails, "Cancellation approved");

        verify(leaveRequestService).cancel(leaveRequest);
        verify(leaveBalanceService).restoreLeaveBalance(leaveRequest, owner);
        verify(notificationService).notifyLeaveCancelled(leaveRequest);
    }

    @Test
    void testApproveCancellationFailsIfNotCancellationPending() {
        leaveRequest.setStatus(Status.APPROVED);
        when(leaveRequestService.getById(100L)).thenReturn(leaveRequest);
        when(membershipAccess.findFor(managerDetails, owner.getId())).thenReturn(Optional.of(ownerMembership));

        assertThrows(LeaveApprovalException.class, () ->
                leaveApprovalService.approveCancellation(100L, managerDetails, "Notes"));
    }

    @Test
    void testDenyCancellationSuccessResetsStatusToApproved() {
        leaveRequest.setStatus(Status.CANCELLATION_PENDING);
        when(leaveRequestService.getById(100L)).thenReturn(leaveRequest);
        when(membershipAccess.findFor(managerDetails, owner.getId())).thenReturn(Optional.of(ownerMembership));

        leaveApprovalService.denyCancellation(100L, managerDetails, "Cancellation denied");

        assertEquals(Status.APPROVED, leaveRequest.getStatus());
        assertEquals("Cancellation denied", leaveRequest.getManagerNotes());
        verify(notificationService).notifyCancellationDenied(leaveRequest);
    }

    @Test
    void testDenyCancellationFailsIfNotCancellationPending() {
        leaveRequest.setStatus(Status.APPROVED);
        when(leaveRequestService.getById(100L)).thenReturn(leaveRequest);
        when(membershipAccess.findFor(managerDetails, owner.getId())).thenReturn(Optional.of(ownerMembership));

        assertThrows(LeaveApprovalException.class, () ->
                leaveApprovalService.denyCancellation(100L, managerDetails, "Reason"));
    }

    @Test
    void testPeerHrCanApproveAnotherHrLeave() {
        leaveRequest.setUser(hrOwner);
        leaveRequest.setStatus(Status.PENDING);
        leaveRequest.setManagerApprovalStatus(Status.PENDING);
        when(leaveRequestService.getById(100L)).thenReturn(leaveRequest);
        when(membershipAccess.findFor(peerHrDetails, hrOwner.getId())).thenReturn(Optional.of(hrOwnerMembership));

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
        when(membershipAccess.findFor(managerDetails, hrOwner.getId())).thenReturn(Optional.of(hrOwnerMembership));

        assertThrows(org.springframework.security.access.AccessDeniedException.class, () ->
                leaveApprovalService.approve(100L, managerDetails, "ok"));
    }

    @Test
    void testHrFromOtherCompanyCannotApprove() {
        leaveRequest.setStatus(Status.PENDING);
        leaveRequest.setManagerApprovalStatus(Status.PENDING);
        when(leaveRequestService.getById(100L)).thenReturn(leaveRequest);
        when(membershipAccess.findFor(peerHrDetails, owner.getId())).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () ->
                leaveApprovalService.approve(100L, peerHrDetails, "ok"));
        verify(leaveRequestService, never()).approve(any(), any(), any());
        verify(leaveBalanceService, never()).deductLeaveBalance(any(), any());
    }

    private static User user(Long id, String email) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setPasswordHash("password");
        u.setIsActive(true);
        return u;
    }

    private static CompanyMembership membership(Long id, User user, Company company, UserRole role, CompanyMembership manager) {
        CompanyMembership membership = new CompanyMembership();
        membership.setId(id);
        membership.setUser(user);
        membership.setCompany(company);
        membership.setRole(role);
        membership.setStatus(MembershipStatus.ACTIVE);
        membership.setManagerMembership(manager);
        return membership;
    }
}
