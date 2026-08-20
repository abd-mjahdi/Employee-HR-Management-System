import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { apiErrorMessage } from '../../core/http/api-error';
import { LeaveRequestReview } from '../../core/models/leave.model';
import { TimeEntryStatus } from '../../core/models/time-entry.model';
import { LeaveRequestService } from '../../core/services/leave-request.service';

const PAGE_SIZE = 20;

@Component({
  selector: 'app-leave-all',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './leave-all.component.html',
  styleUrl: './leave-all.component.scss'
})
export class LeaveAllComponent implements OnInit {
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
  readonly page = signal(0);
  readonly totalPages = signal(0);

  readonly filters = this.fb.group({
    userId: [''],
    status: [''],
    startDate: [''],
    endDate: ['']
  });

  ngOnInit(): void {
    this.loadPage(0);
  }

  applyFilters(): void {
    this.loadPage(0);
  }

  clearFilters(): void {
    this.filters.reset({ userId: '', status: '', startDate: '', endDate: '' });
    this.loadPage(0);
  }

  prevPage(): void {
    if (this.page() > 0) {
      this.loadPage(this.page() - 1);
    }
  }

  nextPage(): void {
    if (this.page() + 1 < this.totalPages()) {
      this.loadPage(this.page() + 1);
    }
  }

  private loadPage(page: number): void {
    this.error.set(null);
    this.loading.set(true);
    const v = this.filters.getRawValue();
    const userRaw = v.userId?.toString().trim();
    this.leaveApi.listAll({
      page,
      size: PAGE_SIZE,
      userId: userRaw ? Number(userRaw) : undefined,
      status: (v.status || undefined) as TimeEntryStatus | undefined,
      startDate: v.startDate || undefined,
      endDate: v.endDate || undefined
    }).subscribe({
      next: (result) => {
        this.rows.set(result.content);
        this.page.set(result.number);
        this.totalPages.set(result.totalPages);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(apiErrorMessage(err));
        this.loading.set(false);
      }
    });
  }
}
