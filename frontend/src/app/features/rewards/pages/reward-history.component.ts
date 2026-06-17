import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { RewardService, Reward } from '../services/reward.service';
import { Subject, forkJoin } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'app-reward-history',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './reward-history.component.html',
  styleUrls: ['./reward-history.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RewardHistoryComponent implements OnInit, OnDestroy {
  rewards: Reward[] = [];
  totalPoints = 0;
  totalRewards = 0;
  loading = true;
  error = '';

  private destroy$ = new Subject<void>();

  constructor(
    private rewardService: RewardService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadRewards();
  }

  loadRewards(): void {
    this.loading = true;
    this.error = '';

    forkJoin({
      summary: this.rewardService.getRewardSummary(),
      allRewards: this.rewardService.getRewards()
    }).pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (result) => {
          this.rewards = result.allRewards;
          this.totalPoints = result.summary.totalPoints;
          this.totalRewards = result.summary.totalRewards;
          this.loading = false;
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.loading = false;
          if (err.status === 0) {
            this.error = 'Cannot connect to server. Please check if the backend is running.';
          } else {
            this.error = 'Failed to load reward history. Please try again.';
          }
          this.cdr.markForCheck();
        }
      });
  }

  trackByRewardId(index: number, reward: Reward): number {
    return reward.id;
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
