import { Routes } from '@angular/router';
import { LoginComponent } from './features/auth/login.component';
import { InviteAcceptComponent } from './features/auth/invite-accept.component';
import { MarketingHomeComponent } from './features/auth/marketing-home.component';
import { DashboardLandingComponent } from './features/dashboard/dashboard-landing.component';
import { authGuard, guestGuard } from './core/guards/auth.guard';
import { marketingHomeGuard, tenantGuard } from './core/guards/tenant.guard';

export const routes: Routes = [
  {
    path: '',
    component: MarketingHomeComponent,
    canActivate: [marketingHomeGuard]
  },
  {
    path: 'login',
    component: LoginComponent,
    canActivate: [guestGuard]
  },
  {
    path: 'invite',
    component: InviteAcceptComponent,
    canActivate: [tenantGuard]
  },
  {
    path: 'dashboard',
    component: DashboardLandingComponent,
    canActivate: [tenantGuard, authGuard]
  },
  {
    path: 'open-company',
    redirectTo: 'login',
    pathMatch: 'full'
  },
  {
    path: '**',
    redirectTo: ''
  }
];
