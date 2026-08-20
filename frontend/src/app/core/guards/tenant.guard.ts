import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { TenantService } from '../tenant/tenant.service';

export const tenantGuard: CanActivateFn = () => {
  const tenant = inject(TenantService);
  const router = inject(Router);
  if (tenant.hasTenant) {
    return true;
  }
  return router.createUrlTree(['/login']);
};

export const marketingHomeGuard: CanActivateFn = () => {
  const tenant = inject(TenantService);
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!tenant.hasTenant) {
    return true;
  }
  if (auth.isAuthenticated()) {
    return router.createUrlTree(['/dashboard']);
  }
  return router.createUrlTree(['/login']);
};
