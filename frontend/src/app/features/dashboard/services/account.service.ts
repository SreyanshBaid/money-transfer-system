import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, forkJoin, of, timeout } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';

export interface Account {
  id: string;
  balance: number;
  currency: string;
  lastUpdated: Date;
}

// API response interface - matches actual backend response
export interface TransactionResponse {
  id: string;
  fromAccountId: number;
  idempotencyKey: string;
  transactionType: 'DEBIT' | 'CREDIT';
  amount: number;
  balanceBefore: number;
  balanceAfter: number;
  status: string;
  description: string;
  toAccountId: number;
  createdAt: string;
}

// UI interface - normalized for frontend use
export interface Transaction {
  id: string;
  type: 'DEBIT' | 'CREDIT';
  amount: number;
  date: Date;
  description: string;
  status: string;
  fromAccountId?: number;
  toAccountId?: number;
  balanceBefore?: number;
  balanceAfter?: number;
}

// View model combining account details and balance
export interface AccountCardViewModel {
  id: string;
  accountNumber: string;
  balance: number;
  currency: string;
  lastUpdated: Date;
  accountType?: string;
}

// AccountService provides account balance and transactions
@Injectable({
  providedIn: 'root'
})
export class AccountService {
  private apiUrl = 'http://localhost:8080/api/v1/accounts';

  constructor(private http: HttpClient) {}

  // Gets all accounts for current user
  getAccounts(): Observable<Account[]> {
    return this.http.get<Account[]>(`${this.apiUrl}`);
  }

  // Gets account information by ID
  getAccount(accountId: number): Observable<Account> {
    return this.http.get<Account>(`${this.apiUrl}/${accountId}`);
  }

  // Gets account balance by ID
  getBalance(accountId: number): Observable<number> {
    return this.http
      .get<number | { balance: number }>(`${this.apiUrl}/${accountId}/balance`)
      .pipe(
        map(response => (typeof response === 'number' ? response : response.balance))
      );
  }

  // Gets transaction history by account ID
  getTransactions(accountId: number): Observable<Transaction[]> {
    console.log(`Fetching transactions for account ${accountId}`);
    return this.http.get<TransactionResponse[]>(
      `${this.apiUrl}/${accountId}/transactions`
    ).pipe(
      timeout(10000), // 10 second timeout
      map(transactions => {
        console.log(`Received ${transactions?.length || 0} transactions`);
        if (!transactions || !Array.isArray(transactions)) {
          console.warn('Invalid transaction response format:', transactions);
          return [];
        }
        return transactions.map(tx => ({
          id: tx.id,
          type: tx.transactionType,
          amount: tx.amount,
          date: new Date(tx.createdAt),
          description: tx.description,
          status: tx.status,
          fromAccountId: tx.fromAccountId,
          toAccountId: tx.toAccountId,
          balanceBefore: tx.balanceBefore,
          balanceAfter: tx.balanceAfter
        }));
      })
    );
  }

  // Gets all accounts with their current balances as a combined view model
  // Uses forkJoin to fetch all balances in parallel, avoiding nested subscriptions
  getAllAccountsWithBalances(): Observable<AccountCardViewModel[]> {
    return this.getAccounts().pipe(
      switchMap(accounts => {
        // Create an array of balance requests for all accounts
        const balanceRequests: Record<string, Observable<number>> = {};
        
        accounts.forEach(account => {
          const accountId = Number(account.id);
          if (!Number.isNaN(accountId)) {
            balanceRequests[account.id] = this.getBalance(accountId);
          }
        });

        // If no valid accounts, return empty array
        if (Object.keys(balanceRequests).length === 0) {
          return of([] as AccountCardViewModel[]);
        }

        // Use forkJoin to fetch all balances in parallel
        return forkJoin(balanceRequests).pipe(
          map(balances => {
            // Combine account data with their respective balances
            return accounts.map(account => ({
              id: account.id,
              accountNumber: account.id,
              balance: balances[account.id] || account.balance,
              currency: account.currency,
              lastUpdated: account.lastUpdated,
              accountType: 'Checking' // Default type, can be enhanced with API
            } as AccountCardViewModel));
          })
        );
      })
    );
  }

  // Alternative implementation using switchMap for more flexible data fetching
  // This can be combined with user profile data in the component
  getAccountsWithDetails(): Observable<AccountCardViewModel[]> {
    return this.getAccounts().pipe(
      switchMap(accounts => {
        if (accounts.length === 0) {
          return of([] as AccountCardViewModel[]);
        }

        // Create balance requests for all accounts
        const balanceObservables = accounts.map(account => 
          this.getBalance(Number(account.id)).pipe(
            map(balance => ({
              ...account,
              balance
            }))
          )
        );

        return forkJoin(balanceObservables).pipe(
          map(accountsWithBalances => 
            accountsWithBalances.map(account => ({
              id: account.id,
              accountNumber: account.id,
              balance: account.balance,
              currency: account.currency,
              lastUpdated: account.lastUpdated,
              accountType: 'Checking'
            } as AccountCardViewModel))
          )
        );
      })
    );
  }
}
