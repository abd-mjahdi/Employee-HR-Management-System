import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { Department } from '../../core/models/department.model';
import { LeaveBalanceReport } from '../../core/models/report.model';
import { AuthService } from '../../core/services/auth.service';
import { DepartmentService } from '../../core/services/department.service';
import { ReportService } from '../../core/services/report.service';
import { ReportDateFilterComponent } from './report-date-filter.component';
import { applyReportError } from './report-error';

@Component({
  selector: 'app-leave-balances-report',
  standalone: true,
  imports: [FormsModule, ReportDateFilterComponent],
  templateUrl: './leave-balances-report.component.html',
  styleUrl: './report-results.scss'
})
export class LeaveBalancesReportComponent implements OnInit {
  private readonly reports = inject(ReportService);
  private readonly departmentsApi = inject(DepartmentService);
  private readonly auth = inject(AuthService);

  readonly yearRaw = signal(String(new Date().getFullYear()));
  readonly departmentIdRaw = signal('');
  readonly departments = signal<Department[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly report = signal<LeaveBalanceReport | null>(null);
  readonly isHr = this.auth.hasRole('HR_ADMIN');

  ngOnInit(): void {
    if (this.isHr) {
      this.departmentsApi.list().subscribe({
        next: (departments) => this.departments.set(departments),
        error: (err: HttpErrorResponse) => applyReportError(err, (message) => this.error.set(message))
      });
    }
    this.onLoad();
  }

  onYearChange(value: string | number | null): void {
    this.yearRaw.set(value == null || value === '' ? '' : `${value}`);
  }

  onDepartmentChange(value: string | number | null): void {
    this.departmentIdRaw.set(value == null || value === '' ? '' : `${value}`);
  }

  onLoad(): void {
    this.loading.set(true);
    this.error.set(null);
    this.reports.leaveBalances(this.parseYear(), this.parseDepartmentId()).subscribe({
      next: (report) => {
        this.report.set(report);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.report.set(null);
        applyReportError(err, (message) => {
          this.error.set(message);
          this.loading.set(false);
        });
      }
    });
  }

  private parseYear(): number | undefined {
    const raw = String(this.yearRaw() ?? '').trim();
    if (!raw) {
      return undefined;
    }
    const value = Number(raw);
    return Number.isFinite(value) ? value : undefined;
  }

  private parseDepartmentId(): number | undefined {
    if (!this.isHr) {
      return undefined;
    }
    const raw = String(this.departmentIdRaw() ?? '').trim();
    if (!raw) {
      return undefined;
    }
    const value = Number(raw);
    return Number.isFinite(value) ? value : undefined;
  }
}
