package com.example.employeetimetracking.unit.controller;

import com.example.employeetimetracking.controller.ReportController;
import com.example.employeetimetracking.service.ReportService;
import com.example.employeetimetracking.tenant.MembershipAccess;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportContractTest {

    private static final Set<String> KEPT_PATHS = Set.of(
            "/employee-time",
            "/team-leave",
            "/payroll",
            "/leave-balances",
            "/department-utilization",
            "/project-hours",
            "/compliance"
    );

    @Test
    void reportController_hasOnlyTheSevenKeptPaths() {
        RequestMapping typeMapping = ReportController.class.getAnnotation(RequestMapping.class);
        assertEquals("/reports", typeMapping.value()[0]);

        Set<String> paths = new TreeSet<>();
        for (Method method : ReportController.class.getDeclaredMethods()) {
            GetMapping get = method.getAnnotation(GetMapping.class);
            if (get == null) {
                continue;
            }
            if (get.value().length > 0) {
                paths.add(get.value()[0]);
            }
        }
        assertEquals(KEPT_PATHS, paths);
    }

    @Test
    void removedReportPaths_haveNoHandler() throws Exception {
        ReportController controller = new ReportController(mock(ReportService.class), mock(MembershipAccess.class));
        var mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(get("/reports/absence-patterns")
                        .param("startDate", "2026-04-13")
                        .param("endDate", "2026-04-19"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/reports/overtime-summary")
                        .param("startDate", "2026-04-13")
                        .param("endDate", "2026-04-19"))
                .andExpect(status().isNotFound());
    }

    @Test
    void managerRestrictedReports_requireHrAdmin() throws Exception {
        assertEquals("hasRole('HR_ADMIN')",
                ReportController.class.getDeclaredMethod("departmentUtilization",
                                java.time.LocalDate.class, java.time.LocalDate.class)
                        .getAnnotation(PreAuthorize.class).value());
        assertEquals("hasRole('HR_ADMIN')",
                ReportController.class.getDeclaredMethod("payroll",
                                java.time.LocalDate.class, java.time.LocalDate.class, String.class)
                        .getAnnotation(PreAuthorize.class).value());
        assertEquals("hasRole('HR_ADMIN')",
                ReportController.class.getDeclaredMethod("payrollCsv",
                                java.time.LocalDate.class, java.time.LocalDate.class)
                        .getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void reportService_doesNotThrowInvalidTimeEntryException() throws Exception {
        Path source = Path.of("src/main/java/com/example/employeetimetracking/service/ReportService.java");
        String text = Files.readString(source);
        assertFalse(text.contains("InvalidTimeEntryException"));
    }

    @Test
    void reportDtos_doNotDeclareCompanyId() throws ClassNotFoundException {
        String pkg = "com.example.employeetimetracking.dto.response";
        for (String simple : Set.of(
                "EmployeeTimeReportDto",
                "TeamLeaveReportDto",
                "TeamLeaveRequestItemDto",
                "PayrollReportDto",
                "PayrollEmployeeHoursDto",
                "LeaveBalanceReportDto",
                "LeaveBalanceReportItemDto",
                "DepartmentUtilizationReportDto",
                "DepartmentUtilizationItemDto",
                "ProjectHoursReportDto",
                "ProjectHoursItemDto",
                "ComplianceReportDto",
                "ComplianceEntitlementIssueDto",
                "TimeSummaryItemDto"
        )) {
            Class<?> type = Class.forName(pkg + "." + simple);
            for (Field field : type.getDeclaredFields()) {
                assertFalse("companyId".equals(field.getName()) || "company_id".equals(field.getName()),
                        type.getName() + "." + field.getName());
            }
        }
    }
}
