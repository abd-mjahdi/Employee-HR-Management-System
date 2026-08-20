import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { apiErrorMessage } from '../../core/http/api-error';
import { Department } from '../../core/models/department.model';
import { DepartmentService } from '../../core/services/department.service';

@Component({
  selector: 'app-departments',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './departments.component.html',
  styleUrl: './departments.component.scss'
})
export class DepartmentsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly departmentsApi = inject(DepartmentService);

  readonly departments = signal<Department[]>([]);
  readonly error = signal<string | null>(null);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly editingId = signal<number | null>(null);

  readonly form = this.fb.group({
    departmentName: ['', Validators.required],
    departmentCode: ['', Validators.required],
    isActive: [true]
  });

  ngOnInit(): void {
    this.load();
  }

  onSubmit(): void {
    this.error.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const departmentName = this.form.controls.departmentName.value?.trim();
    const departmentCode = this.form.controls.departmentCode.value?.trim();
    if (!departmentName || !departmentCode) {
      return;
    }
    this.saving.set(true);
    const editingId = this.editingId();
    const request$ = editingId != null
      ? this.departmentsApi.update(editingId, {
          departmentName,
          departmentCode,
          isActive: !!this.form.controls.isActive.value
        })
      : this.departmentsApi.create({ departmentName, departmentCode });
    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.clearForm();
        this.load();
      },
      error: (err: HttpErrorResponse) => {
        this.saving.set(false);
        this.error.set(apiErrorMessage(err));
      }
    });
  }

  edit(row: Department): void {
    this.error.set(null);
    this.editingId.set(row.id);
    this.form.reset({
      departmentName: row.departmentName,
      departmentCode: row.departmentCode,
      isActive: row.isActive
    });
  }

  cancelEdit(): void {
    this.clearForm();
  }

  private clearForm(): void {
    this.editingId.set(null);
    this.form.reset({
      departmentName: '',
      departmentCode: '',
      isActive: true
    });
  }

  private load(): void {
    this.loading.set(true);
    this.departmentsApi.list().subscribe({
      next: (rows) => {
        this.departments.set(rows);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(apiErrorMessage(err));
        this.loading.set(false);
      }
    });
  }
}
