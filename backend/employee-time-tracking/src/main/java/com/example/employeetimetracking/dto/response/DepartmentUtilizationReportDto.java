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
public class DepartmentUtilizationReportDto {
    private LocalDate startDate;
    private LocalDate endDate;

    private BigDecimal totalHours;
    private Integer departmentsCount;
    private Integer employeesCount;

    private List<DepartmentUtilizationItemDto> departments; // sorted by totalHours desc
}

