export interface TimeSummaryItem {
  key: string;
  totalHours: number;
}

export interface EmployeeTimeReport {
  userId: number;
  startDate: string;
  endDate: string;
  totalHours: number;
  averageHoursPerDay: number;
  daysWithEntries: number;
  entriesCount: number;
  dailyHours: TimeSummaryItem[];
  projectBreakdown: TimeSummaryItem[];
}

export interface TeamLeaveRequestItem {
  id: number;
  userId: number;
  employeeName: string;
  leaveTypeName: string;
  startDate: string;
  endDate: string;
  totalDays: number;
}

export interface TeamLeaveReport {
  managerId: number;
  startDate: string;
  endDate: string;
  requestsCount: number;
  totalLeaveDays: number;
  requests: TeamLeaveRequestItem[];
}

export interface PayrollEmployeeHours {
  employeeId: number;
  name: string;
  regularHours: number;
  overtimeHours: number;
  totalHours: number;
}

export interface PayrollReport {
  startDate: string;
  endDate: string;
  dailyOvertimeThresholdHours: number;
  weeklyOvertimeThresholdHours: number;
  totalRegularHours: number;
  totalOvertimeHours: number;
  totalHours: number;
  employees: PayrollEmployeeHours[];
}

export interface LeaveBalanceReportItem {
  employeeId: number;
  employeeName: string;
  departmentId: number;
  departmentCode: string;
  departmentName: string;
  leaveTypeId: number;
  leaveTypeName: string;
  year: number;
  annualAllocation: number;
  currentBalance: number;
}

export interface LeaveBalanceReport {
  year: number;
  departmentId: number | null;
  employeesCount: number;
  balancesCount: number;
  balances: LeaveBalanceReportItem[];
}

export interface DepartmentUtilizationItem {
  departmentId: number;
  departmentCode: string;
  departmentName: string;
  totalHours: number;
  employeesCount: number;
}

export interface DepartmentUtilizationReport {
  startDate: string;
  endDate: string;
  totalHours: number;
  departmentsCount: number;
  employeesCount: number;
  departments: DepartmentUtilizationItem[];
}

export interface ProjectHoursItem {
  projectId: number;
  projectCode: string;
  projectName: string;
  totalHours: number;
  employeesCount: number;
}

export interface ProjectHoursReport {
  startDate: string;
  endDate: string;
  projectsCount: number;
  employeesCount: number;
  totalHours: number;
  projects: ProjectHoursItem[];
}

export interface ComplianceEntitlementIssue {
  employeeId: number;
  employeeName: string;
  leaveTypeId: number;
  leaveTypeName: string;
  year: number;
  issue: string;
}

export interface ComplianceReport {
  startDate: string;
  endDate: string;
  year: number;
  entitlementIssuesCount: number;
  entitlementIssues: ComplianceEntitlementIssue[];
}
