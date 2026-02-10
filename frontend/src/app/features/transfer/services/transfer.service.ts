import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface TransferRequest {
  sourceAccountId: string;
  destinationAccountId: string;
  amount: number;
  description?: string;
  idempotencyKey: string;
}

export interface TransferResponse {
  id: string;
  status: 'SUCCESS' | 'PENDING' | 'FAILED';
  amount: number;
  timestamp: Date;
  message?: string;
}

// Handles money transfer API calls
@Injectable({
  providedIn: 'root'
})
export class TransferService {
  private apiUrl = 'http://localhost:8080/api/v1/transfers';

  constructor(private http: HttpClient) {}

  // Initiates a money transfer
  transfer(request: TransferRequest): Observable<TransferResponse> {
    return this.http.post<TransferResponse>(`${this.apiUrl}`, request);
  }

  // Gets transfer history
  getHistory(limit: number = 20): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}`, {
      params: { limit: limit.toString() }
    });
  }

  // Generates a UUID v4 for idempotency key
  generateIdempotencyKey(): string {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
      const r = Math.random() * 16 | 0;
      const v = c === 'x' ? r : (r & 0x3 | 0x8);
      return v.toString(16);
    });
  }
}
