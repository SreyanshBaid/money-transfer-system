import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Reward {
  id: number;
  userId: number;
  transactionLogId: string;
  points: number;
  reason: string;
  createdAt: string;
}

export interface RewardSummary {
  userId: number;
  username: string;
  totalPoints: number;
  totalRewards: number;
  recentRewards: Reward[];
}

@Injectable({
  providedIn: 'root'
})
export class RewardService {
  private apiUrl = 'http://localhost:8080/api/v1/rewards';

  constructor(private http: HttpClient) {}

  getRewards(): Observable<Reward[]> {
    return this.http.get<Reward[]>(`${this.apiUrl}`);
  }

  getRewardSummary(): Observable<RewardSummary> {
    return this.http.get<RewardSummary>(`${this.apiUrl}/summary`);
  }
}
