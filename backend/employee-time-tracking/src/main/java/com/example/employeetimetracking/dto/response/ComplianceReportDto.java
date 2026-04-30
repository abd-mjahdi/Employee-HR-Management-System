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
public class ComplianceReportDto {
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer year;

    // Leave granted in period
    private BigDecimal totalLeaveDaysGranted;
    private List<TimeSummaryItemDto> leaveDaysByType; // key = leave type name

    // Overtime in period
    private OvertimeSummaryReportDto overtimeSummary;

    // Break adherence (based on recorded unpaid breaks)
    private Boolean breakAdherenceSupported;
    private String breakAdherenceNote;
    private Integer breakIssuesCount;
    private List<ComplianceBreakIssueDto> breakIssues;

    // Minimum legal entitlements (basic consistency checks)
    private Integer entitlementIssuesCount;
    private List<ComplianceEntitlementIssueDto> entitlementIssues;
}

