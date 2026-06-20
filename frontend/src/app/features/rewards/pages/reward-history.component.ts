import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { RewardService, Reward, RedeemResponse } from '../services/reward.service';
import { AccountService, Account } from '../../dashboard/services/account.service';
import { Subject, forkJoin } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'app-reward-history',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reward-history.component.html',
  styleUrls: ['./reward-history.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RewardHistoryComponent implements OnInit, OnDestroy {
  rewards: Reward[] = [];
  totalPoints = 0;
  totalRewards = 0;
  totalEarned = 0;
  totalRedeemed = 0;
  loading = true;
  error = '';

  accounts: Account[] = [];
  accountsLoading = false;

  redeemPointsAmount = 0;
  selectedAccountId: number | null = null;
  redeemLoading = false;
  redeemError = '';
  redeemSuccess: RedeemResponse | null = null;
  maxRedeemable = 0;

  private destroy$ = new Subject<void>();

  constructor(
    private rewardService: RewardService,
    private accountService: AccountService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading = true;
    this.error = '';

    this.accountService.getAccounts().pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (accts) => {
          this.accounts = accts;
          this.cdr.markForCheck();
        },
        error: () => {
          this.accounts = [];
          this.cdr.markForCheck();
        }
      });

    forkJoin({
      summary: this.rewardService.getRewardSummary(),
      allRewards: this.rewardService.getRewards()
    }).pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (result) => {
          this.rewards = result.allRewards;
          this.totalPoints = result.summary.totalPoints;
          this.totalRewards = result.summary.totalRewards;
          this.totalEarned = result.summary.totalEarned;
          this.totalRedeemed = result.summary.totalRedeemed;
          this.maxRedeemable = result.summary.totalPoints;
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

  redeemPoints(): void {
    if (this.redeemPointsAmount <= 0 || !this.selectedAccountId) {
      this.redeemError = 'Please enter a valid number of points and select an account.';
      this.cdr.markForCheck();
      return;
    }

    if (this.redeemPointsAmount > this.totalPoints) {
      this.redeemError = 'You only have ' + this.totalPoints + ' points available.';
      this.cdr.markForCheck();
      return;
    }

    this.redeemLoading = true;
    this.redeemError = '';
    this.redeemSuccess = null;
    this.cdr.markForCheck();

    this.rewardService.redeemPoints({
      points: this.redeemPointsAmount,
      accountId: this.selectedAccountId
    }).pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.redeemLoading = false;
          this.redeemSuccess = response;
          this.totalPoints = response.remainingPoints;
          this.maxRedeemable = response.remainingPoints;
          this.redeemPointsAmount = 0;
          this.selectedAccountId = null;
          this.rewardService.refreshRewardSummary();
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.redeemLoading = false;
          const detail = err.error?.detail || err.error?.message || err.message || 'Redemption failed. Please try again.';
          this.redeemError = detail;
          this.cdr.markForCheck();
        }
      });
  }

  loadRewards(): void {
    this.loadData();
  }

  closeSuccessModal(): void {
    this.redeemSuccess = null;
    this.cdr.markForCheck();
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
