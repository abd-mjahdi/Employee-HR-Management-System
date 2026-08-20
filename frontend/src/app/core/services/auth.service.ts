import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, catchError, throwError, map } from 'rxjs';
import { apiErrorMessage } from '../http/api-error';
import { LoginRequest, LoginResponse, User, UserRole } from '../models/auth.model';
import { TenantService } from '../tenant/tenant.service';

const TOKEN_KEY = 'auth_jwt_token';
const USER_KEY = 'auth_user_data';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private tenant = inject(TenantService);

  readonly token = signal<string | null>(this.getStoredToken());
  readonly currentUser = signal<User | null>(this.getStoredUser());
  readonly isLoading = signal<boolean>(false);
  readonly authError = signal<string | null>(null);

  readonly isAuthenticated = computed(() => !!this.token());

  constructor() {
    if (this.token() && this.tenant.hasTenant) {
      this.fetchCurrentUser().subscribe({
        error: () => this.logout(false)
      });
    }
  }

  login(credentials: LoginRequest): Observable<LoginResponse> {
    this.isLoading.set(true);
    this.authError.set(null);

    if (!this.tenant.hasTenant) {
      this.isLoading.set(false);
      const errorMsg = 'Open your company login URL before signing in.';
      this.authError.set(errorMsg);
      return throwError(() => new Error(errorMsg));
    }

    return this.http.post<LoginResponse>(this.tenant.url('/auth/login'), credentials).pipe(
      tap((res) => {
        this.isLoading.set(false);
        if (res.success && res.token) {
          this.setSession(res.token, {
            email: res.email || credentials.email,
            role: res.role || 'EMPLOYEE'
          });
          this.fetchCurrentUser().subscribe();
        } else {
          this.authError.set(res.message || 'Login failed');
        }
      }),
      catchError((err: HttpErrorResponse) => {
        this.isLoading.set(false);
        const errorMsg = apiErrorMessage(err);
        this.authError.set(errorMsg);
        return throwError(() => new Error(errorMsg));
      })
    );
  }

  fetchCurrentUser(): Observable<User> {
    return this.http.get<Record<string, unknown>>(this.tenant.url('/users/me')).pipe(
      map((dto) => this.toUser(dto)),
      tap((user) => {
        this.currentUser.set(user);
        localStorage.setItem(USER_KEY, JSON.stringify(user));
      }),
      catchError((err) => throwError(() => err))
    );
  }

  logout(redirectToLogin = true): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this.token.set(null);
    this.currentUser.set(null);
    this.authError.set(null);
    if (redirectToLogin) {
      this.router.navigate(['/login']);
    }
  }

  hasRole(role: UserRole): boolean {
    return this.currentUser()?.role === role;
  }

  hasAnyRole(roles: UserRole[]): boolean {
    const role = this.currentUser()?.role;
    return !!role && roles.includes(role);
  }

  getToken(): string | null {
    return this.token();
  }

  private setSession(token: string, partialUser: Partial<User>): void {
    localStorage.setItem(TOKEN_KEY, token);
    this.token.set(token);

    const user: User = {
      email: partialUser.email || '',
      role: (partialUser.role as UserRole) || 'EMPLOYEE'
    };

    this.currentUser.set(user);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  }

  private toUser(dto: Record<string, unknown>, fallback?: Partial<User>): User {
    const role = (dto['userRole'] || dto['role'] || fallback?.role || 'EMPLOYEE') as UserRole;
    return {
      id: dto['id'] as number | undefined,
      email: (dto['email'] as string) || fallback?.email || '',
      firstName: dto['firstName'] as string | undefined,
      lastName: dto['lastName'] as string | undefined,
      role,
      departmentId: dto['departmentId'] as number | undefined,
      active: (dto['isActive'] as boolean | undefined) ?? (dto['active'] as boolean | undefined)
    };
  }

  private getStoredToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  private getStoredUser(): User | null {
    const raw = localStorage.getItem(USER_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw);
    } catch {
      return null;
    }
  }
}
