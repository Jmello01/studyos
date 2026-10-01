import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  {
    path: 'dashboard',
    loadComponent: () => import('./features/dashboard/dashboard-page').then((m) => m.DashboardPage),
  },
  {
    path: 'sessions',
    loadComponent: () => import('./features/sessions/sessions-page').then((m) => m.SessionsPage),
  },
  {
    path: 'planning',
    loadComponent: () => import('./features/planning/planning-page').then((m) => m.PlanningPage),
  },
  {
    path: 'catalog',
    loadComponent: () => import('./features/catalog/catalog-page').then((m) => m.CatalogPage),
  },
  { path: '**', redirectTo: 'dashboard' },
];
