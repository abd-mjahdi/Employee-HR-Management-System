import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { apiErrorMessage } from '../../core/http/api-error';
import { TimeEntry, TimeEntryStatus, TimeEntrySummary } from '../../core/models/time-entry.model';
import { TimeEntryService } from '../../core/services/time-entry.service';

@Component({
  selector: 'app-time-team',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './time-team.component.html',
  styleUrl: './time-team.component.scss'
})
export class TimeTeamComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly timeApi = inject(TimeEntryService);

  readonly statuses: TimeEntryStatus[] = [
    'PENDING',
    'APPROVED',
    'DENIED',
    'CANCELLED',
    'PENDING_CORRECTION',
    'CANCELLATION_PENDING'
  ];

  readonly entries = signal<TimeEntry[]>([]);
  readonly summary = signal<TimeEntrySummary | null>(null);
  readonly error = signal<string | null>(null);
  readonly loading = signal(true);

  readonly filters = this.fb.group({
    status: [''],
    startDate: [''],
    endDate: [''],
    name: ['']
  });

  readonly summaryForm = this.fb.group({
    startDate: ['',],
    endDate: [''],
    userId: ['']
  });

  ngOnInit(): void {
    this.loadEntries();
  }

  employeeName(row: TimeEntry): string {
    return [row.userFirstName, row.userLastName].filter(Boolean).join(' ').trim();
  }

  applyFilters(): void {
    this.loadEntries();
  }

  clearFilters(): void {
    this.filters.reset({ status: '', startDate: '', endDate: '', name: '' });
    this.loadEntries();
  }

  loadSummary(): void {
    const v = this.summaryForm.getRawValue();
    const startDate = v.startDate?.trim();
    const endDate = v.endDate?.trim();
    if (!startDate || !endDate) {
      this.error.set('Start date and end date are required for summary.');
      return;
    }
    this.error.set(null);
    const userRaw = v.userId?.toString().trim();
    this.timeApi.summary({
      startDate,
      endDate,
      userId: userRaw ? Number(userRaw) : undefined
    }).subscribe({
      next: (result) => this.summary.set(result),
      error: (err: HttpErrorResponse) => this.error.set(apiErrorMessage(err))
    });
  }

  private loadEntries(): void {
    this.error.set(null);
    this.loading.set(true);
    const v = this.filters.getRawValue();
    this.timeApi.getTeam({
      status: (v.status || undefined) as TimeEntryStatus | undefined,
      startDate: v.startDate || undefined,
      endDate: v.endDate || undefined,
      name: v.name?.trim() || undefined
    }).subscribe({
      next: (rows) => {
        this.entries.set(rows);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(apiErrorMessage(err));
        this.loading.set(false);
      }
    });
  }
}
