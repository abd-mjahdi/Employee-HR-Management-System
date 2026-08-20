import { Routes } from '@angular/router';
import { LoginComponent } from './features/auth/login.component';
import { InviteAcceptComponent } from './features/auth/invite-accept.component';
import { MarketingHomeComponent } from './features/auth/marketing-home.component';
import { DashboardLandingComponent } from './features/dashboard/dashboard-landing.component';
import { DepartmentsComponent } from './features/departments/departments.component';
import { LeaveAllComponent } from './features/leave/leave-all.component';
import { LeaveApprovalsComponent } from './features/leave/leave-approvals.component';
import { LeaveFormComponent } from './features/leave/leave-form.component';
import { LeaveListComponent } from './features/leave/leave-list.component';
import { LeaveTeamComponent } from './features/leave/leave-team.component';
import { AppLayoutComponent } from './features/layout/app-layout.component';
import { ProfileComponent } from './features/profile/profile.component';
import { ProjectsComponent } from './features/projects/projects.component';
import { TimeApprovalsComponent } from './features/time/time-approvals.component';
import { TimeFormComponent } from './features/time/time-form.component';
import { TimeListComponent } from './features/time/time-list.component';
import { TimeTeamComponent } from './features/time/time-team.component';
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
      { path: 'time/new', component: TimeFormComponent },
      {
        path: 'time/approvals',
        component: TimeApprovalsComponent,
        canActivate: [roleGuard],
        data: { roles: MANAGER_PLUS }
      },
      {
        path: 'time/team',
        component: TimeTeamComponent,
        canActivate: [roleGuard],
        data: { roles: MANAGER_PLUS }
      },
      { path: 'time/:id/edit', component: TimeFormComponent },
      { path: 'time', component: TimeListComponent },
      { path: 'leave/new', component: LeaveFormComponent },
      {
        path: 'leave/approvals',
        component: LeaveApprovalsComponent,
        canActivate: [roleGuard],
        data: { roles: MANAGER_PLUS }
      },
      {
        path: 'leave/team',
        component: LeaveTeamComponent,
        canActivate: [roleGuard],
        data: { roles: MANAGER_PLUS }
      },
      {
        path: 'leave/all',
        component: LeaveAllComponent,
        canActivate: [roleGuard],
        data: { roles: HR_ONLY }
      },
      { path: 'leave', component: LeaveListComponent },
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
