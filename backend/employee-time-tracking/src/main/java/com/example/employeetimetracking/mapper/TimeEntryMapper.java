package com.example.employeetimetracking.mapper;

import com.example.employeetimetracking.dto.response.TimeEntryDto;
import com.example.employeetimetracking.model.entities.TimeEntry;
import org.springframework.stereotype.Component;

@Component
public class TimeEntryMapper {
    public TimeEntryDto toDto(TimeEntry timeEntry) {
        return new TimeEntryDto(
                timeEntry.getId(),
                timeEntry.getUser().getId(),
                timeEntry.getUser().getFirstName(),
                timeEntry.getUser().getLastName(),
                timeEntry.getEntryDate(),
                timeEntry.getClockInTime(),
                timeEntry.getClockOutTime(),
                timeEntry.getTotalHours(),
                timeEntry.getProject().getId(),
                timeEntry.getProject().getProjectName(),
                timeEntry.getProject().getProjectCode(),
                timeEntry.getDescription(),
                timeEntry.getStatus()
        );
    }
}

