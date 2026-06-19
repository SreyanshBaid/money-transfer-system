import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface UserRegistrationRequest {
  username: string;
  password: string;
  email: string;
  fullName: string;
}

export interface UserResponse {
  id: number;
  username: string;
  email: string;
  fullName: string;
  role: string;
  enabled: boolean;
  createdAt: string;
  lastLogin: string | null;
}

export interface CreateAccountRequest {
  accountNumber: string;
  accountHolder: string;
  balance: number;
  accountType: string;
  status: string;
}

export interface AccountResponse {
  id: number;
  accountNumber: string;
  accountHolder: string;
  balance: number;
  accountType: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface AccountBalanceResponse {
  accountId: number;
  accountNumber: string;
  balance: number;
  status: string;
  updatedAt: string;
}

export interface TransactionLogResponse {
  id: string;
  fromAccountId: number;
  toAccountId: number | null;
  idempotencyKey: string;
  transactionType: string;
  amount: number;
  balanceBefore: number;
  balanceAfter: number;
  status: string;
  description: string;
  createdAt: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private apiUrl = 'http://localhost:8080/api/v1';

  constructor(private http: HttpClient) {}

  healthCheck(): Observable<string> {
    return this.http.get(`${this.apiUrl}/admin/health`, { responseType: 'text' });
  }

  getAllUsers(): Observable<UserResponse[]> {
    return this.http.get<UserResponse[]>(`${this.apiUrl}/users`);
  }

  registerUser(request: UserRegistrationRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>(`${this.apiUrl}/users/register`, request);
  }

  getUserByUsername(username: string): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.apiUrl}/users/${username}`);
  }

  createAccountForUser(userId: number, request: CreateAccountRequest): Observable<AccountResponse> {
    return this.http.post<AccountResponse>(`${this.apiUrl}/admin/users/${userId}/accounts`, request);
  }

  getAccount(accountId: number): Observable<AccountResponse> {
    return this.http.get<AccountResponse>(`${this.apiUrl}/admin/accounts/${accountId}`);
  }

  getAccountBalance(accountId: number): Observable<AccountBalanceResponse> {
    return this.http.get<AccountBalanceResponse>(`${this.apiUrl}/admin/accounts/${accountId}/balance`);
  }

  getAccountTransactions(accountId: number, page = 0, size = 12): Observable<PaginatedResponse<TransactionLogResponse>> {
    return this.http.get<PaginatedResponse<TransactionLogResponse>>(
      `${this.apiUrl}/admin/accounts/${accountId}/transactions?page=${page}&size=${size}`
    );
  }
}
