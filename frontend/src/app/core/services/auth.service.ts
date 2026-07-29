import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, catchError, throwError, of } from 'rxjs';
import { LoginRequest, LoginResponse, User, UserRole } from '../models/auth.model';

const API_BASE_URL = 'http://localhost:8080';
const TOKEN_KEY = 'auth_jwt_token';
const USER_KEY = 'auth_user_data';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  // Reactive state signals
  readonly token = signal<string | null>(this.getStoredToken());
  readonly currentUser = signal<User | null>(this.getStoredUser());
  readonly isLoading = signal<boolean>(false);
  readonly authError = signal<string | null>(null);

  // Computed state
  readonly isAuthenticated = computed(() => !!this.token());

  constructor() {
    // If token exists on app init, attempt to refresh current user profile
    if (this.token()) {
      this.fetchCurrentUser().subscribe({
        error: () => this.logout() // token invalid/expired
      });
    }
  }

  login(credentials: LoginRequest): Observable<LoginResponse> {
    this.isLoading.set(true);
    this.authError.set(null);

    return this.http.post<LoginResponse>(`${API_BASE_URL}/auth/login`, credentials).pipe(
      tap((res) => {
        this.isLoading.set(false);
        if (res.success && res.token) {
          this.setSession(res.token, {
            email: res.email || credentials.email,
            role: res.role || 'EMPLOYEE'
          });
          // Fetch full user profile
          this.fetchCurrentUser().subscribe();
        } else {
          this.authError.set(res.message || 'Login failed');
        }
      }),
      catchError((err: HttpErrorResponse) => {
        this.isLoading.set(false);
        let errorMsg = 'An unexpected error occurred. Please try again.';

        if (err.status === 401) {
          errorMsg = 'Invalid email or password. Please check your credentials.';
        } else if (err.status === 403) {
          errorMsg = 'Your account has been deactivated. Please contact your HR administrator.';
        } else if (err.error && typeof err.error === 'object' && err.error.message) {
          errorMsg = err.error.message;
        } else if (err.status === 0) {
          errorMsg = 'Unable to connect to the backend server. Please verify the server is running on port 8080.';
        }

        this.authError.set(errorMsg);
        return throwError(() => new Error(errorMsg));
      })
    );
  }

  fetchCurrentUser(): Observable<User> {
    return this.http.get<User>(`${API_BASE_URL}/users/me`).pipe(
      tap((user) => {
        this.currentUser.set(user);
        localStorage.setItem(USER_KEY, JSON.stringify(user));
      }),
      catchError((err) => {
        return throwError(() => err);
      })
    );
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this.token.set(null);
    this.currentUser.set(null);
    this.authError.set(null);
    this.router.navigate(['/login']);
  }

  hasRole(role: UserRole): boolean {
    const user = this.currentUser();
    return user?.role === role;
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
