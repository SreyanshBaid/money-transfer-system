import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { TransferService, TransferResponse } from '../services/transfer.service';

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
  success = false;
  successMessage = '';
  sourceAccountId: string | null = null;

  constructor(
    private fb: FormBuilder,
    private transferService: TransferService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    // Get source account ID from route query params
    this.route.queryParams.subscribe(params => {
      this.sourceAccountId = params['accountId'];
      this.initializeForm();
    });
  }

  private initializeForm(): void {
    this.transferForm = this.fb.group({
      destinationAccountId: ['', [Validators.required, Validators.minLength(1)]],
      amount: ['', [Validators.required, Validators.min(0.01)]],
      description: ['', [Validators.maxLength(500)]]
    });
  }

  get f() {
    return this.transferForm.controls;
  }

  onSubmit(): void {
    this.submitted = true;
    this.error = '';
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

    this.transferService.transfer(request).subscribe({
      next: (response: TransferResponse) => {
        this.success = true;
        this.successMessage = `Transfer of $${request.amount.toFixed(2)} initiated successfully!`;
        this.loading = false;
        
        // Reset form
        this.transferForm.reset();
        this.submitted = false;

        // Redirect after 2 seconds
        setTimeout(() => {
          this.router.navigate(['/dashboard']);
        }, 2000);
      },
      error: (err) => {
        this.loading = false;
        console.error('Transfer error:', err);
        
        if (err.status === 400) {
          this.error = err.error?.message || 'Invalid transfer details. Please check and try again.';
        } else if (err.status === 401) {
          this.error = 'You are not authorized to perform this transfer.';
        } else if (err.status === 404) {
          this.error = 'Source or destination account not found.';
        } else if (err.status === 429) {
          this.error = 'Rate limit exceeded. Please try again later.';
        } else {
          this.error = err.error?.message || 'Transfer failed. Please try again.';
        }
      }
    });
  }

  onCancel(): void {
    this.router.navigate(['/dashboard']);
  }
}
