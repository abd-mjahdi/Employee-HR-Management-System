import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { LeaveBalance } from '../models/leave.model';
import { TenantService } from '../tenant/tenant.service';

@Injectable({
  providedIn: 'root'
})
export class LeaveBalanceService {
  private readonly http = inject(HttpClient);
  private readonly tenant = inject(TenantService);

  getMe(): Observable<LeaveBalance[]> {
    return this.http.get<LeaveBalance[]>(this.tenant.url('/leave-balances/me'));
  }

  getForUser(userId: number): Observable<LeaveBalance[]> {
    return this.http.get<LeaveBalance[]>(this.tenant.url(`/leave-balances/user/${userId}`));
  }
}
