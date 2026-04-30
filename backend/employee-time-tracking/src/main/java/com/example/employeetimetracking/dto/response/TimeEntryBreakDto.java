package com.example.employeetimetracking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TimeEntryBreakDto {
    private Long id;
    private LocalTime breakStart;
    private LocalTime breakEnd;
    private Boolean isUnpaid;
    private Integer durationMinutes;
}

