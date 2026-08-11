package com.example.employeetimetracking.service;

import com.example.employeetimetracking.dto.request.CreateTimeEntryDto;
import com.example.employeetimetracking.dto.request.CreateTimeEntryBreakDto;
import com.example.employeetimetracking.dto.response.TimeEntrySummaryDto;
import com.example.employeetimetracking.dto.response.TimeEntryPersonalStatsDto;
import com.example.employeetimetracking.dto.response.TimeEntryDto;
import com.example.employeetimetracking.dto.response.TimeEntryBreakDto;
import com.example.employeetimetracking.dto.response.TimeSummaryItemDto;
import com.example.employeetimetracking.exception.InvalidTimeEntryException;
import com.example.employeetimetracking.mapper.TimeEntryMapper;
import com.example.employeetimetracking.model.entities.Project;
import com.example.employeetimetracking.model.entities.TimeEntry;
import com.example.employeetimetracking.model.entities.TimeEntryBreak;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.Status;
import com.example.employeetimetracking.model.enums.UserRole;
import com.example.employeetimetracking.repository.TimeEntryBreakRepository;
import com.example.employeetimetracking.repository.TimeEntryRepository;
import com.example.employeetimetracking.security.CustomUserDetails;
import com.example.employeetimetracking.specification.TimeEntrySpecification;
import jakarta.transaction.Transactional;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class TimeEntryService {
    private final TimeEntryRepository timeEntryRepository;
    private final TimeEntryBreakRepository timeEntryBreakRepository;
    private final TimeEntryMapper timeEntryMapper;
    private final ProjectService projectService;
    private final UserService userService;
    private final LeaveRequestService leaveRequestService;

    @Autowired
    public TimeEntryService(
                            TimeEntryRepository timeEntryRepository,
            TimeEntryBreakRepository timeEntryBreakRepository,
                            TimeEntryMapper timeEntryMapper,
                            ProjectService projectService,
                            UserService userService,
                            LeaveRequestService leaveRequestService
    ) {
        this.timeEntryRepository = timeEntryRepository;
        this.timeEntryBreakRepository = timeEntryBreakRepository;
        this.timeEntryMapper = timeEntryMapper;
        this.projectService = projectService;
        this.userService = userService;
        this.leaveRequestService = leaveRequestService;
    }

    public List<TimeEntryDto> getRecentTimeEntries(User user) {
        Pageable limit = PageRequest.of(0, 8);
        List<TimeEntry> recentTimeEntries = timeEntryRepository.findByUserIdOrderByEntryDateDesc(user.getId(), limit);
        return toDtos(recentTimeEntries);
    }

    public BigDecimal getHoursThisWeek(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1);

        List<TimeEntry> timeEntriesThisWeek = timeEntryRepository
                .findByUserIdAndEntryDateBetweenAndStatus(userId, startOfWeek, today, Status.APPROVED);

        BigDecimal hoursThisWeek = BigDecimal.ZERO;
        for (TimeEntry timeEntry : timeEntriesThisWeek) {
            hoursThisWeek = hoursThisWeek.add(timeEntry.getTotalHours());
        }
        return hoursThisWeek;
    }

    public BigDecimal getHoursThisMonth(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);

        List<TimeEntry> timeEntriesThisMonth = timeEntryRepository
                .findByUserIdAndEntryDateBetweenAndStatus(userId, startOfMonth, today, Status.APPROVED);

        BigDecimal hoursThisMonth = BigDecimal.ZERO;
        for (TimeEntry te : timeEntriesThisMonth) {
            hoursThisMonth = hoursThisMonth.add(te.getTotalHours());
        }
        return hoursThisMonth;
    }

    public TimeEntryPersonalStatsDto getMyStats(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);

        BigDecimal totalHoursThisWeek = getHoursThisWeek(userId);
        List<TimeEntry> monthEntries = timeEntryRepository
                .findByUserIdAndEntryDateBetweenAndStatus(userId, startOfMonth, today, Status.APPROVED);

        BigDecimal monthTotalHours = monthEntries.stream()
                .map(TimeEntry::getTotalHours)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long distinctLoggedDays = monthEntries.stream()
                .map(TimeEntry::getEntryDate)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        BigDecimal averageHoursPerDayThisMonth = distinctLoggedDays == 0
                ? BigDecimal.ZERO
                : monthTotalHours.divide(BigDecimal.valueOf(distinctLoggedDays), 2, RoundingMode.HALF_UP);

        Map<String, BigDecimal> projectHours = monthEntries.stream()
                .filter(te -> te.getProject() != null && te.getProject().getProjectCode() != null)
                .collect(Collectors.groupingBy(
                        te -> te.getProject().getProjectCode(),
                        Collectors.reducing(BigDecimal.ZERO, TimeEntry::getTotalHours, BigDecimal::add)
                ));

        String topProjectCode = null;
        BigDecimal topProjectHours = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : projectHours.entrySet()) {
            if (topProjectCode == null || entry.getValue().compareTo(topProjectHours) > 0) {
                topProjectCode = entry.getKey();
                topProjectHours = entry.getValue();
            }
        }

        return new TimeEntryPersonalStatsDto(
                totalHoursThisWeek,
                averageHoursPerDayThisMonth,
                topProjectCode,
                topProjectHours
        );
    }

    public Integer getUserPendingCount(Long userId) {
        return timeEntryRepository.countByUserIdAndStatus(userId, Status.PENDING);
    }

    public Integer getPendingTimeApprovalsCount(Long userId) {
        return timeEntryRepository.countByUserManagerIdAndStatus(userId, Status.PENDING);
    }

    public TimeEntry createTimeEntryEntity(CreateTimeEntryDto request, User user, Project project) {
        TimeEntry te = new TimeEntry();
        te.setUser(user);
        te.setEntryDate(request.getEntryDate());
        te.setClockInTime(request.getClockInTime());
        te.setClockOutTime(request.getClockOutTime());
        te.setDescription(request.getDescription());
        te.setProject(project);
        te.setStatus(Status.PENDING);
        applySubmittedBreaks(te, request.getBreaks(), false);
        recalculateHours(te, te.getBreaks());
        return te;
    }

    private static boolean statusBlocksOverlap(Status s) {
        if (s == null) {
            return true;
        }
        return s == Status.PENDING || s == Status.APPROVED || s == Status.PENDING_CORRECTION;
    }

    private static boolean intervalsOverlap(LocalTime aStart, LocalTime aEnd, LocalTime bStart, LocalTime bEnd) {
        return aStart != null && aEnd != null && bStart != null && bEnd != null
                && aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }

    private void assertNoTimeOverlap(User user, LocalDate date, LocalTime in, LocalTime out, Long excludeId) {
        List<TimeEntry> onDay = timeEntryRepository.findByUserIdAndEntryDate(user.getId(), date);
        for (TimeEntry other : onDay) {
            if (excludeId != null && other.getId() != null && other.getId().equals(excludeId)) {
                continue;
            }
            if (!statusBlocksOverlap(other.getStatus())) {
                continue;
            }
            if (intervalsOverlap(in, out, other.getClockInTime(), other.getClockOutTime())) {
                throw new InvalidTimeEntryException("Time range overlaps with another entry on " + date);
            }
        }
    }

    public void validateTimeEntry(TimeEntry te) {
        LocalDate now = LocalDate.now();
        if (te.getClockOutTime().isBefore(te.getClockInTime())) {
            throw new InvalidTimeEntryException("Clock out time must be after clock in time");
        }
        if (te.getEntryDate().isAfter(now)) {
            throw new InvalidTimeEntryException("Entry date cannot be in the future");
        }
        assertNoTimeOverlap(te.getUser(), te.getEntryDate(), te.getClockInTime(), te.getClockOutTime(), te.getId());
        if (leaveRequestService.hasActiveLeaveRequestOnDate(te.getUser(), te.getEntryDate(),
                List.of(Status.PENDING, Status.APPROVED, Status.CANCELLATION_PENDING))) {
            throw new InvalidTimeEntryException("Time entry not allowed: user is on leave for this date");
        }
        if (te.getTotalHours().compareTo(BigDecimal.valueOf(24)) > 0) {
            throw new InvalidTimeEntryException("Total hours cannot exceed 24 hours for a single day");
        }
        if (!te.getProject().getIsActive()) {
            throw new InvalidTimeEntryException("Project is not active");
        }
    }

    /**
     * Sets {@code totalHours} to payable hours: clock span minus unpaid breaks.
     * Call whenever clock times or breaks change — never set totalHours directly.
     */
    private void recalculateHours(TimeEntry te, List<TimeEntryBreak> breaks) {
        if (te.getClockInTime() == null || te.getClockOutTime() == null) {
            return;
        }
        long totalMinutes = ChronoUnit.MINUTES.between(te.getClockInTime(), te.getClockOutTime());
        if (totalMinutes < 0) {
            throw new InvalidTimeEntryException("Clock out time must be after clock in time");
        }
        long unpaidBreakMinutes = 0;
        if (breaks != null) {
            for (TimeEntryBreak b : breaks) {
                if (Boolean.TRUE.equals(b.getIsUnpaid())) {
                    unpaidBreakMinutes += ChronoUnit.MINUTES.between(b.getBreakStart(), b.getBreakEnd());
                }
            }
        }
        long netMinutes = totalMinutes - unpaidBreakMinutes;
        if (netMinutes < 0) {
            throw new InvalidTimeEntryException("Break time cannot exceed worked time");
        }
        te.setTotalHours(
                BigDecimal.valueOf(netMinutes)
                        .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP)
        );
    }

    private void recalculateHoursFromDb(TimeEntry te) {
        List<TimeEntryBreak> breaks = te.getId() == null
                ? List.of()
                : timeEntryBreakRepository.findByTimeEntryIdOrderByBreakStartAsc(te.getId());
        recalculateHours(te, breaks);
    }

    private TimeEntryBreak toBreakEntity(TimeEntry te, CreateTimeEntryBreakDto dto) {
        Boolean unpaid = dto.getIsUnpaid() == null ? true : dto.getIsUnpaid();
        TimeEntryBreak b = new TimeEntryBreak();
        b.setTimeEntry(te);
        b.setBreakStart(dto.getBreakStart());
        b.setBreakEnd(dto.getBreakEnd());
        b.setIsUnpaid(unpaid);
        return b;
    }

    private void assertSubmittedBreaksValid(LocalTime clockIn, LocalTime clockOut, List<CreateTimeEntryBreakDto> breaks) {
        if (breaks == null || breaks.isEmpty()) {
            return;
        }
        for (CreateTimeEntryBreakDto dto : breaks) {
            assertBreakValid(dto.getBreakStart(), dto.getBreakEnd(), clockIn, clockOut);
        }
        for (int i = 0; i < breaks.size(); i++) {
            for (int j = i + 1; j < breaks.size(); j++) {
                CreateTimeEntryBreakDto a = breaks.get(i);
                CreateTimeEntryBreakDto b = breaks.get(j);
                if (a.getBreakStart().isBefore(b.getBreakEnd()) && b.getBreakStart().isBefore(a.getBreakEnd())) {
                    throw new InvalidTimeEntryException("Break overlaps with another submitted break");
                }
            }
        }
    }

    /**
     * @param replace when true, clears existing breaks then applies the submitted set
     *                (used for update). When false (create), only adds submitted breaks.
     */
    private void applySubmittedBreaks(TimeEntry te, List<CreateTimeEntryBreakDto> breakDtos, boolean replace) {
        if (te.getBreaks() == null) {
            te.setBreaks(new ArrayList<>());
        }
        if (replace) {
            te.getBreaks().clear();
        }
        if (breakDtos == null || breakDtos.isEmpty()) {
            return;
        }
        assertSubmittedBreaksValid(te.getClockInTime(), te.getClockOutTime(), breakDtos);
        for (CreateTimeEntryBreakDto dto : breakDtos) {
            te.getBreaks().add(toBreakEntity(te, dto));
        }
    }

    private TimeEntryBreakDto toBreakDto(TimeEntryBreak b) {
        int mins = (int) ChronoUnit.MINUTES.between(b.getBreakStart(), b.getBreakEnd());
        return new TimeEntryBreakDto(b.getId(), b.getBreakStart(), b.getBreakEnd(), b.getIsUnpaid(), mins);
    }

    private TimeEntryDto toDto(TimeEntry te) {
        List<TimeEntryBreakDto> breaks;
        if (te.getId() != null) {
            breaks = timeEntryBreakRepository.findByTimeEntryIdOrderByBreakStartAsc(te.getId()).stream()
                    .map(this::toBreakDto)
                    .toList();
        } else {
            breaks = te.getBreaks() == null ? List.of() : te.getBreaks().stream().map(this::toBreakDto).toList();
        }
        return timeEntryMapper.toDto(te, breaks);
    }

    private List<TimeEntryDto> toDtos(List<TimeEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        List<Long> ids = entries.stream()
                .map(TimeEntry::getId)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, List<TimeEntryBreak>> breaksByEntry = ids.isEmpty()
                ? Map.of()
                : timeEntryBreakRepository.findByTimeEntryIdIn(ids).stream()
                .collect(Collectors.groupingBy(b -> b.getTimeEntry().getId()));
        return entries.stream()
                .map(te -> {
                    List<TimeEntryBreakDto> breaks = breaksByEntry.getOrDefault(te.getId(), List.of()).stream()
                            .sorted(Comparator.comparing(TimeEntryBreak::getBreakStart))
                            .map(this::toBreakDto)
                            .toList();
                    return timeEntryMapper.toDto(te, breaks);
                })
                .toList();
    }

    private static void assertBreakValid(LocalTime breakStart, LocalTime breakEnd, LocalTime clockIn, LocalTime clockOut) {
        if (breakStart == null || breakEnd == null) {
            throw new InvalidTimeEntryException("Break start and end are required");
        }
        if (!breakEnd.isAfter(breakStart)) {
            throw new InvalidTimeEntryException("Break end must be after break start");
        }
        if (clockIn != null && breakStart.isBefore(clockIn)) {
            throw new InvalidTimeEntryException("Break cannot start before clock-in time");
        }
        if (clockOut != null && breakEnd.isAfter(clockOut)) {
            throw new InvalidTimeEntryException("Break cannot end after clock-out time");
        }
    }

    private void assertNoBreakOverlap(Long timeEntryId, LocalTime start, LocalTime end) {
        List<TimeEntryBreak> existing = timeEntryBreakRepository.findByTimeEntryIdOrderByBreakStartAsc(timeEntryId);
        for (TimeEntryBreak b : existing) {
            if (start.isBefore(b.getBreakEnd()) && b.getBreakStart().isBefore(end)) {
                throw new InvalidTimeEntryException("Break overlaps with an existing break");
            }
        }
    }

    private void assertExistingBreaksStillValid(TimeEntry te) {
        if (te.getId() == null) {
            return;
        }
        List<TimeEntryBreak> existing = timeEntryBreakRepository.findByTimeEntryIdOrderByBreakStartAsc(te.getId());
        for (TimeEntryBreak b : existing) {
            assertBreakValid(b.getBreakStart(), b.getBreakEnd(), te.getClockInTime(), te.getClockOutTime());
        }
        for (int i = 0; i < existing.size(); i++) {
            for (int j = i + 1; j < existing.size(); j++) {
                TimeEntryBreak a = existing.get(i);
                TimeEntryBreak b = existing.get(j);
                if (a.getBreakStart().isBefore(b.getBreakEnd()) && b.getBreakStart().isBefore(a.getBreakEnd())) {
                    throw new InvalidTimeEntryException("Existing breaks overlap");
                }
            }
        }
    }

    private void validateForUpdate(TimeEntry te) {
        LocalDate now = LocalDate.now();
        if (te.getClockOutTime().isBefore(te.getClockInTime())) {
            throw new InvalidTimeEntryException("Clock out time must be after clock in time");
        }
        if (te.getEntryDate().isAfter(now)) {
            throw new InvalidTimeEntryException("Entry date cannot be in the future");
        }
        if (te.getTotalHours().compareTo(BigDecimal.valueOf(24)) > 0) {
            throw new InvalidTimeEntryException("Total hours cannot exceed 24 hours for a single day");
        }
        if (!te.getProject().getIsActive()) {
            throw new InvalidTimeEntryException("Project is not active");
        }
        assertNoTimeOverlap(te.getUser(), te.getEntryDate(), te.getClockInTime(), te.getClockOutTime(), te.getId());
    }

    private TimeEntry getById(Long id) {
        return timeEntryRepository.findById(id)
                .orElseThrow(() -> new InvalidTimeEntryException("Time entry not found with id: " + id));
    }

    private boolean isOwner(User actor, TimeEntry te) {
        return te.getUser() != null
                && te.getUser().getId() != null
                && te.getUser().getId().equals(actor.getId());
    }

    private boolean isHrAdmin(User actor) {
        return actor.getUserRole() == UserRole.HR_ADMIN;
    }

    private boolean isDirectSupervisorOf(User actor, User entryOwner) {
        return entryOwner.getManager() != null
                && entryOwner.getManager().getId() != null
                && entryOwner.getManager().getId().equals(actor.getId());
    }

    /** Owner, direct manager, or HR can view an entry (and its breaks). */
    private void assertCanViewEntry(User actor, TimeEntry te) {
        if (isOwner(actor, te) || isHrAdmin(actor) || isDirectSupervisorOf(actor, te.getUser())) {
            return;
        }
        throw new InvalidTimeEntryException("You cannot access this time entry");
    }

    /** Owner, direct manager, or HR can edit a pending entry / its breaks. */
    private void assertCanEditPendingEntry(User actor, TimeEntry te) {
        assertCanViewEntry(actor, te);
    }

    /** Direct manager or HR can approve/reject/unlock — never the entry owner. */
    private void assertCanDecideOnEntry(User actor, TimeEntry te) {
        if (isOwner(actor, te)) {
            throw new InvalidTimeEntryException("You cannot approve or reject your own time entry");
        }
        if (isHrAdmin(actor) || isDirectSupervisorOf(actor, te.getUser())) {
            return;
        }
        throw new InvalidTimeEntryException("You cannot manage this time entry");
    }

    @Transactional
    public TimeEntryDto create(CreateTimeEntryDto request, Long userId) {
        User user = userService.getById(userId);
        Project project = projectService.getById(request.getProjectId());
        TimeEntry te = createTimeEntryEntity(request, user, project);
        validateTimeEntry(te);
        return toDto(timeEntryRepository.save(te));
    }

    public List<TimeEntryDto> getByUserId(Long userId, Status status, LocalDate startDate, LocalDate endDate) {
        Specification<TimeEntry> spec = Specification.where(TimeEntrySpecification.hasStatus(status)
                .and(TimeEntrySpecification.hasUserId(userId))
                .and(TimeEntrySpecification.afterDate(startDate))
                .and(TimeEntrySpecification.beforeDate(endDate)));
        return toDtos(timeEntryRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "entryDate")));
    }

    public List<TimeEntryDto> getTeamEntries(
            Long actorId,
            boolean hrAdmin,
            Status status,
            LocalDate startDate,
            LocalDate endDate,
            String name
    ) {
        Specification<TimeEntry> spec = Specification.where(TimeEntrySpecification.hasStatus(status)
                .and(TimeEntrySpecification.afterDate(startDate))
                .and(TimeEntrySpecification.beforeDate(endDate))
                .and(TimeEntrySpecification.hasName(name)));
        if (!hrAdmin) {
            spec = spec.and(TimeEntrySpecification.hasManagerId(actorId));
        }
        return toDtos(timeEntryRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "entryDate")));
    }

    @Transactional
    public void approve(Long id, Long approverId) {
        User approver = userService.getById(approverId);
        TimeEntry te = getById(id);
        if (te.getStatus() != Status.PENDING) {
            throw new InvalidTimeEntryException("Only pending time entries can be approved");
        }
        assertCanDecideOnEntry(approver, te);
        te.setStatus(Status.APPROVED);
        te.setApprovedBy(approver);
        te.setApprovedAt(LocalDateTime.now());
        te.setRejectionReason(null);
    }

    @Transactional
    public void reject(Long id, Long approverId, String reason) {
        User approver = userService.getById(approverId);
        TimeEntry te = getById(id);
        if (te.getStatus() != Status.PENDING) {
            throw new InvalidTimeEntryException("Only pending time entries can be rejected");
        }
        assertCanDecideOnEntry(approver, te);
        te.setStatus(Status.DENIED);
        te.setApprovedBy(approver);
        te.setApprovedAt(LocalDateTime.now());
        te.setRejectionReason(reason);
    }

    @Transactional
    public TimeEntryDto update(Long id, CreateTimeEntryDto request, Long actorId) {
        TimeEntry te = getById(id);
        User actor = userService.getById(actorId);
        if (te.getStatus() != Status.PENDING) {
            throw new InvalidTimeEntryException("Only pending time entries can be updated");
        }
        assertCanEditPendingEntry(actor, te);
        Project project = projectService.getById(request.getProjectId());
        te.setEntryDate(request.getEntryDate());
        te.setClockInTime(request.getClockInTime());
        te.setClockOutTime(request.getClockOutTime());
        te.setProject(project);
        te.setDescription(request.getDescription());
        if (request.getBreaks() != null) {
            applySubmittedBreaks(te, request.getBreaks(), true);
            recalculateHours(te, te.getBreaks());
        } else {
            assertExistingBreaksStillValid(te);
            recalculateHoursFromDb(te);
        }
        validateForUpdate(te);
        return toDto(te);
    }

    @Transactional
    public void deletePending(Long id, Long actorId) {
        TimeEntry te = getById(id);
        User actor = userService.getById(actorId);
        if (!isOwner(actor, te)) {
            throw new InvalidTimeEntryException("Only the entry owner can cancel a pending time entry");
        }
        if (te.getStatus() != Status.PENDING) {
            throw new InvalidTimeEntryException("Only pending time entries can be cancelled");
        }
        te.setStatus(Status.CANCELLED);
    }

    @Transactional
    public TimeEntryBreakDto addBreak(Long timeEntryId, CreateTimeEntryBreakDto dto, Long actorId) {
        TimeEntry te = getById(timeEntryId);
        User actor = userService.getById(actorId);

        if (te.getStatus() != Status.PENDING) {
            throw new InvalidTimeEntryException("Breaks can only be updated for pending time entries");
        }
        assertCanEditPendingEntry(actor, te);

        Boolean unpaid = dto.getIsUnpaid() == null ? true : dto.getIsUnpaid();
        assertBreakValid(dto.getBreakStart(), dto.getBreakEnd(), te.getClockInTime(), te.getClockOutTime());
        assertNoBreakOverlap(te.getId(), dto.getBreakStart(), dto.getBreakEnd());

        TimeEntryBreak b = new TimeEntryBreak();
        b.setTimeEntry(te);
        b.setBreakStart(dto.getBreakStart());
        b.setBreakEnd(dto.getBreakEnd());
        b.setIsUnpaid(unpaid);
        TimeEntryBreak saved = timeEntryBreakRepository.save(b);
        if (te.getBreaks() != null) {
            te.getBreaks().add(saved);
        }
        recalculateHoursFromDb(te);
        return toBreakDto(saved);
    }

    @Transactional
    public List<TimeEntryBreakDto> listBreaks(Long timeEntryId, Long actorId) {
        TimeEntry te = getById(timeEntryId);
        User actor = userService.getById(actorId);
        assertCanViewEntry(actor, te);
        return timeEntryBreakRepository.findByTimeEntryIdOrderByBreakStartAsc(timeEntryId).stream()
                .map(this::toBreakDto)
                .toList();
    }

    @Transactional
    public void deleteBreak(Long timeEntryId, Long breakId, Long actorId) {
        TimeEntry te = getById(timeEntryId);
        User actor = userService.getById(actorId);
        if (te.getStatus() != Status.PENDING) {
            throw new InvalidTimeEntryException("Breaks can only be updated for pending time entries");
        }
        assertCanEditPendingEntry(actor, te);

        TimeEntryBreak b = timeEntryBreakRepository.findById(breakId)
                .orElseThrow(() -> new InvalidTimeEntryException("Break not found with id: " + breakId));
        if (b.getTimeEntry() == null || b.getTimeEntry().getId() == null || !b.getTimeEntry().getId().equals(timeEntryId)) {
            throw new InvalidTimeEntryException("Break does not belong to this time entry");
        }

        timeEntryBreakRepository.delete(b);
        if (te.getBreaks() != null) {
            te.getBreaks().removeIf(existing -> existing.getId() != null && existing.getId().equals(breakId));
        }
        recalculateHoursFromDb(te);
    }

    @Transactional
    public void requestCorrection(Long id, Long userId, String explanation) {
        TimeEntry te = getById(id);
        User actor = userService.getById(userId);
        if (!isOwner(actor, te)) {
            throw new InvalidTimeEntryException("Only the entry owner can request a correction");
        }
        if (te.getStatus() != Status.APPROVED) {
            throw new InvalidTimeEntryException("Only approved entries can be sent for correction");
        }
        te.setCorrectionReason(explanation);
        te.setStatus(Status.PENDING_CORRECTION);
    }

    @Transactional
    public void approveCorrectionUnlock(Long id, Long approverId) {
        User approver = userService.getById(approverId);
        TimeEntry te = getById(id);
        if (te.getStatus() != Status.PENDING_CORRECTION) {
            throw new InvalidTimeEntryException("Entry is not pending correction");
        }
        assertCanDecideOnEntry(approver, te);
        // Unlock for edit: treat as a fresh pending submission
        te.setStatus(Status.PENDING);
        te.setApprovedBy(null);
        te.setApprovedAt(null);
    }

    @Transactional
    public void denyCorrectionUnlock(Long id, Long approverId) {
        User approver = userService.getById(approverId);
        TimeEntry te = getById(id);
        if (te.getStatus() != Status.PENDING_CORRECTION) {
            throw new InvalidTimeEntryException("Entry is not pending correction");
        }
        assertCanDecideOnEntry(approver, te);
        // Keep the original approval; correction is refused
        te.setStatus(Status.APPROVED);
    }

    @Transactional
    public List<TimeEntryDto> getPendingApprovalQueue(Long actorId, boolean hrAdmin) {
        List<Status> queueStatuses = List.of(Status.PENDING, Status.PENDING_CORRECTION);
        Specification<TimeEntry> spec = Specification.where(TimeEntrySpecification.hasStatusIn(queueStatuses));
        if (!hrAdmin) {
            spec = spec.and(TimeEntrySpecification.hasManagerId(actorId));
        }
        return toDtos(timeEntryRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "createdAt")));
    }

    @Transactional
    public byte[] export(
            Long actorId,
            LocalDate startDate,
            LocalDate endDate,
            Long userId,
            String format,
            CustomUserDetails authUser
    ) {
        if (startDate == null || endDate == null) {
            throw new InvalidTimeEntryException("startDate and endDate are required");
        }
        if (startDate.isAfter(endDate)) {
            throw new InvalidTimeEntryException("startDate cannot be after endDate");
        }
        String f = format == null ? "" : format.trim();
        if (!f.equalsIgnoreCase("csv") && !f.equalsIgnoreCase("xlsx")) {
            throw new InvalidTimeEntryException("format must be csv or xlsx");
        }
        List<TimeEntry> entries = resolveExportEntries(actorId, userId, startDate, endDate, authUser);
        entries = entries.stream()
                .sorted(Comparator.comparing(TimeEntry::getEntryDate).thenComparing(TimeEntry::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
        if (f.equalsIgnoreCase("csv")) {
            return toCsvBytes(entries);
        }
        return toXlsxBytes(entries);
    }

    private List<TimeEntry> resolveExportEntries(
            Long actorId,
            Long userId,
            LocalDate startDate,
            LocalDate endDate,
            CustomUserDetails authUser
    ) {
        boolean isHrAdmin = authUser.hasRole("HR_ADMIN");
        if (userId != null) {
            User target = userService.getById(userId);
            if (!isHrAdmin && !isDirectSupervisorOf(userService.getById(actorId), target)) {
                throw new InvalidTimeEntryException("You cannot access this user's time entries for export");
            }
            return timeEntryRepository.findByUserIdAndEntryDateBetweenAndStatus(
                    userId, startDate, endDate, Status.APPROVED);
        }
        if (isHrAdmin) {
            Specification<TimeEntry> spec = Specification
                    .where(TimeEntrySpecification.hasStatus(Status.APPROVED))
                    .and(TimeEntrySpecification.afterDate(startDate))
                    .and(TimeEntrySpecification.beforeDate(endDate));
            return timeEntryRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "entryDate", "id"));
        }
        return timeEntryRepository.findAll(
                Specification
                        .where(TimeEntrySpecification.hasStatus(Status.APPROVED))
                        .and(TimeEntrySpecification.hasManagerId(actorId))
                        .and(TimeEntrySpecification.afterDate(startDate))
                        .and(TimeEntrySpecification.beforeDate(endDate)),
                Sort.by(Sort.Direction.ASC, "entryDate", "id")
        );
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

    private static byte[] toCsvBytes(List<TimeEntry> entries) {
        StringBuilder sb = new StringBuilder();
        sb.append("id,email,entryDate,clockIn,clockOut,totalHours,projectCode,status\n");
        for (TimeEntry te : entries) {
            sb.append(te.getId() == null ? "" : te.getId()).append(',');
            sb.append(csvEsc(te.getUser() != null ? te.getUser().getEmail() : null)).append(',');
            sb.append(te.getEntryDate() != null ? te.getEntryDate().toString() : "").append(',');
            sb.append(te.getClockInTime() != null ? te.getClockInTime().toString() : "").append(',');
            sb.append(te.getClockOutTime() != null ? te.getClockOutTime().toString() : "").append(',');
            sb.append(te.getTotalHours() != null ? te.getTotalHours().toPlainString() : "").append(',');
            sb.append(csvEsc(te.getProject() != null ? te.getProject().getProjectCode() : null)).append(',');
            sb.append(te.getStatus() != null ? te.getStatus().name() : "");
            sb.append('\n');
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private static byte[] toXlsxBytes(List<TimeEntry> entries) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("time-entries");
            Row h = sheet.createRow(0);
            h.createCell(0).setCellValue("id");
            h.createCell(1).setCellValue("email");
            h.createCell(2).setCellValue("entryDate");
            h.createCell(3).setCellValue("clockIn");
            h.createCell(4).setCellValue("clockOut");
            h.createCell(5).setCellValue("totalHours");
            h.createCell(6).setCellValue("projectCode");
            h.createCell(7).setCellValue("status");
            int r = 1;
            for (TimeEntry te : entries) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(te.getId() == null ? 0 : te.getId());
                row.createCell(1).setCellValue(te.getUser() != null && te.getUser().getEmail() != null ? te.getUser().getEmail() : "");
                row.createCell(2).setCellValue(te.getEntryDate() != null ? te.getEntryDate().toString() : "");
                row.createCell(3).setCellValue(te.getClockInTime() != null ? te.getClockInTime().toString() : "");
                row.createCell(4).setCellValue(te.getClockOutTime() != null ? te.getClockOutTime().toString() : "");
                if (te.getTotalHours() != null) {
                    row.createCell(5).setCellValue(te.getTotalHours().doubleValue());
                } else {
                    row.createCell(5).setCellValue("");
                }
                row.createCell(6).setCellValue(te.getProject() != null && te.getProject().getProjectCode() != null ? te.getProject().getProjectCode() : "");
                row.createCell(7).setCellValue(te.getStatus() != null ? te.getStatus().name() : "");
            }
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new InvalidTimeEntryException("Failed to build export file");
        }
    }

    public TimeEntrySummaryDto summary(Long actorId, Long userId, LocalDate startDate, LocalDate endDate, CustomUserDetails authUser) {
        if (startDate == null || endDate == null) {
            throw new InvalidTimeEntryException("startDate and endDate are required");
        }
        if (startDate.isAfter(endDate)) {
            throw new InvalidTimeEntryException("startDate cannot be after endDate");
        }

        List<TimeEntry> entries;
        boolean isHrAdmin = authUser.hasRole("HR_ADMIN");
        if (userId != null) {
            User target = userService.getById(userId);
            if (!isHrAdmin && !isDirectSupervisorOf(userService.getById(actorId), target)) {
                throw new InvalidTimeEntryException("You cannot access this user's summary");
            }
            entries = timeEntryRepository.findByUserIdAndEntryDateBetweenAndStatus(userId, startDate, endDate, Status.APPROVED);
        } else if (isHrAdmin) {
            Specification<TimeEntry> spec = Specification
                    .where(TimeEntrySpecification.hasStatus(Status.APPROVED))
                    .and(TimeEntrySpecification.afterDate(startDate))
                    .and(TimeEntrySpecification.beforeDate(endDate));
            entries = timeEntryRepository.findAll(spec);
        } else {
            entries = timeEntryRepository.findAll(Specification
                    .where(TimeEntrySpecification.hasStatus(Status.APPROVED))
                    .and(TimeEntrySpecification.hasManagerId(actorId))
                    .and(TimeEntrySpecification.afterDate(startDate))
                    .and(TimeEntrySpecification.beforeDate(endDate)));
        }

        BigDecimal totalHours = entries.stream()
                .map(TimeEntry::getTotalHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<TimeSummaryItemDto> byDate = toSummary(entries.stream()
                .collect(Collectors.groupingBy(timeEntry -> timeEntry.getEntryDate().toString(),
                        Collectors.reducing(BigDecimal.ZERO, TimeEntry::getTotalHours, BigDecimal::add))));

        List<TimeSummaryItemDto> byProject = toSummary(entries.stream()
                .collect(Collectors.groupingBy(timeEntry -> timeEntry.getProject().getProjectCode(),
                        Collectors.reducing(BigDecimal.ZERO, TimeEntry::getTotalHours, BigDecimal::add))));

        List<TimeSummaryItemDto> byEmployee = toSummary(entries.stream()
                .collect(Collectors.groupingBy(timeEntry -> timeEntry.getUser().getFirstName() + " " + timeEntry.getUser().getLastName(),
                        Collectors.reducing(BigDecimal.ZERO, TimeEntry::getTotalHours, BigDecimal::add))));

        return new TimeEntrySummaryDto(totalHours, byDate, byProject, byEmployee);
    }

    private List<TimeSummaryItemDto> toSummary(Map<String, BigDecimal> source) {
        return source.entrySet().stream()
                .map(e -> new TimeSummaryItemDto(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(TimeSummaryItemDto::getKey))
                .toList();
    }
}
