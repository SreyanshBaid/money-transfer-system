import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { TransferService, TransferResponse } from '../services/transfer.service';
import { finalize } from 'rxjs/operators';

// Transfer form with validation and submission logic
@Component({
  selector: 'app-initiate-transfer',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './initiate-transfer.component.html',
  styleUrls: ['./initiate-transfer.component.css']
})
export class InitiateTransferComponent implements OnInit {
  transferForm!: FormGroup;
  loading = false;
  submitted = false;
  error = '';
  errorCode = '';
  success = false;
  successMessage = '';
  sourceAccountId: string | null = null;

  constructor(
    private fb: FormBuilder,
    private transferService: TransferService,
    private router: Router,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // Get source account ID from route query params
    this.route.queryParams.subscribe((params: any) => {
      this.sourceAccountId = params['accountId'];
      this.initializeForm();
    });
  }

  private initializeForm(): void {
    this.transferForm = this.fb.group({
      destinationAccountId: ['', [Validators.required, Validators.minLength(1)]],
      amount: ['', [Validators.required, Validators.min(0.01), Validators.pattern(/^\d+(\.\d+)?$/)]],
      description: ['', [Validators.maxLength(500)]]
    });
  }

  get f() {
    return this.transferForm.controls;
  }

  onSubmit(): void {
    this.submitted = true;
    this.error = '';
    this.errorCode = '';
    this.success = false;

    if (!this.sourceAccountId) {
      this.error = 'Source account ID is missing. Please go back and select an account.';
      return;
    }

    // Stop if form is invalid
    if (this.transferForm.invalid) {
      return;
    }

    this.loading = true;
    const formValue = this.transferForm.value;

    const request = {
      sourceAccountId: this.sourceAccountId,
      destinationAccountId: formValue.destinationAccountId,
      amount: Number(formValue.amount),
      description: formValue.description || '',
      idempotencyKey: this.transferService.generateIdempotencyKey()
    };

    this.transferService.transfer(request).pipe(
      finalize(() => {
        this.loading = false;
        this.cdr.detectChanges();
      })
    ).subscribe({
      next: (response: TransferResponse) => {
        this.success = true;
        this.successMessage = `Transfer of $${request.amount.toFixed(2)} initiated successfully!`;
        
        // Reset form
        this.transferForm.reset();
        this.submitted = false;

        // Redirect after 2 seconds
        setTimeout(() => {
          this.router.navigate(['/dashboard']);
        }, 2000);

        this.cdr.detectChanges();
      },
      error: (err: any) => {
        console.error('Transfer error:', err);
        const message = this.getErrorMessage(err);
        this.errorCode = this.getErrorCode(err) || this.getHttpFallbackCode(err);
        
        if (err.status === 400) {
          this.error = message || 'Invalid transfer details. Please check and try again.';
        } else if (err.status === 401) {
          this.error = 'You are not authorized to perform this transfer.';
        } else if (err.status === 404) {
          this.error = 'Source or destination account not found.';
        } else if (err.status === 409) {
          this.error = message || 'Transfer could not be completed due to a conflict.';
        } else if (err.status === 429) {
          this.error = 'Rate limit exceeded. Please try again later.';
        } else {
          this.error = message || err.statusText || 'Transfer failed. Please try again.';
        }

        this.cdr.detectChanges();
      }
    });
  }

  private getErrorMessage(err: any): string {
    if (!err) {
      return '';
    }

    if (typeof err.error === 'string') {
      try {
        const parsed = JSON.parse(err.error);
        return parsed?.message || '';
      } catch {
        return err.error;
      }
    }

    return err.error?.message || err.message || '';
  }

  private getErrorCode(err: any): string {
    if (!err) {
      return '';
    }

    if (typeof err.error === 'string') {
      try {
        const parsed = JSON.parse(err.error);
        return parsed?.code || '';
      } catch {
        return '';
      }
    }

    return err.error?.code || '';
  }

  private getHttpFallbackCode(err: any): string {
    if (!err?.status) {
      return '';
    }

    return `HTTP-${err.status}`;
  }

  onCancel(): void {
    this.router.navigate(['/dashboard']);
  }
}
