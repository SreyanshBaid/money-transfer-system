import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { AccountService, Transaction } from '../services/account.service';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

// Transaction history table with formatting
@Component({
  selector: 'app-transaction-history',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './transaction-history.component.html',
  styleUrls: ['./transaction-history.component.css']
})
export class TransactionHistoryComponent implements OnInit, OnDestroy {
  transactions: Transaction[] = [];
  loading = true;
  error = '';
  accountId: string | null = null;
  private destroy$ = new Subject<void>();

  constructor(
    private accountService: AccountService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    // Get account ID from route query params
    this.route.queryParams.pipe(
      takeUntil(this.destroy$)
    ).subscribe(params => {
      this.accountId = params['accountId'];
      console.log('Account ID from params:', this.accountId);
      
      if (!this.accountId) {
        this.error = 'No account selected. Please go back and select an account.';
        this.loading = false;
        return;
      }
      this.loadTransactions();
    });
  }

  private loadTransactions(): void {
    if (!this.accountId) {
      this.error = 'Account ID is missing';
      this.loading = false;
      console.warn('Account ID is missing');
      return;
    }

    this.loading = true;
    this.error = '';

    const accountIdNum = Number(this.accountId);
    if (isNaN(accountIdNum)) {
      this.error = 'Invalid account ID format';
      this.loading = false;
      console.warn('Invalid account ID format:', this.accountId);
      return;
    }

    console.log('Loading transactions for account:', accountIdNum);
    console.log('API URL will be: http://localhost:8080/api/v1/accounts/' + accountIdNum + '/transactions');
    
    this.accountService.getTransactions(accountIdNum)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (transactions) => {
          console.log('✅ Transactions loaded successfully:', transactions);
          this.transactions = transactions;
          this.loading = false;
          console.log('Loading flag set to:', this.loading);
          console.log('Transactions array length:', this.transactions.length);
          console.log('Error flag:', this.error);
        },
        error: (err) => {
          console.error('❌ Failed to load transactions:', err);
          console.error('Error object:', JSON.stringify(err, null, 2));
          console.error('Error status:', err.status);
          console.error('Error message:', err.message);
          console.error('Error error:', err.error);
          this.loading = false;
          
          if (err.status === 404) {
            this.error = 'Account not found or you do not have permission to view it';
          } else if (err.status === 401) {
            this.error = 'You are not authorized to view this account. Please log in again.';
          } else if (err.status === 403) {
            this.error = 'Access denied to this account';
          } else if (err.status === 0) {
            this.error = 'Cannot connect to server. Please check if the backend is running.';
          } else if (err.name === 'TimeoutError') {
            this.error = 'Request timed out. Please try again.';
          } else {
            this.error = `Failed to load transaction history: ${err.message || err.statusText || 'Unknown error'}`;
          }
        },
        complete: () => {
          console.log('Transaction loading completed');
        }
      });
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  getStatusClass(status: string): string {
    const normalizedStatus = status?.toUpperCase() || '';
    switch (normalizedStatus) {
      case 'COMPLETED':
      case 'SUCCESS':
        return 'status-success';
      case 'PENDING':
        return 'status-pending';
      case 'FAILED':
        return 'status-failed';
      default:
        return '';
    }
  }

  getAmountClass(type: string): string {
    return type === 'DEBIT' ? 'amount-debit' : 'amount-credit';
  }
}
