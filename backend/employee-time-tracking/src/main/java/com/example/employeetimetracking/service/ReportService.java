package com.example.employeetimetracking.service;

import com.example.employeetimetracking.dto.response.EmployeeTimeReportDto;
import com.example.employeetimetracking.dto.response.TeamLeaveReportDto;
import com.example.employeetimetracking.dto.response.TeamLeaveRequestItemDto;
import com.example.employeetimetracking.dto.response.TimeSummaryItemDto;
import com.example.employeetimetracking.exception.InvalidTimeEntryException;
import com.example.employeetimetracking.model.entities.LeaveRequest;
import com.example.employeetimetracking.model.entities.TimeEntry;
import com.example.employeetimetracking.model.enums.Status;
import com.example.employeetimetracking.repository.LeaveRequestRepository;
import com.example.employeetimetracking.repository.TimeEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ReportService {
    private final TimeEntryRepository timeEntryRepository;
    private final LeaveRequestRepository leaveRequestRepository;

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
}

