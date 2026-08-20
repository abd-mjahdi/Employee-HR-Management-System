package com.example.employeetimetracking.service;

import com.example.employeetimetracking.dto.response.*;
import com.example.employeetimetracking.exception.UserNotFoundException;
import com.example.employeetimetracking.mapper.UserMapper;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.UserRole;
import com.example.employeetimetracking.repository.UserRepository;
import com.example.employeetimetracking.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class DashboardService {
    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final LeaveRequestService leaveRequestService;
    private final TimeEntryService timeEntryService;
    private final LeaveBalanceService leaveBalanceService;
    @Autowired
    public DashboardService(UserRepository userRepository ,
                       LeaveRequestService leaveRequestService,
                       TimeEntryService timeEntryService,
                       LeaveBalanceService leaveBalanceService,
                       UserMapper userMapper){
        this.userRepository = userRepository;
        this.leaveRequestService = leaveRequestService;
        this.timeEntryService = timeEntryService;
        this.leaveBalanceService = leaveBalanceService;
        this.userMapper = userMapper;
    }
    public UserDashboardDto getDashboardData(CustomUserDetails authenticatedUser){
        Long id = authenticatedUser.getId();
        User user = userRepository.findById(id).orElseThrow(()-> new UserNotFoundException("User not found"));

        UserResponseDto userResponseDto = userMapper.toDto(user);
        int currentYear = LocalDate.now().getYear();
        List<LeaveBalanceDto> leaveBalances = leaveBalanceService.getByUserIdAndYear(id, currentYear);
        List<LeaveRequestDto> upcomingLeave = leaveRequestService.getUpcomingLeave(id, 10);
        List<LeaveRequestDto> recentLeaveRequests = leaveRequestService.getRecentLeaveRequests(id, 7);

        // 7 most recent time entries
        List<TimeEntryDto> recentTimeEntries = timeEntryService.getRecentTimeEntries(user);

        DashboardStatsDto stats = getDashboardStats(user, authenticatedUser.getRole());
        return new UserDashboardDto(userResponseDto,leaveBalances,upcomingLeave,recentLeaveRequests,recentTimeEntries,stats);
    }

    public DashboardStatsDto getDashboardStats(User user, UserRole role) {
        Long userId = user.getId();

        BigDecimal hoursWeek = timeEntryService.getHoursThisWeek(userId);
        BigDecimal hoursMonth = timeEntryService.getHoursThisMonth(userId);
        Integer userPendingTime = timeEntryService.getUserPendingCount(userId);
        Integer userPendingLeave = leaveRequestService.getUserPendingCount(userId);

        Integer pendingTimeApprovals = null;
        Integer pendingLeaveApprovals = null;
        Integer teamOnLeave = null;
        Integer activeUsers = null;

        if (role == UserRole.MANAGER || role == UserRole.HR_ADMIN) {
            pendingTimeApprovals = timeEntryService.getPendingTimeApprovalsCount(userId);
            pendingLeaveApprovals = leaveRequestService.getPendingLeaveApprovalsCount(
                    userId, role == UserRole.HR_ADMIN);
            teamOnLeave = leaveRequestService.getTeamMembersOnLeaveToday(userId);
        }

        if (role == UserRole.HR_ADMIN) {
            activeUsers = userRepository.countByIsActive(true);
        }

        return new DashboardStatsDto(
                hoursWeek,
                hoursMonth,
                userPendingTime,
                userPendingLeave,
                pendingTimeApprovals,
                pendingLeaveApprovals,
                teamOnLeave,
                activeUsers
        );
    }


}
