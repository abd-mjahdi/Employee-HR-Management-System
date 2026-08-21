import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ComplianceReport,
  DepartmentUtilizationReport,
  EmployeeTimeReport,
  LeaveBalanceReport,
  PayrollReport,
  ProjectHoursReport,
  TeamLeaveReport
} from '../models/report.model';
import { TenantService } from '../tenant/tenant.service';

@Injectable({
  providedIn: 'root'
})
export class ReportService {
  private readonly http = inject(HttpClient);
  private readonly tenant = inject(TenantService);

  employeeTime(start: string, end: string, userId?: number | null): Observable<EmployeeTimeReport> {
    let params = this.rangeParams(start, end);
    params = this.setIfPresent(params, 'userId', userId);
    return this.http.get<EmployeeTimeReport>(this.tenant.url('/reports/employee-time'), { params });
  }

  teamLeave(start: string, end: string): Observable<TeamLeaveReport> {
    return this.http.get<TeamLeaveReport>(this.tenant.url('/reports/team-leave'), {
      params: this.rangeParams(start, end)
    });
  }

  payroll(start: string, end: string): Observable<PayrollReport> {
    return this.http.get<PayrollReport>(this.tenant.url('/reports/payroll'), {
      params: this.rangeParams(start, end)
    });
  }

  payrollCsv(start: string, end: string): Observable<Blob> {
    const params = this.rangeParams(start, end).set('format', 'csv');
    return this.http.get(this.tenant.url('/reports/payroll'), {
      params,
      responseType: 'blob'
    });
  }

  leaveBalances(year?: number | null, departmentId?: number | null): Observable<LeaveBalanceReport> {
    let params = new HttpParams();
    params = this.setIfPresent(params, 'year', year);
    params = this.setIfPresent(params, 'departmentId', departmentId);
    return this.http.get<LeaveBalanceReport>(this.tenant.url('/reports/leave-balances'), { params });
  }

  departmentUtilization(start: string, end: string): Observable<DepartmentUtilizationReport> {
    return this.http.get<DepartmentUtilizationReport>(
      this.tenant.url('/reports/department-utilization'),
      { params: this.rangeParams(start, end) }
    );
  }

  projectHours(start: string, end: string): Observable<ProjectHoursReport> {
    return this.http.get<ProjectHoursReport>(this.tenant.url('/reports/project-hours'), {
      params: this.rangeParams(start, end)
    });
  }

  compliance(start: string, end: string): Observable<ComplianceReport> {
    return this.http.get<ComplianceReport>(this.tenant.url('/reports/compliance'), {
      params: this.rangeParams(start, end)
    });
  }

  private rangeParams(start: string, end: string): HttpParams {
    return new HttpParams().set('startDate', start).set('endDate', end);
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
