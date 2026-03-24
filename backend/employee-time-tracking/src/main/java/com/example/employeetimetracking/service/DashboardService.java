package com.example.employeetimetracking.service;

import com.example.employeetimetracking.dto.response.*;
import com.example.employeetimetracking.mapper.LeaveBalanceMapper;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.UserRole;
import com.example.employeetimetracking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class DashboardService {
    private final UserService userService;
    private final LeaveBalanceMapper leaveBalanceMapper;
    private final UserRepository userRepository;
    private final LeaveRequestService leaveRequestService;
    private final TimeEntryService timeEntryService;
    @Autowired
    public DashboardService(UserRepository userRepository ,
                       LeaveRequestService leaveRequestService,
                       TimeEntryService timeEntryService,
                       LeaveBalanceMapper leaveBalanceMapper,
                       UserService userService){
        this.userRepository = userRepository;
        this.leaveRequestService = leaveRequestService;
        this.timeEntryService = timeEntryService;
        this.leaveBalanceMapper = leaveBalanceMapper;
        this.userService = userService;
    }
    public UserDashboardDto getDashboardData(User authenticatedUser){

        UserResponseDto userResponseDto = userService.getUserDetails(authenticatedUser);
        List<LeaveBalanceDto> leaveBalances = authenticatedUser.getLeaveBalanceList().stream().map(leaveBalanceMapper::toDto).toList();
        List<LeaveRequestDto> upcomingLeave = leaveRequestService.getUpcomingLeave(authenticatedUser);

        // 7 most recent time entries
        List<TimeEntryDto> recentTimeEntries = timeEntryService.getRecentTimeEntries(authenticatedUser);

        DashboardStatsDto stats = getDashboardStats(authenticatedUser);
        return new UserDashboardDto(userResponseDto,leaveBalances,upcomingLeave,recentTimeEntries,stats);
    }

    public DashboardStatsDto getDashboardStats(User user) {
        UserRole role = user.getUserRole();
        Long userId = user.getId();

        BigDecimal hoursWeek = timeEntryService.getHoursThisWeek(userId);
        BigDecimal hoursMonth = timeEntryService.getHoursThisMonth(userId);
        Integer userPendingTime = timeEntryService.getUserPendingCount(userId);
        Integer userPendingLeave = leaveRequestService.getUserPendingCount(userId);

        Integer pendingTimeApprovals = null;
        Integer pendingLeaveApprovals = null;
        Integer teamOnLeave = null;
        Integer activeUsers = null;
        Integer hrPending = null;

        if (role == UserRole.MANAGER || role == UserRole.HR_ADMIN) {
            pendingTimeApprovals = timeEntryService.getPendingTimeApprovalsCount(userId);
            pendingLeaveApprovals = leaveRequestService.getPendingLeaveApprovalsCount(userId);
            teamOnLeave = leaveRequestService.getTeamMembersOnLeaveToday(userId);
        }

        if (role == UserRole.HR_ADMIN) {
            activeUsers = userRepository.countByIsActive(true);
            hrPending = leaveRequestService.getPendingHrApprovalsCount();
        }

        return new DashboardStatsDto(
                hoursWeek,
                hoursMonth,
                userPendingTime,
                userPendingLeave,
                pendingTimeApprovals,
                pendingLeaveApprovals,
                teamOnLeave,
                activeUsers,
                hrPending
        );
    }


}
