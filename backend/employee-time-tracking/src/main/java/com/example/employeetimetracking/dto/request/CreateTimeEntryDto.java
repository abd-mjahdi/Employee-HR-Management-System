package com.example.employeetimetracking.dto.request;

import com.example.employeetimetracking.model.entities.Project;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public class CreateTimeEntryDto {
    @NotNull
    private LocalDate entryDate;

    @NotNull
    private LocalTime clockInTime;

    @NotNull
    private LocalTime clockOutTime;

    @NotNull
    private Long projectId;

    private String description;

}
