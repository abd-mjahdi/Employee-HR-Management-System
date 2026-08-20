import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TenantService } from '../tenant/tenant.service';
import {
  CorrectionRequest,
  CreateTimeEntryBreakRequest,
  CreateTimeEntryRequest,
  TimeEntry,
  TimeEntryBreak,
  TimeEntryPersonalStats,
  TimeEntryRejection,
  TimeEntryStatus,
  TimeEntrySummary
} from '../models/time-entry.model';

export interface TimeEntryListParams {
  status?: TimeEntryStatus;
  startDate?: string;
  endDate?: string;
}

export interface TimeEntryTeamParams extends TimeEntryListParams {
  name?: string;
}

export interface TimeEntrySummaryParams {
  startDate: string;
  endDate: string;
  userId?: number;
}

@Injectable({
  providedIn: 'root'
})
export class TimeEntryService {
  private readonly http = inject(HttpClient);
  private readonly tenant = inject(TenantService);

  create(body: CreateTimeEntryRequest): Observable<TimeEntry> {
    return this.http.post<TimeEntry>(this.tenant.url('/time-entries'), body);
  }

  getMe(filters?: TimeEntryListParams): Observable<TimeEntry[]> {
    return this.http.get<TimeEntry[]>(this.tenant.url('/time-entries/me'), {
      params: this.filterParams(filters)
    });
  }

  getMyStats(): Observable<TimeEntryPersonalStats> {
    return this.http.get<TimeEntryPersonalStats>(this.tenant.url('/time-entries/stats/me'));
  }

  update(id: number, body: CreateTimeEntryRequest): Observable<TimeEntry> {
    return this.http.put<TimeEntry>(this.tenant.url(`/time-entries/${id}`), body);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(this.tenant.url(`/time-entries/${id}`));
  }

  addBreak(id: number, body: CreateTimeEntryBreakRequest): Observable<TimeEntryBreak> {
    return this.http.post<TimeEntryBreak>(this.tenant.url(`/time-entries/${id}/breaks`), body);
  }

  listBreaks(id: number): Observable<TimeEntryBreak[]> {
    return this.http.get<TimeEntryBreak[]>(this.tenant.url(`/time-entries/${id}/breaks`));
  }

  deleteBreak(id: number, breakId: number): Observable<void> {
    return this.http.delete<void>(this.tenant.url(`/time-entries/${id}/breaks/${breakId}`));
  }

  requestCorrection(id: number, body: CorrectionRequest): Observable<void> {
    return this.http.post<void>(this.tenant.url(`/time-entries/${id}/correction-request`), body);
  }

  getTeam(filters?: TimeEntryTeamParams): Observable<TimeEntry[]> {
    return this.http.get<TimeEntry[]>(this.tenant.url('/time-entries/team'), {
      params: this.filterParams(filters)
    });
  }

  getPendingApproval(): Observable<TimeEntry[]> {
    return this.http.get<TimeEntry[]>(this.tenant.url('/time-entries/pending-approval'));
  }

  approve(id: number): Observable<void> {
    return this.http.post<void>(this.tenant.url(`/time-entries/${id}/approve`), null);
  }

  reject(id: number, body: TimeEntryRejection): Observable<void> {
    return this.http.post<void>(this.tenant.url(`/time-entries/${id}/reject`), body);
  }

  approveCorrection(id: number): Observable<void> {
    return this.http.post<void>(this.tenant.url(`/time-entries/${id}/correction-approve`), null);
  }

  denyCorrection(id: number): Observable<void> {
    return this.http.post<void>(this.tenant.url(`/time-entries/${id}/correction-deny`), null);
  }

  summary(params: TimeEntrySummaryParams): Observable<TimeEntrySummary> {
    let httpParams = new HttpParams()
      .set('startDate', params.startDate)
      .set('endDate', params.endDate);
    if (params.userId != null) {
      httpParams = httpParams.set('userId', String(params.userId));
    }
    return this.http.get<TimeEntrySummary>(this.tenant.url('/time-entries/summary'), {
      params: httpParams
    });
  }

  private filterParams(filters?: TimeEntryTeamParams): HttpParams {
    let params = new HttpParams();
    if (!filters) {
      return params;
    }
    params = this.setIfPresent(params, 'status', filters.status);
    params = this.setIfPresent(params, 'startDate', filters.startDate);
    params = this.setIfPresent(params, 'endDate', filters.endDate);
    params = this.setIfPresent(params, 'name', filters.name);
    return params;
  }

  private setIfPresent(params: HttpParams, key: string, value: string | number | undefined | null): HttpParams {
    if (value == null) {
      return params;
    }
    const text = String(value).trim();
    if (!text) {
      return params;
    }
    return params.set(key, text);
  }
}
