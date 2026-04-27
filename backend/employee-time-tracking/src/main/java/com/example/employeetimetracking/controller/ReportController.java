package com.example.employeetimetracking.controller;

import com.example.employeetimetracking.dto.response.DepartmentUtilizationReportDto;
import com.example.employeetimetracking.dto.response.EmployeeTimeReportDto;
import com.example.employeetimetracking.dto.response.LeaveBalanceReportDto;
import com.example.employeetimetracking.dto.response.PayrollReportDto;
import com.example.employeetimetracking.dto.response.TeamLeaveReportDto;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.security.CustomUserDetails;
import com.example.employeetimetracking.service.ReportService;
import com.example.employeetimetracking.service.UserService;
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
    private final UserService userService;

    @Autowired
    public ReportController(ReportService reportService, UserService userService) {
        this.reportService = reportService;
        this.userService = userService;
    }

    // Task 135
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

    // Task 136
    @PreAuthorize("hasAnyRole('MANAGER','HR_ADMIN')")
    @GetMapping("/team-leave")
    public ResponseEntity<TeamLeaveReportDto> teamLeave(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @AuthenticationPrincipal CustomUserDetails authenticatedUser
    ) {
        // report is always for the caller's team
        return ResponseEntity.ok(reportService.generateTeamLeaveReport(authenticatedUser.getId(), startDate, endDate));
    }

    // Task 137
    @PreAuthorize("hasRole('HR_ADMIN')")
    @GetMapping("/payroll")
    public ResponseEntity<?> payroll(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam(required = false, defaultValue = "json") String format
    ) {
        PayrollReportDto report = reportService.generatePayrollReport(startDate, endDate);
        if ("csv".equalsIgnoreCase(format)) {
            byte[] csv = payrollToCsv(report);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"payroll.csv\"")
                    .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                    .body(csv);
        }
        return ResponseEntity.ok(report);
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
            User me = userService.getById(authenticatedUser.getId());
            Long myDeptId = me.getDepartment() != null ? me.getDepartment().getId() : null;
            if (departmentId != null && myDeptId != null && !departmentId.equals(myDeptId)) {
                throw new AccessDeniedException("You can only access leave balances for your department");
            }
            departmentId = myDeptId; // managers default to their own department
        }
        return ResponseEntity.ok(reportService.generateLeaveBalanceReport(year, departmentId));
    }

    @PreAuthorize("hasAnyRole('MANAGER','HR_ADMIN')")
    @GetMapping("/department-utilization")
    public ResponseEntity<DepartmentUtilizationReportDto> departmentUtilization(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    ) {
        return ResponseEntity.ok(reportService.generateDepartmentUtilizationReport(startDate, endDate));
    }

    private void assertCanViewUserReport(CustomUserDetails caller, Long targetUserId) {
        if (targetUserId == null) {
            throw new AccessDeniedException("Invalid userId");
        }
        if (targetUserId.equals(caller.getId())) {
            return;
        }
        if (caller.hasRole("HR_ADMIN")) {
            return;
        }
        if (caller.hasRole("MANAGER")) {
            User target = userService.getById(targetUserId);
            if (target.getManager() != null && target.getManager().getId() != null
                    && target.getManager().getId().equals(caller.getId())) {
                return;
            }
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
}

