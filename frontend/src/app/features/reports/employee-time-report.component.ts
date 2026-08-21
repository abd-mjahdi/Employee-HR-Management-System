import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { EmployeeTimeReport } from '../../core/models/report.model';
import { AuthService } from '../../core/services/auth.service';
import { ReportService } from '../../core/services/report.service';
import { currentMonthRange } from './report-dates';
import { ReportDateFilterComponent } from './report-date-filter.component';
import { applyReportError } from './report-error';

@Component({
  selector: 'app-employee-time-report',
  standalone: true,
  imports: [FormsModule, ReportDateFilterComponent],
  templateUrl: './employee-time-report.component.html',
  styleUrl: './report-results.scss'
})
export class EmployeeTimeReportComponent {
  private readonly reports = inject(ReportService);
  private readonly auth = inject(AuthService);

  private readonly defaults = currentMonthRange();
  readonly startDate = signal(this.defaults.startDate);
  readonly endDate = signal(this.defaults.endDate);
  readonly userIdRaw = signal('');
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly report = signal<EmployeeTimeReport | null>(null);
  readonly canPickUser = this.auth.hasAnyRole(['MANAGER', 'HR_ADMIN']);

  onLoad(): void {
    this.loading.set(true);
    this.error.set(null);
    const userId = this.parseUserId();
    this.reports.employeeTime(this.startDate(), this.endDate(), userId).subscribe({
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

  private parseUserId(): number | undefined {
    if (!this.canPickUser) {
      return undefined;
    }
    const raw = String(this.userIdRaw() ?? '').trim();
    if (!raw) {
      return undefined;
    }
    const value = Number(raw);
    return Number.isFinite(value) ? value : undefined;
  }
}
