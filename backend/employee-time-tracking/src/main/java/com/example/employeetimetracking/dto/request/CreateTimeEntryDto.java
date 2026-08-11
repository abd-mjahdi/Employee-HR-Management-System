package com.example.employeetimetracking.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
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

    /**
     * Optional breaks submitted with the time entry (timesheet-style).
     * On create: omitted/empty means no breaks.
     * On update: null keeps existing breaks; non-null replaces the full set.
     */
    @Valid
    private List<CreateTimeEntryBreakDto> breaks;
}
