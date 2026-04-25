package com.example.employeetimetracking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TimeEntryPersonalStatsDto {
    private BigDecimal totalHoursThisWeek;
    private BigDecimal averageHoursPerDayThisMonth;
    private String topProjectCodeThisMonth;
    private BigDecimal topProjectHoursThisMonth;
}
