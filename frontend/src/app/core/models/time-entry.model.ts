export type TimeEntryStatus =
  | 'PENDING'
  | 'APPROVED'
  | 'DENIED'
  | 'CANCELLED'
  | 'PENDING_CORRECTION'
  | 'CANCELLATION_PENDING';

export interface TimeEntryBreak {
  id: number;
  breakStart: string;
  breakEnd: string;
  isUnpaid: boolean;
  durationMinutes: number | null;
}

export interface TimeEntry {
  id: number;
  userId: number;
  userFirstName: string | null;
  userLastName: string | null;
  entryDate: string;
  clockInTime: string;
  clockOutTime: string;
  totalHours: number;
  projectId: number | null;
  projectName: string | null;
  projectCode: string | null;
  description: string | null;
  rejectionReason: string | null;
  correctionReason: string | null;
  status: TimeEntryStatus;
  breaks: TimeEntryBreak[];
}

export interface CreateTimeEntryBreakRequest {
  breakStart: string;
  breakEnd: string;
  isUnpaid?: boolean;
}

export interface CreateTimeEntryRequest {
  entryDate: string;
  clockInTime: string;
  clockOutTime: string;
  projectId: number;
  description?: string | null;
  breaks?: CreateTimeEntryBreakRequest[] | null;
}

export interface TimeEntryPersonalStats {
  totalHoursThisWeek: number;
  averageHoursPerDayThisMonth: number;
  topProjectCodeThisMonth: string | null;
  topProjectHoursThisMonth: number | null;
}

export interface TimeSummaryItem {
  key: string;
  totalHours: number;
}

export interface TimeEntrySummary {
  totalHours: number;
  byDate: TimeSummaryItem[];
  byProject: TimeSummaryItem[];
  byEmployee: TimeSummaryItem[];
}

export interface CorrectionRequest {
  explanation: string;
}

export interface TimeEntryRejection {
  reason: string;
}
