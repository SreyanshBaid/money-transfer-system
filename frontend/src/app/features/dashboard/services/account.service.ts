import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, forkJoin, of, timeout, throwError } from 'rxjs';
import { map, switchMap, catchError } from 'rxjs/operators';

export interface Account {
  id: string | number;
  accountNumber: string;
  accountHolder: string;
  balance: number;
  accountType: string;
  status: string;
  createdAt: string;
  updatedAt: string;
  currency?: string;
  lastUpdated?: Date;
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

// Paginated response interface - matches backend PaginatedResponse
export interface PaginatedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

// View model combining account details and balance
export interface AccountCardViewModel {
  id: string | number;
  accountNumber: string;
  accountHolder: string;
  balance: number;
  accountType: string;
  status: string;
  createdAt: Date;
  updatedAt: Date;
  currency?: string;
  lastUpdated?: Date;
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
        map((response: any) => (typeof response === 'number' ? response : response.balance))
      );
  }

  // Gets transaction history by account ID with optional pagination
  getTransactions(accountId: number, page: number = 0, size: number = 12): Observable<PaginatedResponse<Transaction>> {
    console.log(`🔵 Fetching transactions for account ${accountId} (page: ${page}, size: ${size})`);
    const url = `${this.apiUrl}/${accountId}/transactions?page=${page}&size=${size}`;
    console.log(`🔵 URL: ${url}`);
    
    return this.http.get<PaginatedResponse<TransactionResponse>>(url).pipe(
      timeout(10000), // 10 second timeout
      map((response: PaginatedResponse<TransactionResponse>) => {
        console.log(`✅ Received page ${response.page + 1}/${response.totalPages} with ${response.content?.length || 0} transactions`);
        console.log('Raw response:', response);
        
        if (!response.content || !Array.isArray(response.content)) {
          console.warn('⚠️ Invalid transaction response format:', response);
          return {
            content: [],
            page: response.page || 0,
            size: response.size || size,
            totalElements: 0,
            totalPages: 0,
            first: true,
            last: true
          };
        }
        
        const transactions = response.content.map(tx => ({
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
        
        return {
          content: transactions,
          page: response.page,
          size: response.size,
          totalElements: response.totalElements,
          totalPages: response.totalPages,
          first: response.first,
          last: response.last
        };
      }),
      catchError((error: any) => {
        console.error('❌ Error in getTransactions:', error);
        console.error('❌ Error status:', error.status);
        console.error('❌ Error statusText:', error.statusText);
        console.error('❌ Error message:', error.message);
        return throwError(() => error);
      })
    );
  }

  // Gets all accounts with their current balances as a combined view model
  // Uses forkJoin to fetch all balances in parallel, avoiding nested subscriptions
  getAllAccountsWithBalances(): Observable<AccountCardViewModel[]> {
    return this.getAccounts().pipe(
      switchMap((accounts: Account[]) => {
        // Create an array of balance requests for all accounts
        const balanceRequests: Record<string, Observable<number>> = {};
        
        accounts.forEach((account: Account) => {
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
          map((balances: Record<string | number, number>) => {
            // Combine account data with their respective balances
            return accounts.map((account: Account) => ({
              id: account.id,
              accountNumber: account.accountNumber,
              accountHolder: account.accountHolder,
              balance: balances[account.id] || account.balance,
              accountType: account.accountType,
              status: account.status,
              createdAt: new Date(account.createdAt),
              updatedAt: new Date(account.updatedAt),
              currency: account.currency || 'USD',
              lastUpdated: new Date(account.updatedAt)
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
      switchMap((accounts: Account[]) => {
        if (accounts.length === 0) {
          return of([] as AccountCardViewModel[]);
        }

        // Create balance requests for all accounts
        const balanceObservables = accounts.map((account: Account) => 
          this.getBalance(Number(account.id)).pipe(
            map((balance: number) => ({
              ...account,
              balance
            }))
          )
        );

        return forkJoin(balanceObservables).pipe(
          map((accountsWithBalances: (Account & { balance: number })[]) => 
            accountsWithBalances.map((account: Account & { balance: number }) => ({
              id: account.id,
              accountNumber: account.accountNumber,
              accountHolder: account.accountHolder,
              balance: account.balance,
              accountType: account.accountType,
              status: account.status,
              createdAt: new Date(account.createdAt),
              updatedAt: new Date(account.updatedAt),
              currency: account.currency || 'USD',
              lastUpdated: new Date(account.updatedAt)
            } as AccountCardViewModel))
          )
        );
      })
    );
  }
}
