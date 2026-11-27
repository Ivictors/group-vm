import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../environments/environment';

export interface Price { currency: 'BRL'; amount: string }
export interface ProductSummary { id: string; name: string; price: Price }
export interface Product extends ProductSummary {
  description?: string;
  weightGrams: number;
  lengthCm: number;
  widthCm: number;
  heightCm: number;
}
export interface ProductPage {
  items: ProductSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

@Injectable({ providedIn: 'root' })
export class CatalogApi {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.catalogApiBaseUrl}/api/v1/products`;

  list(page: number) {
    return this.http.get<ProductPage>(this.url, { params: { page, size: 20 } });
  }

  detail(id: string) {
    return this.http.get<Product>(`${this.url}/${encodeURIComponent(id)}`);
  }
}
