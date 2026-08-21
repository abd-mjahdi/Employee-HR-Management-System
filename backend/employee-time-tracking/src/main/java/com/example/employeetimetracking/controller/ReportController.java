package com.example.employeetimetracking.controller;

import com.example.employeetimetracking.dto.response.DepartmentUtilizationReportDto;
import com.example.employeetimetracking.dto.response.EmployeeTimeReportDto;
import com.example.employeetimetracking.dto.response.ComplianceReportDto;
import com.example.employeetimetracking.dto.response.LeaveBalanceReportDto;
import com.example.employeetimetracking.dto.response.PayrollReportDto;
import com.example.employeetimetracking.dto.response.ProjectHoursReportDto;
import com.example.employeetimetracking.dto.response.TeamLeaveReportDto;
import com.example.employeetimetracking.security.CustomUserDetails;
import com.example.employeetimetracking.service.ReportService;
import com.example.employeetimetracking.tenant.MembershipAccess;
import com.example.employeetimetracking.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/reports")
public class ReportController {
    private final ReportService reportService;
    private final MembershipAccess membershipAccess;

    @Autowired
    public ReportController(ReportService reportService, MembershipAccess membershipAccess) {
        this.reportService = reportService;
        this.membershipAccess = membershipAccess;
    }

    @GetMapping("/employee-time")
    public ResponseEntity<EmployeeTimeReportDto> employeeTime(
            @RequestParam(required = false) Long userId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @AuthenticationPrincipal CustomUserDetails authenticatedUser
    ) {
        Long targetUserId = userId == null ? authenticatedUser.getId() : userId;
        assertCanViewUserReport(authenticatedUser, targetUserId);
        return ResponseEntity.ok(reportService.generateEmployeeTimeReport(targetUserId, startDate, endDate));
    }

    @PreAuthorize("hasAnyRole('MANAGER','HR_ADMIN')")
    @GetMapping("/team-leave")
    public ResponseEntity<TeamLeaveReportDto> teamLeave(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @AuthenticationPrincipal CustomUserDetails authenticatedUser
    ) {
        boolean hr = authenticatedUser.hasRole("HR_ADMIN");
        return ResponseEntity.ok(reportService.generateTeamLeaveReport(
                authenticatedUser.getId(), hr, startDate, endDate));
    }

    @PreAuthorize("hasRole('HR_ADMIN')")
    @GetMapping("/payroll")
    public ResponseEntity<PayrollReportDto> payroll(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam(required = false, defaultValue = "json") String format
    ) {
        requirePayrollJsonFormat(format);
        return ResponseEntity.ok(reportService.generatePayrollReport(startDate, endDate));
    }

    @PreAuthorize("hasRole('HR_ADMIN')")
    @GetMapping(value = "/payroll", params = "format=csv")
    public ResponseEntity<byte[]> payrollCsv(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    ) {
        PayrollReportDto report = reportService.generatePayrollReport(startDate, endDate);
        byte[] csv = payrollToCsv(report);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"payroll.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }

    @PreAuthorize("hasAnyRole('MANAGER','HR_ADMIN')")
    @GetMapping("/leave-balances")
    public ResponseEntity<LeaveBalanceReportDto> leaveBalances(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long departmentId,
            @AuthenticationPrincipal CustomUserDetails authenticatedUser
    ) {
        boolean isHr = authenticatedUser.hasRole("HR_ADMIN");
        if (!isHr) {
            Long myDeptId = membershipAccess.findFor(authenticatedUser, authenticatedUser.getId())
                    .map(m -> m.getDepartment() == null ? null : m.getDepartment().getId())
                    .orElse(null);
            if (myDeptId == null) {
                throw new AccessDeniedException("You can only access leave balances for your department");
            }
            if (departmentId != null && !departmentId.equals(myDeptId)) {
                throw new AccessDeniedException("You can only access leave balances for your department");
            }
            departmentId = myDeptId;
        }
        return ResponseEntity.ok(reportService.generateLeaveBalanceReport(year, departmentId));
    }

    // Managers use GET /time-entries/team and GET /time-entries/summary.
    @PreAuthorize("hasRole('HR_ADMIN')")
    @GetMapping("/department-utilization")
    public ResponseEntity<DepartmentUtilizationReportDto> departmentUtilization(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    ) {
        return ResponseEntity.ok(reportService.generateDepartmentUtilizationReport(startDate, endDate));
    }

    // Managers use GET /time-entries/team and GET /time-entries/summary.
    @PreAuthorize("hasRole('HR_ADMIN')")
    @GetMapping("/project-hours")
    public ResponseEntity<ProjectHoursReportDto> projectHours(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    ) {
        return ResponseEntity.ok(reportService.generateProjectHours(startDate, endDate));
    }

    @PreAuthorize("hasRole('HR_ADMIN')")
    @GetMapping("/compliance")
    public ResponseEntity<ComplianceReportDto> compliance(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    ) {
        return ResponseEntity.ok(reportService.generateComplianceReport(startDate, endDate));
    }

    private void assertCanViewUserReport(CustomUserDetails caller, Long targetUserId) {
        if (targetUserId == null) {
            throw new AccessDeniedException("Invalid userId");
        }
        membershipAccess.find(targetUserId, TenantContext.require().companyId())
                .orElseThrow(() -> new AccessDeniedException("You cannot access this user's report"));
        if (targetUserId.equals(caller.getId())) {
            return;
        }
        if (caller.hasRole("HR_ADMIN")) {
            return;
        }
        if (caller.hasRole("MANAGER") && membershipAccess.isDirectManagerOf(caller.getId(), targetUserId)) {
            return;
        }
        throw new AccessDeniedException("You cannot access this user's report");
    }

    private static byte[] payrollToCsv(PayrollReportDto report) {
        StringBuilder sb = new StringBuilder();
        sb.append("employeeId,name,regularHours,overtimeHours,totalHours\n");
        report.getEmployees().forEach(e -> {
            sb.append(e.getEmployeeId() == null ? "" : e.getEmployeeId()).append(',');
            sb.append(csvEsc(e.getName())).append(',');
            sb.append(e.getRegularHours() == null ? "" : e.getRegularHours().toPlainString()).append(',');
            sb.append(e.getOvertimeHours() == null ? "" : e.getOvertimeHours().toPlainString()).append(',');
            sb.append(e.getTotalHours() == null ? "" : e.getTotalHours().toPlainString()).append('\n');
        });
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String csvEsc(String s) {
        if (s == null) {
            return "";
        }
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private static void requirePayrollJsonFormat(String format) {
        String f = format == null ? "json" : format.trim();
        if ("json".equalsIgnoreCase(f)) {
            return;
        }
        throw new IllegalArgumentException("format must be json or csv");
    }
}

