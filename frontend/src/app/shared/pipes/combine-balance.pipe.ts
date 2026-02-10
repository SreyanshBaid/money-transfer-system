import { Pipe, PipeTransform } from '@angular/core';
import { AccountCardViewModel } from '../../features/dashboard/services/account.service';

@Pipe({
  name: 'combineBalance',
  standalone: true
})
export class CombineBalancePipe implements PipeTransform {
  /**
   * Combines all account balances from an array of accounts
   * @param accounts Array of account view models
   * @returns Total combined balance across all accounts
   */
  transform(accounts: AccountCardViewModel[] | null | undefined): number {
    if (!accounts || !Array.isArray(accounts) || accounts.length === 0) {
      return 0;
    }
    
    return accounts.reduce((total, account) => {
      const balance = typeof account.balance === 'number' ? account.balance : 0;
      return total + balance;
    }, 0);
  }
}
