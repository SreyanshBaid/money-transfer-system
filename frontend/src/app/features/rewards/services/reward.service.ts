import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';

export interface Reward {
  id: number;
  userId: number;
  transactionLogId: string;
  points: number;
  reason: string;
  entryType?: string;
  accountId?: number;
  status?: string;
  depositTxnId?: string;
  createdAt: string;
}

export interface RewardSummary {
  userId: number;
  username: string;
  totalPoints: number;
  totalRewards: number;
  totalEarned: number;
  totalRedeemed: number;
  recentRewards: Reward[];
}

export interface RedeemRequest {
  points: number;
  accountId: number;
}

export interface RedeemResponse {
  rewardId: number;
  userId: number;
  depositTxnId: string;
  pointsRedeemed: number;
  amountCredited: number;
  accountId: number;
  status: string;
  remainingPoints: number;
  createdAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class RewardService {
  private apiUrl = 'http://localhost:8080/api/v1/rewards';
  private rewardRefreshSubject = new Subject<void>();
  readonly rewardRefresh$ = this.rewardRefreshSubject.asObservable();

  constructor(private http: HttpClient) {}

  getRewards(): Observable<Reward[]> {
    return this.http.get<Reward[]>(`${this.apiUrl}`);
  }

  getRewardSummary(): Observable<RewardSummary> {
    return this.http.get<RewardSummary>(`${this.apiUrl}/summary`);
  }

  redeemPoints(request: RedeemRequest): Observable<RedeemResponse> {
    return this.http.post<RedeemResponse>(`${this.apiUrl}/redeem`, request);
  }

  refreshRewardSummary(): void {
    this.rewardRefreshSubject.next();
  }
}
