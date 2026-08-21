import { WritableSignal } from '@angular/core';
import { AuthService } from '../../core/services/auth.service';

export function hrAdminMayRequest(auth: AuthService, error: WritableSignal<string | null>): boolean {
  if (auth.hasRole('HR_ADMIN')) {
    return true;
  }
  error.set('You do not have access to this report.');
  return false;
}
