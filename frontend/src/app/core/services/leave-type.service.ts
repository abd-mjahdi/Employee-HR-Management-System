import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { LeaveType } from '../models/leave.model';
import { TenantService } from '../tenant/tenant.service';

@Injectable({
  providedIn: 'root'
})
export class LeaveTypeService {
  private readonly http = inject(HttpClient);
  private readonly tenant = inject(TenantService);

  list(): Observable<LeaveType[]> {
    return this.http.get<LeaveType[]>(this.tenant.url('/leave-types'));
  }
}
