package com.example.employeetimetracking.service;

import com.example.employeetimetracking.dto.response.EmployeeTimeReportDto;
import com.example.employeetimetracking.dto.response.PayrollEmployeeHoursDto;
import com.example.employeetimetracking.dto.response.PayrollReportDto;
import com.example.employeetimetracking.dto.response.TeamLeaveReportDto;
import com.example.employeetimetracking.dto.response.TeamLeaveRequestItemDto;
import com.example.employeetimetracking.dto.response.TimeSummaryItemDto;
import com.example.employeetimetracking.exception.InvalidTimeEntryException;
import com.example.employeetimetracking.model.entities.LeaveRequest;
import com.example.employeetimetracking.model.entities.TimeEntry;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.Status;
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

    @Value("${reports.payroll.overtime.daily-hours:8.0}")
    private BigDecimal dailyOvertimeThresholdHours;

    @Value("${reports.payroll.overtime.weekly-hours:40.0}")
    private BigDecimal weeklyOvertimeThresholdHours;

    @Autowired
    public ReportService(TimeEntryRepository timeEntryRepository, LeaveRequestRepository leaveRequestRepository) {
        this.timeEntryRepository = timeEntryRepository;
        this.leaveRequestRepository = leaveRequestRepository;
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
}

