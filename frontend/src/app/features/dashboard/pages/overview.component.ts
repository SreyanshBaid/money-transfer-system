import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AccountService, Account } from '../services/account.service';
import { AuthService } from '../../../core/auth/auth.service';

// Fetch and display welcome message and balance
@Component({
  selector: 'app-overview',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './overview.component.html',
  styleUrls: ['./overview.component.css']
})
export class OverviewComponent implements OnInit {
  account: Account | null = null;
  loading = true;
  error = '';
  username: string = '';

  constructor(
    private accountService: AccountService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadAccount();
    this.loadUser();
  }

  private loadAccount(): void {
    this.accountService.getAccount().subscribe({
      next: (account) => {
        this.account = account;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load account information';
        this.loading = false;
        console.error('Failed to load account:', err);
      }
    });
  }

  private loadUser(): void {
    this.authService.getCurrentUser().subscribe({
      next: (user) => {
        if (user) {
          this.username = user.username;
        }
      }
    });
  }

  navigateToTransfer(): void {
    this.router.navigate(['/transfer']);
  }

  navigateToHistory(): void {
    this.router.navigate(['/history']);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
