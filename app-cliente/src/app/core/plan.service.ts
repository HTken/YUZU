import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';

import { API_URL } from './config';

export interface MiPlan {
  plan: string;
  precioMensual: number;
  maxUsuarios: number;
  usuariosActivos: number;
  maxProductos: number;
  productosActivos: number;
  maxMesas: number;
  mesasActivas: number;
  maxInsumos: number;
  insumosActivos: number;
  /** Fecha en formato 2026-12-14. */
  venceEn: string;
  diasRestantes: number;
  estado: string;
}

export function formatearFecha(iso: string): string {
  const fecha = new Date(`${iso}T00:00:00`);
  return new Intl.DateTimeFormat('es-PE', { day: 'numeric', month: 'short', year: 'numeric' }).format(
    fecha,
  );
}

export function textoVencimiento(dias: number): string {
  if (dias < 0) {
    const atras = -dias;
    return `Venció hace ${atras} ${atras === 1 ? 'día' : 'días'}`;
  }
  if (dias === 0) {
    return 'Vence hoy';
  }
  if (dias === 1) {
    return 'Vence mañana';
  }
  return `Vence en ${dias} días`;
}

export function etiquetaEstado(estado: string): string {
  switch (estado) {
    case 'ACTIVO':
      return 'Activo';
    case 'EN_GRACIA':
      return 'En periodo de gracia';
    case 'SUSPENDIDO':
      return 'Suspendido';
    default:
      return estado;
  }
}

@Injectable({ providedIn: 'root' })
export class PlanService {
  private readonly http = inject(HttpClient);
  private readonly estado = signal<MiPlan | null>(null);
  private readonly huboFallo = signal(false);

  readonly plan = this.estado.asReadonly();
  /** Es true si la última carga del plan falló. */
  readonly fallo = this.huboFallo.asReadonly();
  readonly suspendido = computed(() => this.estado()?.estado === 'SUSPENDIDO');

  cargar(): void {
    this.huboFallo.set(false);
    this.http.get<MiPlan>(`${API_URL}/api/mi-plan`).subscribe({
      next: (p) => this.estado.set(p),
      error: () => this.huboFallo.set(true),
    });
  }
}
