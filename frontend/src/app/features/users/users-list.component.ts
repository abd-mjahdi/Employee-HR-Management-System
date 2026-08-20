import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { apiErrorMessage } from '../../core/http/api-error';
import { UserRole } from '../../core/models/auth.model';
import { Department } from '../../core/models/department.model';
import { UserResponse } from '../../core/models/user.model';
import { DepartmentService } from '../../core/services/department.service';
import { UserService } from '../../core/services/user.service';

const PAGE_SIZE = 20;

@Component({
  selector: 'app-users-list',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './users-list.component.html',
  styleUrl: './users-list.component.scss'
})
export class UsersListComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly usersApi = inject(UserService);
  private readonly departmentsApi = inject(DepartmentService);
  private readonly router = inject(Router);

  readonly roles: UserRole[] = ['EMPLOYEE', 'MANAGER', 'HR_ADMIN'];
  readonly users = signal<UserResponse[]>([]);
  readonly departments = signal<Department[]>([]);
  readonly error = signal<string | null>(null);
  readonly loading = signal(true);
  readonly filtering = signal(false);
  readonly page = signal(0);
  readonly totalPages = signal(0);

  readonly filters = this.fb.group({
    name: [''],
    departmentId: [''],
    role: [''],
    active: ['']
  });

  ngOnInit(): void {
    this.departmentsApi.list().subscribe({
      next: (rows) => this.departments.set(rows),
      error: (err: HttpErrorResponse) => this.error.set(apiErrorMessage(err))
    });
    this.loadPage(0);
  }

  departmentLabel(id: number | null): string {
    if (id == null) {
      return '';
    }
    const match = this.departments().find((d) => d.id === id);
    return match ? match.departmentName : String(id);
  }

  applyFilters(): void {
    this.error.set(null);
    if (!this.hasFilters()) {
      this.clearFilters();
      return;
    }
    this.filtering.set(true);
    this.loading.set(true);
    this.usersApi.search(this.searchParams()).subscribe({
      next: (rows) => {
        this.users.set(rows);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(apiErrorMessage(err));
        this.loading.set(false);
      }
    });
  }

  clearFilters(): void {
    this.filters.reset({ name: '', departmentId: '', role: '', active: '' });
    this.filtering.set(false);
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

  open(id: number): void {
    void this.router.navigate(['/people', id]);
  }

  private loadPage(page: number): void {
    this.error.set(null);
    this.loading.set(true);
    this.usersApi.getPage({ page, size: PAGE_SIZE }).subscribe({
      next: (result) => {
        this.users.set(result.content);
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

  private hasFilters(): boolean {
    const v = this.filters.getRawValue();
    return !!(v.name?.trim() || v.departmentId || v.role || v.active);
  }

  private searchParams() {
    const v = this.filters.getRawValue();
    return {
      name: v.name?.trim() || undefined,
      departmentId: v.departmentId ? Number(v.departmentId) : undefined,
      role: (v.role || undefined) as UserRole | undefined,
      active: v.active === '' ? undefined : v.active === 'true'
    };
  }
}
