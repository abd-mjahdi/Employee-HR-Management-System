package com.example.employeetimetracking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TimeEntrySummaryDto {
    private BigDecimal totalHours;
    private List<TimeSummaryItemDto> byDate;
    private List<TimeSummaryItemDto> byProject;
    private List<TimeSummaryItemDto> byEmployee;
}

