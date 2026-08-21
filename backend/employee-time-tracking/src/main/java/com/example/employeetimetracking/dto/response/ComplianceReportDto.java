package com.example.employeetimetracking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceReportDto {
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer year;
    private Integer entitlementIssuesCount;
    private List<ComplianceEntitlementIssueDto> entitlementIssues;
}
