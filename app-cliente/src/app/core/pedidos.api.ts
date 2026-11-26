import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from './config';

export type EstadoItem = 'PENDIENTE' | 'ENVIADO' | 'LISTO' | 'SERVIDO' | 'ANULADO';

export interface ItemPedido {
  id: number;
  productoId: number;
  nombre: string;
  precioUnitario: number;
  cantidad: number;
  nota: string | null;
  /** PENDIENTE = aún en la mesa; ENVIADO = en cocina; LISTO = listo para servir; SERVIDO = ya en la mesa. */
  estado: EstadoItem;
  subtotal: number;
}

export interface Pedido {
  id: number;
  mesaId: number;
  mesa: string;
  salon: string;
  estado: string;
  mozo: string;
  /** Fecha en formato 2026-11-18T07:05:12Z (hora UTC). */
  abiertoEn: string;
  items: ItemPedido[];
  total: number;
  /** Cuántos productos faltan por enviar a cocina. */
  pendientes: number;
}

/** Lo mínimo de cada cuenta abierta, para pintar el mapa de mesas. */
export interface PedidoResumen {
  id: number;
  mesaId: number;
  mozo: string;
  abiertoEn: string;
  total: number;
  productos: number;
  pendientes: number;
  /** Platos que cocina todavía prepara. */
  enCocina: number;
  /** Platos listos para que el mozo los sirva. */
  listos: number;
}

export interface ItemRequest {
  productoId: number;
  cantidad: number;
  nota: string | null;
}

export interface ItemActualizar {
  cantidad: number;
  nota: string | null;
}

/** Pedidos de salón. El restaurante lo decide el servidor según tu sesión, nunca se envía desde aquí. */
@Injectable({ providedIn: 'root' })
export class PedidosApi {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_URL}/api/pedidos`;

  abiertos(): Observable<PedidoResumen[]> {
    return this.http.get<PedidoResumen[]>(`${this.url}/abiertos`);
  }

  obtener(id: number): Observable<Pedido> {
    return this.http.get<Pedido>(`${this.url}/${id}`);
  }

  /** Abre la cuenta de una mesa. Si ya había una abierta, devuelve esa misma. */
  abrir(mesaId: number): Observable<Pedido> {
    return this.http.post<Pedido>(this.url, { mesaId });
  }

  agregar(pedidoId: number, datos: ItemRequest): Observable<Pedido> {
    return this.http.post<Pedido>(`${this.url}/${pedidoId}/items`, datos);
  }

  actualizar(pedidoId: number, itemId: number, datos: ItemActualizar): Observable<Pedido> {
    return this.http.put<Pedido>(`${this.url}/${pedidoId}/items/${itemId}`, datos);
  }

  quitar(pedidoId: number, itemId: number): Observable<Pedido> {
    return this.http.delete<Pedido>(`${this.url}/${pedidoId}/items/${itemId}`);
  }

  enviar(pedidoId: number): Observable<Pedido> {
    return this.http.post<Pedido>(`${this.url}/${pedidoId}/enviar`, {});
  }

  anular(pedidoId: number): Observable<Pedido> {
    return this.http.post<Pedido>(`${this.url}/${pedidoId}/anular`, {});
  }

  /** El mozo marca un plato como servido. */
  servir(pedidoId: number, itemId: number): Observable<Pedido> {
    return this.http.post<Pedido>(`${this.url}/${pedidoId}/items/${itemId}/servir`, {});
  }

  /** El mozo marca como servido todo lo que está listo. */
  servirListos(pedidoId: number): Observable<Pedido> {
    return this.http.post<Pedido>(`${this.url}/${pedidoId}/servir`, {});
  }

  anularItem(pedidoId: number, itemId: number): Observable<Pedido> {
    return this.http.post<Pedido>(`${this.url}/${pedidoId}/items/${itemId}/anular`, {});
  }
}
