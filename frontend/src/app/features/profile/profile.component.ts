import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { apiErrorMessage } from '../../core/http/api-error';
import { UserRole } from '../../core/models/auth.model';
import { AuthService } from '../../core/services/auth.service';
import { UserService } from '../../core/services/user.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss'
})
export class ProfileComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly users = inject(UserService);
  private readonly auth = inject(AuthService);

  readonly email = signal('');
  readonly role = signal<UserRole | ''>('');
  readonly error = signal<string | null>(null);
  readonly saved = signal(false);
  readonly loading = signal(true);
  readonly saving = signal(false);

  readonly form = this.fb.group({
    firstName: ['', [Validators.required, Validators.minLength(2)]],
    lastName: ['', [Validators.required, Validators.minLength(2)]]
  });

  ngOnInit(): void {
    const session = this.auth.currentUser();
    if (session) {
      this.email.set(session.email);
      this.role.set(session.role);
      this.form.patchValue({
        firstName: session.firstName ?? '',
        lastName: session.lastName ?? ''
      });
    }

    this.users.getMe().subscribe({
      next: (user) => {
        this.email.set(user.email);
        this.role.set(user.userRole);
        this.form.patchValue({
          firstName: user.firstName ?? '',
          lastName: user.lastName ?? ''
        });
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(apiErrorMessage(err));
        this.loading.set(false);
      }
    });
  }

  onSubmit(): void {
    this.saved.set(false);
    this.error.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const firstName = this.form.controls.firstName.value?.trim();
    const lastName = this.form.controls.lastName.value?.trim();
    if (!firstName || !lastName) {
      return;
    }
    this.saving.set(true);
    this.users.updateProfile({ firstName, lastName }).subscribe({
      next: () => {
        this.saving.set(false);
        this.saved.set(true);
        this.auth.fetchCurrentUser().subscribe();
      },
      error: (err: HttpErrorResponse) => {
        this.saving.set(false);
        this.error.set(apiErrorMessage(err));
      }
    });
  }
}
