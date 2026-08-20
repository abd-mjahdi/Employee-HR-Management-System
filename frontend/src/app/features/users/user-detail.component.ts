import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { apiErrorMessage } from '../../core/http/api-error';
import { UserRole } from '../../core/models/auth.model';
import { Department } from '../../core/models/department.model';
import { UserResponse } from '../../core/models/user.model';
import { DepartmentService } from '../../core/services/department.service';
import { UserService } from '../../core/services/user.service';

@Component({
  selector: 'app-user-detail',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './user-detail.component.html',
  styleUrl: './user-detail.component.scss'
})
export class UserDetailComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly usersApi = inject(UserService);
  private readonly departmentsApi = inject(DepartmentService);

  readonly roles: UserRole[] = ['EMPLOYEE', 'MANAGER', 'HR_ADMIN'];
  readonly departments = signal<Department[]>([]);
  readonly managers = signal<UserResponse[]>([]);
  readonly hrAdmins = signal<UserResponse[]>([]);
  readonly user = signal<UserResponse | null>(null);
  readonly error = signal<string | null>(null);
  readonly loading = signal(true);
  readonly saving = signal(false);

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
    this.load();
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
    const current = this.user();
    if (!current || this.form.invalid) {
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
    this.usersApi.update(current.id, {
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
      next: (updated) => {
        this.saving.set(false);
        this.applyUser(updated);
      },
      error: (err: HttpErrorResponse) => {
        this.saving.set(false);
        this.error.set(apiErrorMessage(err));
      }
    });
  }

  deactivate(): void {
    const current = this.user();
    if (!current || !window.confirm('Deactivate this person?')) {
      return;
    }
    this.error.set(null);
    this.usersApi.deactivate(current.id).subscribe({
      next: () => this.load(),
      error: (err: HttpErrorResponse) => this.error.set(apiErrorMessage(err))
    });
  }

  activate(): void {
    const current = this.user();
    if (!current || !window.confirm('Activate this person?')) {
      return;
    }
    this.error.set(null);
    this.usersApi.activate(current.id).subscribe({
      next: () => this.load(),
      error: (err: HttpErrorResponse) => this.error.set(apiErrorMessage(err))
    });
  }

  private load(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.error.set('Person not found.');
      this.loading.set(false);
      return;
    }
    this.loading.set(true);
    this.usersApi.getById(id).subscribe({
      next: (user) => {
        this.applyUser(user);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(apiErrorMessage(err));
        this.loading.set(false);
      }
    });
  }

  private applyUser(user: UserResponse): void {
    this.user.set(user);
    this.form.reset({
      username: user.username,
      email: user.email,
      firstName: user.firstName,
      lastName: user.lastName,
      userRole: user.userRole,
      departmentId: user.departmentId != null ? String(user.departmentId) : '',
      managerMembershipId: user.managerId != null ? String(user.managerId) : ''
    });
  }
}
