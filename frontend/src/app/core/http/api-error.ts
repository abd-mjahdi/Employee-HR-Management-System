import { HttpErrorResponse } from '@angular/common/http';

export function apiErrorMessage(err: HttpErrorResponse): string {
  if (err.status === 401) {
    return 'Invalid email or password. Please check your credentials.';
  }
  if (err.status === 403) {
    return 'Your account has been deactivated. Please contact your HR administrator.';
  }
  if (err.status === 404) {
    return 'Company not found for this address.';
  }
  if (err.error && typeof err.error === 'object' && err.error.message) {
    return err.error.message;
  }
  if (err.status === 0) {
    return 'Unable to connect to the backend on this company host (port 8080).';
  }
  return 'An unexpected error occurred. Please try again.';
}
