package com.example.employeetimetracking.unit.controller;

import com.example.employeetimetracking.controller.ReportController;
import com.example.employeetimetracking.dto.response.PayrollEmployeeHoursDto;
import com.example.employeetimetracking.dto.response.PayrollReportDto;
import com.example.employeetimetracking.model.entities.Company;
import com.example.employeetimetracking.model.entities.CompanyMembership;
import com.example.employeetimetracking.model.entities.Department;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.CompanyStatus;
import com.example.employeetimetracking.model.enums.MembershipStatus;
import com.example.employeetimetracking.model.enums.UserRole;
import com.example.employeetimetracking.security.CustomUserDetails;
import com.example.employeetimetracking.service.ReportService;
import com.example.employeetimetracking.tenant.MembershipAccess;
import com.example.employeetimetracking.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    @Mock ReportService reportService;
    @Mock MembershipAccess membershipAccess;

    @InjectMocks
    ReportController reportController;

    CustomUserDetails hr;
    CustomUserDetails manager;
    CustomUserDetails employee;
    User managerUser;
    Company company;
    CompanyMembership targetMembership;
    LocalDate start = LocalDate.of(2026, 1, 1);
    LocalDate end = LocalDate.of(2026, 1, 31);

    @BeforeEach
    void setUp() {
        TenantContext.set(new TenantContext.TenantInfo(1L, "acme", CompanyStatus.ACTIVE));
        Company company = new Company();
        company.setId(1L);
        this.company = company;

        User hrUser = new User();
        hrUser.setId(2L);
        hrUser.setEmail("hr@acme.com");
        hrUser.setPasswordHash("hash");
        hrUser.setIsActive(true);

        CompanyMembership hrMembership = new CompanyMembership();
        hrMembership.setId(2L);
        hrMembership.setUser(hrUser);
        hrMembership.setCompany(company);
        hrMembership.setRole(UserRole.HR_ADMIN);
        hrMembership.setStatus(MembershipStatus.ACTIVE);
        hr = new CustomUserDetails(hrMembership);

        User managerUser = new User();
        managerUser.setId(3L);
        managerUser.setEmail("mgr@acme.com");
        managerUser.setPasswordHash("hash");
        managerUser.setIsActive(true);
        this.managerUser = managerUser;
        CompanyMembership managerMembership = new CompanyMembership();
        managerMembership.setId(3L);
        managerMembership.setUser(managerUser);
        managerMembership.setCompany(company);
        managerMembership.setRole(UserRole.MANAGER);
        managerMembership.setStatus(MembershipStatus.ACTIVE);
        manager = new CustomUserDetails(managerMembership);

        User employeeUser = new User();
        employeeUser.setId(7L);
        employeeUser.setEmail("peer@acme.com");
        employeeUser.setPasswordHash("hash");
        employeeUser.setIsActive(true);
        CompanyMembership employeeMembership = new CompanyMembership();
        employeeMembership.setId(7L);
        employeeMembership.setUser(employeeUser);
        employeeMembership.setCompany(company);
        employeeMembership.setRole(UserRole.EMPLOYEE);
        employeeMembership.setStatus(MembershipStatus.ACTIVE);
        employee = new CustomUserDetails(employeeMembership);

        User other = new User();
        other.setId(5L);
        other.setEmail("emp@acme.com");
        other.setPasswordHash("hash");
        other.setIsActive(true);
        targetMembership = new CompanyMembership();
        targetMembership.setId(5L);
        targetMembership.setUser(other);
        targetMembership.setCompany(company);
        targetMembership.setRole(UserRole.EMPLOYEE);
        targetMembership.setStatus(MembershipStatus.ACTIVE);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void employeeTime_rejectsUserOutsideCurrentCompany() {
        when(membershipAccess.find(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class,
                () -> reportController.employeeTime(99L, start, end, hr));

        verify(reportService, never()).generateEmployeeTimeReport(anyLong(), any(), any());
    }

    @Test
    void employeeTime_hrCanViewUserInCurrentCompany() {
        when(membershipAccess.find(5L, 1L)).thenReturn(Optional.of(targetMembership));

        reportController.employeeTime(5L, start, end, hr);

        verify(reportService).generateEmployeeTimeReport(5L, start, end);
    }

    @Test
    void employeeTime_employeeCannotViewAnotherInCompanyUser() {
        when(membershipAccess.find(5L, 1L)).thenReturn(Optional.of(targetMembership));

        assertThrows(AccessDeniedException.class,
                () -> reportController.employeeTime(5L, start, end, employee));

        verify(reportService, never()).generateEmployeeTimeReport(anyLong(), any(), any());
    }

    @Test
    void employeeTime_managerOfUser5_callsService() {
        when(membershipAccess.find(5L, 1L)).thenReturn(Optional.of(targetMembership));
        when(membershipAccess.isDirectManagerOf(3L, 5L)).thenReturn(true);

        reportController.employeeTime(5L, start, end, manager);

        verify(reportService).generateEmployeeTimeReport(5L, start, end);
    }

    @Test
    void employeeTime_managerRejectedWhenUserOutsideCurrentCompany() {
        when(membershipAccess.find(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class,
                () -> reportController.employeeTime(99L, start, end, manager));

        verify(reportService, never()).generateEmployeeTimeReport(anyLong(), any(), any());
    }

    @Test
    void employeeTime_employeeRejectedWhenUserOutsideCurrentCompany() {
        when(membershipAccess.find(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class,
                () -> reportController.employeeTime(99L, start, end, employee));

        verify(reportService, never()).generateEmployeeTimeReport(anyLong(), any(), any());
    }

    @Test
    void leaveBalances_managerWithoutDepartmentIsDenied() {
        CompanyMembership noDept = new CompanyMembership();
        noDept.setId(3L);
        noDept.setUser(managerUser);
        noDept.setCompany(company);
        noDept.setRole(UserRole.MANAGER);
        noDept.setStatus(MembershipStatus.ACTIVE);
        when(membershipAccess.findFor(manager, manager.getId())).thenReturn(Optional.of(noDept));

        assertThrows(AccessDeniedException.class,
                () -> reportController.leaveBalances(2026, null, manager));

        verify(reportService, never()).generateLeaveBalanceReport(any(), any());
    }

    @Test
    void leaveBalances_managerUsesOwnDepartment() {
        Department dept = new Department();
        dept.setId(9L);
        CompanyMembership withDept = new CompanyMembership();
        withDept.setId(3L);
        withDept.setUser(managerUser);
        withDept.setCompany(company);
        withDept.setRole(UserRole.MANAGER);
        withDept.setStatus(MembershipStatus.ACTIVE);
        withDept.setDepartment(dept);
        when(membershipAccess.findFor(manager, manager.getId())).thenReturn(Optional.of(withDept));

        reportController.leaveBalances(2026, null, manager);

        verify(reportService).generateLeaveBalanceReport(eq(2026), eq(9L));
    }

    @Test
    void leaveBalances_hrCanOmitDepartment() {
        reportController.leaveBalances(2026, null, hr);

        verify(reportService).generateLeaveBalanceReport(eq(2026), isNull());
    }

    @Test
    void payrollCsv_escapesCommaQuoteAndNewlineInNames() {
        PayrollEmployeeHoursDto row = new PayrollEmployeeHoursDto(
                1L,
                "Last, \"Nick\"\nJr",
                new BigDecimal("8.00"),
                new BigDecimal("0.00"),
                new BigDecimal("8.00"));
        PayrollReportDto report = new PayrollReportDto(
                start, end,
                new BigDecimal("8.0"),
                new BigDecimal("40.0"),
                new BigDecimal("8.00"),
                new BigDecimal("0.00"),
                new BigDecimal("8.00"),
                List.of(row));
        when(reportService.generatePayrollReport(start, end)).thenReturn(report);

        ResponseEntity<byte[]> response = reportController.payrollCsv(start, end);
        String csv = new String(response.getBody(), StandardCharsets.UTF_8);

        assertTrue(csv.contains("\"Last, \"\"Nick\"\"\nJr\""));
        assertFalse(csv.contains("Last, \"Nick\"\nJr,8.00"));
    }
}
