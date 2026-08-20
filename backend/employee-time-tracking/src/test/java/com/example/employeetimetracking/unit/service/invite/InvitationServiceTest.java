package com.example.employeetimetracking.unit.service.invite;

import com.example.employeetimetracking.dto.request.AcceptInvitationRequestDto;
import com.example.employeetimetracking.dto.request.CreateInvitationRequestDto;
import com.example.employeetimetracking.dto.response.InvitationAcceptedResponseDto;
import com.example.employeetimetracking.dto.response.InvitationCreatedResponseDto;
import com.example.employeetimetracking.exception.InvitationAlreadyUsedException;
import com.example.employeetimetracking.exception.InvitationExpiredException;
import com.example.employeetimetracking.exception.InvitationNotFoundException;
import com.example.employeetimetracking.model.entities.Company;
import com.example.employeetimetracking.model.entities.CompanyMembership;
import com.example.employeetimetracking.model.entities.Department;
import com.example.employeetimetracking.model.entities.Invitation;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.CompanyStatus;
import com.example.employeetimetracking.model.enums.InvitationStatus;
import com.example.employeetimetracking.model.enums.MembershipStatus;
import com.example.employeetimetracking.model.enums.UserRole;
import com.example.employeetimetracking.repository.CompanyMembershipRepository;
import com.example.employeetimetracking.repository.CompanyRepository;
import com.example.employeetimetracking.repository.DepartmentRepository;
import com.example.employeetimetracking.repository.InvitationRepository;
import com.example.employeetimetracking.repository.UserRepository;
import com.example.employeetimetracking.security.CustomUserDetails;
import com.example.employeetimetracking.service.InvitationService;
import com.example.employeetimetracking.service.LeaveBalanceService;
import com.example.employeetimetracking.service.UserService;
import com.example.employeetimetracking.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

    @Mock InvitationRepository invitationRepository;
    @Mock CompanyRepository companyRepository;
    @Mock DepartmentRepository departmentRepository;
    @Mock CompanyMembershipRepository companyMembershipRepository;
    @Mock UserRepository userRepository;
    @Mock UserService userService;
    @Mock LeaveBalanceService leaveBalanceService;
    @Mock BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    InvitationService invitationService;

    Company company;
    Department department;
    User hrUser;
    CompanyMembership hrMembership;
    CompanyMembership managerMembership;
    CustomUserDetails actor;

    @BeforeEach
    void setUp() {
        TenantContext.set(new TenantContext.TenantInfo(1L, "acme", CompanyStatus.ACTIVE));
        company = new Company();
        company.setId(1L);
        company.setSlug("acme");
        company.setStatus(CompanyStatus.ACTIVE);

        department = new Department();
        department.setId(10L);
        department.setCompany(company);

        hrUser = new User();
        hrUser.setId(1L);
        hrUser.setEmail("hr@acme.com");
        hrUser.setUserRole(UserRole.HR_ADMIN);
        hrUser.setIsActive(true);

        hrMembership = new CompanyMembership();
        hrMembership.setId(1L);
        hrMembership.setUser(hrUser);
        hrMembership.setCompany(company);
        hrMembership.setRole(UserRole.HR_ADMIN);
        hrMembership.setStatus(MembershipStatus.ACTIVE);

        User manager = new User();
        manager.setId(2L);
        manager.setIsActive(true);
        managerMembership = new CompanyMembership();
        managerMembership.setId(2L);
        managerMembership.setUser(manager);
        managerMembership.setCompany(company);
        managerMembership.setRole(UserRole.MANAGER);
        managerMembership.setStatus(MembershipStatus.ACTIVE);

        actor = new CustomUserDetails(hrMembership);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void create_storesHashNotRawToken() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(companyMembershipRepository.findByIdAndCompanyId(1L, 1L)).thenReturn(Optional.of(hrMembership));
        when(departmentRepository.findByIdAndCompanyId(10L, 1L)).thenReturn(Optional.of(department));
        when(userService.requireManagerMembership(2L, UserRole.EMPLOYEE, 1L)).thenReturn(managerMembership);
        when(userRepository.findByEmail("new@acme.com")).thenReturn(Optional.empty());
        when(invitationRepository.findByCompanyIdAndEmailAndStatus(1L, "new@acme.com", InvitationStatus.PENDING))
                .thenReturn(Collections.emptyList());
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(inv -> {
            Invitation invitation = inv.getArgument(0);
            invitation.setId(99L);
            return invitation;
        });

        InvitationCreatedResponseDto response = invitationService.create(
                new CreateInvitationRequestDto("New@acme.com", UserRole.EMPLOYEE, 10L, 2L), actor);

        assertNotNull(response.getToken());
        ArgumentCaptor<Invitation> captor = ArgumentCaptor.forClass(Invitation.class);
        verify(invitationRepository).save(captor.capture());
        Invitation saved = captor.getValue();
        assertEquals(InvitationService.hashToken(response.getToken()), saved.getTokenHash());
        assertEquals("new@acme.com", saved.getEmail());
        assertEquals(InvitationStatus.PENDING, saved.getStatus());
        assertEquals(1L, saved.getCompany().getId());
    }

    @Test
    void accept_wrongTenant_looksLikeNotFound() {
        Invitation invitation = pendingInvitation();
        invitation.getCompany().setId(2L);
        when(invitationRepository.findWithDetailsByTokenHash(anyString())).thenReturn(Optional.of(invitation));

        AcceptInvitationRequestDto request = new AcceptInvitationRequestDto(
                "raw-token", "secret1", "Pat", "Lee");

        assertThrows(InvitationNotFoundException.class, () -> invitationService.accept(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void accept_expired_marksExpired() {
        Invitation invitation = pendingInvitation();
        invitation.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).minusHours(1));
        when(invitationRepository.findWithDetailsByTokenHash(anyString())).thenReturn(Optional.of(invitation));

        AcceptInvitationRequestDto request = new AcceptInvitationRequestDto(
                "raw-token", "secret1", "Pat", "Lee");

        assertThrows(InvitationExpiredException.class, () -> invitationService.accept(request));
        assertEquals(InvitationStatus.EXPIRED, invitation.getStatus());
    }

    @Test
    void accept_newUser_usesInvitationRoleNotClient() {
        Invitation invitation = pendingInvitation();
        when(invitationRepository.findWithDetailsByTokenHash(anyString())).thenReturn(Optional.of(invitation));
        when(userRepository.findByEmail("new@acme.com")).thenReturn(Optional.empty());
        when(userRepository.existsByUsername("new")).thenReturn(false);
        when(passwordEncoder.encode("secret1")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User user = inv.getArgument(0);
            user.setId(50L);
            return user;
        });
        when(companyMembershipRepository.save(any(CompanyMembership.class))).thenAnswer(inv -> inv.getArgument(0));

        InvitationAcceptedResponseDto response = invitationService.accept(
                new AcceptInvitationRequestDto("raw-token", "secret1", "Pat", "Lee"));

        assertEquals("new@acme.com", response.getEmail());
        assertEquals("acme", response.getCompanySlug());
        ArgumentCaptor<CompanyMembership> captor = ArgumentCaptor.forClass(CompanyMembership.class);
        verify(companyMembershipRepository).save(captor.capture());
        assertEquals(UserRole.EMPLOYEE, captor.getValue().getRole());
        assertEquals(MembershipStatus.ACTIVE, captor.getValue().getStatus());
        assertEquals(InvitationStatus.ACCEPTED, invitation.getStatus());
        verify(leaveBalanceService).initializeLeaveBalances(any(User.class));
    }

    @Test
    void revoke_pending_inTenant() {
        Invitation invitation = pendingInvitation();
        when(invitationRepository.findByIdAndCompanyId(99L, 1L)).thenReturn(Optional.of(invitation));

        invitationService.revoke(99L);

        assertEquals(InvitationStatus.REVOKED, invitation.getStatus());
    }

    @Test
    void revoke_alreadyAccepted_rejected() {
        Invitation invitation = pendingInvitation();
        invitation.setStatus(InvitationStatus.ACCEPTED);
        when(invitationRepository.findByIdAndCompanyId(99L, 1L)).thenReturn(Optional.of(invitation));

        assertThrows(InvitationAlreadyUsedException.class, () -> invitationService.revoke(99L));
    }

    private Invitation pendingInvitation() {
        Invitation invitation = new Invitation();
        invitation.setId(99L);
        invitation.setCompany(company);
        invitation.setEmail("new@acme.com");
        invitation.setRole(UserRole.EMPLOYEE);
        invitation.setDepartment(department);
        invitation.setManagerMembership(managerMembership);
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusHours(72));
        invitation.setInvitedByMembership(hrMembership);
        return invitation;
    }
}
