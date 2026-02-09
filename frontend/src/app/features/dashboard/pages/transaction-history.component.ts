import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AccountService, Transaction } from '../../dashboard/services/account.service';
import { DateFormatPipe } from '../../../shared/pipes/date-format.pipe';
import { CurrencyFormatPipe } from '../../../shared/pipes/currency-format.pipe';

// Transaction history table with formatting
@Component({
  selector: 'app-transaction-history',
  standalone: true,
  imports: [CommonModule, DateFormatPipe, CurrencyFormatPipe],
  templateUrl: './transaction-history.component.html',
  styleUrls: ['./transaction-history.component.css']
})
export class TransactionHistoryComponent implements OnInit {
  transactions: Transaction[] = [];
  loading = true;
  error = '';

  constructor(
    private accountService: AccountService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadTransactions();
  }

  private loadTransactions(): void {
    this.accountService.getTransactions(50).subscribe({
      next: (transactions) => {
        this.transactions = transactions;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load transaction history';
        this.loading = false;
        console.error('Failed to load transactions:', err);
      }
    });
  }

  getStatusClass(status: string): string {
    switch (status) {
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

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }
}
