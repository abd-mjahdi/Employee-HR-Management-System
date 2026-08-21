package com.example.employeetimetracking.unit.service.report;

import com.example.employeetimetracking.dto.response.ComplianceReportDto;
import com.example.employeetimetracking.dto.response.DepartmentUtilizationReportDto;
import com.example.employeetimetracking.dto.response.EmployeeTimeReportDto;
import com.example.employeetimetracking.dto.response.PayrollEmployeeHoursDto;
import com.example.employeetimetracking.dto.response.PayrollReportDto;
import com.example.employeetimetracking.dto.response.ProjectHoursReportDto;
import com.example.employeetimetracking.dto.response.TeamLeaveReportDto;
import com.example.employeetimetracking.exception.InvalidDateRangeException;
import com.example.employeetimetracking.model.entities.CompanyMembership;
import com.example.employeetimetracking.model.entities.Department;
import com.example.employeetimetracking.model.entities.LeaveBalance;
import com.example.employeetimetracking.model.entities.LeaveRequest;
import com.example.employeetimetracking.model.entities.LeaveType;
import com.example.employeetimetracking.model.entities.Project;
import com.example.employeetimetracking.model.entities.TimeEntry;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.CompanyStatus;
import com.example.employeetimetracking.model.enums.MembershipStatus;
import com.example.employeetimetracking.model.enums.Status;
import com.example.employeetimetracking.repository.CompanyMembershipRepository;
import com.example.employeetimetracking.repository.LeaveBalanceRepository;
import com.example.employeetimetracking.repository.LeaveRequestRepository;
import com.example.employeetimetracking.repository.LeaveTypeRepository;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
                companyMembershipRepository,
                membershipAccess);
        ReflectionTestUtils.setField(reportService, "dailyOvertimeThresholdHours", new BigDecimal("8"));
        ReflectionTestUtils.setField(reportService, "weeklyOvertimeThresholdHours", new BigDecimal("40"));
        ReflectionTestUtils.setField(reportService, "maxRangeDays", 366);
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
    }

    @Test
    void employeeTime_twoApprovedSameDayDifferentProjects_sumsDailyAndSplitsProjects() {
        LocalDate day = LocalDate.of(2026, 3, 10);
        TimeEntry a = approvedEntry(day, "P1", new BigDecimal("3.00"));
        TimeEntry b = approvedEntry(day, "P2", new BigDecimal("4.50"));
        when(timeEntryRepository.findByCompanyIdAndUserIdAndEntryDateBetweenAndStatus(
                1L, 5L, day, day, Status.APPROVED)).thenReturn(List.of(a, b));

        EmployeeTimeReportDto report = reportService.generateEmployeeTimeReport(5L, day, day);

        assertEquals(new BigDecimal("7.50"), report.getTotalHours());
        assertEquals(2, report.getEntriesCount());
        assertEquals(1, report.getDaysWithEntries());
        assertEquals(1, report.getDailyHours().size());
        assertEquals("2026-03-10", report.getDailyHours().get(0).getKey());
        assertEquals(new BigDecimal("7.50"), report.getDailyHours().get(0).getTotalHours());
        assertEquals(2, report.getProjectBreakdown().size());
        assertEquals("P2", report.getProjectBreakdown().get(0).getKey());
        assertEquals(new BigDecimal("4.50"), report.getProjectBreakdown().get(0).getTotalHours());
        assertEquals("P1", report.getProjectBreakdown().get(1).getKey());
        assertEquals(new BigDecimal("3.00"), report.getProjectBreakdown().get(1).getTotalHours());
    }

    @Test
    void employeeTime_invertedRange_throwsInvalidDateRange() {
        LocalDate start = LocalDate.of(2026, 1, 31);
        LocalDate end = LocalDate.of(2026, 1, 1);
        assertThrows(InvalidDateRangeException.class,
                () -> reportService.generateEmployeeTimeReport(5L, start, end));
    }

    @Test
    void employeeTime_rangeOf367Days_throwsInvalidDateRange() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = start.plusDays(366);
        assertThrows(InvalidDateRangeException.class,
                () -> reportService.generateEmployeeTimeReport(5L, start, end));
    }

    private static TimeEntry approvedEntry(LocalDate date, String projectCode, BigDecimal hours) {
        Project project = new Project();
        project.setProjectCode(projectCode);
        TimeEntry entry = new TimeEntry();
        entry.setEntryDate(date);
        entry.setProject(project);
        entry.setTotalHours(hours);
        entry.setStatus(Status.APPROVED);
        return entry;
    }

    private void stubPayroll(User user, List<TimeEntry> entries) {
        when(timeEntryRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(entries);
        CompanyMembership membership = new CompanyMembership();
        membership.setUser(user);
        membership.setStatus(MembershipStatus.ACTIVE);
        when(companyMembershipRepository.findByCompanyIdAndStatusFetchUser(1L, MembershipStatus.ACTIVE))
                .thenReturn(List.of(membership));
    }

    private static User payrollUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setFirstName("Pat");
        user.setLastName("Roll");
        user.setIsActive(true);
        return user;
    }

    private static CompanyMembership membershipWithDept(User user, Department department) {
        CompanyMembership membership = new CompanyMembership();
        membership.setUser(user);
        membership.setDepartment(department);
        membership.setStatus(MembershipStatus.ACTIVE);
        return membership;
    }

    private static Project project(Long id, String code, String name) {
        Project project = new Project();
        project.setId(id);
        project.setProjectCode(code);
        project.setProjectName(name);
        return project;
    }

    private User stubComplianceUserAndType() {
        User user = payrollUser(5L);
        CompanyMembership membership = new CompanyMembership();
        membership.setUser(user);
        membership.setStatus(MembershipStatus.ACTIVE);
        LeaveType type = new LeaveType();
        type.setId(20L);
        type.setTypeName("Annual");
        type.setIsActive(true);
        when(companyMembershipRepository.findByCompanyIdAndStatusFetchUser(1L, MembershipStatus.ACTIVE))
                .thenReturn(List.of(membership));
        when(leaveTypeRepository.findByCompanyIdAndIsActive(1L, true)).thenReturn(List.of(type));
        return user;
    }

    private static TimeEntry hoursOn(User user, LocalDate date, String hours) {
        TimeEntry entry = new TimeEntry();
        entry.setUser(user);
        entry.setEntryDate(date);
        entry.setTotalHours(new BigDecimal(hours));
        entry.setStatus(Status.APPROVED);
        return entry;
    }

    private static void assertPayrollHours(PayrollReportDto report, String regular, String overtime, String total) {
        assertEquals(1, report.getEmployees().size());
        PayrollEmployeeHoursDto row = report.getEmployees().get(0);
        assertEquals(new BigDecimal(regular), row.getRegularHours());
        assertEquals(new BigDecimal(overtime), row.getOvertimeHours());
        assertEquals(new BigDecimal(total), row.getTotalHours());
        assertEquals(new BigDecimal(regular), report.getTotalRegularHours());
        assertEquals(new BigDecimal(overtime), report.getTotalOvertimeHours());
        assertEquals(new BigDecimal(total), report.getTotalHours());
    }

    @Test
    void teamLeave_managerUsesManagerQuery() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);
        when(leaveRequestRepository.findByStatusInAndDateRangeOverlapForCompany(
                eq(10L), anyList(), eq(start), eq(end), eq(1L))).thenReturn(Collections.emptyList());

        reportService.generateTeamLeaveReport(10L, false, start, end);

        verify(leaveRequestRepository).findByStatusInAndDateRangeOverlapForCompany(
                eq(10L), anyList(), eq(start), eq(end), eq(1L));
        verify(leaveRequestRepository, never()).findByStatusInAndDateRangeOverlapAllForCompany(
                anyList(), eq(start), eq(end), eq(1L));
    }

    @Test
    void teamLeave_hrUsesCompanyWideQuery() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);
        when(leaveRequestRepository.findByStatusInAndDateRangeOverlapAllForCompany(
                anyList(), eq(start), eq(end), eq(1L))).thenReturn(Collections.emptyList());

        reportService.generateTeamLeaveReport(2L, true, start, end);

        verify(leaveRequestRepository).findByStatusInAndDateRangeOverlapAllForCompany(
                anyList(), eq(start), eq(end), eq(1L));
        verify(leaveRequestRepository, never()).findByStatusInAndDateRangeOverlapForCompany(
                eq(2L), anyList(), eq(start), eq(end), eq(1L));
    }

    @Test
    void teamLeave_sumsTotalLeaveDaysTreatingNullAsZero() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);
        LeaveRequest withDays = new LeaveRequest();
        withDays.setId(1L);
        withDays.setStartDate(start);
        withDays.setTotalDays(new BigDecimal("3.50"));
        LeaveRequest nullDays = new LeaveRequest();
        nullDays.setId(2L);
        nullDays.setStartDate(start.plusDays(1));
        nullDays.setTotalDays(null);
        when(leaveRequestRepository.findByStatusInAndDateRangeOverlapForCompany(
                eq(10L), anyList(), eq(start), eq(end), eq(1L)))
                .thenReturn(List.of(withDays, nullDays));

        TeamLeaveReportDto report = reportService.generateTeamLeaveReport(10L, false, start, end);

        assertEquals(2, report.getRequestsCount());
        assertEquals(new BigDecimal("3.50"), report.getTotalLeaveDays());
    }

    @Test
    void payroll_scopesTimeEntriesToCurrentCompany() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);
        when(timeEntryRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(Collections.emptyList());
        when(companyMembershipRepository.findByCompanyIdAndStatusFetchUser(1L, MembershipStatus.ACTIVE))
                .thenReturn(Collections.emptyList());

        reportService.generatePayrollReport(start, end);

        verify(timeEntryRepository).findAll(any(Specification.class), any(Sort.class));
        verify(companyMembershipRepository).findByCompanyIdAndStatusFetchUser(1L, MembershipStatus.ACTIVE);
    }

    @Test
    void payroll_oneDayTenHours_dailyOvertimeTwo() {
        User user = payrollUser(5L);
        LocalDate monday = LocalDate.of(2026, 1, 5);
        stubPayroll(user, List.of(hoursOn(user, monday, "10.00")));

        PayrollReportDto report = reportService.generatePayrollReport(monday, monday.plusDays(6));

        assertPayrollHours(report, "8.00", "2.00", "10.00");
    }

    @Test
    void payroll_fiveDaysNineHours_overtimeIsMaxNotSum() {
        User user = payrollUser(5L);
        LocalDate monday = LocalDate.of(2026, 1, 5);
        stubPayroll(user, List.of(
                hoursOn(user, monday, "9.00"),
                hoursOn(user, monday.plusDays(1), "9.00"),
                hoursOn(user, monday.plusDays(2), "9.00"),
                hoursOn(user, monday.plusDays(3), "9.00"),
                hoursOn(user, monday.plusDays(4), "9.00")));

        PayrollReportDto report = reportService.generatePayrollReport(monday, monday.plusDays(6));

        assertPayrollHours(report, "40.00", "5.00", "45.00");
    }

    @Test
    void payroll_fiveDaysEightHours_overtimeZero() {
        User user = payrollUser(5L);
        LocalDate monday = LocalDate.of(2026, 1, 5);
        stubPayroll(user, List.of(
                hoursOn(user, monday, "8.00"),
                hoursOn(user, monday.plusDays(1), "8.00"),
                hoursOn(user, monday.plusDays(2), "8.00"),
                hoursOn(user, monday.plusDays(3), "8.00"),
                hoursOn(user, monday.plusDays(4), "8.00")));

        PayrollReportDto report = reportService.generatePayrollReport(monday, monday.plusDays(6));

        assertPayrollHours(report, "40.00", "0.00", "40.00");
    }

    @Test
    void leaveBalances_usesCurrentCompanyOnly() {
        when(leaveBalanceRepository.findAllLeaveBalancesForYearAndCompany(2026, 1L))
                .thenReturn(Collections.emptyList());
        when(membershipAccess.mapByUserId(eq(1L), any())).thenReturn(Collections.emptyMap());

        reportService.generateLeaveBalanceReport(2026, null);

        verify(leaveBalanceRepository).findAllLeaveBalancesForYearAndCompany(2026, 1L);
    }

    @Test
    void leaveBalancesForDepartment_usesCurrentCompanyOnly() {
        when(leaveBalanceRepository.findLeaveBalancesForYearAndDepartmentAndCompany(2026, 9L, 1L))
                .thenReturn(Collections.emptyList());
        when(membershipAccess.mapByUserId(eq(1L), any())).thenReturn(Collections.emptyMap());

        reportService.generateLeaveBalanceReport(2026, 9L);

        verify(leaveBalanceRepository).findLeaveBalancesForYearAndDepartmentAndCompany(2026, 9L, 1L);
    }

    @Test
    void leaveBalances_year1999_throwsInvalidDateRange() {
        assertThrows(InvalidDateRangeException.class,
                () -> reportService.generateLeaveBalanceReport(1999, null));
        verify(leaveBalanceRepository, never()).findAllLeaveBalancesForYearAndCompany(eq(1999), any());
        verify(leaveBalanceRepository, never()).findLeaveBalancesForYearAndDepartmentAndCompany(eq(1999), any(), any());
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
    }

    @Test
    void departmentUtilization_twoUsersSameDepartment_sumsHours() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);
        User u1 = payrollUser(1L);
        User u2 = payrollUser(2L);
        User noDept = payrollUser(3L);
        Department dept = new Department();
        dept.setId(9L);
        dept.setDepartmentCode("ENG");
        dept.setDepartmentName("Engineering");
        CompanyMembership m1 = membershipWithDept(u1, dept);
        CompanyMembership m2 = membershipWithDept(u2, dept);
        CompanyMembership m3 = membershipWithDept(noDept, null);
        when(timeEntryRepository.findForDepartmentUtilizationByCompany(1L, Status.APPROVED, start, end))
                .thenReturn(List.of(
                        hoursOn(u1, start, "3.00"),
                        hoursOn(u2, start, "5.00"),
                        hoursOn(noDept, start, "9.00")));
        when(membershipAccess.mapByUserId(eq(1L), any())).thenReturn(Map.of(1L, m1, 2L, m2, 3L, m3));

        DepartmentUtilizationReportDto report = reportService.generateDepartmentUtilizationReport(start, end);

        assertEquals(1, report.getDepartments().size());
        assertEquals(new BigDecimal("8.00"), report.getDepartments().get(0).getTotalHours());
        assertEquals(2, report.getDepartments().get(0).getEmployeesCount());
        assertEquals(new BigDecimal("8.00"), report.getTotalHours());
    }

    @Test
    void projectHours_usesCurrentCompanyOnly() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);
        when(timeEntryRepository.findForProjectHoursByCompany(1L, Status.APPROVED, start, end))
                .thenReturn(Collections.emptyList());

        reportService.generateProjectHours(start, end);

        verify(timeEntryRepository).findForProjectHoursByCompany(1L, Status.APPROVED, start, end);
    }

    @Test
    void projectHours_twoProjects_reportTotalMatchesSum() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);
        User user = payrollUser(5L);
        Project p1 = project(11L, "P1", "Alpha");
        Project p2 = project(12L, "P2", "Beta");
        TimeEntry e1 = hoursOn(user, start, "3.00");
        e1.setProject(p1);
        TimeEntry e2 = hoursOn(user, start, "5.00");
        e2.setProject(p2);
        when(timeEntryRepository.findForProjectHoursByCompany(1L, Status.APPROVED, start, end))
                .thenReturn(List.of(e1, e2));

        ProjectHoursReportDto report = reportService.generateProjectHours(start, end);

        assertEquals(2, report.getProjects().size());
        BigDecimal rowSum = report.getProjects().stream()
                .map(item -> item.getTotalHours())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(new BigDecimal("8.00"), report.getTotalHours());
        assertEquals(report.getTotalHours(), rowSum);
    }

    @Test
    void compliance_usesCurrentCompanyPeopleAndTypes() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);
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
        verify(leaveRequestRepository, never()).findByStatusInAndDateRangeOverlapAllForCompany(
                anyList(), eq(start), eq(end), eq(1L));
        verify(timeEntryRepository, never()).findAll(any(Specification.class), any(Sort.class));
    }

    @Test
    void compliance_missingBalance_oneEntitlementIssue() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);
        stubComplianceUserAndType();
        when(leaveBalanceRepository.findAllLeaveBalancesForYearAndCompany(2026, 1L))
                .thenReturn(Collections.emptyList());

        ComplianceReportDto report = reportService.generateComplianceReport(start, end);

        assertEquals(1, report.getEntitlementIssuesCount());
        assertEquals(1, report.getEntitlementIssues().size());
    }

    @Test
    void compliance_matchingBalance_zeroIssues() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);
        User user = stubComplianceUserAndType();
        LeaveType type = new LeaveType();
        type.setId(20L);
        LeaveBalance balance = new LeaveBalance();
        balance.setUser(user);
        balance.setLeaveType(type);
        when(leaveBalanceRepository.findAllLeaveBalancesForYearAndCompany(2026, 1L))
                .thenReturn(List.of(balance));

        ComplianceReportDto report = reportService.generateComplianceReport(start, end);

        assertEquals(0, report.getEntitlementIssuesCount());
        assertEquals(0, report.getEntitlementIssues().size());
    }

    @Test
    void compliance_crossYearRange_throwsInvalidDateRange() {
        assertThrows(InvalidDateRangeException.class,
                () -> reportService.generateComplianceReport(
                        LocalDate.of(2026, 12, 1), LocalDate.of(2027, 1, 1)));
    }
}
