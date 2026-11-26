import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from './config';

export interface RestauranteResumen {
  id: number;
  nombre: string;
  subdominio: string;
  estado: string;
  planId: number;
  plan: string;
  /** Fecha en formato 2026-12-14. */
  venceEn: string;
  diasRestantes: number;
  usuariosActivos: number;
  maxUsuarios: number;
}

export interface PlanInfo {
  id: number;
  codigo: string;
  nombre: string;
  precioMensual: number;
  maxUsuarios: number;
  maxProductos: number;
  maxMesas: number;
  maxInsumos: number;
  activo: boolean;
}

export interface ResultadoVencimientos {
  aGracia: number;
  suspendidos: number;
}

export interface RestauranteCrear {
  nombre: string;
  subdominio: string;
  planId: number;
  diasDePrueba: number;
  adminUsuario: string;
  adminNombre: string;
  adminClave: string;
}

@Injectable({ providedIn: 'root' })
export class PlataformaApi {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_URL}/api/plataforma`;

  planes(): Observable<PlanInfo[]> {
    return this.http.get<PlanInfo[]>(`${this.url}/planes`);
  }

  restaurantes(): Observable<RestauranteResumen[]> {
    return this.http.get<RestauranteResumen[]>(`${this.url}/restaurantes`);
  }

  crear(datos: RestauranteCrear): Observable<RestauranteResumen> {
    return this.http.post<RestauranteResumen>(`${this.url}/restaurantes`, datos);
  }

  cambiarPlan(id: number, planId: number): Observable<RestauranteResumen> {
    return this.http.put<RestauranteResumen>(`${this.url}/restaurantes/${id}/plan`, { planId });
  }

  suspender(id: number): Observable<RestauranteResumen> {
    return this.http.post<RestauranteResumen>(`${this.url}/restaurantes/${id}/suspender`, {});
  }

  reactivar(id: number): Observable<RestauranteResumen> {
    return this.http.post<RestauranteResumen>(`${this.url}/restaurantes/${id}/reactivar`, {});
  }

  renovar(
    id: number,
    meses: number,
    metodo: string,
    referencia: string | null,
  ): Observable<RestauranteResumen> {
    return this.http.post<RestauranteResumen>(`${this.url}/restaurantes/${id}/renovar`, {
      meses,
      metodo,
      referencia,
    });
  }

  revisarVencimientos(): Observable<ResultadoVencimientos> {
    return this.http.post<ResultadoVencimientos>(`${this.url}/vencimientos/procesar`, {});
  }
}
