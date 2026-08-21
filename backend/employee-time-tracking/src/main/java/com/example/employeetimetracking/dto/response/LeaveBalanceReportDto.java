package com.example.employeetimetracking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LeaveBalanceReportDto {
    private Integer year;
    private Long departmentId;

    private Integer employeesCount;
    private Integer balancesCount;

    private List<LeaveBalanceReportItemDto> balances;
}
