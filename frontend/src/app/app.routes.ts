import { Routes } from '@angular/router';
import { LoginComponent } from './features/auth/login.component';
import { InviteAcceptComponent } from './features/auth/invite-accept.component';
import { MarketingHomeComponent } from './features/auth/marketing-home.component';
import { DashboardLandingComponent } from './features/dashboard/dashboard-landing.component';
import { AppLayoutComponent } from './features/layout/app-layout.component';
import { PagePlaceholderComponent } from './features/layout/page-placeholder.component';
import { authGuard, guestGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { marketingHomeGuard, tenantGuard } from './core/guards/tenant.guard';
import { UserRole } from './core/models/auth.model';

const MANAGER_PLUS: UserRole[] = ['MANAGER', 'HR_ADMIN'];
const HR_ONLY: UserRole[] = ['HR_ADMIN'];

export const routes: Routes = [
  {
    path: '',
    component: MarketingHomeComponent,
    pathMatch: 'full',
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
    path: '',
    component: AppLayoutComponent,
    canActivate: [tenantGuard, authGuard],
    children: [
      { path: 'dashboard', component: DashboardLandingComponent },
      { path: 'profile', component: PagePlaceholderComponent },
      { path: 'time/new', component: PagePlaceholderComponent },
      {
        path: 'time/approvals',
        component: PagePlaceholderComponent,
        canActivate: [roleGuard],
        data: { roles: MANAGER_PLUS }
      },
      {
        path: 'time/team',
        component: PagePlaceholderComponent,
        canActivate: [roleGuard],
        data: { roles: MANAGER_PLUS }
      },
      { path: 'time/:id/edit', component: PagePlaceholderComponent },
      { path: 'time', component: PagePlaceholderComponent },
      { path: 'leave/new', component: PagePlaceholderComponent },
      {
        path: 'leave/approvals',
        component: PagePlaceholderComponent,
        canActivate: [roleGuard],
        data: { roles: MANAGER_PLUS }
      },
      {
        path: 'leave/team',
        component: PagePlaceholderComponent,
        canActivate: [roleGuard],
        data: { roles: MANAGER_PLUS }
      },
      {
        path: 'leave/all',
        component: PagePlaceholderComponent,
        canActivate: [roleGuard],
        data: { roles: HR_ONLY }
      },
      { path: 'leave', component: PagePlaceholderComponent },
      {
        path: 'people/new',
        component: PagePlaceholderComponent,
        canActivate: [roleGuard],
        data: { roles: HR_ONLY }
      },
      {
        path: 'people/invite',
        component: PagePlaceholderComponent,
        canActivate: [roleGuard],
        data: { roles: HR_ONLY }
      },
      {
        path: 'people/:id',
        component: PagePlaceholderComponent,
        canActivate: [roleGuard],
        data: { roles: HR_ONLY }
      },
      {
        path: 'people',
        component: PagePlaceholderComponent,
        canActivate: [roleGuard],
        data: { roles: HR_ONLY }
      },
      {
        path: 'team',
        component: PagePlaceholderComponent,
        canActivate: [roleGuard],
        data: { roles: MANAGER_PLUS }
      },
      {
        path: 'departments',
        component: PagePlaceholderComponent,
        canActivate: [roleGuard],
        data: { roles: HR_ONLY }
      },
      {
        path: 'projects',
        component: PagePlaceholderComponent,
        canActivate: [roleGuard],
        data: { roles: HR_ONLY }
      }
    ]
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
