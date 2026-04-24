package com.example.employeetimetracking.service;

import com.example.employeetimetracking.dto.request.CreateTimeEntryDto;
import com.example.employeetimetracking.dto.response.TimeEntrySummaryDto;
import com.example.employeetimetracking.dto.response.TimeEntryDto;
import com.example.employeetimetracking.dto.response.TimeSummaryItemDto;
import com.example.employeetimetracking.exception.InvalidTimeEntryException;
import com.example.employeetimetracking.mapper.TimeEntryMapper;
import com.example.employeetimetracking.model.entities.Project;
import com.example.employeetimetracking.model.entities.TimeEntry;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.Status;
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
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TimeEntryService {
    private static final BigDecimal HOURS_FOR_AUTO_APPROVE = BigDecimal.valueOf(8);
    private static final long HOURS_IN_AUTO_APPROVE_WINDOW = 48L;

    private final TimeEntryRepository timeEntryRepository;
    private final TimeEntryMapper timeEntryMapper;
    private final ProjectService projectService;
    private final UserService userService;
    private final LeaveRequestService leaveRequestService;

    @Autowired
    public TimeEntryService(
            TimeEntryRepository timeEntryRepository,
            TimeEntryMapper timeEntryMapper,
            ProjectService projectService,
            UserService userService,
            LeaveRequestService leaveRequestService
    ) {
        this.timeEntryRepository = timeEntryRepository;
        this.timeEntryMapper = timeEntryMapper;
        this.projectService = projectService;
        this.userService = userService;
        this.leaveRequestService = leaveRequestService;
    }

    public List<TimeEntryDto> getRecentTimeEntries(User user) {
        Pageable limit = PageRequest.of(0, 8);
        List<TimeEntry> recentTimeEntries = timeEntryRepository.findByUserIdOrderByEntryDateDesc(user.getId(), limit);
        return recentTimeEntries.stream().map(timeEntryMapper::toDto).toList();
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
        long minutes = ChronoUnit.MINUTES.between(
                request.getClockInTime(),
                request.getClockOutTime()
        );
        te.setTotalHours(
                BigDecimal.valueOf(minutes)
                        .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP)
        );
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

    private void assertNoOverlapInBatch(List<TimeEntry> batch) {
        Map<LocalDate, List<TimeEntry>> byDate = batch.stream()
                .collect(Collectors.groupingBy(TimeEntry::getEntryDate));
        for (Map.Entry<LocalDate, List<TimeEntry>> e : byDate.entrySet()) {
            List<TimeEntry> list = e.getValue();
            for (int i = 0; i < list.size(); i++) {
                for (int j = i + 1; j < list.size(); j++) {
                    TimeEntry a = list.get(i);
                    TimeEntry b = list.get(j);
                    if (intervalsOverlap(
                            a.getClockInTime(), a.getClockOutTime(),
                            b.getClockInTime(), b.getClockOutTime()
                    )) {
                        throw new InvalidTimeEntryException("Bulk entries overlap on " + e.getKey());
                    }
                }
            }
        }
        for (TimeEntry te : batch) {
            assertNoTimeOverlap(te.getUser(), te.getEntryDate(), te.getClockInTime(), te.getClockOutTime(), null);
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
                List.of(Status.PENDING, Status.APPROVED))) {
            throw new InvalidTimeEntryException("Time entry not allowed: user is on leave for this date");
        }
        if (te.getTotalHours().compareTo(BigDecimal.valueOf(24)) > 0) {
            throw new InvalidTimeEntryException("Total hours cannot exceed 24 hours for a single day");
        }
        if (!te.getProject().getIsActive()) {
            throw new InvalidTimeEntryException("Project is not active");
        }
    }

    private BigDecimal calculateTotalHours(CreateTimeEntryDto request) {
        long minutes = ChronoUnit.MINUTES.between(
                request.getClockInTime(),
                request.getClockOutTime()
        );
        return BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
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

    private void applyAutoApproveIfEligible(TimeEntry te) {
        if (!shouldAutoApprove(te)) {
            return;
        }
        te.setStatus(Status.APPROVED);
        te.setApprovedAt(LocalDateTime.now());
        te.setApprovedBy(null);
    }

    private boolean shouldAutoApprove(TimeEntry te) {
        if (te.getTotalHours() == null) {
            return false;
        }
        if (te.getTotalHours().compareTo(HOURS_FOR_AUTO_APPROVE) >= 0) {
            return false;
        }
        if (te.getEntryDate() == null) {
            return false;
        }
        long hoursAfterEntryDateStart = ChronoUnit.HOURS.between(
                te.getEntryDate().atStartOfDay(), LocalDateTime.now());
        return hoursAfterEntryDateStart <= HOURS_IN_AUTO_APPROVE_WINDOW;
    }

    private TimeEntry getById(Long id) {
        return timeEntryRepository.findById(id)
                .orElseThrow(() -> new InvalidTimeEntryException("Time entry not found with id: " + id));
    }

    private boolean isDirectSupervisorOf(User approver, User entryOwner) {
        return entryOwner.getManager() != null
                && entryOwner.getManager().getId() != null
                && entryOwner.getManager().getId().equals(approver.getId());
    }

    private void assertCanManageEntry(User approver, TimeEntry te) {
        if (!isDirectSupervisorOf(approver, te.getUser())) {
            throw new InvalidTimeEntryException("You cannot manage this time entry");
        }
    }

    @Transactional
    public TimeEntryDto create(CreateTimeEntryDto request, Long userId) {
        User user = userService.getById(userId);
        Project project = projectService.getById(request.getProjectId());
        TimeEntry te = createTimeEntryEntity(request, user, project);
        validateTimeEntry(te);
        applyAutoApproveIfEligible(te);
        return timeEntryMapper.toDto(timeEntryRepository.save(te));
    }

    public List<TimeEntryDto> getByUserId(Long userId, Status status, LocalDate startDate, LocalDate endDate) {
        Specification<TimeEntry> spec = Specification.where(TimeEntrySpecification.hasStatus(status)
                .and(TimeEntrySpecification.hasUserId(userId))
                .and(TimeEntrySpecification.afterDate(startDate))
                .and(TimeEntrySpecification.beforeDate(endDate)));
        return timeEntryRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "entryDate"))
                .stream()
                .map(timeEntryMapper::toDto)
                .toList();
    }

    public List<TimeEntryDto> getTeamEntries(Long managerId, Status status, LocalDate startDate, LocalDate endDate, String name) {
        Specification<TimeEntry> spec = Specification.where(TimeEntrySpecification.hasStatus(status)
                .and(TimeEntrySpecification.hasManagerId(managerId))
                .and(TimeEntrySpecification.afterDate(startDate))
                .and(TimeEntrySpecification.beforeDate(endDate))
                .and(TimeEntrySpecification.hasName(name)));
        return timeEntryRepository.findAll(spec).stream().map(timeEntryMapper::toDto).toList();
    }

    @Transactional
    public void approve(Long id, Long approverId) {
        User approver = userService.getById(approverId);
        TimeEntry te = getById(id);
        if (te.getStatus() != Status.PENDING) {
            throw new InvalidTimeEntryException("Only pending time entries can be approved");
        }
        assertCanManageEntry(approver, te);
        te.setStatus(Status.APPROVED);
        te.setApprovedBy(approver);
        te.setApprovedAt(LocalDateTime.now());
    }

    @Transactional
    public void reject(Long id, Long approverId, String reason) {
        User approver = userService.getById(approverId);
        TimeEntry te = getById(id);
        if (te.getStatus() != Status.PENDING) {
            throw new InvalidTimeEntryException("Only pending time entries can be rejected");
        }
        assertCanManageEntry(approver, te);
        te.setStatus(Status.DENIED);
        te.setApprovedBy(approver);
        te.setApprovedAt(LocalDateTime.now());
        String existingDescription = te.getDescription() == null ? "" : te.getDescription().trim();
        String rejectionNote = "Rejected reason: " + reason;
        te.setDescription(existingDescription.isEmpty() ? rejectionNote : existingDescription + "\n" + rejectionNote);
    }

    @Transactional
    public TimeEntryDto update(Long id, CreateTimeEntryDto request, Long actorId, boolean isManager) {
        TimeEntry te = getById(id);
        User actor = userService.getById(actorId);
        if (te.getStatus() != Status.PENDING) {
            throw new InvalidTimeEntryException("Only pending time entries can be updated");
        }
        if (isManager) {
            assertCanManageEntry(actor, te);
        } else if (!te.getUser().getId().equals(actorId)) {
            throw new InvalidTimeEntryException("You cannot update this time entry");
        }
        Project project = projectService.getById(request.getProjectId());
        te.setEntryDate(request.getEntryDate());
        te.setClockInTime(request.getClockInTime());
        te.setClockOutTime(request.getClockOutTime());
        te.setProject(project);
        te.setDescription(request.getDescription());
        te.setTotalHours(calculateTotalHours(request));
        validateForUpdate(te);
        return timeEntryMapper.toDto(te);
    }

    @Transactional
    public void deletePending(Long id, Long actorId) {
        TimeEntry te = getById(id);
        if (!te.getUser().getId().equals(actorId)) {
            throw new InvalidTimeEntryException("You cannot delete this time entry");
        }
        if (te.getStatus() != Status.PENDING) {
            throw new InvalidTimeEntryException("Only pending time entries can be deleted");
        }
        if (te.getCreatedAt() == null || te.getCreatedAt().isBefore(LocalDateTime.now().minusHours(24))) {
            throw new InvalidTimeEntryException("Time entry can only be deleted within 24 hours of creation");
        }
        te.setStatus(Status.CANCELLED);
    }

    @Transactional
    public List<TimeEntryDto> bulkCreate(List<CreateTimeEntryDto> requests, Long userId) {
        if (requests == null || requests.isEmpty()) {
            throw new InvalidTimeEntryException("Bulk request cannot be empty");
        }
        User user = userService.getById(userId);
        List<TimeEntry> entities = requests.stream()
                .map(request -> {
                    Project project = projectService.getById(request.getProjectId());
                    TimeEntry te = createTimeEntryEntity(request, user, project);
                    validateTimeEntryWithoutOverlap(te);
                    return te;
                })
                .toList();
        assertNoOverlapInBatch(entities);
        for (TimeEntry te : entities) {
            applyAutoApproveIfEligible(te);
        }
        return timeEntryRepository.saveAll(entities).stream().map(timeEntryMapper::toDto).toList();
    }

    /** Validates everything except same-day DB overlap; used before batch grouping. */
    private void validateTimeEntryWithoutOverlap(TimeEntry te) {
        LocalDate now = LocalDate.now();
        if (te.getClockOutTime().isBefore(te.getClockInTime())) {
            throw new InvalidTimeEntryException("Clock out time must be after clock in time");
        }
        if (te.getEntryDate().isAfter(now)) {
            throw new InvalidTimeEntryException("Entry date cannot be in the future");
        }
        if (leaveRequestService.hasActiveLeaveRequestOnDate(te.getUser(), te.getEntryDate(),
                List.of(Status.PENDING, Status.APPROVED))) {
            throw new InvalidTimeEntryException("Time entry not allowed: user is on leave for this date");
        }
        if (te.getTotalHours().compareTo(BigDecimal.valueOf(24)) > 0) {
            throw new InvalidTimeEntryException("Total hours cannot exceed 24 hours for a single day");
        }
        if (!te.getProject().getIsActive()) {
            throw new InvalidTimeEntryException("Project is not active");
        }
    }

    @Transactional
    public void requestCorrection(Long id, Long userId, String explanation) {
        TimeEntry te = getById(id);
        if (!te.getUser().getId().equals(userId)) {
            throw new InvalidTimeEntryException("You cannot request a correction for this time entry");
        }
        if (te.getStatus() != Status.APPROVED) {
            throw new InvalidTimeEntryException("Only approved entries can be sent for correction");
        }
        String base = te.getDescription() == null ? "" : te.getDescription().trim();
        String tag = "Correction request: " + (explanation == null ? "" : explanation.trim());
        te.setDescription(base.isEmpty() ? tag : base + "\n" + tag);
        te.setStatus(Status.PENDING_CORRECTION);
    }

    @Transactional
    public void approveCorrectionUnlock(Long id, Long approverId) {
        User approver = userService.getById(approverId);
        TimeEntry te = getById(id);
        if (te.getStatus() != Status.PENDING_CORRECTION) {
            throw new InvalidTimeEntryException("Entry is not pending correction");
        }
        assertCanManageEntry(approver, te);
        te.setStatus(Status.PENDING);
    }

    @Transactional
    public List<TimeEntryDto> getPendingApprovalQueue(Long actorId, boolean hrAdmin) {
        List<TimeEntry> list;
        if (hrAdmin) {
            list = timeEntryRepository.findAll(
                    Specification.where(TimeEntrySpecification.hasStatus(Status.PENDING)),
                    Sort.by(Sort.Direction.ASC, "createdAt")
            );
        } else {
            list = timeEntryRepository.findByUserManagerIdAndStatusOrderByCreatedAtAsc(actorId, Status.PENDING);
        }
        return list.stream().map(timeEntryMapper::toDto).toList();
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
