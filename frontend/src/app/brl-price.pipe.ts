import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'brlPrice' })
export class BrlPricePipe implements PipeTransform {
  transform(amount: string): string {
    const [integer, cents] = amount.split('.');
    return `R$ ${integer.replace(/\B(?=(\d{3})+(?!\d))/g, '.')},${cents}`;
  }
}
