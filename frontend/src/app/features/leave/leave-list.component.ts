import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { apiErrorMessage } from '../../core/http/api-error';
import { LeaveBalance, LeaveRequest } from '../../core/models/leave.model';
import { LeaveBalanceService } from '../../core/services/leave-balance.service';
import { LeaveRequestService } from '../../core/services/leave-request.service';

@Component({
  selector: 'app-leave-list',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './leave-list.component.html',
  styleUrl: './leave-list.component.scss'
})
export class LeaveListComponent implements OnInit {
  private readonly balancesApi = inject(LeaveBalanceService);
  private readonly requestsApi = inject(LeaveRequestService);

  readonly balances = signal<LeaveBalance[]>([]);
  readonly requests = signal<LeaveRequest[]>([]);
  readonly error = signal<string | null>(null);
  readonly loading = signal(true);
  readonly cancellingId = signal<number | null>(null);
  readonly cancelReason = signal('');

  ngOnInit(): void {
    this.load();
  }

  startCancel(id: number): void {
    this.cancellingId.set(id);
    this.cancelReason.set('');
    this.error.set(null);
  }

  submitCancel(id: number): void {
    this.error.set(null);
    const reason = this.cancelReason().trim();
    this.requestsApi.cancel(id, reason ? { reason } : null).subscribe({
      next: () => {
        this.cancellingId.set(null);
        this.cancelReason.set('');
        this.load(false);
      },
      error: (err: HttpErrorResponse) => this.error.set(apiErrorMessage(err))
    });
  }

  private load(showLoading = true): void {
    if (showLoading) {
      this.loading.set(true);
    }
    let pending = 2;
    const done = () => {
      pending -= 1;
      if (pending === 0) {
        this.loading.set(false);
      }
    };
    this.balancesApi.getMe().subscribe({
      next: (rows) => {
        this.balances.set(rows);
        done();
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(apiErrorMessage(err));
        done();
      }
    });
    this.requestsApi.getMe().subscribe({
      next: (rows) => {
        this.requests.set(rows);
        done();
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(apiErrorMessage(err));
        done();
      }
    });
  }
}
