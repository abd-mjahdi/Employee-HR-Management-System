package com.example.employeetimetracking.service;

import com.example.employeetimetracking.dto.response.TimeEntryDto;
import com.example.employeetimetracking.mapper.TimeEntryMapper;
import com.example.employeetimetracking.model.entities.TimeEntry;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.Status;
import com.example.employeetimetracking.repository.TimeEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;

@Service
public class TimeEntryService {
    private final TimeEntryRepository timeEntryRepository;
    private final TimeEntryMapper timeEntryMapper;
    @Autowired
    public TimeEntryService(
                            TimeEntryRepository timeEntryRepository,
                            TimeEntryMapper timeEntryMapper
    ){
        this.timeEntryRepository = timeEntryRepository;
        this.timeEntryMapper = timeEntryMapper;
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


}
