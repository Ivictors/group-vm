import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { CatalogApi, ProductPage } from './catalog-api';
import { BrlPricePipe } from './brl-price.pipe';

@Component({ selector: 'app-catalog-list', imports: [RouterLink, BrlPricePipe], templateUrl: './catalog-list.html' })
export class CatalogList implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  readonly state = signal<'loading' | 'ready' | 'error'>('loading');
  readonly result = signal<ProductPage | null>(null);
  readonly page = signal(0);

  constructor(private readonly api: CatalogApi) {}

  ngOnInit() { this.load(0); }

  load(page: number) {
    this.page.set(page);
    this.state.set('loading');
    this.api.list(page).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: result => { this.result.set(result); this.state.set('ready'); },
      error: () => this.state.set('error')
    });
  }
}
