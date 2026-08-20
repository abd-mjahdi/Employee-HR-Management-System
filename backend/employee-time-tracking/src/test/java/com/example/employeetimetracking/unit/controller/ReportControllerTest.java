package com.example.employeetimetracking.unit.controller;

import com.example.employeetimetracking.controller.ReportController;
import com.example.employeetimetracking.model.entities.Company;
import com.example.employeetimetracking.model.entities.CompanyMembership;
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
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
    CompanyMembership targetMembership;
    LocalDate start = LocalDate.of(2026, 1, 1);
    LocalDate end = LocalDate.of(2026, 1, 31);

    @BeforeEach
    void setUp() {
        TenantContext.set(new TenantContext.TenantInfo(1L, "acme", CompanyStatus.ACTIVE));
        Company company = new Company();
        company.setId(1L);

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
}
