import { Routes } from '@angular/router';

import { authGuard } from './core/auth.guard';

const seccion = () => import('./pages/seccion/seccion').then((m) => m.Seccion);

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
        loadComponent: seccion,
        data: { titulo: 'Mesas', descripcion: 'Salones, mesas y pedidos en curso.' },
      },
      {
        path: 'caja',
        loadComponent: seccion,
        data: {
          titulo: 'Caja',
          descripcion: 'Apertura y cierre, ingresos, egresos y ventas del día.',
        },
      },
      {
        path: 'inventario',
        loadComponent: seccion,
        data: {
          titulo: 'Inventario',
          descripcion: 'Stock, entradas y salidas de insumos y productos.',
        },
      },
      {
        path: 'informes',
        loadComponent: seccion,
        data: {
          titulo: 'Informes',
          descripcion: 'Ventas, compras, finanzas y operaciones.',
        },
      },
      {
        path: 'configuracion',
        loadComponent: seccion,
        data: {
          titulo: 'Configuración',
          descripcion: 'Datos del restaurante, usuarios, productos y mesas.',
        },
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
