package com.example.employeetimetracking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AbsencePatternsReportDto {
    private LocalDate startDate;
    private LocalDate endDate;

    private Integer employeesCount;
    private Integer flaggedEmployeesCount;

    private List<AbsencePatternEmployeeDto> employees; // sorted: flagged first, then most days
}

