import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';

const API_BASE_URL = 'http://localhost:8080';

interface AcceptInvitationResponse {
  email: string;
  companySlug: string;
  message: string;
}

@Component({
  selector: 'app-invite-accept',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './invite-accept.component.html',
  styleUrl: './invite-accept.component.scss'
})
export class InviteAcceptComponent {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private http = inject(HttpClient);
  private router = inject(Router);

  isLoading = signal(false);
  error = signal<string | null>(null);
  success = signal(false);

  form = this.fb.group({
    firstName: ['', [Validators.required, Validators.maxLength(50)]],
    lastName: ['', [Validators.required, Validators.maxLength(50)]],
    password: ['', [Validators.required, Validators.minLength(6)]]
  });

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.error.set('This invite link is missing a token.');
      return;
    }

    const { firstName, lastName, password } = this.form.value;
    this.isLoading.set(true);
    this.error.set(null);

    this.http.post<AcceptInvitationResponse>(`${API_BASE_URL}/auth/invitations/accept`, {
      token,
      firstName,
      lastName,
      password
    }).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.success.set(true);
      },
      error: (err: HttpErrorResponse) => {
        this.isLoading.set(false);
        this.error.set(this.messageFrom(err));
      }
    });
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }

  private messageFrom(err: HttpErrorResponse): string {
    if (err.status === 0) {
      return 'Unable to connect to the server.';
    }
    if (err.error && typeof err.error === 'object' && err.error.message) {
      return err.error.message;
    }
    return 'Could not accept this invitation.';
  }
}
