package com.example.employeetimetracking.service;

import com.example.employeetimetracking.dto.response.EmployeeTimeReportDto;
import com.example.employeetimetracking.dto.response.ComplianceEntitlementIssueDto;
import com.example.employeetimetracking.dto.response.ComplianceReportDto;
import com.example.employeetimetracking.dto.response.DepartmentUtilizationItemDto;
import com.example.employeetimetracking.dto.response.DepartmentUtilizationReportDto;
import com.example.employeetimetracking.dto.response.LeaveBalanceReportDto;
import com.example.employeetimetracking.dto.response.LeaveBalanceReportItemDto;
import com.example.employeetimetracking.dto.response.PayrollEmployeeHoursDto;
import com.example.employeetimetracking.dto.response.PayrollReportDto;
import com.example.employeetimetracking.dto.response.ProjectHoursItemDto;
import com.example.employeetimetracking.dto.response.ProjectHoursReportDto;
import com.example.employeetimetracking.dto.response.TeamLeaveReportDto;
import com.example.employeetimetracking.dto.response.TeamLeaveRequestItemDto;
import com.example.employeetimetracking.dto.response.TimeSummaryItemDto;
import com.example.employeetimetracking.exception.InvalidDateRangeException;
import com.example.employeetimetracking.exception.InvalidUserException;
import com.example.employeetimetracking.model.entities.LeaveBalance;
import com.example.employeetimetracking.model.entities.LeaveRequest;
import com.example.employeetimetracking.model.entities.TimeEntry;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.entities.CompanyMembership;
import com.example.employeetimetracking.model.entities.Department;
import com.example.employeetimetracking.model.entities.LeaveType;
import com.example.employeetimetracking.model.enums.MembershipStatus;
import com.example.employeetimetracking.model.enums.Status;
import com.example.employeetimetracking.repository.CompanyMembershipRepository;
import com.example.employeetimetracking.repository.LeaveBalanceRepository;
import com.example.employeetimetracking.repository.LeaveRequestRepository;
import com.example.employeetimetracking.repository.TimeEntryRepository;
import com.example.employeetimetracking.repository.LeaveTypeRepository;
import com.example.employeetimetracking.specification.TimeEntrySpecification;
import com.example.employeetimetracking.tenant.MembershipAccess;
import com.example.employeetimetracking.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class ReportService {
    private final TimeEntryRepository timeEntryRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final CompanyMembershipRepository companyMembershipRepository;
    private final MembershipAccess membershipAccess;

    @Value("${reports.payroll.overtime.daily-hours:8.0}")
    private BigDecimal dailyOvertimeThresholdHours;

    @Value("${reports.payroll.overtime.weekly-hours:40.0}")
    private BigDecimal weeklyOvertimeThresholdHours;

    @Value("${reports.max-range-days:366}")
    private int maxRangeDays;

    @Autowired
    public ReportService(TimeEntryRepository timeEntryRepository,
                         LeaveRequestRepository leaveRequestRepository,
                         LeaveBalanceRepository leaveBalanceRepository,
                         LeaveTypeRepository leaveTypeRepository,
                         CompanyMembershipRepository companyMembershipRepository,
                         MembershipAccess membershipAccess) {
        this.timeEntryRepository = timeEntryRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.companyMembershipRepository = companyMembershipRepository;
        this.membershipAccess = membershipAccess;
    }

    public EmployeeTimeReportDto generateEmployeeTimeReport(Long userId, LocalDate startDate, LocalDate endDate) {
        if (userId == null) {
            throw new InvalidUserException("userId is required");
        }
        requireRange(startDate, endDate);

        List<TimeEntry> entries = timeEntryRepository.findByCompanyIdAndUserIdAndEntryDateBetweenAndStatus(
                currentCompanyId(), userId, startDate, endDate, Status.APPROVED
        );

        BigDecimal totalHours = entries.stream()
                .map(this::hours)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        Map<String, BigDecimal> daily = entries.stream()
                .filter(te -> te.getEntryDate() != null)
                .collect(Collectors.groupingBy(
                        te -> te.getEntryDate().toString(),
                        Collectors.reducing(BigDecimal.ZERO, this::hours, BigDecimal::add)
                ));

        Map<String, BigDecimal> byProject = entries.stream()
                .filter(te -> te.getProject() != null && te.getProject().getProjectCode() != null)
                .collect(Collectors.groupingBy(
                        te -> te.getProject().getProjectCode(),
                        Collectors.reducing(BigDecimal.ZERO, this::hours, BigDecimal::add)
                ));

        int daysWithEntries = daily.size();
        BigDecimal averageHoursPerDay = daysWithEntries == 0
                ? BigDecimal.ZERO
                : totalHours.divide(BigDecimal.valueOf(daysWithEntries), 2, RoundingMode.HALF_UP);

        List<TimeSummaryItemDto> dailyHours = daily.entrySet().stream()
                .map(e -> new TimeSummaryItemDto(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(TimeSummaryItemDto::getKey))
                .toList();

        List<TimeSummaryItemDto> projectBreakdown = byProject.entrySet().stream()
                .map(e -> new TimeSummaryItemDto(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(TimeSummaryItemDto::getTotalHours).reversed())
                .toList();

        return new EmployeeTimeReportDto(
                userId,
                startDate,
                endDate,
                totalHours,
                averageHoursPerDay,
                daysWithEntries,
                entries.size(),
                dailyHours,
                projectBreakdown
        );
    }

    public TeamLeaveReportDto generateTeamLeaveReport(Long callerId, boolean hr, LocalDate startDate, LocalDate endDate) {
        if (callerId == null) {
            throw new InvalidUserException("managerId is required");
        }
        requireRange(startDate, endDate);

        List<Status> statuses = List.of(Status.APPROVED, Status.CANCELLATION_PENDING);
        List<LeaveRequest> requests = hr
                ? leaveRequestRepository.findByStatusInAndDateRangeOverlapAllForCompany(
                        statuses, startDate, endDate, currentCompanyId())
                : leaveRequestRepository.findByStatusInAndDateRangeOverlapForCompany(
                        callerId, statuses, startDate, endDate, currentCompanyId());

        BigDecimal totalDays = requests.stream()
                .map(lr -> lr.getTotalDays() == null ? BigDecimal.ZERO : lr.getTotalDays())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<TeamLeaveRequestItemDto> requestItems = requests.stream()
                .sorted(Comparator.comparing(LeaveRequest::getStartDate).thenComparing(LeaveRequest::getId, Comparator.nullsLast(Long::compareTo)))
                .map(lr -> new TeamLeaveRequestItemDto(
                        lr.getId(),
                        lr.getUser() != null ? lr.getUser().getId() : null,
                        lr.getUser() != null ? (lr.getUser().getFirstName() + " " + lr.getUser().getLastName()) : null,
                        lr.getLeaveType() != null ? lr.getLeaveType().getTypeName() : null,
                        lr.getStartDate(),
                        lr.getEndDate(),
                        lr.getTotalDays()
                ))
                .toList();

        return new TeamLeaveReportDto(
                callerId,
                startDate,
                endDate,
                requests.size(),
                totalDays,
                requestItems
        );
    }

    public PayrollReportDto generatePayrollReport(LocalDate startDate, LocalDate endDate) {
        requireRange(startDate, endDate);
        if (dailyOvertimeThresholdHours == null || dailyOvertimeThresholdHours.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidDateRangeException("Daily overtime threshold must be >= 0");
        }
        if (weeklyOvertimeThresholdHours == null || weeklyOvertimeThresholdHours.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidDateRangeException("Weekly overtime threshold must be >= 0");
        }

        Specification<TimeEntry> spec = scoped(TimeEntrySpecification.hasStatus(Status.APPROVED)
                .and(TimeEntrySpecification.afterDate(startDate))
                .and(TimeEntrySpecification.beforeDate(endDate)));

        List<TimeEntry> entries = timeEntryRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "user.id", "entryDate", "id"));

        Map<Long, List<TimeEntry>> byUser = entries.stream()
                .filter(te -> te.getUser() != null && te.getUser().getId() != null)
                .collect(Collectors.groupingBy(te -> te.getUser().getId()));

        List<PayrollEmployeeHoursDto> employees = companyMembershipRepository
                .findByCompanyIdAndStatusFetchUser(currentCompanyId(), MembershipStatus.ACTIVE)
                .stream()
                .map(CompanyMembership::getUser)
                .filter(u -> u != null && u.getId() != null)
                .map(u -> toPayrollEmployeeHours(
                        u.getId(),
                        byUser.getOrDefault(u.getId(), List.of()),
                        u))
                .sorted(Comparator.comparing(PayrollEmployeeHoursDto::getEmployeeId))
                .toList();

        BigDecimal totalRegular = employees.stream()
                .map(PayrollEmployeeHoursDto::getRegularHours)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalOvertime = employees.stream()
                .map(PayrollEmployeeHoursDto::getOvertimeHours)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal total = totalRegular.add(totalOvertime);

        return new PayrollReportDto(
                startDate,
                endDate,
                dailyOvertimeThresholdHours,
                weeklyOvertimeThresholdHours,
                totalRegular,
                totalOvertime,
                total,
                employees
        );
    }

    /**
     * Overtime for each ISO week is max(sum of daily overtime, weekly overtime).
     * Daily overtime is hours above the configured daily threshold; weekly overtime is week total above the weekly threshold.
     */
    private PayrollEmployeeHoursDto toPayrollEmployeeHours(Long userId, List<TimeEntry> entries, User user) {
        String name = user == null ? null : (user.getFirstName() + " " + user.getLastName());
        if (entries == null) {
            entries = List.of();
        }

        // weekKey -> (date -> totalHours)
        Map<String, Map<LocalDate, BigDecimal>> weekToDayTotals = new TreeMap<>();
        WeekFields wf = WeekFields.ISO;

        for (TimeEntry te : entries) {
            if (te.getEntryDate() == null) {
                continue;
            }
            BigDecimal h = hours(te);
            int week = te.getEntryDate().get(wf.weekOfWeekBasedYear());
            int year = te.getEntryDate().get(wf.weekBasedYear());
            String weekKey = year + "-W" + String.format("%02d", week);
            weekToDayTotals
                    .computeIfAbsent(weekKey, k -> new HashMap<>())
                    .merge(te.getEntryDate(), h, BigDecimal::add);
        }

        BigDecimal regularTotal = BigDecimal.ZERO;
        BigDecimal overtimeTotal = BigDecimal.ZERO;

        for (Map<LocalDate, BigDecimal> dayTotals : weekToDayTotals.values()) {
            BigDecimal weekTotal = dayTotals.values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal dailyOvertimeSum = BigDecimal.ZERO;
            for (BigDecimal dayHours : dayTotals.values()) {
                BigDecimal ot = dayHours.subtract(dailyOvertimeThresholdHours);
                if (ot.compareTo(BigDecimal.ZERO) > 0) {
                    dailyOvertimeSum = dailyOvertimeSum.add(ot);
                }
            }

            BigDecimal weeklyOvertime = weekTotal.subtract(weeklyOvertimeThresholdHours);
            if (weeklyOvertime.compareTo(BigDecimal.ZERO) < 0) {
                weeklyOvertime = BigDecimal.ZERO;
            }

            // Overtime for the week is whichever rule yields more overtime for that week.
            BigDecimal overtimeWeek = dailyOvertimeSum.max(weeklyOvertime);
            BigDecimal regularWeek = weekTotal.subtract(overtimeWeek);

            overtimeTotal = overtimeTotal.add(overtimeWeek);
            regularTotal = regularTotal.add(regularWeek);
        }

        regularTotal = regularTotal.setScale(2, RoundingMode.HALF_UP);
        overtimeTotal = overtimeTotal.setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = regularTotal.add(overtimeTotal).setScale(2, RoundingMode.HALF_UP);

        return new PayrollEmployeeHoursDto(userId, name, regularTotal, overtimeTotal, total);
    }

    public LeaveBalanceReportDto generateLeaveBalanceReport(Integer year, Long departmentId) {
        int y = year == null ? LocalDate.now().getYear() : year;
        if (y < 2000 || y > 3000) {
            throw new InvalidDateRangeException("year is invalid");
        }

        List<LeaveBalance> balances = departmentId == null
                ? leaveBalanceRepository.findAllLeaveBalancesForYearAndCompany(y, currentCompanyId())
                : leaveBalanceRepository.findLeaveBalancesForYearAndDepartmentAndCompany(
                        y, departmentId, currentCompanyId());
        // Unknown or other-tenant departmentId matches no membership in this company; result is empty.

        Map<Long, CompanyMembership> memberships = membershipsFor(balances.stream()
                .map(lb -> lb.getUser() == null ? null : lb.getUser().getId())
                .filter(Objects::nonNull)
                .toList());

        List<LeaveBalanceReportItemDto> items = balances.stream()
                .map(lb -> toLeaveBalanceItem(lb, y, departmentOf(lb.getUser(), memberships)))
                .sorted(Comparator
                        .comparing(LeaveBalanceReportItemDto::getEmployeeName, Comparator.nullsLast(String::compareTo))
                        .thenComparing(LeaveBalanceReportItemDto::getLeaveTypeName, Comparator.nullsLast(String::compareTo)))
                .toList();

        int employeesCount = (int) items.stream()
                .map(LeaveBalanceReportItemDto::getEmployeeId)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        return new LeaveBalanceReportDto(
                y,
                departmentId,
                employeesCount,
                items.size(),
                items
        );
    }

    private LeaveBalanceReportItemDto toLeaveBalanceItem(LeaveBalance lb, int year, Department department) {
        BigDecimal allocation = null;
        if (lb.getLeaveType() != null && lb.getLeaveType().getLeavePolicy() != null) {
            allocation = lb.getLeaveType().getLeavePolicy().getAnnualAllocation();
        }

        return new LeaveBalanceReportItemDto(
                lb.getUser() != null ? lb.getUser().getId() : null,
                lb.getUser() != null ? (lb.getUser().getFirstName() + " " + lb.getUser().getLastName()) : null,
                department == null ? null : department.getId(),
                department == null ? null : department.getDepartmentCode(),
                department == null ? null : department.getDepartmentName(),
                lb.getLeaveType() != null ? lb.getLeaveType().getId() : null,
                lb.getLeaveType() != null ? lb.getLeaveType().getTypeName() : null,
                year,
                allocation,
                lb.getCurrentBalance()
        );
    }

    public DepartmentUtilizationReportDto generateDepartmentUtilizationReport(LocalDate startDate, LocalDate endDate) {
        requireRange(startDate, endDate);

        List<TimeEntry> entries = timeEntryRepository.findForDepartmentUtilizationByCompany(
                currentCompanyId(), Status.APPROVED, startDate, endDate);
        Map<Long, CompanyMembership> memberships = membershipsFor(entries.stream()
                .map(te -> te.getUser() == null ? null : te.getUser().getId())
                .filter(Objects::nonNull)
                .toList());

        // departmentId -> entries
        Map<Long, List<TimeEntry>> byDepartmentId = entries.stream()
                .filter(te -> departmentOf(te.getUser(), memberships) != null)
                .collect(Collectors.groupingBy(te -> departmentOf(te.getUser(), memberships).getId()));

        List<DepartmentUtilizationItemDto> departments = byDepartmentId.values().stream()
                .map(list -> toDepartmentUtilizationItem(list, memberships))
                .sorted(Comparator.comparing(DepartmentUtilizationItemDto::getTotalHours).reversed())
                .toList();

        BigDecimal totalHours = departments.stream()
                .map(DepartmentUtilizationItemDto::getTotalHours)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int employeesCount = (int) entries.stream()
                .map(te -> te.getUser() != null ? te.getUser().getId() : null)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        return new DepartmentUtilizationReportDto(
                startDate,
                endDate,
                totalHours.setScale(2, RoundingMode.HALF_UP),
                departments.size(),
                employeesCount,
                departments
        );
    }

    private DepartmentUtilizationItemDto toDepartmentUtilizationItem(List<TimeEntry> entries, Map<Long, CompanyMembership> memberships) {
        if (entries == null || entries.isEmpty()) {
            return new DepartmentUtilizationItemDto(null, null, null, BigDecimal.ZERO, 0);
        }

        var dept = departmentOf(entries.get(0).getUser(), memberships);
        Long deptId = dept == null ? null : dept.getId();
        String deptCode = dept == null ? null : dept.getDepartmentCode();
        String deptName = dept == null ? null : dept.getDepartmentName();

        BigDecimal totalHours = entries.stream()
                .map(this::hours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int employeesCount = (int) entries.stream()
                .map(te -> te.getUser() != null ? te.getUser().getId() : null)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        return new DepartmentUtilizationItemDto(
                deptId,
                deptCode,
                deptName,
                totalHours.setScale(2, RoundingMode.HALF_UP),
                employeesCount
        );
    }

    public ProjectHoursReportDto generateProjectHours(LocalDate startDate, LocalDate endDate) {
        requireRange(startDate, endDate);

        List<TimeEntry> entries = timeEntryRepository.findForProjectHoursByCompany(
                currentCompanyId(), Status.APPROVED, startDate, endDate);

        Map<Long, List<TimeEntry>> byProjectId = entries.stream()
                .filter(te -> te.getProject() != null && te.getProject().getId() != null)
                .collect(Collectors.groupingBy(te -> te.getProject().getId()));

        List<ProjectHoursItemDto> projects = byProjectId.values().stream()
                .map(this::toProjectHoursItem)
                .sorted(Comparator.comparing(ProjectHoursItemDto::getTotalHours, Comparator.nullsLast(BigDecimal::compareTo)).reversed())
                .toList();

        int employeesCount = (int) entries.stream()
                .map(te -> te.getUser() != null ? te.getUser().getId() : null)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        BigDecimal totalHours = projects.stream()
                .map(ProjectHoursItemDto::getTotalHours)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return new ProjectHoursReportDto(
                startDate,
                endDate,
                projects.size(),
                employeesCount,
                totalHours,
                projects
        );
    }

    private ProjectHoursItemDto toProjectHoursItem(List<TimeEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return new ProjectHoursItemDto(null, null, null, BigDecimal.ZERO, 0);
        }

        var p = entries.get(0).getProject();
        Long projectId = p.getId();
        String projectCode = p.getProjectCode();
        String projectName = p.getProjectName();

        BigDecimal total = entries.stream()
                .map(this::hours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int employeesCount = (int) entries.stream()
                .map(te -> te.getUser() != null ? te.getUser().getId() : null)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        return new ProjectHoursItemDto(
                projectId,
                projectCode,
                projectName,
                total.setScale(2, RoundingMode.HALF_UP),
                employeesCount
        );
    }

    public ComplianceReportDto generateComplianceReport(LocalDate startDate, LocalDate endDate) {
        requireRange(startDate, endDate);

        int year = startDate.getYear();
        if (startDate.getYear() != endDate.getYear()) {
            throw new InvalidDateRangeException("Compliance report must be within a single calendar year");
        }

        Long companyId = currentCompanyId();
        List<User> activeUsers = companyMembershipRepository
                .findByCompanyIdAndStatusFetchUser(companyId, MembershipStatus.ACTIVE)
                .stream()
                .map(CompanyMembership::getUser)
                .filter(u -> u != null && Boolean.TRUE.equals(u.getIsActive()))
                .toList();
        List<LeaveType> activeLeaveTypes = leaveTypeRepository.findByCompanyIdAndIsActive(companyId, true);
        List<LeaveBalance> yearBalances = leaveBalanceRepository.findAllLeaveBalancesForYearAndCompany(year, companyId);

        Map<String, LeaveBalance> balanceByKey = yearBalances.stream()
                .filter(lb -> lb.getUser() != null && lb.getUser().getId() != null && lb.getLeaveType() != null && lb.getLeaveType().getId() != null)
                .collect(Collectors.toMap(
                        lb -> lb.getUser().getId() + ":" + lb.getLeaveType().getId(),
                        lb -> lb,
                        (a, b) -> a
                ));

        List<ComplianceEntitlementIssueDto> issues = new ArrayList<>();
        for (User u : activeUsers) {
            for (LeaveType lt : activeLeaveTypes) {
                String key = u.getId() + ":" + lt.getId();
                if (!balanceByKey.containsKey(key)) {
                    issues.add(new ComplianceEntitlementIssueDto(
                            u.getId(),
                            u.getFirstName() + " " + u.getLastName(),
                            lt.getId(),
                            lt.getTypeName(),
                            year,
                            "Missing leave balance record for employee/year/leave type"
                    ));
                }
            }
        }

        return new ComplianceReportDto(
                startDate,
                endDate,
                year,
                issues.size(),
                // Cap payload size; entitlementIssuesCount is the full count.
                issues.stream().limit(200).toList()
        );
    }

    private Map<Long, CompanyMembership> membershipsFor(java.util.Collection<Long> userIds) {
        return membershipAccess.mapByUserId(currentCompanyId(), userIds);
    }

    private Department departmentOf(User user, Map<Long, CompanyMembership> memberships) {
        if (user == null || user.getId() == null || memberships == null) {
            return null;
        }
        CompanyMembership membership = memberships.get(user.getId());
        return membership == null ? null : membership.getDepartment();
    }

    private BigDecimal hours(TimeEntry te) {
        if (te == null || te.getTotalHours() == null) {
            return BigDecimal.ZERO;
        }
        return te.getTotalHours();
    }

    private void requireRange(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new InvalidDateRangeException("startDate and endDate are required");
        }
        if (start.isAfter(end)) {
            throw new InvalidDateRangeException("startDate cannot be after endDate");
        }
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        if (days > maxRangeDays) {
            throw new InvalidDateRangeException("Date range cannot exceed " + maxRangeDays + " days");
        }
    }

    private static Long currentCompanyId() {
        return TenantContext.require().companyId();
    }

    private static Specification<TimeEntry> scoped(Specification<TimeEntry> extra) {
        return Specification.where(TimeEntrySpecification.belongsToCurrentCompany()).and(extra);
    }
}

