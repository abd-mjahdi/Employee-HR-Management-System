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
public class LeaveBalanceReportItemDto {
    private Long employeeId;
    private String employeeName;
    private Long departmentId;
    private String departmentCode;
    private String departmentName;

    private Long leaveTypeId;
    private String leaveTypeName;

    private Integer year;
    private BigDecimal annualAllocation;
    private BigDecimal currentBalance;
    private BigDecimal balancePctOfAllocation; // 0..100 (null when allocation missing)
}

