import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from './config';

/** Un plato por preparar dentro de un ticket. */
export interface ItemCocina {
  id: number;
  nombre: string;
  cantidad: number;
  nota: string | null;
}

/** Los platos que un mozo envió juntos desde una mesa. Cada envío es un ticket nuevo. */
export interface TicketCocina {
  /** Identifica el ticket: cuenta + momento del envío. */
  clave: string;
  pedidoId: number;
  mesa: string;
  salon: string;
  mozo: string;
  /** Fecha en formato 2026-11-18T07:05:12Z (hora UTC). */
  enviadoEn: string;
  items: ItemCocina[];
}

/** Un plato que cocina ya marcó como listo y que todavía no se sirvió. */
export interface ListoCocina {
  id: number;
  pedidoId: number;
  mesa: string;
  nombre: string;
  cantidad: number;
  nota: string | null;
  listoEn: string;
}

/** Un plato que se anuló mientras cocina lo tenía en pantalla. Se muestra hasta que cocina toca "Entendido". */
export interface AnuladoCocina {
  id: number;
  pedidoId: number;
  mesa: string;
  nombre: string;
  cantidad: number;
  nota: string | null;
  /** true si cocina ya lo había marcado como listo. */
  yaEstabaListo: boolean;
  anuladoEn: string;
}

export interface Cocina {
  pendientes: TicketCocina[];
  listos: ListoCocina[];
  anulados: AnuladoCocina[];
}

/** Pantalla de cocina. El restaurante lo decide el servidor según tu sesión, nunca se envía desde aquí. */
@Injectable({ providedIn: 'root' })
export class CocinaApi {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_URL}/api/cocina`;

  estado(): Observable<Cocina> {
    return this.http.get<Cocina>(this.url);
  }

  /** Marca platos como listos. Devuelve la pantalla actualizada. */
  listo(itemIds: number[]): Observable<Cocina> {
    return this.http.post<Cocina>(`${this.url}/listo`, { itemIds });
  }

  /** Cocina confirma que vio el aviso de plato anulado. Devuelve la pantalla actualizada. */
  anuladosVistos(itemIds: number[]): Observable<Cocina> {
    return this.http.post<Cocina>(`${this.url}/anulados/visto`, { itemIds });
  }

  /** Deshace un "listo" por error. Devuelve la pantalla actualizada. */
  regresar(itemIds: number[]): Observable<Cocina> {
    return this.http.post<Cocina>(`${this.url}/regresar`, { itemIds });
  }
}
