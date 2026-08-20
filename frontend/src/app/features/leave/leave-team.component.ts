import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { apiErrorMessage } from '../../core/http/api-error';
import { LeaveRequestReview } from '../../core/models/leave.model';
import { TimeEntryStatus } from '../../core/models/time-entry.model';
import { LeaveRequestService } from '../../core/services/leave-request.service';

@Component({
  selector: 'app-leave-team',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './leave-team.component.html',
  styleUrl: './leave-team.component.scss'
})
export class LeaveTeamComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly leaveApi = inject(LeaveRequestService);

  readonly statuses: TimeEntryStatus[] = [
    'PENDING',
    'APPROVED',
    'DENIED',
    'CANCELLED',
    'PENDING_CORRECTION',
    'CANCELLATION_PENDING'
  ];

  readonly rows = signal<LeaveRequestReview[]>([]);
  readonly error = signal<string | null>(null);
  readonly loading = signal(true);

  readonly filters = this.fb.group({
    status: [''],
    startDate: [''],
    endDate: ['']
  });

  ngOnInit(): void {
    this.load();
  }

  applyFilters(): void {
    this.load();
  }

  clearFilters(): void {
    this.filters.reset({ status: '', startDate: '', endDate: '' });
    this.load();
  }

  private load(): void {
    this.error.set(null);
    this.loading.set(true);
    const v = this.filters.getRawValue();
    this.leaveApi.team({
      status: (v.status || undefined) as TimeEntryStatus | undefined,
      startDate: v.startDate || undefined,
      endDate: v.endDate || undefined
    }).subscribe({
      next: (rows) => {
        this.rows.set(rows);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(apiErrorMessage(err));
        this.loading.set(false);
      }
    });
  }
}
