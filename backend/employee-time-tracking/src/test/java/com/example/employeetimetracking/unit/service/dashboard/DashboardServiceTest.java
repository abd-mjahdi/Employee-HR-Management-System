package com.example.employeetimetracking.unit.service.dashboard;

import com.example.employeetimetracking.dto.response.DashboardStatsDto;
import com.example.employeetimetracking.dto.response.UserResponseDto;
import com.example.employeetimetracking.exception.UserNotFoundException;
import com.example.employeetimetracking.mapper.UserMapper;
import com.example.employeetimetracking.model.entities.Company;
import com.example.employeetimetracking.model.entities.CompanyMembership;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.CompanyStatus;
import com.example.employeetimetracking.model.enums.MembershipStatus;
import com.example.employeetimetracking.model.enums.UserRole;
import com.example.employeetimetracking.repository.CompanyMembershipRepository;
import com.example.employeetimetracking.security.CustomUserDetails;
import com.example.employeetimetracking.service.DashboardService;
import com.example.employeetimetracking.service.LeaveBalanceService;
import com.example.employeetimetracking.service.LeaveRequestService;
import com.example.employeetimetracking.service.TimeEntryService;
import com.example.employeetimetracking.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock CompanyMembershipRepository companyMembershipRepository;
    @Mock LeaveRequestService leaveRequestService;
    @Mock TimeEntryService timeEntryService;
    @Mock LeaveBalanceService leaveBalanceService;
    @Mock UserMapper userMapper;

    @InjectMocks
    DashboardService dashboardService;

    Company company;
    User hr;
    CompanyMembership hrMembership;

    @BeforeEach
    void setUp() {
        TenantContext.set(new TenantContext.TenantInfo(1L, "acme", CompanyStatus.ACTIVE));
        company = new Company();
        company.setId(1L);
        company.setSlug("acme");
        company.setStatus(CompanyStatus.ACTIVE);

        hr = new User();
        hr.setId(2L);
        hr.setEmail("hr@acme.com");
        hr.setPasswordHash("hash");
        hr.setFirstName("Pat");
        hr.setLastName("Admin");
        hr.setIsActive(true);

        hrMembership = new CompanyMembership();
        hrMembership.setId(2L);
        hrMembership.setUser(hr);
        hrMembership.setCompany(company);
        hrMembership.setRole(UserRole.HR_ADMIN);
        hrMembership.setStatus(MembershipStatus.ACTIVE);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void dashboard_loadsMembershipInCurrentCompany() {
        when(companyMembershipRepository.findByUserIdAndCompanyId(2L, 1L)).thenReturn(Optional.of(hrMembership));
        when(userMapper.toDto(hr, hrMembership)).thenReturn(new UserResponseDto());
        when(leaveBalanceService.getByUserIdAndYear(anyLong(), anyInt())).thenReturn(Collections.emptyList());
        when(leaveRequestService.getUpcomingLeave(anyLong(), anyInt())).thenReturn(Collections.emptyList());
        when(leaveRequestService.getRecentLeaveRequests(anyLong(), anyInt())).thenReturn(Collections.emptyList());
        when(timeEntryService.getRecentTimeEntries(hr)).thenReturn(Collections.emptyList());
        stubHrStats();

        dashboardService.getDashboardData(new CustomUserDetails(hrMembership));

        verify(companyMembershipRepository).findByUserIdAndCompanyId(2L, 1L);
        verify(companyMembershipRepository).countByCompanyIdAndStatus(1L, MembershipStatus.ACTIVE);
    }

    @Test
    void dashboard_missingMembershipInCurrentCompany_isNotFound() {
        when(companyMembershipRepository.findByUserIdAndCompanyId(2L, 1L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> dashboardService.getDashboardData(new CustomUserDetails(hrMembership)));
    }

    @Test
    void hrActiveUsers_countsMembershipsInCurrentCompany() {
        stubHrStats();

        DashboardStatsDto stats = dashboardService.getDashboardStats(hr, UserRole.HR_ADMIN);

        verify(companyMembershipRepository).countByCompanyIdAndStatus(1L, MembershipStatus.ACTIVE);
        assertEquals(7, stats.getTotalActiveEmployees());
    }

    private void stubHrStats() {
        when(timeEntryService.getHoursThisWeek(2L)).thenReturn(BigDecimal.ZERO);
        when(timeEntryService.getHoursThisMonth(2L)).thenReturn(BigDecimal.ZERO);
        when(timeEntryService.getUserPendingCount(2L)).thenReturn(0);
        when(leaveRequestService.getUserPendingCount(2L)).thenReturn(0);
        when(timeEntryService.getPendingTimeApprovalsCount(2L)).thenReturn(0);
        when(leaveRequestService.getPendingLeaveApprovalsCount(2L, true)).thenReturn(0);
        when(leaveRequestService.getTeamMembersOnLeaveToday(2L)).thenReturn(0);
        when(companyMembershipRepository.countByCompanyIdAndStatus(1L, MembershipStatus.ACTIVE)).thenReturn(7L);
    }
}
