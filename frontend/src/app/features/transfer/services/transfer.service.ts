import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface TransferRequest {
  toAccountId: string;
  amount: number;
  description?: string;
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
  private apiUrl = 'http://localhost:8080/api/transfers';

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
}
