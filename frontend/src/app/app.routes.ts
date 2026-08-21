import { Routes } from '@angular/router';
import { LoginComponent } from './features/auth/login.component';
import { InviteAcceptComponent } from './features/auth/invite-accept.component';
import { MarketingHomeComponent } from './features/auth/marketing-home.component';
import { DashboardLandingComponent } from './features/dashboard/dashboard-landing.component';
import { AppLayoutComponent } from './features/layout/app-layout.component';
import { ProfileComponent } from './features/profile/profile.component';
import { DepartmentsComponent } from './features/departments/departments.component';
import { ProjectsComponent } from './features/projects/projects.component';
import { UsersListComponent } from './features/users/users-list.component';
import { UserCreateComponent } from './features/users/user-create.component';
import { UserDetailComponent } from './features/users/user-detail.component';
import { InviteCreateComponent } from './features/users/invite-create.component';
import { TeamComponent } from './features/users/team.component';
import { TimeListComponent } from './features/time/time-list.component';
import { TimeFormComponent } from './features/time/time-form.component';
import { TimeApprovalsComponent } from './features/time/time-approvals.component';
import { TimeTeamComponent } from './features/time/time-team.component';
import { LeaveListComponent } from './features/leave/leave-list.component';
import { LeaveFormComponent } from './features/leave/leave-form.component';
import { LeaveApprovalsComponent } from './features/leave/leave-approvals.component';
import { LeaveTeamComponent } from './features/leave/leave-team.component';
import { LeaveAllComponent } from './features/leave/leave-all.component';
import { EmployeeTimeReportComponent } from './features/reports/employee-time-report.component';
import { TeamLeaveReportComponent } from './features/reports/team-leave-report.component';
import { LeaveBalancesReportComponent } from './features/reports/leave-balances-report.component';
import { PayrollReportComponent } from './features/reports/payroll-report.component';
import { DepartmentUtilizationReportComponent } from './features/reports/department-utilization-report.component';
import { ProjectHoursReportComponent } from './features/reports/project-hours-report.component';
import { ComplianceReportComponent } from './features/reports/compliance-report.component';
import { authGuard, guestGuard } from './core/guards/auth.guard';
import { marketingHomeGuard, tenantGuard } from './core/guards/tenant.guard';
import { roleGuard } from './core/guards/role.guard';

const MANAGER_PLUS = ['MANAGER', 'HR_ADMIN'] as const;
const HR_ONLY = ['HR_ADMIN'] as const;

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
    path: '',
    component: AppLayoutComponent,
    canActivate: [tenantGuard, authGuard],
    canActivateChild: [roleGuard],
    children: [
      { path: 'dashboard', component: DashboardLandingComponent },
      { path: 'profile', component: ProfileComponent },
      { path: 'time', component: TimeListComponent },
      { path: 'time/new', component: TimeFormComponent },
      { path: 'time/approvals', component: TimeApprovalsComponent, canActivate: [roleGuard], data: { roles: MANAGER_PLUS } },
      { path: 'time/team', component: TimeTeamComponent, canActivate: [roleGuard], data: { roles: MANAGER_PLUS } },
      { path: 'time/:id/edit', component: TimeFormComponent },
      { path: 'leave', component: LeaveListComponent },
      { path: 'leave/new', component: LeaveFormComponent },
      { path: 'leave/approvals', component: LeaveApprovalsComponent, canActivate: [roleGuard], data: { roles: MANAGER_PLUS } },
      { path: 'leave/team', component: LeaveTeamComponent, canActivate: [roleGuard], data: { roles: MANAGER_PLUS } },
      { path: 'leave/all', component: LeaveAllComponent, canActivate: [roleGuard], data: { roles: HR_ONLY } },
      { path: 'team', component: TeamComponent, canActivate: [roleGuard], data: { roles: MANAGER_PLUS } },
      { path: 'people', component: UsersListComponent, canActivate: [roleGuard], data: { roles: HR_ONLY } },
      { path: 'people/new', component: UserCreateComponent, canActivate: [roleGuard], data: { roles: HR_ONLY } },
      { path: 'people/invite', component: InviteCreateComponent, canActivate: [roleGuard], data: { roles: HR_ONLY } },
      { path: 'people/:id', component: UserDetailComponent, canActivate: [roleGuard], data: { roles: HR_ONLY } },
      { path: 'departments', component: DepartmentsComponent, canActivate: [roleGuard], data: { roles: HR_ONLY } },
      { path: 'projects', component: ProjectsComponent, canActivate: [roleGuard], data: { roles: HR_ONLY } },
      { path: 'reports/employee-time', component: EmployeeTimeReportComponent },
      { path: 'reports/team-leave', component: TeamLeaveReportComponent, canActivate: [roleGuard], data: { roles: MANAGER_PLUS } },
      { path: 'reports/leave-balances', component: LeaveBalancesReportComponent, canActivate: [roleGuard], data: { roles: MANAGER_PLUS } },
      { path: 'reports/payroll', component: PayrollReportComponent, canActivate: [roleGuard], data: { roles: HR_ONLY } },
      { path: 'reports/department-utilization', component: DepartmentUtilizationReportComponent, canActivate: [roleGuard], data: { roles: HR_ONLY } },
      { path: 'reports/project-hours', component: ProjectHoursReportComponent, canActivate: [roleGuard], data: { roles: HR_ONLY } },
      { path: 'reports/compliance', component: ComplianceReportComponent, canActivate: [roleGuard], data: { roles: HR_ONLY } }
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
