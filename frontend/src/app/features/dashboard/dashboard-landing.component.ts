import { Component, inject } from '@angular/core';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-dashboard-landing',
  standalone: true,
  templateUrl: './dashboard-landing.component.html',
  styleUrl: './dashboard-landing.component.scss'
})
export class DashboardLandingComponent {
  readonly currentUser = inject(AuthService).currentUser;
}
