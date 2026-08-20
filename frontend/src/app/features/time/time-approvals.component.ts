import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { apiErrorMessage } from '../../core/http/api-error';
import { TimeEntry } from '../../core/models/time-entry.model';
import { TimeEntryService } from '../../core/services/time-entry.service';

@Component({
  selector: 'app-time-approvals',
  standalone: true,
  templateUrl: './time-approvals.component.html',
  styleUrl: './time-approvals.component.scss'
})
export class TimeApprovalsComponent implements OnInit {
  private readonly timeApi = inject(TimeEntryService);

  readonly entries = signal<TimeEntry[]>([]);
  readonly error = signal<string | null>(null);
  readonly loading = signal(true);
  readonly rejectingId = signal<number | null>(null);
  readonly reason = signal('');

  ngOnInit(): void {
    this.load();
  }

  employeeName(row: TimeEntry): string {
    return [row.userFirstName, row.userLastName].filter(Boolean).join(' ').trim();
  }

  approve(id: number): void {
    this.error.set(null);
    this.timeApi.approve(id).subscribe({
      next: () => this.load(),
      error: (err: HttpErrorResponse) => this.error.set(apiErrorMessage(err))
    });
  }

  startReject(id: number): void {
    this.rejectingId.set(id);
    this.reason.set('');
    this.error.set(null);
  }

  reject(id: number): void {
    const reason = this.reason().trim();
    if (!reason) {
      this.error.set('Reason is required.');
      return;
    }
    this.error.set(null);
    this.timeApi.reject(id, { reason }).subscribe({
      next: () => {
        this.rejectingId.set(null);
        this.reason.set('');
        this.load();
      },
      error: (err: HttpErrorResponse) => this.error.set(apiErrorMessage(err))
    });
  }

  approveCorrection(id: number): void {
    this.error.set(null);
    this.timeApi.approveCorrection(id).subscribe({
      next: () => this.load(),
      error: (err: HttpErrorResponse) => this.error.set(apiErrorMessage(err))
    });
  }

  denyCorrection(id: number): void {
    this.error.set(null);
    this.timeApi.denyCorrection(id).subscribe({
      next: () => this.load(),
      error: (err: HttpErrorResponse) => this.error.set(apiErrorMessage(err))
    });
  }

  private load(): void {
    this.loading.set(true);
    this.timeApi.getPendingApproval().subscribe({
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
