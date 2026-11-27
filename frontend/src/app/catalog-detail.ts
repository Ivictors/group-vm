import { Component, DestroyRef, inject, input, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { CatalogApi, Product } from './catalog-api';
import { BrlPricePipe } from './brl-price.pipe';

@Component({ selector: 'app-catalog-detail', imports: [RouterLink, BrlPricePipe], templateUrl: './catalog-detail.html' })
export class CatalogDetail implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  readonly id = input.required<string>();
  readonly state = signal<'loading' | 'ready' | 'missing' | 'error'>('loading');
  readonly product = signal<Product | null>(null);

  constructor(private readonly api: CatalogApi) {}

  ngOnInit() { this.load(); }

  load() {
    this.state.set('loading');
    this.api.detail(this.id()).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: product => { this.product.set(product); this.state.set('ready'); },
      error: (error: HttpErrorResponse) => this.state.set(error.status === 404 ? 'missing' : 'error')
    });
  }
}
