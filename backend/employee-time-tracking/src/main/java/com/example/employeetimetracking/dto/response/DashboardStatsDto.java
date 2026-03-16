package com.example.employeetimetracking.dto.response;

import java.math.BigDecimal;

public class DashboardStatsDto {

    private BigDecimal hoursThisWeek;
    private BigDecimal hoursThisMonth;
    private Integer pendingTimeEntriesCount;
    private Integer pendingLeaveRequestsCount;

    // Manager-specific stats (null for employees)
    private Integer pendingTimeApprovalsCount;
    private Integer pendingLeaveApprovalsCount;
    private Integer teamMembersOnLeaveToday;

    // HR-specific stats (null for non-HR)
    private Integer totalActiveEmployees;
    private Integer pendingHrApprovalsCount;
}
