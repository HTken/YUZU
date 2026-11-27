import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, map, tap } from 'rxjs';

import { API_URL } from './config';

export interface LoginRequest {
  usuario: string;
  clave: string;
  /** Código de la app autenticadora. Solo hace falta si el servidor respondió requiere2fa=true. */
  codigoTotp?: string;
}

interface SesionApi {
  expiraEnSegundos: number;
  usuario: string;
  nombre: string;
}

interface LoginRespuestaApi {
  requiere2fa: boolean;
  sesion: SesionApi | null;
}

export interface Sesion extends SesionApi {
  /** Momento en que vence el token, en milisegundos desde 1970. */
  expiraEn: number;
}

const CLAVE_ALMACEN = 'yuzu_plataforma_sesion';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly estado = signal<Sesion | null>(this.leer());

  readonly sesion = this.estado.asReadonly();

  estaAutenticado(): boolean {
    const actual = this.estado();
    if (actual && actual.expiraEn <= Date.now()) {
      this.limpiar();
      return false;
    }
    return actual !== null;
  }

  /** Si requiere2fa viene en true, no se guarda ninguna sesión todavía: hay que reenviar el mismo login
   *  con el código de la app autenticadora en codigoTotp. */
  login(datos: LoginRequest): Observable<{ requiere2fa: boolean }> {
    return this.http
      .post<LoginRespuestaApi>(`${API_URL}/api/plataforma/auth/login`, datos, { withCredentials: true })
      .pipe(
        tap((r) => {
          if (!r.requiere2fa && r.sesion) {
            this.guardar({ ...r.sesion, expiraEn: Date.now() + r.sesion.expiraEnSegundos * 1000 });
          }
        }),
        map((r) => ({ requiere2fa: r.requiere2fa })),
      );
  }

  logout(): void {
    // El token vive en una cookie httpOnly: solo el backend puede borrarla.
    this.http.post(`${API_URL}/api/plataforma/auth/logout`, {}, { withCredentials: true }).subscribe({
      complete: () => this.cerrarSesionLocal(),
      error: () => this.cerrarSesionLocal(),
    });
  }

  private cerrarSesionLocal(): void {
    this.limpiar();
    void this.router.navigateByUrl('/login');
  }

  private guardar(sesion: Sesion): void {
    this.estado.set(sesion);
    try {
      localStorage.setItem(CLAVE_ALMACEN, JSON.stringify(sesion));
    } catch {
      // Sin almacenamiento disponible: la sesión dura solo mientras la pestaña esté abierta.
    }
  }

  private limpiar(): void {
    this.estado.set(null);
    try {
      localStorage.removeItem(CLAVE_ALMACEN);
    } catch {
      // Nada que limpiar.
    }
  }

  private leer(): Sesion | null {
    try {
      const crudo = localStorage.getItem(CLAVE_ALMACEN);
      if (!crudo) {
        return null;
      }
      const sesion = JSON.parse(crudo) as Sesion;
      return sesion.expiraEn > Date.now() ? sesion : null;
    } catch {
      return null;
    }
  }
}
