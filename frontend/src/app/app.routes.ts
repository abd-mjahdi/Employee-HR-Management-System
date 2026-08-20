import { Routes } from '@angular/router';
import { LoginComponent } from './features/auth/login.component';
import { InviteAcceptComponent } from './features/auth/invite-accept.component';
import { MarketingHomeComponent } from './features/auth/marketing-home.component';
import { DashboardLandingComponent } from './features/dashboard/dashboard-landing.component';
import { DepartmentsComponent } from './features/departments/departments.component';
import { AppLayoutComponent } from './features/layout/app-layout.component';
import { PagePlaceholderComponent } from './features/layout/page-placeholder.component';
import { ProfileComponent } from './features/profile/profile.component';
import { ProjectsComponent } from './features/projects/projects.component';
import { InviteCreateComponent } from './features/users/invite-create.component';
import { TeamComponent } from './features/users/team.component';
import { UserCreateComponent } from './features/users/user-create.component';
import { UserDetailComponent } from './features/users/user-detail.component';
import { UsersListComponent } from './features/users/users-list.component';
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
      { path: 'profile', component: ProfileComponent },
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
        component: UserCreateComponent,
        canActivate: [roleGuard],
        data: { roles: HR_ONLY }
      },
      {
        path: 'people/invite',
        component: InviteCreateComponent,
        canActivate: [roleGuard],
        data: { roles: HR_ONLY }
      },
      {
        path: 'people/:id',
        component: UserDetailComponent,
        canActivate: [roleGuard],
        data: { roles: HR_ONLY }
      },
      {
        path: 'people',
        component: UsersListComponent,
        canActivate: [roleGuard],
        data: { roles: HR_ONLY }
      },
      {
        path: 'team',
        component: TeamComponent,
        canActivate: [roleGuard],
        data: { roles: MANAGER_PLUS }
      },
      {
        path: 'departments',
        component: DepartmentsComponent,
        canActivate: [roleGuard],
        data: { roles: HR_ONLY }
      },
      {
        path: 'projects',
        component: ProjectsComponent,
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
