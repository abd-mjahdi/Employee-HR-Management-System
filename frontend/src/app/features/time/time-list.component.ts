import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { apiErrorMessage } from '../../core/http/api-error';
import { TimeEntry, TimeEntryPersonalStats, TimeEntryStatus } from '../../core/models/time-entry.model';
import { TimeEntryService } from '../../core/services/time-entry.service';

@Component({
  selector: 'app-time-list',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './time-list.component.html',
  styleUrl: './time-list.component.scss'
})
export class TimeListComponent implements OnInit {
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
  readonly stats = signal<TimeEntryPersonalStats | null>(null);
  readonly error = signal<string | null>(null);
  readonly loading = signal(true);
  readonly correctingId = signal<number | null>(null);
  readonly explanation = signal('');

  readonly filters = this.fb.group({
    status: [''],
    startDate: [''],
    endDate: ['']
  });

  ngOnInit(): void {
    this.timeApi.getMyStats().subscribe({
      next: (stats) => this.stats.set(stats),
      error: (err: HttpErrorResponse) => this.error.set(apiErrorMessage(err))
    });
    this.loadEntries();
  }

  applyFilters(): void {
    this.loadEntries();
  }

  clearFilters(): void {
    this.filters.reset({ status: '', startDate: '', endDate: '' });
    this.loadEntries();
  }

  deleteEntry(id: number): void {
    if (!window.confirm('Delete this time entry?')) {
      return;
    }
    this.error.set(null);
    this.timeApi.delete(id).subscribe({
      next: () => {
        this.loadEntries();
        this.timeApi.getMyStats().subscribe({
          next: (stats) => this.stats.set(stats),
          error: (err: HttpErrorResponse) => this.error.set(apiErrorMessage(err))
        });
      },
      error: (err: HttpErrorResponse) => this.error.set(apiErrorMessage(err))
    });
  }

  startCorrection(id: number): void {
    this.correctingId.set(id);
    this.explanation.set('');
    this.error.set(null);
  }

  submitCorrection(id: number): void {
    const explanation = this.explanation().trim();
    if (!explanation) {
      this.error.set('Explanation is required.');
      return;
    }
    this.error.set(null);
    this.timeApi.requestCorrection(id, { explanation }).subscribe({
      next: () => {
        this.correctingId.set(null);
        this.explanation.set('');
        this.loadEntries();
      },
      error: (err: HttpErrorResponse) => this.error.set(apiErrorMessage(err))
    });
  }

  private loadEntries(): void {
    this.error.set(null);
    this.loading.set(true);
    const v = this.filters.getRawValue();
    this.timeApi.getMe({
      status: (v.status || undefined) as TimeEntryStatus | undefined,
      startDate: v.startDate || undefined,
      endDate: v.endDate || undefined
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
