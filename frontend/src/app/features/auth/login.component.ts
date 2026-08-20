import { Component, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { TenantService } from '../../core/tenant/tenant.service';
import { companyDomainSuffix, companyPortalLoginUrl, parseCompanyDomainInput } from '../../core/tenant/tenant.util';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  readonly tenant = inject(TenantService);

  showPassword = signal<boolean>(false);
  rememberMe = signal<boolean>(true);
  domainError = signal<string | null>(null);

  isLoading = this.authService.isLoading;
  authError = this.authService.authError;

  readonly domainSuffix = companyDomainSuffix(window.location.hostname);

  loginForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(4)]]
  });

  domainForm = this.fb.group({
    company: ['', [Validators.required]]
  });

  togglePasswordVisibility(): void {
    this.showPassword.update((prev) => !prev);
  }

  fillDemoCredentials(): void {
    if (this.tenant.slug === 'globex') {
      this.loginForm.patchValue({
        email: 'emma.frost@globex.com',
        password: 'password'
      });
      return;
    }
    this.loginForm.patchValue({
      email: 'alice.morgan@company.com',
      password: 'password'
    });
  }

  onFindCompany(): void {
    this.domainError.set(null);
    if (this.domainForm.invalid) {
      this.domainForm.markAllAsTouched();
      return;
    }
    const slug = parseCompanyDomainInput(this.domainForm.controls.company.value);
    if (!slug) {
      this.domainError.set('Enter a valid company domain.');
      return;
    }
    window.location.assign(companyPortalLoginUrl(slug, window.location));
  }

  onSubmit(): void {
    if (!this.tenant.hasTenant) {
      return;
    }
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
