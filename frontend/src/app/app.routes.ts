import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';
import { loggedInGuard } from './guards/logged-in.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./components/login/login').then(m => m.LoginComponent),
    canActivate: [loggedInGuard],
  },
  {
    path: '',
    loadComponent: () =>
      import('./components/main/main').then(m => m.MainComponent),
    canActivate: [authGuard],
  },
  {
    path: '**',
    loadComponent: () =>
      import('./components/not-found/not-found').then(m => m.NotFoundComponent),
  },
];
