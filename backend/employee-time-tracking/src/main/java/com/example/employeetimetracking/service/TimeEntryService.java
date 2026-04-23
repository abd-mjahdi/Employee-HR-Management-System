package com.example.employeetimetracking.service;

import com.example.employeetimetracking.dto.request.CreateTimeEntryDto;
import com.example.employeetimetracking.dto.response.TimeEntryDto;
import com.example.employeetimetracking.exception.InvalidTimeEntryException;
import com.example.employeetimetracking.mapper.TimeEntryMapper;
import com.example.employeetimetracking.model.entities.Project;
import com.example.employeetimetracking.model.entities.TimeEntry;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.Status;
import com.example.employeetimetracking.repository.TimeEntryRepository;
import com.example.employeetimetracking.specification.TimeEntrySpecification;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;

@Service
public class TimeEntryService {
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
    ){
        this.timeEntryRepository = timeEntryRepository;
        this.timeEntryMapper = timeEntryMapper;
        this.projectService = projectService;
        this.userService = userService;
        this.leaveRequestService = leaveRequestService;
    }
    public List<TimeEntryDto> getRecentTimeEntries(User user){
        Pageable limit = PageRequest.of(0,8);
        List<TimeEntry> recentTimeEntries = timeEntryRepository.findByUserIdOrderByEntryDateDesc(user.getId(), limit);
        return recentTimeEntries.stream().map(timeEntryMapper::toDto).toList();
    }

    public BigDecimal getHoursThisWeek(Long userId){
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1);

        List<TimeEntry> timeEntriesThisWeek = timeEntryRepository.findByUserIdAndEntryDateBetweenAndStatus(userId, startOfWeek, today ,Status.APPROVED);

        BigDecimal hoursThisWeek = BigDecimal.ZERO;

        for (TimeEntry timeEntry : timeEntriesThisWeek) {
            hoursThisWeek = hoursThisWeek.add(timeEntry.getTotalHours());
        }

        return hoursThisWeek;
    }

    public BigDecimal getHoursThisMonth(Long userId){
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
    // Number of time entries from their direct reports with status=PENDING waiting for the manager to approve
    public Integer getUserPendingCount(Long userId){
        return timeEntryRepository.countByUserIdAndStatus(userId,Status.PENDING);
    }

    public Integer getPendingTimeApprovalsCount(Long userId){
        return timeEntryRepository.countByUserManagerIdAndStatus(userId , Status.PENDING);
    }

    public TimeEntry createTimeEntryEntity(CreateTimeEntryDto request ,User user, Project project){
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

    public boolean existsTimeEntryForUserOnDate(User user, LocalDate entryDate){
        return timeEntryRepository.existsByUserAndEntryDate(user,entryDate);
    }

    public void validateTimeEntry(TimeEntry te){
        LocalDate now = LocalDate.now();

        if(te.getClockOutTime().isBefore(te.getClockInTime())){
            throw new InvalidTimeEntryException("Clock out time must be after clock in time");
        }

        if(te.getEntryDate().isAfter(now)){
            throw new InvalidTimeEntryException("Entry date cannot be in the future");
        }

        if(existsTimeEntryForUserOnDate(te.getUser(), te.getEntryDate())){
            throw new InvalidTimeEntryException("Time entry already exists in the date: "+te.getEntryDate());
        }

        if(leaveRequestService.hasActiveLeaveRequestOnDate(te.getUser(),te.getEntryDate(), List.of(Status.PENDING, Status.APPROVED))){
            throw new InvalidTimeEntryException("Time entry not allowed: user is on leave for this date");
        }

        if(te.getTotalHours().compareTo(BigDecimal.valueOf(24))>0){
            throw new InvalidTimeEntryException("Total hours cannot exceed 24 hours for a single day");
        }

        if(!te.getProject().getIsActive()){
            throw new InvalidTimeEntryException("Project is not active");
        }
    }

    @Transactional
    public TimeEntryDto create(CreateTimeEntryDto request , Long userId){
        User user = userService.getById(userId);
        Project project = projectService.getById(request.getProjectId());
        TimeEntry te = createTimeEntryEntity(request, user,project);
        validateTimeEntry(te);
        return timeEntryMapper.toDto(timeEntryRepository.save(te));
    }

    public List<TimeEntryDto> getByUserId(Long userId, Status status, LocalDate startDate, LocalDate endDate){
        Specification<TimeEntry> spec = Specification.where(TimeEntrySpecification.hasStatus(status)
                .and(TimeEntrySpecification.hasUserId(userId))
                .and(TimeEntrySpecification.afterDate(startDate))
                .and(TimeEntrySpecification.beforeDate(endDate)));
        return timeEntryRepository.findAll(spec).stream().map(timeEntryMapper::toDto).toList();
    }




}
