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
public class TeamLeaveReportDto {
    private Long managerId;
    private LocalDate startDate;
    private LocalDate endDate;

    private Integer requestsCount;
    private BigDecimal totalLeaveDays;

    private List<TeamLeaveRequestItemDto> requests;     // who was out when
    private List<TimeSummaryItemDto> totalDaysByEmployee; // key=employee name, totalDays
    private List<TimeSummaryItemDto> totalDaysByLeaveType; // key=leave type name, totalDays
    private List<TeamLeaveRequestItemDto> upcomingApprovedLeave; // startDate > today
}
