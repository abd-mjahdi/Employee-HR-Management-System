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
public class DepartmentUtilizationItemDto {
    private Long departmentId;
    private String departmentCode;
    private String departmentName;

    private BigDecimal totalHours;
    private Integer employeesCount;
}
