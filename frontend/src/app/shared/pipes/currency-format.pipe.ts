import { Pipe, PipeTransform } from '@angular/core';

// Currency formatting pipe
@Pipe({
  name: 'currencyFormat',
  standalone: true
})
export class CurrencyFormatPipe implements PipeTransform {
  transform(value: any): string {
    if (value === null || value === undefined) return '';

    const amount = parseFloat(value);
    if (isNaN(amount)) return '';

    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format(amount);
  }
}
