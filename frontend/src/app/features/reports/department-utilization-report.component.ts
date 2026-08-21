import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { DepartmentUtilizationReport } from '../../core/models/report.model';
import { AuthService } from '../../core/services/auth.service';
import { ReportService } from '../../core/services/report.service';
import { currentMonthRange } from './report-dates';
import { ReportDateFilterComponent } from './report-date-filter.component';
import { applyReportError } from './report-error';
import { hrAdminMayRequest } from './report-hr';

@Component({
  selector: 'app-department-utilization-report',
  standalone: true,
  imports: [ReportDateFilterComponent],
  templateUrl: './department-utilization-report.component.html',
  styleUrl: './report-results.scss'
})
export class DepartmentUtilizationReportComponent implements OnInit {
  private readonly reports = inject(ReportService);
  private readonly auth = inject(AuthService);

  private readonly defaults = currentMonthRange();
  readonly startDate = signal(this.defaults.startDate);
  readonly endDate = signal(this.defaults.endDate);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly report = signal<DepartmentUtilizationReport | null>(null);

  ngOnInit(): void {
    this.onLoad();
  }

  onLoad(): void {
    if (!hrAdminMayRequest(this.auth, this.error)) {
      this.report.set(null);
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.reports.departmentUtilization(this.startDate(), this.endDate()).subscribe({
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
}
