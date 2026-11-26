import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from './config';

export type Unidad = 'KG' | 'G' | 'L' | 'ML' | 'UNIDAD';

export const UNIDADES: { valor: Unidad; etiqueta: string }[] = [
  { valor: 'KG', etiqueta: 'Kilogramos (kg)' },
  { valor: 'G', etiqueta: 'Gramos (g)' },
  { valor: 'L', etiqueta: 'Litros (L)' },
  { valor: 'ML', etiqueta: 'Mililitros (ml)' },
  { valor: 'UNIDAD', etiqueta: 'Unidades' },
];

/** Cómo se escribe la unidad al lado de una cantidad: "2.5 kg", "12 u.". */
export function abreviatura(unidad: string): string {
  switch (unidad) {
    case 'KG':
      return 'kg';
    case 'G':
      return 'g';
    case 'L':
      return 'L';
    case 'ML':
      return 'ml';
    default:
      return 'u.';
  }
}

export type EstadoInsumo = 'OK' | 'BAJO' | 'AGOTADO' | 'INACTIVO';

export interface Insumo {
  id: number;
  nombre: string;
  unidad: Unidad;
  stockActual: number;
  stockMinimo: number;
  activo: boolean;
  estado: EstadoInsumo;
  /** Si ya tiene movimientos, su unidad de medida no se puede cambiar. */
  unidadFija: boolean;
}

export interface InsumoDatos {
  nombre: string;
  unidad: Unidad;
  stockMinimo: number;
  activo: boolean;
  /** Solo se usa al crear. */
  stockInicial?: number;
}

export type TipoMovimiento = 'ENTRADA' | 'SALIDA' | 'MERMA' | 'AJUSTE';

export const TIPOS_MOVIMIENTO: { valor: TipoMovimiento; etiqueta: string; ayuda: string }[] = [
  { valor: 'ENTRADA', etiqueta: 'Entrada', ayuda: 'Compra o reposición: suma al stock.' },
  { valor: 'SALIDA', etiqueta: 'Salida', ayuda: 'Lo que se usó o sacó del almacén: resta del stock.' },
  { valor: 'MERMA', etiqueta: 'Merma', ayuda: 'Lo que se malogró, se cayó o venció: resta del stock.' },
  { valor: 'AJUSTE', etiqueta: 'Ajuste', ayuda: 'Corrige el stock después de contar lo que hay de verdad.' },
];

/**
 * En el historial también aparecen VENTA (lo descuenta el sistema al cobrar) y DEVOLUCION (vuelve al stock lo
 * de una venta anulada): nadie los registra a mano.
 */
export type TipoHistorial = TipoMovimiento | 'VENTA' | 'DEVOLUCION';

export function etiquetaTipo(tipo: string): string {
  if (tipo === 'VENTA') {
    return 'Venta';
  }
  if (tipo === 'DEVOLUCION') {
    return 'Devolución';
  }
  return TIPOS_MOVIMIENTO.find((t) => t.valor === tipo)?.etiqueta ?? tipo;
}

export interface MovimientoInsumo {
  id: number;
  tipo: TipoHistorial;
  /** Con signo: positiva sumó al stock y negativa restó. */
  cantidad: number;
  stockResultante: number;
  costoTotal: number | null;
  /** Solo en las ventas: lo que faltó porque el stock registrado no alcanzaba. */
  faltante: number | null;
  nota: string | null;
  registradoPor: string;
  /** Formato 2026-11-18T07:05:12Z (hora UTC). */
  creadoEn: string;
}

export interface MovimientoDatos {
  tipo: TipoMovimiento;
  /** En un ajuste es lo que se contó (el total que hay), no la diferencia. */
  cantidad: number;
  costoTotal: number | null;
  nota: string | null;
}

/** Cantidades que escribe una persona: hasta 9 enteros y 3 decimales, con punto o coma. */
export const PATRON_CANTIDAD = /^\d{1,9}([.,]\d{1,3})?$/;
const PATRON_ENTERO = /^\d{1,9}$/;

/** Si lo escrito es una cantidad válida para esa unidad (en unidades no hay decimales). */
export function cantidadValida(texto: string, unidad: string): boolean {
  const limpio = texto.trim();
  return (unidad === 'UNIDAD' ? PATRON_ENTERO : PATRON_CANTIDAD).test(limpio);
}

/** "2.5" en vez de "2.500"; con separador de miles: "1,250.5". */
export function cantidadTexto(valor: number): string {
  return valor.toLocaleString('es-PE', { maximumFractionDigits: 3 });
}

/** "2.5 kg" */
export function conUnidad(valor: number, unidad: string): string {
  return `${cantidadTexto(valor)} ${abreviatura(unidad)}`;
}

/** Con signo para el historial: "+5 kg" o "−2.5 kg". */
export function cantidadConSigno(valor: number, unidad: string): string {
  const signo = valor > 0 ? '+' : valor < 0 ? '−' : '';
  return `${signo}${conUnidad(Math.abs(valor), unidad)}`;
}

export interface CalculoMovimiento {
  /** Cuánto quedaría en el stock; null si no se puede calcular. */
  resultado: number | null;
  /** Qué está mal, si algo; vacío si el movimiento es válido. */
  error: string;
}

const MILI = 1000;
/** Lo máximo que cabe en la base de datos: 999 999 999.999, en milésimas. */
const MAXIMO_MILI = 999_999_999_999;

/**
 * Lo mismo que hace el servidor, para avisar antes de enviar: cuánto quedará en el stock
 * o por qué no se puede. Se calcula en milésimas enteras para evitar errores de decimales.
 */
export function calcularMovimiento(
  tipo: TipoMovimiento,
  actual: number,
  cantidad: number,
  unidad: string,
): CalculoMovimiento {
  const stock = Math.round(actual * MILI);
  const cant = Math.round(cantidad * MILI);

  if (!Number.isFinite(cant) || cant < 0) {
    return { resultado: null, error: 'Escribe una cantidad válida' };
  }

  if (tipo === 'AJUSTE') {
    if (cant === stock) {
      return {
        resultado: null,
        error: `El stock ya es ${conUnidad(actual, unidad)}. No hay nada que ajustar.`,
      };
    }
    return { resultado: cant / MILI, error: '' };
  }

  if (cant === 0) {
    return { resultado: null, error: 'La cantidad debe ser mayor a 0' };
  }

  if (tipo === 'ENTRADA') {
    if (stock + cant > MAXIMO_MILI) {
      return { resultado: null, error: 'El stock resultante es demasiado grande' };
    }
    return { resultado: (stock + cant) / MILI, error: '' };
  }

  if (cant > stock) {
    return { resultado: null, error: `No hay tanto stock. Hay ${conUnidad(actual, unidad)}` };
  }
  return { resultado: (stock - cant) / MILI, error: '' };
}

export interface LineaReceta {
  insumoId: number;
  insumo: string;
  unidad: Unidad;
  /** Lo que gasta UNA unidad del producto, en la unidad del insumo. */
  cantidad: number;
}

export interface Receta {
  productoId: number;
  lineas: LineaReceta[];
}

/** Cuántos insumos tiene la receta de un producto. Los productos sin receta no aparecen. */
export interface ResumenReceta {
  productoId: number;
  lineas: number;
}

export interface LineaRecetaDatos {
  insumoId: number;
  cantidad: number;
}

/** Cantidad de una receta: hasta 5 enteros y 3 decimales, con punto o coma. */
export const PATRON_RECETA = /^\d{1,5}([.,]\d{1,3})?$/;

/** Una receta puede tener hasta 30 insumos. */
export const MAXIMO_LINEAS_RECETA = 30;

@Injectable({ providedIn: 'root' })
export class InventarioApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${API_URL}/api/inventario`;

  insumos(): Observable<Insumo[]> {
    return this.http.get<Insumo[]>(`${this.base}/insumos`);
  }

  crear(datos: InsumoDatos): Observable<Insumo> {
    return this.http.post<Insumo>(`${this.base}/insumos`, datos);
  }

  actualizar(id: number, datos: InsumoDatos): Observable<Insumo> {
    return this.http.put<Insumo>(`${this.base}/insumos/${id}`, datos);
  }

  /** Devuelve el insumo con su stock ya actualizado. */
  movimiento(id: number, datos: MovimientoDatos): Observable<Insumo> {
    return this.http.post<Insumo>(`${this.base}/insumos/${id}/movimientos`, datos);
  }

  historial(id: number): Observable<MovimientoInsumo[]> {
    return this.http.get<MovimientoInsumo[]>(`${this.base}/insumos/${id}/movimientos`);
  }

  resumenRecetas(): Observable<ResumenReceta[]> {
    return this.http.get<ResumenReceta[]>(`${this.base}/recetas`);
  }

  receta(productoId: number): Observable<Receta> {
    return this.http.get<Receta>(`${this.base}/recetas/${productoId}`);
  }

  /** Reemplaza la receta completa. Una lista vacía la quita. */
  guardarReceta(productoId: number, lineas: LineaRecetaDatos[]): Observable<Receta> {
    return this.http.put<Receta>(`${this.base}/recetas/${productoId}`, { lineas });
  }
}
