import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { apiErrorMessage } from '../../core/http/api-error';
import { Project } from '../../core/models/project.model';
import { TimeEntry } from '../../core/models/time-entry.model';
import { ProjectService } from '../../core/services/project.service';
import { TimeEntryService } from '../../core/services/time-entry.service';

@Component({
  selector: 'app-time-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './time-form.component.html',
  styleUrl: './time-form.component.scss'
})
export class TimeFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly timeApi = inject(TimeEntryService);
  private readonly projectsApi = inject(ProjectService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly projects = signal<Project[]>([]);
  readonly error = signal<string | null>(null);
  readonly saving = signal(false);
  readonly loading = signal(false);
  readonly notFound = signal(false);
  readonly editingId = signal<number | null>(null);

  readonly form = this.fb.group({
    entryDate: ['', Validators.required],
    clockInTime: ['', Validators.required],
    clockOutTime: ['', Validators.required],
    projectId: ['', Validators.required],
    description: [''],
    breaks: this.fb.array([])
  });

  get breaks(): FormArray {
    return this.form.controls.breaks;
  }

  ngOnInit(): void {
    this.projectsApi.listActive().subscribe({
      next: (rows) => this.projects.set(rows),
      error: (err: HttpErrorResponse) => this.error.set(apiErrorMessage(err))
    });

    const rawId = this.route.snapshot.paramMap.get('id');
    if (!rawId) {
      return;
    }
    const id = Number(rawId);
    this.editingId.set(id);
    const fromState = history.state?.['entry'] as TimeEntry | undefined;
    if (fromState?.id === id) {
      this.applyEntry(fromState);
      return;
    }
    this.loading.set(true);
    this.timeApi.getMe().subscribe({
      next: (rows) => {
        const found = rows.find((row) => row.id === id);
        this.loading.set(false);
        if (!found) {
          this.notFound.set(true);
          this.error.set('Time entry not found.');
          return;
        }
        this.applyEntry(found);
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(apiErrorMessage(err));
      }
    });
  }

  addBreak(): void {
    this.breaks.push(this.breakGroup());
  }

  removeBreak(index: number): void {
    this.breaks.removeAt(index);
  }

  onSubmit(): void {
    this.error.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.error.set('Fill in the required fields.');
      return;
    }
    const body = this.toRequest();
    if (!body) {
      return;
    }
    const editingId = this.editingId();
    this.saving.set(true);
    const request$ = editingId != null
      ? this.timeApi.update(editingId, body)
      : this.timeApi.create(body);
    request$.subscribe({
      next: () => {
        this.saving.set(false);
        void this.router.navigate(['/time']);
      },
      error: (err: HttpErrorResponse) => {
        this.saving.set(false);
        this.error.set(apiErrorMessage(err));
      }
    });
  }

  deleteEntry(): void {
    const editingId = this.editingId();
    if (editingId == null || !window.confirm('Delete this time entry?')) {
      return;
    }
    this.error.set(null);
    this.timeApi.delete(editingId).subscribe({
      next: () => void this.router.navigate(['/time']),
      error: (err: HttpErrorResponse) => this.error.set(apiErrorMessage(err))
    });
  }

  private applyEntry(entry: TimeEntry): void {
    this.breaks.clear();
    for (const br of entry.breaks ?? []) {
      this.breaks.push(this.breakGroup(this.forTimeInput(br.breakStart), this.forTimeInput(br.breakEnd), br.isUnpaid));
    }
    this.form.patchValue({
      entryDate: entry.entryDate,
      clockInTime: this.forTimeInput(entry.clockInTime),
      clockOutTime: this.forTimeInput(entry.clockOutTime),
      projectId: entry.projectId != null ? String(entry.projectId) : '',
      description: entry.description ?? ''
    });
  }

  private breakGroup(start = '', end = '', isUnpaid = true) {
    return this.fb.group({
      breakStart: [start, Validators.required],
      breakEnd: [end, Validators.required],
      isUnpaid: [isUnpaid]
    });
  }

  private toRequest() {
    const v = this.form.getRawValue();
    const entryDate = v.entryDate;
    const clockInTime = this.withSeconds(v.clockInTime);
    const clockOutTime = this.withSeconds(v.clockOutTime);
    const projectId = Number(v.projectId);
    if (!entryDate || !clockInTime || !clockOutTime || !projectId) {
      return null;
    }
    const breaks = this.breaks.controls.map((ctrl) => {
      const b = ctrl.getRawValue() as { breakStart: string; breakEnd: string; isUnpaid: boolean };
      return {
        breakStart: this.withSeconds(b.breakStart),
        breakEnd: this.withSeconds(b.breakEnd),
        isUnpaid: !!b.isUnpaid
      };
    });
    return {
      entryDate,
      clockInTime,
      clockOutTime,
      projectId,
      description: v.description?.trim() || null,
      breaks
    };
  }

  private forTimeInput(value: string | null | undefined): string {
    const raw = (value ?? '').trim();
    return raw.length >= 5 ? raw.slice(0, 5) : raw;
  }

  private withSeconds(value: string | null | undefined): string {
    const raw = (value ?? '').trim();
    if (!raw) {
      return raw;
    }
    return raw.length === 5 ? `${raw}:00` : raw;
  }
}
