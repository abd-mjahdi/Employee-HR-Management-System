import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CreateLeaveRequest,
  LeaveApprovalNotes,
  LeaveCancelRequest,
  LeaveDenyRequest,
  LeaveRequest,
  LeaveRequestReview
} from '../models/leave.model';
import { SpringPage } from '../models/page.model';
import { TimeEntryStatus } from '../models/time-entry.model';
import { TenantService } from '../tenant/tenant.service';

export interface LeaveRequestListParams {
  userId?: number;
  status?: TimeEntryStatus;
  startDate?: string;
  endDate?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export interface LeaveTeamParams {
  status?: TimeEntryStatus;
  startDate?: string;
  endDate?: string;
}

@Injectable({
  providedIn: 'root'
})
export class LeaveRequestService {
  private readonly http = inject(HttpClient);
  private readonly tenant = inject(TenantService);

  create(body: CreateLeaveRequest): Observable<LeaveRequest> {
    return this.http.post<LeaveRequest>(this.tenant.url('/leave-requests'), body);
  }

  getMe(): Observable<LeaveRequest[]> {
    return this.http.get<LeaveRequest[]>(this.tenant.url('/leave-requests/me'));
  }

  getById(id: number): Observable<LeaveRequest> {
    return this.http.get<LeaveRequest>(this.tenant.url(`/leave-requests/${id}`));
  }

  listAll(filters?: LeaveRequestListParams): Observable<SpringPage<LeaveRequestReview>> {
    return this.http.get<SpringPage<LeaveRequestReview>>(this.tenant.url('/leave-requests'), {
      params: this.listParams(filters)
    });
  }

  pending(): Observable<LeaveRequestReview[]> {
    return this.http.get<LeaveRequestReview[]>(this.tenant.url('/leave-requests/pending'));
  }

  cancellationPending(): Observable<LeaveRequestReview[]> {
    return this.http.get<LeaveRequestReview[]>(this.tenant.url('/leave-requests/cancellation-pending'));
  }

  team(filters?: LeaveTeamParams): Observable<LeaveRequestReview[]> {
    return this.http.get<LeaveRequestReview[]>(this.tenant.url('/leave-requests/team'), {
      params: this.filterParams(filters)
    });
  }

  approve(id: number, body?: LeaveApprovalNotes | null): Observable<void> {
    return this.http.post<void>(this.tenant.url(`/leave-requests/${id}/approve`), body ?? null);
  }

  deny(id: number, body: LeaveDenyRequest): Observable<void> {
    return this.http.post<void>(this.tenant.url(`/leave-requests/${id}/deny`), body);
  }

  cancel(id: number, body?: LeaveCancelRequest | null): Observable<void> {
    return this.http.post<void>(this.tenant.url(`/leave-requests/${id}/cancel`), body ?? null);
  }

  cancelApprove(id: number, body?: LeaveApprovalNotes | null): Observable<void> {
    return this.http.post<void>(this.tenant.url(`/leave-requests/${id}/cancel-approve`), body ?? null);
  }

  cancelDeny(id: number, body: LeaveDenyRequest): Observable<void> {
    return this.http.post<void>(this.tenant.url(`/leave-requests/${id}/cancel-deny`), body);
  }

  private listParams(filters?: LeaveRequestListParams): HttpParams {
    let params = this.filterParams(filters);
    if (!filters) {
      return params;
    }
    params = this.setIfPresent(params, 'userId', filters.userId);
    params = this.setIfPresent(params, 'page', filters.page);
    params = this.setIfPresent(params, 'size', filters.size);
    params = this.setIfPresent(params, 'sort', filters.sort);
    return params;
  }

  private filterParams(filters?: LeaveTeamParams): HttpParams {
    let params = new HttpParams();
    if (!filters) {
      return params;
    }
    params = this.setIfPresent(params, 'status', filters.status);
    params = this.setIfPresent(params, 'startDate', filters.startDate);
    params = this.setIfPresent(params, 'endDate', filters.endDate);
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
