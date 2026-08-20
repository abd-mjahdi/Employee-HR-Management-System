import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getToken();
  const publicAuth = req.url.includes('/auth/login') || req.url.includes('/auth/invitations/accept');

  const outbound = token && !publicAuth
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(outbound).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 401 && token && !publicAuth) {
        authService.logout(true);
      }
      return throwError(() => err);
    })
  );
};
