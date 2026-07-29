import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-dashboard-landing',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard-landing.component.html',
  styleUrl: './dashboard-landing.component.scss'
})
export class DashboardLandingComponent {
  private authService = inject(AuthService);

  currentUser = this.authService.currentUser;
  token = this.authService.token;

  logout(): void {
    this.authService.logout();
  }
}
