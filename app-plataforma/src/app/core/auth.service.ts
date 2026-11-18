import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, map, tap } from 'rxjs';

import { API_URL } from './config';

export interface LoginRequest {
  usuario: string;
  clave: string;
}

interface LoginRespuesta {
  token: string;
  tipo: string;
  expiraEnSegundos: number;
  usuario: string;
  nombre: string;
}

export interface Sesion extends LoginRespuesta {
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

  login(datos: LoginRequest): Observable<Sesion> {
    return this.http.post<LoginRespuesta>(`${API_URL}/api/plataforma/auth/login`, datos).pipe(
      map((r): Sesion => ({ ...r, expiraEn: Date.now() + r.expiraEnSegundos * 1000 })),
      tap((s) => this.guardar(s)),
    );
  }

  logout(): void {
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
