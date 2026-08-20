import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UserDashboard } from '../models/dashboard.model';
import { TenantService } from '../tenant/tenant.service';

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly tenant = inject(TenantService);

  getMine(): Observable<UserDashboard> {
    return this.http.get<UserDashboard>(this.tenant.url('/users/me/dashboard'));
  }
}
