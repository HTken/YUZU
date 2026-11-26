import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from './config';

/** Lo vendido en un día de Lima. La fecha va como AAAA-MM-DD. */
export interface VentaDia {
  fecha: string;
  ventas: number;
  total: number;
}

export interface MetodoPagoInforme {
  metodo: string;
  /** En cuántas cuentas se usó. Una cuenta pagada con dos métodos cuenta en los dos. */
  cuentas: number;
  total: number;
}

export interface ProductoVendido {
  productoId: number;
  nombre: string;
  categoria: string;
  cantidad: number;
  importe: number;
}

export interface InformeVentas {
  desde: string;
  hasta: string;
  dias: number;
  /** Cuentas cobradas. */
  ventas: number;
  total: number;
  ticketPromedio: number;
  /** Cuentas anuladas sin cobrarse. */
  cuentasAnuladas: number;
  /** Ventas que se cobraron y luego se anularon. No suman en el total. */
  ventasAnuladas: number;
  porDia: VentaDia[];
  porMetodo: MetodoPagoInforme[];
  productos: ProductoVendido[];
}

export interface InsumoPeriodo {
  insumoId: number;
  nombre: string;
  unidad: string;
  /** Lo que descontaron las ventas. */
  consumo: number;
  merma: number;
  entradas: number;
  /** Lo que costaron las entradas que tienen costo. */
  compras: number;
  /** Lo que se vendió sin que el stock registrado alcanzara. */
  faltante: number;
}

export interface InformeInsumos {
  desde: string;
  hasta: string;
  compras: number;
  insumosConFaltante: number;
  insumos: InsumoPeriodo[];
}

export type CriterioProductos = 'CANTIDAD' | 'IMPORTE';

/** Los productos ordenados de mayor a menor por lo que se pide; a igualdad, por lo otro y luego por nombre. */
export function ordenarProductos(lista: ProductoVendido[], criterio: CriterioProductos): ProductoVendido[] {
  const principal = (p: ProductoVendido): number => (criterio === 'CANTIDAD' ? p.cantidad : p.importe);
  const otro = (p: ProductoVendido): number => (criterio === 'CANTIDAD' ? p.importe : p.cantidad);
  return [...lista].sort(
    (a, b) =>
      principal(b) - principal(a) || otro(b) - otro(a) || a.nombre.localeCompare(b.nombre, 'es', { sensitivity: 'base' }),
  );
}

/** Un monto en soles con separador de miles, por ejemplo "S/ 1,927.00". Es para cifras grandes que se leen de un vistazo. */
export function dinero(monto: number): string {
  return `S/ ${monto.toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

/** Ancho de una barra (0 a 100) respecto de la más larga. Lo que no es cero siempre se alcanza a ver. */
export function anchoBarra(valor: number, maximo: number): number {
  if (!(valor > 0) || !(maximo > 0)) {
    return 0;
  }
  return Math.round(Math.max(2, Math.min(100, (valor / maximo) * 100)) * 10) / 10;
}

/** "62 %" de un total; "menos de 1 %" si es muy poco; vacío si no hay total. */
export function porcentaje(parte: number, total: number): string {
  if (!(total > 0) || !(parte > 0)) {
    return '';
  }
  const valor = (parte / total) * 100;
  return valor < 1 ? 'menos de 1 %' : `${Math.round(valor)} %`;
}

@Injectable({ providedIn: 'root' })
export class InformesApi {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_URL}/api/informes`;

  ventas(desde: string, hasta: string): Observable<InformeVentas> {
    return this.http.get<InformeVentas>(`${this.url}/ventas`, { params: { desde, hasta } });
  }

  insumos(desde: string, hasta: string): Observable<InformeInsumos> {
    return this.http.get<InformeInsumos>(`${this.url}/insumos`, { params: { desde, hasta } });
  }
}
