import { LeaveBalance, LeaveRequest } from './leave.model';
import { TimeEntry } from './time-entry.model';
import { UserResponse } from './user.model';

export interface DashboardStats {
  hoursThisWeek: number | null;
  hoursThisMonth: number | null;
  pendingTimeEntriesCount: number | null;
  pendingLeaveRequestsCount: number | null;
  pendingTimeApprovalsCount: number | null;
  pendingLeaveApprovalsCount: number | null;
  teamMembersOnLeaveToday: number | null;
  totalActiveEmployees: number | null;
}

export interface UserDashboard {
  user: UserResponse;
  leaveBalances: LeaveBalance[];
  upcomingLeave: LeaveRequest[];
  recentLeaveRequests: LeaveRequest[];
  recentTimeEntries: TimeEntry[];
  stats: DashboardStats;
}
