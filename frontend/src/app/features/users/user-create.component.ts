import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { apiErrorMessage } from '../../core/http/api-error';
import { UserRole } from '../../core/models/auth.model';
import { Department } from '../../core/models/department.model';
import { UserCreatedResponse, UserResponse } from '../../core/models/user.model';
import { DepartmentService } from '../../core/services/department.service';
import { UserService } from '../../core/services/user.service';

@Component({
  selector: 'app-user-create',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './user-create.component.html',
  styleUrl: './user-create.component.scss'
})
export class UserCreateComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly usersApi = inject(UserService);
  private readonly departmentsApi = inject(DepartmentService);

  readonly roles: UserRole[] = ['EMPLOYEE', 'MANAGER', 'HR_ADMIN'];
  readonly departments = signal<Department[]>([]);
  readonly managers = signal<UserResponse[]>([]);
  readonly hrAdmins = signal<UserResponse[]>([]);
  readonly error = signal<string | null>(null);
  readonly saving = signal(false);
  readonly created = signal<UserCreatedResponse | null>(null);

  readonly form = this.fb.group({
    username: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    userRole: ['EMPLOYEE' as UserRole, Validators.required],
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
    const role = this.form.controls.userRole.value;
    if (role === 'MANAGER') {
      return this.hrAdmins();
    }
    if (role === 'EMPLOYEE') {
      return this.managers();
    }
    return [];
  }

  needsManager(): boolean {
    const role = this.form.controls.userRole.value;
    return role === 'EMPLOYEE' || role === 'MANAGER';
  }

  onSubmit(): void {
    this.error.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const username = v.username?.trim();
    const email = v.email?.trim();
    const firstName = v.firstName?.trim();
    const lastName = v.lastName?.trim();
    const userRole = v.userRole as UserRole;
    const departmentId = Number(v.departmentId);
    if (!username || !email || !firstName || !lastName || !userRole || !departmentId) {
      return;
    }
    const managerRaw = v.managerMembershipId?.toString().trim();
    const managerMembershipId = managerRaw ? Number(managerRaw) : null;
    this.saving.set(true);
    this.usersApi.create({
      username,
      email,
      firstName,
      lastName,
      userRole,
      departmentId,
      ...(managerMembershipId != null && userRole !== 'HR_ADMIN'
        ? { managerMembershipId }
        : {})
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
