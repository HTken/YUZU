import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from './config';
import { Parametros3DS } from './culqi.service';

export interface CulqiConfig {
  publicKey: string;
}

export interface CobroRequest {
  tokenId: string;
  meses: number;
  email: string;
  /** Del flujo 3D Secure (Culqi3DS): se manda siempre que se tenga. */
  deviceFingerPrintId?: string | null;
  /** Solo en el reintento, con el resultado de la autenticación del banco. */
  authentication3DS?: Parametros3DS | null;
}

export interface ResultadoCobro {
  cargo: string;
  /** Fecha en formato 2026-12-14. */
  venceEn: string;
  estado: string;
}

export interface ActivarSuscripcionRequest {
  tokenId: string;
  email: string;
  authentication3DS?: Parametros3DS | null;
}

export interface SuscripcionCulqi {
  activa: boolean;
  marcaTarjeta: string | null;
  ultimosCuatro: string | null;
}

@Injectable({ providedIn: 'root' })
export class PagosApi {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_URL}/api/pagos/culqi`;

  config(): Observable<CulqiConfig> {
    return this.http.get<CulqiConfig>(`${this.url}/config`);
  }

  cobrar(datos: CobroRequest): Observable<ResultadoCobro> {
    return this.http.post<ResultadoCobro>(`${this.url}/cobrar`, datos);
  }

  suscripcion(): Observable<SuscripcionCulqi> {
    return this.http.get<SuscripcionCulqi>(`${this.url}/suscripcion`);
  }

  activarSuscripcion(datos: ActivarSuscripcionRequest): Observable<SuscripcionCulqi> {
    return this.http.post<SuscripcionCulqi>(`${this.url}/suscripcion`, datos);
  }

  cancelarSuscripcion(): Observable<SuscripcionCulqi> {
    return this.http.delete<SuscripcionCulqi>(`${this.url}/suscripcion`);
  }
}
