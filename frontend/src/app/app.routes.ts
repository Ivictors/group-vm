import { Routes } from '@angular/router';
import { CatalogList } from './catalog-list';
import { CatalogDetail } from './catalog-detail';

export const routes: Routes = [
  { path: '', component: CatalogList },
  { path: 'products/:id', component: CatalogDetail },
  { path: '**', redirectTo: '' }
];
