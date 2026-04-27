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
public class OvertimeSummaryReportDto {
    private LocalDate startDate;
    private LocalDate endDate;

    private BigDecimal dailyOvertimeThresholdHours;
    private BigDecimal weeklyOvertimeThresholdHours;

    private Integer employeesCount;
    private Integer overtimeEmployeesCount;

    private BigDecimal totalOvertimeHours;

    private List<OvertimeSummaryEmployeeDto> employees; // sorted by overtime desc
}

