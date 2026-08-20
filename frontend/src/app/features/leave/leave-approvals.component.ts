import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { apiErrorMessage } from '../../core/http/api-error';
import { LeaveRequestReview } from '../../core/models/leave.model';
import { LeaveRequestService } from '../../core/services/leave-request.service';

type LeaveAction = 'approve' | 'deny' | 'cancelApprove' | 'cancelDeny';

@Component({
  selector: 'app-leave-approvals',
  standalone: true,
  templateUrl: './leave-approvals.component.html',
  styleUrl: './leave-approvals.component.scss'
})
export class LeaveApprovalsComponent implements OnInit {
  private readonly leaveApi = inject(LeaveRequestService);

  readonly pending = signal<LeaveRequestReview[]>([]);
  readonly cancellationPending = signal<LeaveRequestReview[]>([]);
  readonly error = signal<string | null>(null);
  readonly loading = signal(true);
  readonly actingId = signal<number | null>(null);
  readonly action = signal<LeaveAction | null>(null);
  readonly text = signal('');

  ngOnInit(): void {
    this.load();
  }

  start(id: number, action: LeaveAction): void {
    this.actingId.set(id);
    this.action.set(action);
    this.text.set('');
    this.error.set(null);
  }

  isActing(id: number, action: LeaveAction): boolean {
    return this.actingId() === id && this.action() === action;
  }

  submit(): void {
    const id = this.actingId();
    const action = this.action();
    if (id == null || !action) {
      return;
    }
    const value = this.text().trim();
    if ((action === 'deny' || action === 'cancelDeny') && !value) {
      this.error.set('Reason is required.');
      return;
    }
    this.error.set(null);
    const notes = value ? { notes: value } : null;
    const reason = { reason: value };
    const request$ =
      action === 'approve' ? this.leaveApi.approve(id, notes) :
      action === 'deny' ? this.leaveApi.deny(id, reason) :
      action === 'cancelApprove' ? this.leaveApi.cancelApprove(id, notes) :
      this.leaveApi.cancelDeny(id, reason);
    request$.subscribe({
      next: () => {
        this.actingId.set(null);
        this.action.set(null);
        this.text.set('');
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
    this.leaveApi.pending().subscribe({
      next: (rows) => {
        this.pending.set(rows);
        done();
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(apiErrorMessage(err));
        done();
      }
    });
    this.leaveApi.cancellationPending().subscribe({
      next: (rows) => {
        this.cancellationPending.set(rows);
        done();
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(apiErrorMessage(err));
        done();
      }
    });
  }
}
