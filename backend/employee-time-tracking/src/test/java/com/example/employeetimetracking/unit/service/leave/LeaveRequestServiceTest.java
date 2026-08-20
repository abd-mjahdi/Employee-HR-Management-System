package com.example.employeetimetracking.unit.service.leave;

import com.example.employeetimetracking.dto.request.CreateLeaveRequestDto;
import com.example.employeetimetracking.dto.response.LeaveRequestDto;
import com.example.employeetimetracking.dto.response.LeaveRequestReviewDto;
import com.example.employeetimetracking.exception.LeaveRequestNotFoundException;
import com.example.employeetimetracking.mapper.LeaveRequestMapper;
import com.example.employeetimetracking.model.entities.Company;
import com.example.employeetimetracking.model.entities.CompanyMembership;
import com.example.employeetimetracking.model.entities.LeaveBalance;
import com.example.employeetimetracking.model.entities.LeavePolicy;
import com.example.employeetimetracking.model.entities.LeaveRequest;
import com.example.employeetimetracking.model.entities.LeaveType;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.CompanyStatus;
import com.example.employeetimetracking.model.enums.MembershipStatus;
import com.example.employeetimetracking.model.enums.Status;
import com.example.employeetimetracking.model.enums.UserRole;
import com.example.employeetimetracking.repository.CompanyRepository;
import com.example.employeetimetracking.repository.LeaveRequestRepository;
import com.example.employeetimetracking.security.CustomUserDetails;
import com.example.employeetimetracking.service.LeaveBalanceService;
import com.example.employeetimetracking.service.LeavePolicyService;
import com.example.employeetimetracking.service.LeaveRequestService;
import com.example.employeetimetracking.service.LeaveTypeService;
import com.example.employeetimetracking.service.UserService;
import com.example.employeetimetracking.tenant.MembershipAccess;
import com.example.employeetimetracking.tenant.TenantContext;
import com.example.employeetimetracking.util.WorkingDaysCalculator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveRequestServiceTest {

    @Mock LeaveRequestRepository leaveRequestRepository;
    @Mock LeaveTypeService leaveTypeService;
    @Mock LeavePolicyService leavePolicyService;
    @Mock LeaveBalanceService leaveBalanceService;
    @Mock UserService userService;
    @Mock WorkingDaysCalculator workingDaysCalculator;
    @Mock MembershipAccess membershipAccess;
    @Mock CompanyRepository companyRepository;

    LeaveRequestService leaveRequestService;

    Company company;
    User employee;
    User manager;
    User hrAdmin;
    LeaveType vacation;
    LeaveRequest pending;

    @BeforeEach
    void setUp() {
        TenantContext.set(new TenantContext.TenantInfo(1L, "acme", CompanyStatus.ACTIVE));
        leaveRequestService = new LeaveRequestService(
                leaveRequestRepository,
                leaveTypeService,
                new LeaveRequestMapper(),
                leavePolicyService,
                leaveBalanceService,
                userService,
                workingDaysCalculator,
                membershipAccess,
                companyRepository);

        company = new Company();
        company.setId(1L);
        company.setSlug("acme");
        company.setStatus(CompanyStatus.ACTIVE);

        manager = user(10L, "Bob", "Manager", "manager@acme.com");
        employee = user(5L, "Alice", "Morgan", "alice@acme.com");
        hrAdmin = user(2L, "Pat", "Admin", "pat@acme.com");

        vacation = new LeaveType();
        vacation.setId(10L);
        vacation.setCompany(company);
        vacation.setTypeName("Vacation");
        vacation.setIsActive(true);

        pending = leaveRequest(100L, employee, Status.PENDING);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void getById_missingInCurrentCompany_notFound() {
        when(leaveRequestRepository.findByIdAndCompanyId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(LeaveRequestNotFoundException.class, () -> leaveRequestService.getById(99L));
        verify(leaveRequestRepository, never()).findById(99L);
    }

    @Test
    void listMine_usesCurrentCompanyOnly() {
        when(leaveRequestRepository.findByCompanyIdAndUserIdOrderByCreatedAtDesc(1L, 5L))
                .thenReturn(List.of(pending));

        List<LeaveRequestDto> result = leaveRequestService.getByUserIdOrderByCreatedAtDesc(5L);

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getId());
        verify(leaveRequestRepository).findByCompanyIdAndUserIdOrderByCreatedAtDesc(1L, 5L);
        verify(leaveRequestRepository, never()).findByUserIdOrderByCreatedAtDesc(5L);
    }

    @Test
    void searchAll_scopesToCurrentCompany() {
        when(leaveRequestRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(pending)));

        leaveRequestService.searchAll(null, null, null, null, PageRequest.of(0, 10));

        verify(leaveRequestRepository).findAll(any(Specification.class), any(Pageable.class));
        verify(leaveRequestRepository, never()).findAll();
    }

    @Test
    void hrPendingQueue_usesCurrentCompanyOnly() {
        LeaveRequest hrLeave = leaveRequest(200L, hrAdmin, Status.PENDING);
        when(leaveRequestRepository.findByUserManagerIdAndStatusForCompany(2L, Status.PENDING, 1L))
                .thenReturn(List.of(pending));
        when(leaveRequestRepository.findByUserUserRoleAndStatusAndUserIdNotForCompany(
                UserRole.HR_ADMIN, Status.PENDING, 2L, 1L))
                .thenReturn(List.of(hrLeave));

        List<LeaveRequestReviewDto> result = leaveRequestService.getPendingForReviewer(2L, true);

        assertEquals(2, result.size());
        verify(leaveRequestRepository).findByUserManagerIdAndStatusForCompany(2L, Status.PENDING, 1L);
        verify(leaveRequestRepository).findByUserUserRoleAndStatusAndUserIdNotForCompany(
                UserRole.HR_ADMIN, Status.PENDING, 2L, 1L);
        verify(leaveRequestRepository, never()).findByUserUserRoleAndStatusAndUserIdNot(
                UserRole.HR_ADMIN, Status.PENDING, 2L);
        verify(leaveRequestRepository, never()).findByUserManagerIdAndStatus(2L, Status.PENDING);
    }

    @Test
    void hrPendingCount_usesCurrentCompanyOnly() {
        when(leaveRequestRepository.countByManagerIdAndStatusForCompany(2L, Status.PENDING, 1L)).thenReturn(1);
        when(leaveRequestRepository.countByUserUserRoleAndStatusAndUserIdNotForCompany(
                UserRole.HR_ADMIN, Status.PENDING, 2L, 1L)).thenReturn(3);

        Integer count = leaveRequestService.getPendingLeaveApprovalsCount(2L, true);

        assertEquals(4, count);
        verify(leaveRequestRepository, never()).countByUserUserRoleAndStatusAndUserIdNot(
                UserRole.HR_ADMIN, Status.PENDING, 2L);
        verify(leaveRequestRepository, never()).countByManagerIdAndStatus(2L, Status.PENDING);
    }

    @Test
    void create_bindsCompanyFromContext() {
        LocalDate start = LocalDate.now().plusDays(7);
        LocalDate end = start.plusDays(1);
        CompanyMembership managerMembership = membership(10L, manager, UserRole.MANAGER, null);
        CompanyMembership employeeMembership = membership(5L, employee, UserRole.EMPLOYEE, managerMembership);

        LeavePolicy policy = new LeavePolicy();
        policy.setMinNoticeDays(0);
        policy.setAllowsNegativeBalance(true);
        policy.setAnnualAllocation(new BigDecimal("20"));

        LeaveBalance balance = new LeaveBalance();
        balance.setCurrentBalance(new BigDecimal("20"));

        when(userService.getById(5L)).thenReturn(employee);
        when(leaveTypeService.getById(10L)).thenReturn(vacation);
        when(leavePolicyService.getPolicyByLeaveTypeId(10L)).thenReturn(policy);
        when(leaveBalanceService.getByUserIdAndLeaveTypeIdAndYear(5L, 10L, start.getYear())).thenReturn(balance);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(workingDaysCalculator.calculate(start, end)).thenReturn(BigDecimal.ONE);
        when(membershipAccess.findInCurrentCompany(5L)).thenReturn(Optional.of(employeeMembership));
        when(leaveRequestRepository.findOverlappingRequestsForCompany(
                eq(5L), eq(1L), anyList(), eq(start), eq(end))).thenReturn(List.of());
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> {
            LeaveRequest saved = inv.getArgument(0);
            saved.setId(300L);
            return saved;
        });

        LeaveRequestDto dto = leaveRequestService.create(
                new CreateLeaveRequestDto(10L, start, end, "Family trip"), 5L);

        assertEquals(300L, dto.getId());
        ArgumentCaptor<LeaveRequest> captor = ArgumentCaptor.forClass(LeaveRequest.class);
        verify(leaveRequestRepository).save(captor.capture());
        assertEquals(1L, captor.getValue().getCompany().getId());
        verify(leaveRequestRepository, never()).findOverlappingRequests(eq(5L), anyList(), eq(start), eq(end));
    }

    @Test
    void getIfAllowed_otherCompanyId_notFound() {
        when(leaveRequestRepository.findByIdAndCompanyId(99L, 1L)).thenReturn(Optional.empty());
        CustomUserDetails hr = new CustomUserDetails(membership(2L, hrAdmin, UserRole.HR_ADMIN, null));

        assertThrows(LeaveRequestNotFoundException.class, () -> leaveRequestService.getIfAllowed(99L, hr));
        verify(leaveRequestRepository, never()).findById(99L);
    }

    private LeaveRequest leaveRequest(Long id, User owner, Status status) {
        LeaveRequest request = new LeaveRequest();
        request.setId(id);
        request.setCompany(company);
        request.setUser(owner);
        request.setLeaveType(vacation);
        request.setStartDate(LocalDate.now().plusDays(3));
        request.setEndDate(LocalDate.now().plusDays(4));
        request.setTotalDays(BigDecimal.ONE);
        request.setReason("Time off");
        request.setStatus(status);
        return request;
    }

    private static User user(Long id, String first, String last, String email) {
        User user = new User();
        user.setId(id);
        user.setFirstName(first);
        user.setLastName(last);
        user.setEmail(email);
        user.setPasswordHash("hash");
        user.setIsActive(true);
        return user;
    }

    private CompanyMembership membership(Long id, User user, UserRole role, CompanyMembership manager) {
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
