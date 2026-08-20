package com.example.employeetimetracking.unit.service.report;

import com.example.employeetimetracking.model.enums.CompanyStatus;
import com.example.employeetimetracking.model.enums.MembershipStatus;
import com.example.employeetimetracking.model.enums.Status;
import com.example.employeetimetracking.repository.CompanyMembershipRepository;
import com.example.employeetimetracking.repository.LeaveBalanceRepository;
import com.example.employeetimetracking.repository.LeaveRequestRepository;
import com.example.employeetimetracking.repository.LeaveTypeRepository;
import com.example.employeetimetracking.repository.TimeEntryBreakRepository;
import com.example.employeetimetracking.repository.TimeEntryRepository;
import com.example.employeetimetracking.service.ReportService;
import com.example.employeetimetracking.tenant.MembershipAccess;
import com.example.employeetimetracking.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock TimeEntryRepository timeEntryRepository;
    @Mock LeaveRequestRepository leaveRequestRepository;
    @Mock LeaveBalanceRepository leaveBalanceRepository;
    @Mock LeaveTypeRepository leaveTypeRepository;
    @Mock TimeEntryBreakRepository timeEntryBreakRepository;
    @Mock CompanyMembershipRepository companyMembershipRepository;
    @Mock MembershipAccess membershipAccess;

    ReportService reportService;

    @BeforeEach
    void setUp() {
        TenantContext.set(new TenantContext.TenantInfo(1L, "acme", CompanyStatus.ACTIVE));
        reportService = new ReportService(
                timeEntryRepository,
                leaveRequestRepository,
                leaveBalanceRepository,
                leaveTypeRepository,
                timeEntryBreakRepository,
                companyMembershipRepository,
                membershipAccess);
        ReflectionTestUtils.setField(reportService, "dailyOvertimeThresholdHours", new BigDecimal("8"));
        ReflectionTestUtils.setField(reportService, "weeklyOvertimeThresholdHours", new BigDecimal("40"));
        ReflectionTestUtils.setField(reportService, "breakRequiredAfterHours", new BigDecimal("6"));
        ReflectionTestUtils.setField(reportService, "breakRequiredMinutes", 30);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void employeeTime_usesCurrentCompanyOnly() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);
        when(timeEntryRepository.findByCompanyIdAndUserIdAndEntryDateBetweenAndStatus(
                1L, 5L, start, end, Status.APPROVED)).thenReturn(Collections.emptyList());

        reportService.generateEmployeeTimeReport(5L, start, end);

        verify(timeEntryRepository).findByCompanyIdAndUserIdAndEntryDateBetweenAndStatus(
                1L, 5L, start, end, Status.APPROVED);
        verify(timeEntryRepository, never()).findByUserIdAndEntryDateBetweenAndStatus(
                5L, start, end, Status.APPROVED);
    }

    @Test
    void teamLeave_usesCurrentCompanyOnly() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);
        when(leaveRequestRepository.findByStatusInAndDateRangeOverlapForCompany(
                eq(10L), anyList(), eq(start), eq(end), eq(1L))).thenReturn(Collections.emptyList());
        when(leaveRequestRepository.findByUserManagerIdAndStatusInAndStartDateAfterOrderByStartDateAscForCompany(
                eq(10L), anyList(), any(LocalDate.class), eq(1L))).thenReturn(Collections.emptyList());

        reportService.generateTeamLeaveReport(10L, start, end);

        verify(leaveRequestRepository).findByStatusInAndDateRangeOverlapForCompany(
                eq(10L), anyList(), eq(start), eq(end), eq(1L));
        verify(leaveRequestRepository, never()).findByStatusInAndDateRangeOverlap(
                eq(10L), anyList(), eq(start), eq(end));
    }

    @Test
    void payroll_scopesTimeEntriesToCurrentCompany() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);
        when(timeEntryRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(Collections.emptyList());

        reportService.generatePayrollReport(start, end);

        verify(timeEntryRepository).findAll(any(Specification.class), any(Sort.class));
        verify(timeEntryRepository, never()).findForProjectHours(Status.APPROVED, start, end);
    }

    @Test
    void leaveBalances_usesCurrentCompanyOnly() {
        when(leaveBalanceRepository.findAllLeaveBalancesForYearAndCompany(2026, 1L))
                .thenReturn(Collections.emptyList());
        when(membershipAccess.mapByUserId(eq(1L), any())).thenReturn(Collections.emptyMap());

        reportService.generateLeaveBalanceReport(2026, null);

        verify(leaveBalanceRepository).findAllLeaveBalancesForYearAndCompany(2026, 1L);
        verify(leaveBalanceRepository, never()).findAllLeaveBalancesForYear(2026);
    }

    @Test
    void leaveBalancesForDepartment_usesCurrentCompanyOnly() {
        when(leaveBalanceRepository.findLeaveBalancesForYearAndDepartmentAndCompany(2026, 9L, 1L))
                .thenReturn(Collections.emptyList());
        when(membershipAccess.mapByUserId(eq(1L), any())).thenReturn(Collections.emptyMap());

        reportService.generateLeaveBalanceReport(2026, 9L);

        verify(leaveBalanceRepository).findLeaveBalancesForYearAndDepartmentAndCompany(2026, 9L, 1L);
        verify(leaveBalanceRepository, never()).findLeaveBalancesForYearAndDepartment(2026, 9L);
    }

    @Test
    void departmentUtilization_usesCurrentCompanyOnly() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);
        when(timeEntryRepository.findForDepartmentUtilizationByCompany(1L, Status.APPROVED, start, end))
                .thenReturn(Collections.emptyList());
        when(membershipAccess.mapByUserId(eq(1L), any())).thenReturn(Collections.emptyMap());

        reportService.generateDepartmentUtilizationReport(start, end);

        verify(timeEntryRepository).findForDepartmentUtilizationByCompany(1L, Status.APPROVED, start, end);
        verify(timeEntryRepository, never()).findForDepartmentUtilization(Status.APPROVED, start, end);
    }

    @Test
    void absencePatterns_usesCurrentCompanyOnly() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);
        when(leaveRequestRepository.findByStatusInAndDateRangeOverlapAllForCompany(
                anyList(), eq(start), eq(end), eq(1L))).thenReturn(Collections.emptyList());

        reportService.generateAbsencePatternsReport(start, end);

        verify(leaveRequestRepository).findByStatusInAndDateRangeOverlapAllForCompany(
                anyList(), eq(start), eq(end), eq(1L));
        verify(leaveRequestRepository, never()).findByStatusInAndDateRangeOverlapAll(anyList(), eq(start), eq(end));
    }

    @Test
    void projectHours_usesCurrentCompanyOnly() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);
        when(timeEntryRepository.findForProjectHoursByCompany(1L, Status.APPROVED, start, end))
                .thenReturn(Collections.emptyList());

        reportService.generateProjectHours(start, end);

        verify(timeEntryRepository).findForProjectHoursByCompany(1L, Status.APPROVED, start, end);
        verify(timeEntryRepository, never()).findForProjectHours(Status.APPROVED, start, end);
    }

    @Test
    void compliance_usesCurrentCompanyPeopleAndTypes() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);
        when(leaveRequestRepository.findByStatusInAndDateRangeOverlapAllForCompany(
                anyList(), eq(start), eq(end), eq(1L))).thenReturn(Collections.emptyList());
        when(timeEntryRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(Collections.emptyList());
        when(companyMembershipRepository.findByCompanyIdAndStatusFetchUser(1L, MembershipStatus.ACTIVE))
                .thenReturn(Collections.emptyList());
        when(leaveTypeRepository.findByCompanyIdAndIsActive(1L, true)).thenReturn(Collections.emptyList());
        when(leaveBalanceRepository.findAllLeaveBalancesForYearAndCompany(2026, 1L))
                .thenReturn(Collections.emptyList());

        reportService.generateComplianceReport(start, end);

        verify(companyMembershipRepository).findByCompanyIdAndStatusFetchUser(1L, MembershipStatus.ACTIVE);
        verify(leaveTypeRepository).findByCompanyIdAndIsActive(1L, true);
        verify(leaveBalanceRepository).findAllLeaveBalancesForYearAndCompany(2026, 1L);
        verify(leaveTypeRepository, never()).findByIsActive(true);
        verify(leaveBalanceRepository, never()).findAllLeaveBalancesForYear(2026);
    }
}
