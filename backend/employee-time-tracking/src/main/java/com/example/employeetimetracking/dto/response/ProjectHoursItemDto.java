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
public class ProjectHoursItemDto {
    private Long projectId;
    private String projectCode;
    private String projectName;

    private BigDecimal totalHours;
    private Integer employeesCount;
}
