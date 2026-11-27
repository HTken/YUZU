import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from './config';

export interface TotpEstado {
  activo: boolean;
}

/** El secreto no queda guardado hasta que se confirma con un código (ver activar()). */
export interface TotpSetup {
  secreto: string;
  otpauthUrl: string;
}

@Injectable({ providedIn: 'root' })
export class DosFactoresApi {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_URL}/api/plataforma/2fa`;

  estado(): Observable<TotpEstado> {
    return this.http.get<TotpEstado>(this.url);
  }

  iniciar(): Observable<TotpSetup> {
    return this.http.post<TotpSetup>(`${this.url}/iniciar`, {});
  }

  activar(secreto: string, codigo: string): Observable<TotpEstado> {
    return this.http.post<TotpEstado>(`${this.url}/activar`, { secreto, codigo });
  }

  desactivar(): Observable<TotpEstado> {
    return this.http.post<TotpEstado>(`${this.url}/desactivar`, {});
  }
}
