import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from './config';

export interface CulqiConfig {
  publicKey: string;
}

export interface CobroRequest {
  tokenId: string;
  meses: number;
  email: string;
}

export interface ResultadoCobro {
  cargo: string;
  /** Fecha en formato 2026-12-14. */
  venceEn: string;
  estado: string;
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
}
