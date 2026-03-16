package com.example.employeetimetracking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserDashboardDto {
    private UserResponseDto user;
    private List<LeaveBalanceDto> leaveBalances;
    private List<LeaveRequestDto> upcomingLeave;
    private List<TimeEntryDto> recentTimeEntries;
    private DashboardStatsDto stats;  // hours this week, pending approvals count
}