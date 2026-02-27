package com.example.employeetimetracking.dto.response;

import com.example.employeetimetracking.model.entities.Project;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TimeEntryDto {
    private Long id;
    private Long userId;
    private String userFirstName;
    private String userLastName;
    private LocalDate entryDate;
    private LocalTime clockInTime;
    private LocalTime clockOutTime;
    private BigDecimal totalHours;
    private Long projectId;
    private String projectName;
    private String projectCode;
    private String description;
    private Status status;
}
