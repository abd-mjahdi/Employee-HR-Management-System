import { Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { UserRole } from '../../core/models/auth.model';
import { TenantService } from '../../core/tenant/tenant.service';

interface NavItem {
  label: string;
  route: string;
  roles: UserRole[];
  exact: boolean;
}

const ALL_ROLES: UserRole[] = ['EMPLOYEE', 'MANAGER', 'HR_ADMIN'];
const MANAGER_PLUS: UserRole[] = ['MANAGER', 'HR_ADMIN'];
const HR_ONLY: UserRole[] = ['HR_ADMIN'];

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app-layout.component.html',
  styleUrl: './app-layout.component.scss'
})
export class AppLayoutComponent {
  private readonly auth = inject(AuthService);
  readonly tenant = inject(TenantService);

  readonly currentUser = this.auth.currentUser;

  readonly navItems: NavItem[] = [
    { label: 'Home', route: '/dashboard', roles: ALL_ROLES, exact: true },
    { label: 'Time', route: '/time', roles: ALL_ROLES, exact: true },
    { label: 'Leave', route: '/leave', roles: ALL_ROLES, exact: true },
    { label: 'Profile', route: '/profile', roles: ALL_ROLES, exact: true },
    { label: 'Team', route: '/team', roles: MANAGER_PLUS, exact: true },
    { label: 'Time approvals', route: '/time/approvals', roles: MANAGER_PLUS, exact: true },
    { label: 'Team time', route: '/time/team', roles: MANAGER_PLUS, exact: true },
    { label: 'Leave approvals', route: '/leave/approvals', roles: MANAGER_PLUS, exact: true },
    { label: 'Team leave', route: '/leave/team', roles: MANAGER_PLUS, exact: true },
    { label: 'People', route: '/people', roles: HR_ONLY, exact: true },
    { label: 'Invite', route: '/people/invite', roles: HR_ONLY, exact: true },
    { label: 'Departments', route: '/departments', roles: HR_ONLY, exact: true },
    { label: 'Projects', route: '/projects', roles: HR_ONLY, exact: true },
    { label: 'All leave', route: '/leave/all', roles: HR_ONLY, exact: true },
    { label: 'Employee time', route: '/reports/employee-time', roles: ALL_ROLES, exact: true },
    { label: 'Team leave', route: '/reports/team-leave', roles: MANAGER_PLUS, exact: true },
    { label: 'Leave balances', route: '/reports/leave-balances', roles: MANAGER_PLUS, exact: true },
    { label: 'Payroll', route: '/reports/payroll', roles: HR_ONLY, exact: true },
    { label: 'Department hours', route: '/reports/department-utilization', roles: HR_ONLY, exact: true },
    { label: 'Project hours', route: '/reports/project-hours', roles: HR_ONLY, exact: true },
    { label: 'Compliance', route: '/reports/compliance', roles: HR_ONLY, exact: true }
  ];

  readonly visibleNav = computed(() =>
    this.navItems.filter((item) => this.auth.hasAnyRole(item.roles))
  );

  readonly displayName = computed(() => {
    const user = this.currentUser();
    if (!user) {
      return '';
    }
    const name = [user.firstName, user.lastName].filter(Boolean).join(' ').trim();
    return name || user.email;
  });

  logout(): void {
    this.auth.logout();
  }
}
