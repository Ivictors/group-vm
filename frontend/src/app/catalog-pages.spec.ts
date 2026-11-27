import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { CatalogList } from './catalog-list';
import { CatalogDetail } from './catalog-detail';

const id = '22e19353-7501-4c4b-a943-5766bb2d5179';
const summary = { id, name: 'Chair', price: { currency: 'BRL', amount: '129.90' } };
const unexpectedAdminFields = {
  sku: 'PRIVATE-SKU-99',
  status: 'ACTIVE',
  priceHistory: [{ actor: 'PRIVATE-ACTOR-77', newAmount: '99.00' }]
};

function expectNoAdminFields(text: string) {
  expect(text).not.toContain('PRIVATE-SKU-99');
  expect(text).not.toContain('ACTIVE');
  expect(text).not.toContain('PRIVATE-ACTOR-77');
  expect(text).not.toContain('99,00');
}

describe('Public catalog pages', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('renders loading, empty, error, retry, and a page of only public fields', () => {
    const fixture = TestBed.createComponent(CatalogList);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Carregando');
    http.expectOne('/api/v1/products?page=0&size=20').flush({ items: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Nenhum produto');

    fixture.componentInstance.load(0);
    fixture.detectChanges();
    http.expectOne('/api/v1/products?page=0&size=20').flush({}, { status: 503, statusText: 'Unavailable' });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Não foi possível');
    fixture.nativeElement.querySelector('button.retry').click();
    fixture.detectChanges();
    http.expectOne('/api/v1/products?page=0&size=20').flush({ items: [{ ...summary, ...unexpectedAdminFields }], page: 0, size: 20, totalElements: 21, totalPages: 2 });
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Chair');
    expect(text).toContain('R$ 129,90');
    expectNoAdminFields(text);
    expect(fixture.nativeElement.querySelector('a[href="/products/' + id + '"]')).toBeTruthy();
    fixture.nativeElement.querySelector('button.next').click();
    http.expectOne('/api/v1/products?page=1&size=20').flush({ items: [], page: 1, size: 20, totalElements: 21, totalPages: 2 });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Página 2 de 2');
    fixture.nativeElement.querySelector('button.previous').click();
    http.expectOne('/api/v1/products?page=0&size=20').flush({ items: [{ ...summary, ...unexpectedAdminFields }], page: 0, size: 20, totalElements: 21, totalPages: 2 });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Página 1 de 2');
    expectNoAdminFields(fixture.nativeElement.textContent);
  });

  it('renders public detail, 404, and retry after a transport error', () => {
    const fixture = TestBed.createComponent(CatalogDetail);
    fixture.componentRef.setInput('id', id);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Carregando');
    http.expectOne('/api/v1/products/' + id).flush({ ...summary, ...unexpectedAdminFields, description: 'Wooden chair', weightGrams: 4500, lengthCm: 55, widthCm: 50, heightCm: 90 });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Wooden chair');
    expect(fixture.nativeElement.textContent).toContain('R$ 129,90');
    expect(fixture.nativeElement.textContent).toContain('4.500 g');
    expectNoAdminFields(fixture.nativeElement.textContent);

    fixture.componentInstance.load();
    http.expectOne('/api/v1/products/' + id).flush({}, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Produto não encontrado');

    fixture.componentInstance.load();
    http.expectOne('/api/v1/products/' + id).flush({}, { status: 503, statusText: 'Unavailable' });
    fixture.detectChanges();
    fixture.nativeElement.querySelector('button.retry').click();
    http.expectOne('/api/v1/products/' + id).flush({ ...summary, weightGrams: 4500, lengthCm: 55, widthCm: 50, heightCm: 90 });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Chair');
  });

  it('cancels pending list and detail requests when their components are destroyed', () => {
    const list = TestBed.createComponent(CatalogList);
    list.detectChanges();
    const listRequest = http.expectOne('/api/v1/products?page=0&size=20');
    list.destroy();
    expect(listRequest.cancelled).toBe(true);

    const detail = TestBed.createComponent(CatalogDetail);
    detail.componentRef.setInput('id', id);
    detail.detectChanges();
    const detailRequest = http.expectOne('/api/v1/products/' + id);
    detail.destroy();
    expect(detailRequest.cancelled).toBe(true);
  });
});
