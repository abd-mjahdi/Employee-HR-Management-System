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
public class AbsencePatternEmployeeDto {
    private Long employeeId;
    private String employeeName;
    private Long departmentId;
    private String departmentCode;

    private Integer leaveRequestsCount;
    private BigDecimal totalLeaveDays;

    private Integer mondayStarts;
    private Integer fridayStarts;
    private Integer mondayOrFridayStarts;

    private Integer sickRequestsCount;
    private Integer sickClustersCount; // sick leaves starting within N days of previous sick leave

    private Boolean flagged; // true when any heuristic triggers
    private String flagReason; // short explanation
}

