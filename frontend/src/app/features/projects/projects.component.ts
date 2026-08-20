import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { apiErrorMessage } from '../../core/http/api-error';
import { Project } from '../../core/models/project.model';
import { ProjectService } from '../../core/services/project.service';

@Component({
  selector: 'app-projects',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './projects.component.html',
  styleUrl: './projects.component.scss'
})
export class ProjectsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly projectsApi = inject(ProjectService);

  readonly projects = signal<Project[]>([]);
  readonly error = signal<string | null>(null);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly editingId = signal<number | null>(null);

  readonly form = this.fb.group({
    projectName: ['', Validators.required],
    projectCode: ['', Validators.required],
    description: [''],
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
    const projectName = this.form.controls.projectName.value?.trim();
    const projectCode = this.form.controls.projectCode.value?.trim();
    if (!projectName || !projectCode) {
      return;
    }
    const description = this.form.controls.description.value?.trim() || null;
    this.saving.set(true);
    const editingId = this.editingId();
    const request$ = editingId != null
      ? this.projectsApi.update(editingId, {
          projectName,
          projectCode,
          description,
          isActive: !!this.form.controls.isActive.value
        })
      : this.projectsApi.create({ projectName, projectCode, description });
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

  edit(row: Project): void {
    this.error.set(null);
    this.editingId.set(row.id);
    this.form.reset({
      projectName: row.projectName,
      projectCode: row.projectCode,
      description: row.description ?? '',
      isActive: row.isActive
    });
  }

  cancelEdit(): void {
    this.clearForm();
  }

  private clearForm(): void {
    this.editingId.set(null);
    this.form.reset({
      projectName: '',
      projectCode: '',
      description: '',
      isActive: true
    });
  }

  private load(): void {
    this.loading.set(true);
    this.projectsApi.listActive().subscribe({
      next: (rows) => {
        this.projects.set(rows);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(apiErrorMessage(err));
        this.loading.set(false);
      }
    });
  }
}
