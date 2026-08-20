import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { apiErrorMessage } from '../../core/http/api-error';
import { UserRole } from '../../core/models/auth.model';
import { Department } from '../../core/models/department.model';
import { InvitationCreated } from '../../core/models/invitation.model';
import { UserResponse } from '../../core/models/user.model';
import { DepartmentService } from '../../core/services/department.service';
import { InvitationService } from '../../core/services/invitation.service';
import { UserService } from '../../core/services/user.service';

@Component({
  selector: 'app-invite-create',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './invite-create.component.html',
  styleUrl: './invite-create.component.scss'
})
export class InviteCreateComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly invitations = inject(InvitationService);
  private readonly usersApi = inject(UserService);
  private readonly departmentsApi = inject(DepartmentService);

  readonly roles: UserRole[] = ['EMPLOYEE', 'MANAGER', 'HR_ADMIN'];
  readonly departments = signal<Department[]>([]);
  readonly managers = signal<UserResponse[]>([]);
  readonly hrAdmins = signal<UserResponse[]>([]);
  readonly error = signal<string | null>(null);
  readonly saving = signal(false);
  readonly created = signal<InvitationCreated | null>(null);

  readonly acceptUrl = computed(() => {
    const invite = this.created();
    if (!invite?.token) {
      return '';
    }
    return `${window.location.origin}/invite?token=${invite.token}`;
  });

  readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    role: ['EMPLOYEE' as UserRole, Validators.required],
    departmentId: ['', Validators.required],
    managerMembershipId: ['']
  });

  ngOnInit(): void {
    this.departmentsApi.list().subscribe({
      next: (rows) => this.departments.set(rows),
      error: (err: HttpErrorResponse) => this.error.set(apiErrorMessage(err))
    });
    this.usersApi.search({ role: 'MANAGER' }).subscribe({
      next: (rows) => this.managers.set(rows),
      error: (err: HttpErrorResponse) => this.error.set(apiErrorMessage(err))
    });
    this.usersApi.search({ role: 'HR_ADMIN' }).subscribe({
      next: (rows) => this.hrAdmins.set(rows),
      error: (err: HttpErrorResponse) => this.error.set(apiErrorMessage(err))
    });
  }

  supervisorOptions(): UserResponse[] {
    const role = this.form.controls.role.value;
    if (role === 'MANAGER') {
      return this.hrAdmins();
    }
    if (role === 'EMPLOYEE') {
      return this.managers();
    }
    return [];
  }

  needsManager(): boolean {
    const role = this.form.controls.role.value;
    return role === 'EMPLOYEE' || role === 'MANAGER';
  }

  onSubmit(): void {
    this.error.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const email = v.email?.trim();
    const role = v.role as UserRole;
    const departmentId = Number(v.departmentId);
    if (!email || !role || !departmentId) {
      return;
    }
    const managerRaw = v.managerMembershipId?.toString().trim();
    const managerMembershipId = managerRaw ? Number(managerRaw) : null;
    this.saving.set(true);
    this.invitations.create({
      email,
      role,
      departmentId,
      ...(managerMembershipId != null && role !== 'HR_ADMIN' ? { managerMembershipId } : {})
    }).subscribe({
      next: (res) => {
        this.saving.set(false);
        this.created.set(res);
      },
      error: (err: HttpErrorResponse) => {
        this.saving.set(false);
        this.error.set(apiErrorMessage(err));
      }
    });
  }
}
