import { TimeEntryStatus } from './time-entry.model';

export interface LeaveType {
  id: number;
  typeName: string;
  description: string | null;
  isActive: boolean;
}

export interface LeaveBalance {
  id: number;
  userId: number;
  leaveTypeId: number;
  leaveTypeName: string;
  year: number;
  currentBalance: number;
  lastAccrualDate: string | null;
}

export interface LeaveRequest {
  id: number;
  userId: number;
  leaveTypeId: number;
  leaveTypeName: string;
  startDate: string;
  endDate: string;
  totalDays: number;
  reason: string | null;
  status: TimeEntryStatus;
  managerNotes: string | null;
  cancellationReason: string | null;
}

export interface LeaveRequestReview {
  id: number;
  employeeName: string;
  leaveTypeName: string;
  startDate: string;
  endDate: string;
  totalDays: number;
  reason: string | null;
  status: TimeEntryStatus;
  managerNotes: string | null;
  cancellationReason: string | null;
}

export interface CreateLeaveRequest {
  leaveTypeId: number;
  startDate: string;
  endDate: string;
  reason: string;
}

export interface LeaveDenyRequest {
  reason: string;
}

export interface LeaveCancelRequest {
  reason?: string | null;
}

export interface LeaveApprovalNotes {
  notes?: string | null;
}
