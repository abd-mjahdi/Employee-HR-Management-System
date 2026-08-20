import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { apiErrorMessage } from '../../core/http/api-error';
import { LeaveType } from '../../core/models/leave.model';
import { LeaveRequestService } from '../../core/services/leave-request.service';
import { LeaveTypeService } from '../../core/services/leave-type.service';

@Component({
  selector: 'app-leave-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './leave-form.component.html',
  styleUrl: './leave-form.component.scss'
})
export class LeaveFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly typesApi = inject(LeaveTypeService);
  private readonly requestsApi = inject(LeaveRequestService);
  private readonly router = inject(Router);

  readonly types = signal<LeaveType[]>([]);
  readonly error = signal<string | null>(null);
  readonly saving = signal(false);

  readonly form = this.fb.group({
    leaveTypeId: ['', Validators.required],
    startDate: ['', Validators.required],
    endDate: ['', Validators.required],
    reason: ['', Validators.required]
  });

  ngOnInit(): void {
    this.typesApi.list().subscribe({
      next: (rows) => this.types.set(rows),
      error: (err: HttpErrorResponse) => this.error.set(apiErrorMessage(err))
    });
  }

  onSubmit(): void {
    this.error.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.error.set('Fill in the required fields.');
      return;
    }
    const v = this.form.getRawValue();
    const leaveTypeId = Number(v.leaveTypeId);
    const startDate = v.startDate;
    const endDate = v.endDate;
    const reason = v.reason?.trim();
    if (!leaveTypeId || !startDate || !endDate || !reason) {
      return;
    }
    this.saving.set(true);
    this.requestsApi.create({ leaveTypeId, startDate, endDate, reason }).subscribe({
      next: () => {
        this.saving.set(false);
        void this.router.navigate(['/leave']);
      },
      error: (err: HttpErrorResponse) => {
        this.saving.set(false);
        this.error.set(apiErrorMessage(err));
      }
    });
  }
}
