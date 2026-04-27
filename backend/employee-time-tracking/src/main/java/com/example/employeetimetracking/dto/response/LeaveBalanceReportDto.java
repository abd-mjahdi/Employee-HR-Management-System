package com.example.employeetimetracking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LeaveBalanceReportDto {
    private Integer year;
    private Long departmentId; // null = all departments

    private Integer employeesCount;
    private Integer balancesCount;

    private List<LeaveBalanceReportItemDto> balances;

    // Highlights (simple forecasting-style signals)
    private List<LeaveBalanceReportItemDto> lowestBalances;  // lowest currentBalance
    private List<LeaveBalanceReportItemDto> highestBalances; // highest currentBalance

    private BigDecimal averageBalance; // across all balances returned
}

