import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Account {
  id: string;
  balance: number;
  currency: string;
  lastUpdated: Date;
}

export interface Transaction {
  id: string;
  type: 'DEBIT' | 'CREDIT';
  amount: number;
  date: Date;
  description: string;
  status: 'SUCCESS' | 'PENDING' | 'FAILED';
}

// AccountService provides account balance and transactions
@Injectable({
  providedIn: 'root'
})
export class AccountService {
  private apiUrl = 'http://localhost:8080/api/accounts';

  constructor(private http: HttpClient) {}

  // Gets current user's account information
  getAccount(): Observable<Account> {
    return this.http.get<Account>(`${this.apiUrl}/current`);
  }

  // Gets account balance
  getBalance(): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/current/balance`);
  }

  // Gets transaction history
  getTransactions(limit: number = 20): Observable<Transaction[]> {
    return this.http.get<Transaction[]>(`${this.apiUrl}/current/transactions`, {
      params: { limit: limit.toString() }
    });
  }
}
