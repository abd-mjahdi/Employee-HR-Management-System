package com.example.employeetimetracking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeTimeReportDto {
    private Long userId;
    private LocalDate startDate;
    private LocalDate endDate;

    private BigDecimal totalHours;
    private BigDecimal averageHoursPerDay;
    private Integer daysWithEntries;
    private Integer entriesCount;

    private List<TimeSummaryItemDto> dailyHours;      // key = YYYY-MM-DD
    private List<TimeSummaryItemDto> projectBreakdown; // key = projectCode
}
