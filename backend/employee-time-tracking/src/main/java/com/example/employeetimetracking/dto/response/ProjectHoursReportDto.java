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
public class ProjectHoursReportDto {
    private LocalDate startDate;
    private LocalDate endDate;

    private Integer projectsCount;
    private Integer employeesCount;
    private BigDecimal totalHours;

    private List<ProjectHoursItemDto> projects; // sorted by totalHours desc
}

