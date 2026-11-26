import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from './config';

/** Anchos de papel de las impresoras de tickets: 80 mm (la más común) y 58 mm (las pequeñas). */
export type AnchoTicket = 58 | 80;

/**
 * Datos del negocio que salen en el ticket. Lo que no se llenó llega como null.
 * El nombre es el del restaurante en el sistema y no se cambia desde aquí.
 */
export interface Negocio {
  nombre: string;
  razonSocial: string | null;
  ruc: string | null;
  direccion: string | null;
  telefono: string | null;
  mensajeTicket: string | null;
  anchoTicketMm: AnchoTicket;
}

export interface NegocioRequest {
  razonSocial: string;
  ruc: string;
  direccion: string;
  telefono: string;
  mensajeTicket: string;
  anchoTicketMm: AnchoTicket;
}

/** Datos del negocio. Solo el administrador entra aquí; el restaurante lo decide el servidor según tu sesión. */
@Injectable({ providedIn: 'root' })
export class NegocioApi {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_URL}/api/negocio`;

  obtener(): Observable<Negocio> {
    return this.http.get<Negocio>(this.url);
  }

  guardar(datos: NegocioRequest): Observable<Negocio> {
    return this.http.put<Negocio>(this.url, datos);
  }
}
