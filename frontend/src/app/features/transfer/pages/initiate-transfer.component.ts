import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
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

  constructor(
    private fb: FormBuilder,
    private transferService: TransferService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.initializeForm();
  }

  private initializeForm(): void {
    this.transferForm = this.fb.group({
      toAccountId: ['', [Validators.required, Validators.minLength(1)]],
      amount: ['', [Validators.required, Validators.min(0.01)]],
      description: ['', [Validators.maxLength(255)]]
    });
  }

  get f() {
    return this.transferForm.controls;
  }

  onSubmit(): void {
    this.submitted = true;
    this.error = '';
    this.success = false;

    // Stop if form is invalid
    if (this.transferForm.invalid) {
      return;
    }

    this.loading = true;
    const request = this.transferForm.value;

    this.transferService.transfer(request).subscribe({
      next: (response: TransferResponse) => {
        this.success = true;
        this.successMessage = `Transfer of $${request.amount} initiated successfully!`;
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
        this.error = err.message || 'Transfer failed. Please try again.';
        this.loading = false;
      }
    });
  }

  onCancel(): void {
    this.router.navigate(['/dashboard']);
  }
}
