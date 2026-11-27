import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { CatalogApi } from './catalog-api';

describe('CatalogApi public HTTP contract', () => {
  let api: CatalogApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(CatalogApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('requests a zero-based page from the public endpoint', () => {
    const page = { items: [{ id: '22e19353-7501-4c4b-a943-5766bb2d5179', name: 'Chair', price: { currency: 'BRL', amount: '129.90' } }], page: 1, size: 20, totalElements: 21, totalPages: 2 };
    let received: unknown;
    api.list(1).subscribe(value => received = value);
    const request = http.expectOne('/api/v1/products?page=1&size=20');
    expect(request.request.method).toBe('GET');
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush(page);
    expect(received).toEqual(page);
  });

  it('requests only the public detail and preserves 404', () => {
    let status: number | undefined;
    api.detail('22e19353-7501-4c4b-a943-5766bb2d5179').subscribe({ error: error => status = error.status });
    const request = http.expectOne('/api/v1/products/22e19353-7501-4c4b-a943-5766bb2d5179');
    expect(request.request.method).toBe('GET');
    request.flush({ type: 'about:blank', title: 'Not Found', status: 404 }, { status: 404, statusText: 'Not Found' });
    expect(status).toBe(404);
  });
});
