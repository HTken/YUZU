import { Routes } from '@angular/router';

import { authGuard } from './core/auth.guard';
import { soloRoles } from './core/rol.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login').then((m) => m.Login),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/shell/shell').then((m) => m.Shell),
    children: [
      {
        path: '',
        pathMatch: 'full',
        loadComponent: () => import('./pages/inicio/inicio').then((m) => m.Inicio),
      },
      {
        path: 'mesas',
        children: [
          {
            path: '',
            pathMatch: 'full',
            loadComponent: () => import('./pages/salon/salon').then((m) => m.SalonMesas),
          },
          {
            path: ':mesaId',
            loadComponent: () => import('./pages/salon/pedido').then((m) => m.PedidoMesa),
          },
        ],
      },
      {
        path: 'cocina',
        loadComponent: () => import('./pages/cocina/cocina').then((m) => m.Cocina),
      },
      {
        path: 'caja',
        loadComponent: () => import('./pages/caja/caja').then((m) => m.Caja),
      },
      {
        path: 'inventario',
        children: [
          {
            path: '',
            pathMatch: 'full',
            loadComponent: () => import('./pages/inventario/inventario').then((m) => m.Inventario),
          },
          {
            path: 'recetas',
            loadComponent: () => import('./pages/inventario/recetas').then((m) => m.Recetas),
          },
        ],
      },
      {
        path: 'informes',
        canActivate: [soloRoles('ADMIN')],
        loadComponent: () => import('./pages/informes/informes').then((m) => m.Informes),
      },
      {
        path: 'configuracion',
        children: [
          {
            path: '',
            pathMatch: 'full',
            loadComponent: () =>
              import('./pages/configuracion/configuracion').then((m) => m.Configuracion),
          },
          {
            path: 'usuarios',
            loadComponent: () =>
              import('./pages/configuracion/usuarios/usuarios').then((m) => m.Usuarios),
          },
          {
            path: 'plan',
            loadComponent: () => import('./pages/configuracion/plan/plan').then((m) => m.Plan),
          },
          {
            path: 'productos',
            loadComponent: () =>
              import('./pages/configuracion/productos/productos').then((m) => m.Productos),
          },
          {
            path: 'mesas',
            loadComponent: () => import('./pages/configuracion/mesas/mesas').then((m) => m.Mesas),
          },
          {
            path: 'negocio',
            canActivate: [soloRoles('ADMIN')],
            loadComponent: () =>
              import('./pages/configuracion/negocio/negocio').then((m) => m.DatosNegocio),
          },
          {
            path: 'seguridad',
            canActivate: [soloRoles('ADMIN')],
            loadComponent: () =>
              import('./pages/configuracion/seguridad/seguridad').then((m) => m.Seguridad),
          },
        ],
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
