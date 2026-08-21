package com.example.employeetimetracking.unit.service.leave;

import com.example.employeetimetracking.dto.response.LeaveBalanceDto;
import com.example.employeetimetracking.exception.LeaveBalanceNotFoundException;
import com.example.employeetimetracking.mapper.LeaveBalanceMapper;
import com.example.employeetimetracking.model.entities.Company;
import com.example.employeetimetracking.model.entities.CompanyMembership;
import com.example.employeetimetracking.model.entities.LeaveBalance;
import com.example.employeetimetracking.model.entities.LeavePolicy;
import com.example.employeetimetracking.model.entities.LeaveType;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.AccrualMethod;
import com.example.employeetimetracking.model.enums.CompanyStatus;
import com.example.employeetimetracking.model.enums.MembershipStatus;
import com.example.employeetimetracking.model.enums.UserRole;
import com.example.employeetimetracking.repository.LeaveBalanceRepository;
import com.example.employeetimetracking.repository.UserRepository;
import com.example.employeetimetracking.security.CustomUserDetails;
import com.example.employeetimetracking.service.LeaveBalanceService;
import com.example.employeetimetracking.service.LeaveTypeService;
import com.example.employeetimetracking.tenant.MembershipAccess;
import com.example.employeetimetracking.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveBalanceServiceTest {

    @Mock
    LeaveBalanceRepository leaveBalanceRepository;
    @Mock
    LeaveTypeService leaveTypeService;
    @Mock
    UserRepository userRepository;
    @Mock
    MembershipAccess membershipAccess;

    LeaveBalanceService leaveBalanceService;

    Company company;
    User user;

    @BeforeEach
    void setUp() {
        TenantContext.set(new TenantContext.TenantInfo(1L, "acme", CompanyStatus.ACTIVE));
        leaveBalanceService = new LeaveBalanceService(
                leaveBalanceRepository,
                leaveTypeService,
                new LeaveBalanceMapper(),
                userRepository,
                membershipAccess);

        company = new Company();
        company.setId(1L);
        company.setSlug("acme");
        company.setStatus(CompanyStatus.ACTIVE);

        user = new User();
        user.setId(5L);
        user.setEmail("alice@acme.com");
        user.setPasswordHash("hash");
        user.setIsActive(true);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void initializeLeaveBalances_createsOnlyCurrentCompanyTypes() {
        LeaveType vacation = leaveType(10L, company, AccrualMethod.ANNUAL, new BigDecimal("20"));
        when(leaveTypeService.getAllWithPolicy()).thenReturn(List.of(vacation));
        when(leaveBalanceRepository.findByCompanyIdAndUserIdAndLeaveTypeIdAndYear(
                1L, 5L, 10L, LocalDate.now().getYear())).thenReturn(Optional.empty());
        when(leaveBalanceRepository.save(any(LeaveBalance.class))).thenAnswer(inv -> inv.getArgument(0));

        leaveBalanceService.initializeLeaveBalances(user);

        ArgumentCaptor<LeaveBalance> captor = ArgumentCaptor.forClass(LeaveBalance.class);
        verify(leaveBalanceRepository).save(captor.capture());
        LeaveBalance saved = captor.getValue();
        assertEquals(1L, saved.getCompany().getId());
        assertEquals(5L, saved.getUser().getId());
        assertEquals(10L, saved.getLeaveType().getId());
        assertEquals(0, new BigDecimal("20").compareTo(saved.getCurrentBalance()));
    }

    @Test
    void initializeLeaveBalances_skipsTypesFromOtherCompany() {
        Company other = new Company();
        other.setId(2L);
        LeaveType otherType = leaveType(99L, other, AccrualMethod.ANNUAL, new BigDecimal("15"));
        when(leaveTypeService.getAllWithPolicy()).thenReturn(List.of(otherType));

        leaveBalanceService.initializeLeaveBalances(user);

        verify(leaveBalanceRepository, never()).save(any());
        verify(leaveBalanceRepository, never()).findByCompanyIdAndUserIdAndLeaveTypeIdAndYear(
                anyLong(), anyLong(), anyLong(), anyInt());
    }

    @Test
    void initializeLeaveBalances_skipsExistingBalance() {
        LeaveType vacation = leaveType(10L, company, AccrualMethod.ANNUAL, new BigDecimal("20"));
        when(leaveTypeService.getAllWithPolicy()).thenReturn(List.of(vacation));
        when(leaveBalanceRepository.findByCompanyIdAndUserIdAndLeaveTypeIdAndYear(
                1L, 5L, 10L, LocalDate.now().getYear()))
                .thenReturn(Optional.of(new LeaveBalance()));

        leaveBalanceService.initializeLeaveBalances(user);

        verify(leaveBalanceRepository, never()).save(any());
    }

    @Test
    void getByUserIdAndYear_usesCurrentCompanyOnly() {
        LeaveBalance balance = existingBalance(vacationType(), new BigDecimal("8"));
        when(leaveBalanceRepository.findByCompanyIdAndUserIdAndYear(1L, 5L, 2026))
                .thenReturn(List.of(balance));

        List<LeaveBalanceDto> result = leaveBalanceService.getByUserIdAndYear(5L, 2026);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getLeaveTypeId());
        verify(leaveBalanceRepository).findByCompanyIdAndUserIdAndYear(1L, 5L, 2026);
        verify(leaveBalanceRepository, never()).findByUserIdAndYear(5L, 2026);
    }

    @Test
    void getByUserIdAndLeaveTypeIdAndYear_otherCompany_notFound() {
        when(leaveBalanceRepository.findByCompanyIdAndUserIdAndLeaveTypeIdAndYear(1L, 5L, 99L, 2026))
                .thenReturn(Optional.empty());

        assertThrows(LeaveBalanceNotFoundException.class,
                () -> leaveBalanceService.getByUserIdAndLeaveTypeIdAndYear(5L, 99L, 2026));
        verify(leaveBalanceRepository, never()).findByUserIdAndLeaveTypeIdAndYear(5L, 99L, 2026);
    }

    @Test
    void getLeaveBalanceIfAllowed_missingMembership_accessDenied() {
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(membershipAccess.find(5L, 1L)).thenReturn(Optional.empty());

        CustomUserDetails hr = hrDetails();

        assertThrows(AccessDeniedException.class,
                () -> leaveBalanceService.getLeaveBalanceIfAllowed(5L, hr));
        verify(leaveBalanceRepository, never()).findByCompanyIdAndUserIdAndYear(anyLong(), anyLong(), anyInt());
    }

    @Test
    void rolloverBalance_setsCompanyWithoutTenantContext() {
        TenantContext.clear();
        LeaveBalance current = existingBalance(vacationType(), new BigDecimal("5"));
        current.setYear((short) LocalDate.now().getYear());
        when(leaveBalanceRepository.findByCompanyIdAndUserIdAndLeaveTypeIdAndYear(
                1L, 5L, 10L, LocalDate.now().getYear() + 1)).thenReturn(Optional.empty());
        when(leaveBalanceRepository.save(any(LeaveBalance.class))).thenAnswer(inv -> inv.getArgument(0));

        leaveBalanceService.rolloverBalance(current);

        ArgumentCaptor<LeaveBalance> captor = ArgumentCaptor.forClass(LeaveBalance.class);
        verify(leaveBalanceRepository).save(captor.capture());
        LeaveBalance next = captor.getValue();
        assertNotNull(next.getCompany());
        assertEquals(1L, next.getCompany().getId());
        assertEquals((short) (LocalDate.now().getYear() + 1), next.getYear());
        verify(leaveBalanceRepository, never()).findByUserIdAndLeaveTypeIdAndYear(anyLong(), anyLong(), anyInt());
    }

    @Test
    void rolloverBalance_acceptsPreviousCalendarYear() {
        LeaveBalance current = existingBalance(vacationType(), new BigDecimal("5"));
        current.setYear((short) (LocalDate.now().getYear() - 1));
        int nextYear = LocalDate.now().getYear();
        when(leaveBalanceRepository.findByCompanyIdAndUserIdAndLeaveTypeIdAndYear(
                1L, 5L, 10L, nextYear)).thenReturn(Optional.empty());
        when(leaveBalanceRepository.save(any(LeaveBalance.class))).thenAnswer(inv -> inv.getArgument(0));

        leaveBalanceService.rolloverBalance(current);

        ArgumentCaptor<LeaveBalance> captor = ArgumentCaptor.forClass(LeaveBalance.class);
        verify(leaveBalanceRepository).save(captor.capture());
        assertEquals((short) nextYear, captor.getValue().getYear());
    }

    private CustomUserDetails hrDetails() {
        CompanyMembership membership = new CompanyMembership();
        membership.setId(1L);
        membership.setUser(user);
        membership.setCompany(company);
        membership.setRole(UserRole.HR_ADMIN);
        membership.setStatus(MembershipStatus.ACTIVE);
        return new CustomUserDetails(membership);
    }

    private LeaveType vacationType() {
        return leaveType(10L, company, AccrualMethod.ANNUAL, new BigDecimal("20"));
    }

    private LeaveType leaveType(Long id, Company owner, AccrualMethod method, BigDecimal allocation) {
        LeaveType leaveType = new LeaveType();
        leaveType.setId(id);
        leaveType.setCompany(owner);
        leaveType.setTypeName("Vacation");
        leaveType.setIsActive(true);

        LeavePolicy policy = new LeavePolicy();
        policy.setCompany(owner);
        policy.setLeaveType(leaveType);
        policy.setAccrualMethod(method);
        policy.setAnnualAllocation(allocation);
        policy.setMaxRolloverDays(new BigDecimal("5"));
        leaveType.setLeavePolicy(policy);
        return leaveType;
    }

    private LeaveBalance existingBalance(LeaveType leaveType, BigDecimal current) {
        LeaveBalance balance = new LeaveBalance();
        balance.setId(50L);
        balance.setCompany(company);
        balance.setUser(user);
        balance.setLeaveType(leaveType);
        balance.setYear((short) LocalDate.now().getYear());
        balance.setCurrentBalance(current);
        return balance;
    }
}
