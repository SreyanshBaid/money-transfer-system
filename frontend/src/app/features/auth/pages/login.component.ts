import { Component, OnInit, NgZone, ChangeDetectorRef } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { CommonModule } from '@angular/common';

// Reactive login form with validation and error handling
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {
  loginForm!: FormGroup;
  loading = false;
  submitted = false;
  error = '';
  returnUrl: string = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private ngZone: NgZone,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.initializeForm();
    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/dashboard';
  }

  private initializeForm(): void {
    this.loginForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(20)]],
      password: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(20)]]
    });
  }

  get f() {
    return this.loginForm.controls;
  }

  onSubmit(): void {
    this.submitted = true;
    this.error = '';

    // Stop if form is invalid
    if (this.loginForm.invalid) {
      return;
    }

    this.loading = true;
    const credentials = this.loginForm.value;

    this.authService.login(credentials).subscribe({
      next: () => {
        this.ngZone.run(() => {
          console.log('Login success, setting loading to false');
          this.loading = false;
          this.cdr.detectChanges();
          this.router.navigateByUrl(this.returnUrl);
        });
      },
      error: (err) => {
        this.ngZone.run(() => {
          console.error('Login error:', err);
          console.log('Setting loading to false, current value:', this.loading);
          if (err.status === 401) {
            this.error = 'Wrong credentials! Please try again.';
          } else {
            this.error = err.message || 'Login failed. Please try again.';
          }
          this.loading = false;
          console.log('Loading set to false, new value:', this.loading);
          console.log('Error message set to:', this.error);
          this.cdr.detectChanges();
          console.log('Change detection triggered manually');
        });
      },
      complete: () => {
        console.log('Login request completed');
      }
    });
  }
}
