package com.example.employeetimetracking.service;

import com.example.employeetimetracking.dto.response.EmployeeTimeReportDto;
import com.example.employeetimetracking.dto.response.AbsencePatternEmployeeDto;
import com.example.employeetimetracking.dto.response.AbsencePatternsReportDto;
import com.example.employeetimetracking.dto.response.DepartmentUtilizationItemDto;
import com.example.employeetimetracking.dto.response.DepartmentUtilizationReportDto;
import com.example.employeetimetracking.dto.response.LeaveBalanceReportDto;
import com.example.employeetimetracking.dto.response.LeaveBalanceReportItemDto;
import com.example.employeetimetracking.dto.response.PayrollEmployeeHoursDto;
import com.example.employeetimetracking.dto.response.PayrollReportDto;
import com.example.employeetimetracking.dto.response.TeamLeaveReportDto;
import com.example.employeetimetracking.dto.response.TeamLeaveRequestItemDto;
import com.example.employeetimetracking.dto.response.TimeSummaryItemDto;
import com.example.employeetimetracking.exception.InvalidTimeEntryException;
import com.example.employeetimetracking.model.entities.LeaveBalance;
import com.example.employeetimetracking.model.entities.LeaveRequest;
import com.example.employeetimetracking.model.entities.TimeEntry;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.Status;
import com.example.employeetimetracking.repository.LeaveBalanceRepository;
import com.example.employeetimetracking.repository.LeaveRequestRepository;
import com.example.employeetimetracking.repository.TimeEntryRepository;
import com.example.employeetimetracking.specification.TimeEntrySpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
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

    @Value("${reports.payroll.overtime.daily-hours:8.0}")
    private BigDecimal dailyOvertimeThresholdHours;

    @Value("${reports.payroll.overtime.weekly-hours:40.0}")
    private BigDecimal weeklyOvertimeThresholdHours;

    @Autowired
    public ReportService(TimeEntryRepository timeEntryRepository,
                         LeaveRequestRepository leaveRequestRepository,
                         LeaveBalanceRepository leaveBalanceRepository) {
        this.timeEntryRepository = timeEntryRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
    }

    public EmployeeTimeReportDto generateEmployeeTimeReport(Long userId, LocalDate startDate, LocalDate endDate) {
        if (userId == null) {
            throw new InvalidTimeEntryException("userId is required");
        }
        if (startDate == null || endDate == null) {
            throw new InvalidTimeEntryException("startDate and endDate are required");
        }
        if (startDate.isAfter(endDate)) {
            throw new InvalidTimeEntryException("startDate cannot be after endDate");
        }

        List<TimeEntry> entries = timeEntryRepository.findByUserIdAndEntryDateBetweenAndStatus(
                userId, startDate, endDate, Status.APPROVED
        );

        BigDecimal totalHours = entries.stream()
                .map(TimeEntry::getTotalHours)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> daily = entries.stream()
                .filter(te -> te.getEntryDate() != null)
                .collect(Collectors.groupingBy(
                        te -> te.getEntryDate().toString(),
                        Collectors.reducing(BigDecimal.ZERO, TimeEntry::getTotalHours, BigDecimal::add)
                ));

        Map<String, BigDecimal> byProject = entries.stream()
                .filter(te -> te.getProject() != null && te.getProject().getProjectCode() != null)
                .collect(Collectors.groupingBy(
                        te -> te.getProject().getProjectCode(),
                        Collectors.reducing(BigDecimal.ZERO, TimeEntry::getTotalHours, BigDecimal::add)
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

    public TeamLeaveReportDto generateTeamLeaveReport(Long managerId, LocalDate startDate, LocalDate endDate) {
        if (managerId == null) {
            throw new InvalidTimeEntryException("managerId is required");
        }
        if (startDate == null || endDate == null) {
            throw new InvalidTimeEntryException("startDate and endDate are required");
        }
        if (startDate.isAfter(endDate)) {
            throw new InvalidTimeEntryException("startDate cannot be after endDate");
        }

        List<LeaveRequest> requests = leaveRequestRepository.findByStatusAndDateRangeOverlap(
                managerId, Status.APPROVED, startDate, endDate
        );

        BigDecimal totalDays = requests.stream()
                .map(LeaveRequest::getTotalDays)
                .filter(Objects::nonNull)
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

        Map<String, BigDecimal> byEmployee = requests.stream()
                .filter(lr -> lr.getUser() != null)
                .collect(Collectors.groupingBy(
                        lr -> lr.getUser().getFirstName() + " " + lr.getUser().getLastName(),
                        Collectors.reducing(BigDecimal.ZERO, LeaveRequest::getTotalDays, BigDecimal::add)
                ));

        Map<String, BigDecimal> byLeaveType = requests.stream()
                .filter(lr -> lr.getLeaveType() != null && lr.getLeaveType().getTypeName() != null)
                .collect(Collectors.groupingBy(
                        lr -> lr.getLeaveType().getTypeName(),
                        Collectors.reducing(BigDecimal.ZERO, LeaveRequest::getTotalDays, BigDecimal::add)
                ));

        List<TimeSummaryItemDto> totalDaysByEmployee = byEmployee.entrySet().stream()
                .map(e -> new TimeSummaryItemDto(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(TimeSummaryItemDto::getTotalHours).reversed())
                .toList();

        List<TimeSummaryItemDto> totalDaysByLeaveType = byLeaveType.entrySet().stream()
                .map(e -> new TimeSummaryItemDto(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(TimeSummaryItemDto::getTotalHours).reversed())
                .toList();

        List<TeamLeaveRequestItemDto> upcoming = leaveRequestRepository
                .findByUserManagerIdAndStatusAndStartDateAfterOrderByStartDateAsc(managerId, Status.APPROVED, LocalDate.now())
                .stream()
                .limit(10)
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
                managerId,
                startDate,
                endDate,
                requests.size(),
                totalDays,
                requestItems,
                totalDaysByEmployee,
                totalDaysByLeaveType,
                upcoming
        );
    }

    public PayrollReportDto generatePayrollReport(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new InvalidTimeEntryException("startDate and endDate are required");
        }
        if (startDate.isAfter(endDate)) {
            throw new InvalidTimeEntryException("startDate cannot be after endDate");
        }
        if (dailyOvertimeThresholdHours == null || dailyOvertimeThresholdHours.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidTimeEntryException("Daily overtime threshold must be >= 0");
        }
        if (weeklyOvertimeThresholdHours == null || weeklyOvertimeThresholdHours.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidTimeEntryException("Weekly overtime threshold must be >= 0");
        }

        Specification<TimeEntry> spec = Specification
                .where(TimeEntrySpecification.hasStatus(Status.APPROVED))
                .and(TimeEntrySpecification.afterDate(startDate))
                .and(TimeEntrySpecification.beforeDate(endDate));

        List<TimeEntry> entries = timeEntryRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "user.id", "entryDate", "id"));

        Map<Long, List<TimeEntry>> byUser = entries.stream()
                .filter(te -> te.getUser() != null && te.getUser().getId() != null)
                .collect(Collectors.groupingBy(te -> te.getUser().getId()));

        List<PayrollEmployeeHoursDto> employees = byUser.entrySet().stream()
                .map(e -> toPayrollEmployeeHours(e.getKey(), e.getValue()))
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

    private PayrollEmployeeHoursDto toPayrollEmployeeHours(Long userId, List<TimeEntry> entries) {
        User u = entries.isEmpty() ? null : entries.get(0).getUser();
        String name = u == null ? null : (u.getFirstName() + " " + u.getLastName());

        // weekKey -> (date -> totalHours)
        Map<String, Map<LocalDate, BigDecimal>> weekToDayTotals = new TreeMap<>();
        WeekFields wf = WeekFields.ISO;

        for (TimeEntry te : entries) {
            if (te.getEntryDate() == null) {
                continue;
            }
            BigDecimal h = te.getTotalHours() == null ? BigDecimal.ZERO : te.getTotalHours();
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
            throw new InvalidTimeEntryException("year is invalid");
        }

        List<LeaveBalance> balances = departmentId == null
                ? leaveBalanceRepository.findAllLeaveBalancesForYear(y)
                : leaveBalanceRepository.findLeaveBalancesForYearAndDepartment(y, departmentId);

        List<LeaveBalanceReportItemDto> items = balances.stream()
                .map(lb -> toLeaveBalanceItem(lb, y))
                .sorted(Comparator
                        .comparing(LeaveBalanceReportItemDto::getEmployeeName, Comparator.nullsLast(String::compareTo))
                        .thenComparing(LeaveBalanceReportItemDto::getLeaveTypeName, Comparator.nullsLast(String::compareTo)))
                .toList();

        BigDecimal total = items.stream()
                .map(LeaveBalanceReportItemDto::getCurrentBalance)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avg = items.isEmpty()
                ? BigDecimal.ZERO
                : total.divide(BigDecimal.valueOf(items.size()), 2, RoundingMode.HALF_UP);

        List<LeaveBalanceReportItemDto> lowest = items.stream()
                .sorted(Comparator.comparing(LeaveBalanceReportItemDto::getCurrentBalance, Comparator.nullsLast(BigDecimal::compareTo)))
                .limit(10)
                .toList();

        List<LeaveBalanceReportItemDto> highest = items.stream()
                .sorted(Comparator.comparing(LeaveBalanceReportItemDto::getCurrentBalance, Comparator.nullsLast(BigDecimal::compareTo)).reversed())
                .limit(10)
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
                items,
                lowest,
                highest,
                avg
        );
    }

    private LeaveBalanceReportItemDto toLeaveBalanceItem(LeaveBalance lb, int year) {
        BigDecimal allocation = null;
        if (lb.getLeaveType() != null && lb.getLeaveType().getLeavePolicy() != null) {
            allocation = lb.getLeaveType().getLeavePolicy().getAnnualAllocation();
        }
        BigDecimal current = lb.getCurrentBalance();

        BigDecimal pct = null;
        if (allocation != null && allocation.compareTo(BigDecimal.ZERO) > 0 && current != null) {
            pct = current
                    .multiply(BigDecimal.valueOf(100))
                    .divide(allocation, 2, RoundingMode.HALF_UP);
        }

        return new LeaveBalanceReportItemDto(
                lb.getUser() != null ? lb.getUser().getId() : null,
                lb.getUser() != null ? (lb.getUser().getFirstName() + " " + lb.getUser().getLastName()) : null,
                lb.getUser() != null && lb.getUser().getDepartment() != null ? lb.getUser().getDepartment().getId() : null,
                lb.getUser() != null && lb.getUser().getDepartment() != null ? lb.getUser().getDepartment().getDepartmentCode() : null,
                lb.getUser() != null && lb.getUser().getDepartment() != null ? lb.getUser().getDepartment().getDepartmentName() : null,
                lb.getLeaveType() != null ? lb.getLeaveType().getId() : null,
                lb.getLeaveType() != null ? lb.getLeaveType().getTypeName() : null,
                year,
                allocation,
                current,
                pct
        );
    }

    public DepartmentUtilizationReportDto generateDepartmentUtilizationReport(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new InvalidTimeEntryException("startDate and endDate are required");
        }
        if (startDate.isAfter(endDate)) {
            throw new InvalidTimeEntryException("startDate cannot be after endDate");
        }

        List<TimeEntry> entries = timeEntryRepository.findForDepartmentUtilization(Status.APPROVED, startDate, endDate);

        // departmentId -> entries
        Map<Long, List<TimeEntry>> byDepartmentId = entries.stream()
                .filter(te -> te.getUser() != null && te.getUser().getDepartment() != null && te.getUser().getDepartment().getId() != null)
                .collect(Collectors.groupingBy(te -> te.getUser().getDepartment().getId()));

        List<DepartmentUtilizationItemDto> departments = byDepartmentId.values().stream()
                .map(this::toDepartmentUtilizationItem)
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

    private DepartmentUtilizationItemDto toDepartmentUtilizationItem(List<TimeEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return new DepartmentUtilizationItemDto(null, null, null, BigDecimal.ZERO, 0, BigDecimal.ZERO, List.of());
        }

        var dept = entries.get(0).getUser().getDepartment();
        Long deptId = dept.getId();
        String deptCode = dept.getDepartmentCode();
        String deptName = dept.getDepartmentName();

        BigDecimal totalHours = entries.stream()
                .map(TimeEntry::getTotalHours)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int employeesCount = (int) entries.stream()
                .map(te -> te.getUser() != null ? te.getUser().getId() : null)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        BigDecimal avg = employeesCount == 0
                ? BigDecimal.ZERO
                : totalHours.divide(BigDecimal.valueOf(employeesCount), 2, RoundingMode.HALF_UP);

        Map<String, BigDecimal> projectHours = entries.stream()
                .filter(te -> te.getProject() != null && te.getProject().getProjectCode() != null)
                .collect(Collectors.groupingBy(
                        te -> te.getProject().getProjectCode(),
                        Collectors.reducing(BigDecimal.ZERO, TimeEntry::getTotalHours, BigDecimal::add)
                ));

        List<TimeSummaryItemDto> projectDistribution = projectHours.entrySet().stream()
                .map(e -> new TimeSummaryItemDto(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(TimeSummaryItemDto::getTotalHours).reversed())
                .toList();

        return new DepartmentUtilizationItemDto(
                deptId,
                deptCode,
                deptName,
                totalHours.setScale(2, RoundingMode.HALF_UP),
                employeesCount,
                avg,
                projectDistribution
        );
    }

    public AbsencePatternsReportDto generateAbsencePatternsReport(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new InvalidTimeEntryException("startDate and endDate are required");
        }
        if (startDate.isAfter(endDate)) {
            throw new InvalidTimeEntryException("startDate cannot be after endDate");
        }

        List<LeaveRequest> requests = leaveRequestRepository.findByStatusAndDateRangeOverlapAll(
                Status.APPROVED, startDate, endDate
        );

        Map<Long, List<LeaveRequest>> byUser = requests.stream()
                .filter(lr -> lr.getUser() != null && lr.getUser().getId() != null)
                .collect(Collectors.groupingBy(lr -> lr.getUser().getId()));

        List<AbsencePatternEmployeeDto> employees = byUser.entrySet().stream()
                .map(e -> buildAbsencePatternEmployee(e.getValue()))
                .sorted(Comparator
                        .comparing(AbsencePatternEmployeeDto::getFlagged, Comparator.nullsLast(Boolean::compareTo)).reversed()
                        .thenComparing(AbsencePatternEmployeeDto::getTotalLeaveDays, Comparator.nullsLast(BigDecimal::compareTo)).reversed()
                        .thenComparing(AbsencePatternEmployeeDto::getEmployeeName, Comparator.nullsLast(String::compareTo)))
                .toList();

        int flaggedCount = (int) employees.stream().filter(e -> Boolean.TRUE.equals(e.getFlagged())).count();

        return new AbsencePatternsReportDto(
                startDate,
                endDate,
                employees.size(),
                flaggedCount,
                employees
        );
    }

    private AbsencePatternEmployeeDto buildAbsencePatternEmployee(List<LeaveRequest> requests) {
        requests = requests.stream()
                .sorted(Comparator.comparing(LeaveRequest::getStartDate).thenComparing(LeaveRequest::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();

        User u = requests.isEmpty() ? null : requests.get(0).getUser();
        Long employeeId = u != null ? u.getId() : null;
        String name = u != null ? (u.getFirstName() + " " + u.getLastName()) : null;
        Long deptId = (u != null && u.getDepartment() != null) ? u.getDepartment().getId() : null;
        String deptCode = (u != null && u.getDepartment() != null) ? u.getDepartment().getDepartmentCode() : null;

        BigDecimal totalDays = requests.stream()
                .map(LeaveRequest::getTotalDays)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int mondayStarts = 0;
        int fridayStarts = 0;
        int sickCount = 0;
        int sickClusters = 0;
        LocalDate lastSickStart = null;

        for (LeaveRequest lr : requests) {
            if (lr.getStartDate() != null) {
                DayOfWeek dow = lr.getStartDate().getDayOfWeek();
                if (dow == DayOfWeek.MONDAY) mondayStarts++;
                if (dow == DayOfWeek.FRIDAY) fridayStarts++;
            }

            String leaveType = (lr.getLeaveType() != null && lr.getLeaveType().getTypeName() != null)
                    ? lr.getLeaveType().getTypeName().toLowerCase()
                    : "";
            boolean isSick = leaveType.contains("sick");
            if (isSick) {
                sickCount++;
                if (lr.getStartDate() != null && lastSickStart != null) {
                    long gapDays = java.time.temporal.ChronoUnit.DAYS.between(lastSickStart, lr.getStartDate());
                    if (gapDays >= 0 && gapDays <= 14) {
                        sickClusters++;
                    }
                }
                if (lr.getStartDate() != null) {
                    lastSickStart = lr.getStartDate();
                }
            }
        }

        int monFri = mondayStarts + fridayStarts;

        boolean flagged = false;
        String reason = null;

        // Heuristic 1: Monday/Friday-heavy starts (>=3 and >=50% of requests)
        if (requests.size() >= 3 && monFri * 2 >= requests.size()) {
            flagged = true;
            reason = "High number of Monday/Friday leave starts";
        }
        // Heuristic 2: clustered sick leaves
        if (!flagged && sickClusters >= 2) {
            flagged = true;
            reason = "Clustered sick leave requests (<=14 days apart)";
        }
        // Heuristic 3: unusually high usage in the range (>= 15 days)
        if (!flagged && totalDays.compareTo(BigDecimal.valueOf(15)) >= 0) {
            flagged = true;
            reason = "Unusually high leave usage in period";
        }

        return new AbsencePatternEmployeeDto(
                employeeId,
                name,
                deptId,
                deptCode,
                requests.size(),
                totalDays,
                mondayStarts,
                fridayStarts,
                monFri,
                sickCount,
                sickClusters,
                flagged,
                reason
        );
    }
}

