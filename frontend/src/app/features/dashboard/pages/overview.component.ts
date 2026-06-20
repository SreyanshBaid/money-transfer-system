import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { CommonModule, NgIf } from '@angular/common';
import { Observable, Subject, BehaviorSubject, forkJoin, filter } from 'rxjs';
import { switchMap, takeUntil, tap, finalize, shareReplay } from 'rxjs/operators';
import { FormsModule } from '@angular/forms';
import { AccountService, AccountCardViewModel } from '../services/account.service';
import { AuthService } from '../../../core/auth/auth.service';
import { TokenService } from '../../../core/auth/token.service';
import { User } from '../../../core/auth/auth.models';
import { RewardService, RewardSummary } from '../../rewards/services/reward.service';
import { RewardPointsWidgetComponent } from '../../rewards/components/reward-points-widget.component';
import { CombineBalancePipe } from '../../../shared/pipes';
import { AdminService, UserRegistrationRequest, CreateAccountRequest, AccountResponse, AccountBalanceResponse, TransactionLogResponse } from '../../admin/services/admin.service';

// OverviewComponent fetches user profile and all accounts with balances
// Displays accounts as scrollable cards with proper loading/error handling
@Component({
  selector: 'app-overview',
  standalone: true,
  imports: [CommonModule, NgIf, FormsModule, RewardPointsWidgetComponent, CombineBalancePipe],
  templateUrl: './overview.component.html',
  styleUrls: ['./overview.component.css']
})
export class OverviewComponent implements OnInit, OnDestroy {
  // Observable streams for data
  accounts$: Observable<AccountCardViewModel[]>;
  user$: Observable<User | null>;
  
  // State management
  isLoadingSubject$ = new BehaviorSubject<boolean>(true);
  isLoading$ = this.isLoadingSubject$.asObservable();
  
  errorSubject$ = new BehaviorSubject<string | null>(null);
  error$ = this.errorSubject$.asObservable();

  // Profile dropdown state
  showProfileDropdown = false;
  selectedAccount: AccountCardViewModel | null = null;

  // Admin flag
  isAdmin = false;

  // Register user form
  regModel: UserRegistrationRequest = { username: '', password: '', email: '', fullName: '' };
  regLoading = false;
  regError = '';
  regSuccess = '';
  showRegSuccessModal = false;

  // Create account form
  createForUserId: number | null = null;
  acctModel: CreateAccountRequest = { accountNumber: '', accountHolder: '', balance: 0, accountType: 'CHECKING', status: 'ACTIVE' };
  acctLoading = false;
  acctError = '';
  acctSuccess = '';
  showAcctSuccessModal = false;

  // Transaction search
  txnAccountId: number | null = null;
  txnAccount: AccountResponse | null = null;
  txnBalance: AccountBalanceResponse | null = null;
  transactions: TransactionLogResponse[] = [];
  txnLoading = false;
  txnError = '';
  showTxnModal = false;

  // Account detail lookup
  detailAccountId: number | null = null;
  detailAccount: AccountResponse | null = null;
  detailBalance: AccountBalanceResponse | null = null;
  detailLoading = false;
  detailError = '';
  showDetailModal = false;

  // Reward summary - default with 0 points so badge always visible
  rewardSummary: RewardSummary = {
    userId: 0,
    username: '',
    totalPoints: 0,
    totalRewards: 0,
    totalEarned: 0,
    totalRedeemed: 0,
    recentRewards: []
  };

  private destroy$ = new Subject<void>();

  constructor(
    private cdr: ChangeDetectorRef,
    private accountService: AccountService,
    private authService: AuthService,
    private rewardService: RewardService,
    private tokenService: TokenService,
    private adminService: AdminService,
    private router: Router
  ) {
    console.log('🔍 OverviewComponent: Constructor - Initializing...');
    
    // Initialize user observable from auth service
    this.user$ = this.authService.getCurrentUser();
    
    // Debug: Log user data
    this.user$.subscribe(user => {
      console.log('👤 Greeting: User data received:', user);
      if (!user) {
        console.warn('⚠️ User is NULL - greeting will show "Hi Guest!"');
      } else {
        console.log('✅ User found:', { username: user.username, email: user.email });
      }
    });
    
    // Check if current user is admin
    const storedUser = this.tokenService.getUser();
    this.isAdmin = storedUser?.roles?.includes('ADMIN') ?? false;
    
    // Initialize accounts observable - fetches accounts and their balances
    // shareReplay ensures observable executes only once even with multiple subscribers
    this.accounts$ = this.authService.getAuthState().pipe(
      tap(state => console.log('📊 Auth state:', state)),
      switchMap(authState => {
        if (authState.isAuthenticated) {
          this.isLoadingSubject$.next(true);
          this.errorSubject$.next(null);
          return this.accountService.getAccountsWithDetails().pipe(
            tap(() => {
              // Data arrived successfully
              this.isLoadingSubject$.next(false);
              this.errorSubject$.next(null);
              this.loadRewardSummary();
            }),
            finalize(() => {
              // Ensure loading is false when observable completes
              this.isLoadingSubject$.next(false);
            })
          );
        }
        this.isLoadingSubject$.next(false);
        return new Observable<AccountCardViewModel[]>(obs => {
          obs.next([]);
          obs.complete();
        });
      }),
      shareReplay(1), // Cache the result and share across subscribers
      takeUntil(this.destroy$)
    );
  }

  ngOnInit(): void {
    // Subscribe to accounts to trigger the data fetch
    this.accounts$.pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      error: (err) => {
        this.isLoadingSubject$.next(false);
        this.errorSubject$.next('Failed to load accounts. Please try again.');
        console.error('Error loading accounts:', err);
      }
    });

    // Sidebar is closed via overlay click or close button

    // Only load reward summary after the authenticated user state is ready.
    // This avoids transient "0 points" values when the dashboard mounts before auth finishes.
    this.authService.getAuthState().pipe(
      filter(state => state.isAuthenticated),
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.loadRewardSummary();
    });

    // Refresh rewards whenever the dashboard becomes active again
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd),
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.loadRewardSummary();
    });

    // Refresh rewards whenever another feature signals that a transfer succeeded
    this.rewardService.rewardRefresh$.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.loadRewardSummary();
    });
  }

  loadRewardSummary(): void {
    this.rewardService.getRewardSummary().pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (summary) => {
        this.rewardSummary = summary;
        this.cdr.markForCheck();
      },
      error: () => {
        // Keep default 0-point summary on failure
        this.cdr.markForCheck();
      }
    });
  }

  navigateToRewards(): void {
    this.router.navigate(['/rewards']);
  }

  // ── Register user ──
  registerUser(): void {
    this.regLoading = true;
    this.regError = '';
    this.regSuccess = '';
    this.adminService.registerUser(this.regModel).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.regSuccess = `User ${this.regModel.username} registered successfully.`;
        this.regModel = { username: '', password: '', email: '', fullName: '' };
        this.regLoading = false;
        this.showRegSuccessModal = true;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.regError = err.error?.detail || err.error?.message || err.message || 'Registration failed.';
        this.regLoading = false;
        this.cdr.markForCheck();
      }
    });
  }

  // ── Create account ──
  createAccount(): void {
    if (!this.createForUserId) return;
    this.acctLoading = true;
    this.acctError = '';
    this.acctSuccess = '';
    this.adminService.createAccountForUser(this.createForUserId, this.acctModel)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (acct) => {
          this.acctSuccess = `Account ${acct.accountNumber} created successfully.`;
          this.createForUserId = null;
          this.acctModel = { accountNumber: '', accountHolder: '', balance: 0, accountType: 'CHECKING', status: 'ACTIVE' };
          this.acctLoading = false;
          this.showAcctSuccessModal = true;
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.acctError = err.error?.detail || err.error?.message || err.message || 'Account creation failed.';
          this.acctLoading = false;
          this.cdr.markForCheck();
        }
      });
  }

  // ── Search transactions ──
  searchTransactions(): void {
    if (!this.txnAccountId) return;
    this.txnLoading = true;
    this.txnError = '';
    this.txnAccount = null;
    this.txnBalance = null;
    this.transactions = [];

    this.adminService.getAccount(this.txnAccountId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (acct) => {
          this.txnAccount = acct;
          this.showTxnModal = true;
          this.cdr.markForCheck();
          this.adminService.getAccountBalance(this.txnAccountId!)
            .pipe(takeUntil(this.destroy$))
            .subscribe({ next: (bal) => { this.txnBalance = bal; this.cdr.markForCheck(); } });
          this.adminService.getAccountTransactions(this.txnAccountId!, 0, 5)
            .pipe(takeUntil(this.destroy$))
            .subscribe({
              next: (res) => { this.transactions = res.content; this.txnLoading = false; this.cdr.markForCheck(); },
              error: () => { this.txnLoading = false; this.cdr.markForCheck(); }
            });
        },
        error: () => {
          this.txnError = `Account ${this.txnAccountId} not found.`;
          this.txnLoading = false;
          this.cdr.markForCheck();
        }
      });
  }

  // ── Account details lookup ──
  lookupAccountDetails(): void {
    if (!this.detailAccountId) return;
    this.detailLoading = true;
    this.detailError = '';
    this.detailAccount = null;
    this.detailBalance = null;

    this.adminService.getAccount(this.detailAccountId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (acct) => {
          this.detailAccount = acct;
          this.showDetailModal = true;
          this.cdr.markForCheck();
          this.adminService.getAccountBalance(this.detailAccountId!)
            .pipe(takeUntil(this.destroy$))
            .subscribe({
              next: (bal) => { this.detailBalance = bal; this.detailLoading = false; this.cdr.markForCheck(); },
              error: () => { this.detailLoading = false; this.cdr.markForCheck(); }
            });
        },
        error: () => {
          this.detailError = `Account ${this.detailAccountId} not found.`;
          this.detailLoading = false;
          this.cdr.markForCheck();
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // TrackBy function for *ngFor optimization
  trackByAccountId(index: number, account: AccountCardViewModel): string {
    return String(account.id);
  }

  navigateToTransfer(accountId: string): void {
    this.router.navigate(['/transfer'], { queryParams: { accountId } });
  }

  navigateToHistory(accountId: string): void {
    this.router.navigate(['/history'], { queryParams: { accountId } });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  // Toggle profile sidebar
  toggleProfileDropdown(): void {
    this.showProfileDropdown = !this.showProfileDropdown;
  }

  // Close profile sidebar
  closeProfileSidebar(): void {
    this.showProfileDropdown = false;
  }

  // Get user initials for avatar
  getUserInitials(user: User): string {
    if (!user || !user.username) return '?';
    const names = user.username.split(' ');
    console.log('User:', user);
    if (names.length >= 2) {
      return (names[0][0] + names[1][0]).toUpperCase();
    }
    console.log('User with no full name:', user);
    return user.username.substring(0, 2).toUpperCase();
  }

  // View account details
  viewAccountDetails(account: AccountCardViewModel): void {
    this.selectedAccount = account;
  }

  // Close account details modal
  closeAccountDetails(): void {
    this.selectedAccount = null;
  }

  // Close transaction search modal and reset
  closeTxnModal(): void {
    this.showTxnModal = false;
    this.txnAccountId = null;
    this.txnAccount = null;
    this.txnBalance = null;
    this.transactions = [];
    this.txnError = '';
    this.cdr.markForCheck();
  }

  // Close account detail lookup modal and reset
  closeDetailModal(): void {
    this.showDetailModal = false;
    this.detailAccountId = null;
    this.detailAccount = null;
    this.detailBalance = null;
    this.detailError = '';
    this.cdr.markForCheck();
  }

  // Close register success modal and clear message
  closeRegSuccessModal(): void {
    this.showRegSuccessModal = false;
    this.regSuccess = '';
    this.cdr.markForCheck();
  }

  // Close account create success modal and clear message
  closeAcctSuccessModal(): void {
    this.showAcctSuccessModal = false;
    this.acctSuccess = '';
    this.cdr.markForCheck();
  }
}
