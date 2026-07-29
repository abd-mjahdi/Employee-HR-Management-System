import { Component, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  showPassword = signal<boolean>(false);
  rememberMe = signal<boolean>(true);

  isLoading = this.authService.isLoading;
  authError = this.authService.authError;

  loginForm = this.fb.group({
    email: ['adminadmin@example.com', [Validators.required, Validators.email]],
    password: ['admin123', [Validators.required, Validators.minLength(4)]]
  });

  togglePasswordVisibility(): void {
    this.showPassword.update((prev) => !prev);
  }

  fillAdminCredentials(): void {
    this.loginForm.patchValue({
      email: 'adminadmin@example.com',
      password: 'admin123'
    });
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    const { email, password } = this.loginForm.value;
    if (!email || !password) return;

    this.authService.login({ email, password }).subscribe({
      next: (res) => {
        if (res.success) {
          this.router.navigate(['/dashboard']);
        }
      }
    });
  }
}
