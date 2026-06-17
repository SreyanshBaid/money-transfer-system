import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-reward-points-widget',
  standalone: true,
  template: `
    <div class="reward-badge">
      <span class="reward-icon">&#9733;</span>
      <span class="reward-points">{{ totalPoints }} pts</span>
    </div>
  `,
  styles: [`
    .reward-badge {
      display: flex;
      align-items: center;
      gap: 6px;
      background: rgba(255, 255, 255, 0.2);
      border: 1px solid rgba(255, 255, 255, 0.3);
      border-radius: 20px;
      padding: 6px 14px;
      cursor: pointer;
      transition: all 0.3s;
      white-space: nowrap;
    }
    .reward-badge:hover {
      background: rgba(255, 195, 0, 0.3);
      border-color: var(--school-bus-yellow);
      transform: scale(1.05);
    }
    .reward-icon {
      color: var(--school-bus-yellow);
      font-size: 16px;
    }
    .reward-points {
      color: white;
      font-size: 13px;
      font-weight: 600;
    }
  `]
})
export class RewardPointsWidgetComponent {
  @Input() totalPoints = 0;
}
