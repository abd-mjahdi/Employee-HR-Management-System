import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { TeamLeaveReport } from '../../core/models/report.model';
import { ReportService } from '../../core/services/report.service';
import { currentMonthRange } from './report-dates';
import { ReportDateFilterComponent } from './report-date-filter.component';
import { applyReportError } from './report-error';

@Component({
  selector: 'app-team-leave-report',
  standalone: true,
  imports: [ReportDateFilterComponent],
  templateUrl: './team-leave-report.component.html',
  styleUrl: './report-results.scss'
})
export class TeamLeaveReportComponent {
  private readonly reports = inject(ReportService);

  private readonly defaults = currentMonthRange();
  readonly startDate = signal(this.defaults.startDate);
  readonly endDate = signal(this.defaults.endDate);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly report = signal<TeamLeaveReport | null>(null);

  onLoad(): void {
    this.loading.set(true);
    this.error.set(null);
    this.reports.teamLeave(this.startDate(), this.endDate()).subscribe({
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
