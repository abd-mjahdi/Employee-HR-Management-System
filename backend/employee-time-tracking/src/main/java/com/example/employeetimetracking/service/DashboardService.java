package com.example.employeetimetracking.service;

import com.example.employeetimetracking.dto.response.*;
import com.example.employeetimetracking.mapper.LeaveBalanceMapper;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.UserRole;
import com.example.employeetimetracking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public DashboardStatsDto getDashboardStats(User user){
        UserRole role = user.getUserRole();
        Long userId = user.getId();

        if(role.equals(UserRole.EMPLOYEE)){
            return new DashboardStatsDto(timeEntryService.getHoursThisWeek(userId),
                    timeEntryService.getHoursThisMonth(userId) ,
                    timeEntryService.getUserPendingCount(userId) ,
                    leaveRequestService.getUserPendingCount(userId),
                    null ,
                    null ,
                    null ,
                    null ,
                    null);
        }
        if(role.equals(UserRole.MANAGER)){
            return new DashboardStatsDto(timeEntryService.getHoursThisWeek(userId),
                    timeEntryService.getHoursThisMonth(userId) ,
                    timeEntryService.getUserPendingCount(userId) ,
                    leaveRequestService.getUserPendingCount(userId),
                    timeEntryService.getPendingTimeApprovalsCount(userId),
                    leaveRequestService.getPendingLeaveApprovalsCount(userId),
                    leaveRequestService.getTeamMembersOnLeaveToday(userId),
                    null,
                    null);
        }
        if(role.equals(UserRole.HR_ADMIN)){
            return new DashboardStatsDto(timeEntryService.getHoursThisWeek(userId),
                    timeEntryService.getHoursThisMonth(userId) ,
                    timeEntryService.getUserPendingCount(userId) ,
                    leaveRequestService.getUserPendingCount(userId),
                    timeEntryService.getPendingTimeApprovalsCount(userId),
                    leaveRequestService.getPendingLeaveApprovalsCount(userId),
                    leaveRequestService.getTeamMembersOnLeaveToday(userId),
                    userRepository.countByIsActive(true),
                    leaveRequestService.getPendingHrApprovalsCount());
        }
        throw new IllegalArgumentException("Unexpected or unsupported user role");

    }


}
