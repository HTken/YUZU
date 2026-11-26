import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from './config';

export interface Salon {
  id: number;
  nombre: string;
  orden: number;
  activo: boolean;
  /** Cuántas mesas tiene en total. */
  mesas: number;
  mesasActivas: number;
}

export interface SalonRequest {
  nombre: string;
  orden: number;
  activo: boolean;
}

export interface Mesa {
  id: number;
  salonId: number;
  salon: string;
  nombre: string;
  capacidad: number;
  activo: boolean;
}

export interface MesaRequest {
  salonId: number;
  nombre: string;
  capacidad: number;
  activo: boolean;
}

/** Crea varias mesas de una vez: "Mesa 1", "Mesa 2"… */
export interface MesaLoteRequest {
  salonId: number;
  prefijo: string;
  desde: number;
  cantidad: number;
  capacidad: number;
}

/** Salones y mesas. El restaurante lo decide el servidor según tu sesión, nunca se envía desde aquí. */
@Injectable({ providedIn: 'root' })
export class MesasApi {
  private readonly http = inject(HttpClient);
  private readonly urlSalones = `${API_URL}/api/salones`;
  private readonly urlMesas = `${API_URL}/api/mesas`;

  salones(): Observable<Salon[]> {
    return this.http.get<Salon[]>(this.urlSalones);
  }

  crearSalon(datos: SalonRequest): Observable<Salon> {
    return this.http.post<Salon>(this.urlSalones, datos);
  }

  actualizarSalon(id: number, datos: SalonRequest): Observable<Salon> {
    return this.http.put<Salon>(`${this.urlSalones}/${id}`, datos);
  }

  mesas(): Observable<Mesa[]> {
    return this.http.get<Mesa[]>(this.urlMesas);
  }

  crearMesa(datos: MesaRequest): Observable<Mesa> {
    return this.http.post<Mesa>(this.urlMesas, datos);
  }

  crearLote(datos: MesaLoteRequest): Observable<Mesa[]> {
    return this.http.post<Mesa[]>(`${this.urlMesas}/lote`, datos);
  }

  actualizarMesa(id: number, datos: MesaRequest): Observable<Mesa> {
    return this.http.put<Mesa>(`${this.urlMesas}/${id}`, datos);
  }
}
