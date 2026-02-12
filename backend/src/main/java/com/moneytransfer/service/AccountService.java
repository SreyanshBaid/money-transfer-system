package com.moneytransfer.service;

import com.moneytransfer.domain.entity.Account;
import com.moneytransfer.domain.entity.TransactionLog;
import com.moneytransfer.domain.entity.User;
import com.moneytransfer.domain.exception.AccountNotFoundException;
import com.moneytransfer.domain.exception.UserNotFoundException;
import com.moneytransfer.dto.request.CreateAccountRequest;
import com.moneytransfer.dto.response.AccountBalanceResponse;
import com.moneytransfer.dto.response.AccountResponse;
import com.moneytransfer.dto.response.TransactionLogResponse;
import com.moneytransfer.repository.AccountRepository;
import com.moneytransfer.repository.TransactionLogRepository;
import com.moneytransfer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AccountService: Account data operations including read and write functionality.
 * 
 * Methods for USER role:
 * - Regular methods perform ownership checks
 * - Users can only access their own account data
 * 
 * Methods for ADMIN role:
 * - Admin methods bypass ownership checks
 * - Admins can access any account data
 * - Named with "Admin" suffix for clarity
 * 
 * Account Ownership:
 * - When creating an account, it's automatically owned by the authenticated user
 * - Uses User.addAccount() to maintain bidirectional one-to-many relationship
 * - User.accounts (One-to-Many) <- Account.owner (Many-to-One)
 * - When loading user accounts, use findByUsernameWithAccounts() for eager loading
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionLogRepository transactionLogRepository;
    private final OwnershipService ownershipService;
    private final UserRepository userRepository;

    /**
     * Get account details by ID.
     * Validates user ownership before returning data.
     *
     * @param accountId account ID
     * @return AccountResponse
     */
    public AccountResponse getAccountById(Long accountId) {
        // Validate ownership (admins bypass this check)
        ownershipService.validateAccountOwnership(accountId);
        
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        log.debug("User accessed their account: {}", accountId);
        return toAccountResponse(account);
    }

    /**
     * Get account balance by ID.
     * Validates user ownership before returning balance.
     *
     * @param accountId account ID
     * @return AccountBalanceResponse
     */
    public AccountBalanceResponse getAccountBalance(Long accountId) {
        // Validate ownership (admins bypass this check)
        ownershipService.validateAccountOwnership(accountId);
        
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        log.debug("User accessed their account balance: {}", accountId);
        return toAccountBalanceResponse(account);
    }

    /**
     * Get transaction history for an account.
     * Validates user ownership before returning history.
     *
     * @param accountId account ID
     * @return list of transaction logs
     */
    public List<TransactionLogResponse> getAccountTransactionHistory(Long accountId) {
        // Validate ownership (admins bypass this check)
        ownershipService.validateAccountOwnership(accountId);
        
        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException(accountId);
        }

        log.debug("Retrieving transaction history for account: {}", accountId);
        List<TransactionLog> logs = transactionLogRepository.findByFromAccountIdOrderByCreatedAtDesc(accountId);
        return logs.stream()
                .map(this::toTransactionLogResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all accounts for the current authenticated user.
     * Returns only the user's own accounts (admins are treated as regular users here).
     *
     * @return list of AccountResponse
     */
    public List<AccountResponse> getCurrentUserAccounts() {
        List<Account> accounts = ownershipService.getCurrentUserWithAccounts().getAccounts();
        return accounts.stream()
                .map(this::toAccountResponse)
                .collect(Collectors.toList());
    }

    /**
     * Create a new account for the current authenticated user.
     * The account will automatically be owned by the authenticated user.
     * 
     * The account is linked to the user through the bidirectional one-to-many relationship:
     * User.accounts (one-to-many) <- Account.owner (many-to-one)
     *
     * @param request the account creation request
     * @return AccountResponse with created account details
     * @throws IllegalArgumentException if account number already exists
     */
    @Transactional
    public AccountResponse createAccountForCurrentUser(CreateAccountRequest request) {
        log.info("Creating new account for current user");

        // Check if account number already exists
        if (accountRepository.existsByAccountNumber(request.getAccountNumber())) {
            log.warn("Account creation failed: Account number already exists: {}", request.getAccountNumber());
            throw new IllegalArgumentException("Account number already exists: " + request.getAccountNumber());
        }

        // Get current authenticated user with their accounts
        User currentUser = ownershipService.getCurrentUserWithAccounts();

        // Create new account
        Account newAccount = Account.builder()
                .accountNumber(request.getAccountNumber())
                .accountHolder(request.getAccountHolder())
                .balance(request.getBalance())
                .accountType(request.getAccountType())
                .status(request.getStatus())
                .build();

        // Add account to user - this sets both sides of the bidirectional relationship
        // User.addAccount() calls:
        //   1. accounts.add(account)
        //   2. account.setOwner(this)
        currentUser.addAccount(newAccount);

        // Save user - cascade will persist the account and set user_id foreign key
        userRepository.save(currentUser);

        log.info("Successfully created account {} for user: {}", 
                newAccount.getAccountNumber(), currentUser.getUsername());

        return toAccountResponse(newAccount);
    }

    /**
     * [ADMIN] Create a new account for a specific user.
     * Admins can attach an account to any user by user ID.
     *
     * @param userId the user ID to own the new account
     * @param request the account creation request
     * @return AccountResponse with created account details
     * @throws IllegalArgumentException if account number already exists
     * @throws UserNotFoundException if user is not found
     */
    @Transactional
    public AccountResponse createAccountForUser(Long userId, CreateAccountRequest request) {
        log.info("[ADMIN ACCESS] Creating new account for userId: {}", userId);

        if (accountRepository.existsByAccountNumber(request.getAccountNumber())) {
            log.warn("Account creation failed: Account number already exists: {}", request.getAccountNumber());
            throw new IllegalArgumentException("Account number already exists: " + request.getAccountNumber());
        }

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Account newAccount = Account.builder()
                .accountNumber(request.getAccountNumber())
                .accountHolder(request.getAccountHolder())
                .balance(request.getBalance())
                .accountType(request.getAccountType())
                .status(request.getStatus())
                .build();

        targetUser.addAccount(newAccount);
        userRepository.save(targetUser);

        log.info("[ADMIN ACCESS] Created account {} for userId: {}",
                newAccount.getAccountNumber(), userId);

        return toAccountResponse(newAccount);
    }
    
    // ========================================
    // ADMIN METHODS - No ownership checks
    // ========================================
    
    /**
     * [ADMIN] Get account details - no ownership check.
     * Admins can view any account.
     *
     * @param accountId account ID
     * @return AccountResponse
     */
    public AccountResponse getAccountAdmin(Long accountId) {
        log.info("[ADMIN ACCESS] Retrieving account details for account: {}", accountId);
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        return toAccountResponse(account);
    }

    /**
     * [ADMIN] Get account balance - no ownership check.
     * Admins can view any account balance.
     *
     * @param accountId account ID
     * @return AccountBalanceResponse
     */
    public AccountBalanceResponse getAccountBalanceAdmin(Long accountId) {
        log.info("[ADMIN ACCESS] Retrieving balance for account: {}", accountId);
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        return toAccountBalanceResponse(account);
    }

    /**
     * [ADMIN] Get transaction history - no ownership check.
     * Admins can view any account's transaction history.
     *
     * @param accountId account ID
     * @return list of transaction logs
     */
    public List<TransactionLogResponse> getTransactionHistoryAdmin(Long accountId) {
        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException(accountId);
        }

        log.info("[ADMIN ACCESS] Retrieving transaction history for account: {}", accountId);
        List<TransactionLog> logs = transactionLogRepository.findByFromAccountIdOrderByCreatedAtDesc(accountId);
        return logs.stream()
                .map(this::toTransactionLogResponse)
                .collect(Collectors.toList());
    }

    private AccountResponse toAccountResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .accountHolder(account.getAccountHolder())
                .balance(account.getBalance())
                .accountType(account.getAccountType())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }

    private AccountBalanceResponse toAccountBalanceResponse(Account account) {
        return AccountBalanceResponse.builder()
                .accountId(account.getId())
                .accountNumber(account.getAccountNumber())
                .balance(account.getBalance())
                .status(account.getStatus())
                .updatedAt(account.getUpdatedAt())
                .build();
    }

    private TransactionLogResponse toTransactionLogResponse(TransactionLog logEntry) {
        return TransactionLogResponse.builder()
                .id(logEntry.getId())
                .fromAccountId(logEntry.getFromAccountId())
                .idempotencyKey(logEntry.getIdempotencyKey())
                .transactionType(logEntry.getTransactionType())
                .amount(logEntry.getAmount())
                .balanceBefore(logEntry.getBalanceBefore())
                .balanceAfter(logEntry.getBalanceAfter())
                .status(logEntry.getStatus())
                .description(logEntry.getDescription())
                .toAccountId(logEntry.getToAccountId())
                .createdAt(logEntry.getCreatedAt())
                .build();
    }
}
