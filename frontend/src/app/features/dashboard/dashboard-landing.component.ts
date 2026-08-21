import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { apiErrorMessage } from '../../core/http/api-error';
import { UserDashboard } from '../../core/models/dashboard.model';
import { DashboardService } from '../../core/services/dashboard.service';

@Component({
  selector: 'app-dashboard-landing',
  standalone: true,
  templateUrl: './dashboard-landing.component.html',
  styleUrl: './dashboard-landing.component.scss'
})
export class DashboardLandingComponent implements OnInit {
  private readonly dashboardApi = inject(DashboardService);

  readonly dashboard = signal<UserDashboard | null>(null);
  readonly error = signal<string | null>(null);
  readonly loading = signal(true);

  ngOnInit(): void {
    this.dashboardApi.getMine().subscribe({
      next: (dashboard) => {
        this.dashboard.set(dashboard);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(apiErrorMessage(err));
        this.loading.set(false);
      }
    });
  }
}
