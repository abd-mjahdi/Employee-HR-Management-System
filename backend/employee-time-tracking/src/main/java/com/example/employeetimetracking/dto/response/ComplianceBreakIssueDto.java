package com.example.employeetimetracking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceBreakIssueDto {
    private Long employeeId;
    private String employeeName;
    private LocalDate entryDate;
    private Integer workedMinutes; // clock span minutes
    private Integer unpaidBreakMinutes;
    private Integer requiredBreakMinutes;
    private String issue; // short message
}

