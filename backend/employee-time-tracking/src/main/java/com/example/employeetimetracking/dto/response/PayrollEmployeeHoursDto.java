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
public class PayrollEmployeeHoursDto {
    private Long employeeId;
    private String name;
    private BigDecimal regularHours;
    private BigDecimal overtimeHours;
    private BigDecimal totalHours;
}

