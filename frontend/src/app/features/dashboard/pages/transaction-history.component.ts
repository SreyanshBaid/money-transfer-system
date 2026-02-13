import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { AccountService, Transaction, PaginatedResponse } from '../services/account.service';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

// Transaction history table with formatting
@Component({
  selector: 'app-transaction-history',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './transaction-history.component.html',
  styleUrls: ['./transaction-history.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TransactionHistoryComponent implements OnInit, OnDestroy {
  transactions: Transaction[] = [];
  loading = true;
  error = '';
  accountId: string | null = null;
  
  // Pagination properties
  currentPage = 0;
  pageSize = 12;
  totalPages = 0;
  totalElements = 0;
  isFirstPage = true;
  isLastPage = true;
  
  private destroy$ = new Subject<void>();

  constructor(
    private accountService: AccountService,
    private router: Router,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // Get account ID from route query params
    this.route.queryParams.pipe(
      takeUntil(this.destroy$)
    ).subscribe((params: any) => {
      this.accountId = params['accountId'];
      console.log('Account ID from params:', this.accountId);
      
      if (!this.accountId) {
        this.error = 'No account selected. Please go back and select an account.';
        this.loading = false;
        this.cdr.markForCheck();
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
      this.cdr.markForCheck();
      return;
    }

    this.loading = true;
    this.error = '';

    const accountIdNum = Number(this.accountId);
    if (isNaN(accountIdNum)) {
      this.error = 'Invalid account ID format';
      this.loading = false;
      console.warn('Invalid account ID format:', this.accountId);
      this.cdr.markForCheck();
      return;
    }

    console.log('Loading transactions for account:', accountIdNum, 'page:', this.currentPage);
    
    this.accountService.getTransactions(accountIdNum, this.currentPage, this.pageSize)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: PaginatedResponse<Transaction>) => {
          console.log('✅ Transactions loaded successfully:', response);
          this.transactions = response.content;
          this.totalPages = response.totalPages;
          this.totalElements = response.totalElements;
          this.isFirstPage = response.first;
          this.isLastPage = response.last;
          this.loading = false;
          console.log(`Page ${this.currentPage + 1}/${this.totalPages}, Total: ${this.totalElements} transactions`);
          this.cdr.markForCheck();
        },
        error: (err: any) => {
          console.error('❌ Failed to load transactions:', err);
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
          this.cdr.markForCheck();
        }
      });
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }

  refresh(): void {
    this.currentPage = 0;
    this.loadTransactions();
  }

  // Pagination methods
  nextPage(): void {
    if (!this.isLastPage && this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.loadTransactions();
    }
  }

  previousPage(): void {
    if (!this.isFirstPage && this.currentPage > 0) {
      this.currentPage--;
      this.loadTransactions();
    }
  }

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages && page !== this.currentPage) {
      this.currentPage = page;
      this.loadTransactions();
    }
  }

  getPageNumbers(): number[] {
    const pages: number[] = [];
    const maxPagesToShow = 5;
    
    if (this.totalPages <= maxPagesToShow) {
      // Show all pages if total is small
      for (let i = 0; i < this.totalPages; i++) {
        pages.push(i);
      }
    } else {
      // Show current page and surrounding pages
      const startPage = Math.max(0, this.currentPage - 2);
      const endPage = Math.min(this.totalPages - 1, this.currentPage + 2);
      
      for (let i = startPage; i <= endPage; i++) {
        pages.push(i);
      }
    }
    
    return pages;
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

