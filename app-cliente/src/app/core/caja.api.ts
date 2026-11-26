import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from './config';

export type MetodoPago = 'EFECTIVO' | 'YAPE' | 'PLIN' | 'TARJETA';

export const METODOS: { valor: MetodoPago; etiqueta: string }[] = [
  { valor: 'EFECTIVO', etiqueta: 'Efectivo' },
  { valor: 'YAPE', etiqueta: 'Yape' },
  { valor: 'PLIN', etiqueta: 'Plin' },
  { valor: 'TARJETA', etiqueta: 'Tarjeta' },
];

export function etiquetaMetodo(metodo: string): string {
  return METODOS.find((m) => m.valor === metodo)?.etiqueta ?? metodo;
}

/** Un turno de caja. Las fechas van en formato 2026-11-18T07:05:12Z (hora UTC). */
export interface Turno {
  id: number;
  estado: 'ABIERTA' | 'CERRADA';
  abiertaPor: string;
  abiertaEn: string;
  montoInicial: number;
  cerradaPor: string | null;
  cerradaEn: string | null;
  totalVentas: number | null;
  numVentas: number | null;
  efectivoEsperado: number | null;
  efectivoContado: number | null;
  diferencia: number | null;
  notaCierre: string | null;
}

export interface Pago {
  metodo: MetodoPago;
  monto: number;
}

export interface Venta {
  pedidoId: number;
  ticket: number;
  mesa: string;
  mozo: string;
  cobradoPor: string;
  cobradoEn: string;
  total: number;
  pagos: Pago[];
}

/** Una venta que se cobró y luego se anuló en el mismo turno. Ya no suma; queda como constancia. */
export interface VentaAnulada {
  pedidoId: number;
  ticket: number;
  mesa: string;
  total: number;
  cobradoEn: string;
  anuladoPor: string;
  anuladoEn: string;
  motivo: string;
}

export interface Movimiento {
  id: number;
  tipo: 'INGRESO' | 'EGRESO';
  concepto: string;
  monto: number;
  registradoPor: string;
  creadoEn: string;
}

export interface TotalMetodo {
  metodo: MetodoPago;
  monto: number;
}

/**
 * Todo lo que pasó en el turno. El efectivo esperado es lo que debería haber en el cajón.
 * Las ventas anuladas no suman en el total, en los métodos de pago ni en el efectivo.
 */
export interface ResumenTurno {
  turno: Turno;
  ventas: number;
  totalVentas: number;
  porMetodo: TotalMetodo[];
  ingresos: number;
  egresos: number;
  efectivoEsperado: number;
  /** Cuentas de mesas que todavía no se cobran. */
  cuentasAbiertas: number;
  detalleVentas: Venta[];
  ventasAnuladas: VentaAnulada[];
  movimientos: Movimiento[];
}

export interface EstadoCaja {
  abierta: boolean;
  resumen: ResumenTurno | null;
}

export interface Cobro {
  pedidoId: number;
  ticket: number;
  total: number;
  cobradoEn: string;
}

export interface PagoRequest {
  metodo: MetodoPago;
  monto: number;
}

/** Caja. El restaurante lo decide el servidor según tu sesión, nunca se envía desde aquí. */
@Injectable({ providedIn: 'root' })
export class CajaApi {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_URL}/api/caja`;

  estado(): Observable<EstadoCaja> {
    return this.http.get<EstadoCaja>(`${this.url}/actual`);
  }

  /** Los últimos turnos cerrados. */
  historial(): Observable<Turno[]> {
    return this.http.get<Turno[]>(`${this.url}/turnos`);
  }

  abrir(montoInicial: number): Observable<ResumenTurno> {
    return this.http.post<ResumenTurno>(`${this.url}/abrir`, { montoInicial });
  }

  movimiento(tipo: 'INGRESO' | 'EGRESO', concepto: string, monto: number): Observable<ResumenTurno> {
    return this.http.post<ResumenTurno>(`${this.url}/movimientos`, { tipo, concepto, monto });
  }

  cerrar(efectivoContado: number, nota: string | null): Observable<Turno> {
    return this.http.post<Turno>(`${this.url}/cerrar`, { efectivoContado, nota });
  }

  cobrar(pedidoId: number, pagos: PagoRequest[]): Observable<Cobro> {
    return this.http.post<Cobro>(`${this.url}/pedidos/${pedidoId}/cobrar`, { pagos });
  }

  /** Anula una venta ya cobrada de la caja abierta (solo el administrador). Devuelve el resumen ya actualizado. */
  anularVenta(pedidoId: number, motivo: string, devolverInsumos: boolean): Observable<ResumenTurno> {
    return this.http.post<ResumenTurno>(`${this.url}/ventas/${pedidoId}/anular`, { motivo, devolverInsumos });
  }
}
