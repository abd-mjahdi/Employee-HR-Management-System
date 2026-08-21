import { Component, input, model, output } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-report-date-filter',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './report-date-filter.component.html',
  styleUrl: './report-date-filter.component.scss'
})
export class ReportDateFilterComponent {
  readonly startDate = model('');
  readonly endDate = model('');
  readonly loading = input(false);
  readonly showDates = input(true);
  readonly load = output<void>();

  onLoad(): void {
    this.load.emit();
  }
}
