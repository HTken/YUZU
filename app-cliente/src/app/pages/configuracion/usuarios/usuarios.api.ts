import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '../../../core/config';

export type Rol = 'ADMIN' | 'CAJERO' | 'MOZO' | 'COCINA' | 'REPARTIDOR';

export const ROLES: { valor: Rol; etiqueta: string }[] = [
  { valor: 'ADMIN', etiqueta: 'Administrador' },
  { valor: 'CAJERO', etiqueta: 'Cajero' },
  { valor: 'MOZO', etiqueta: 'Mozo' },
  { valor: 'COCINA', etiqueta: 'Cocina' },
  { valor: 'REPARTIDOR', etiqueta: 'Repartidor' },
];

export interface UsuarioDto {
  id: number;
  usuario: string;
  nombre: string;
  email: string | null;
  rol: Rol;
  activo: boolean;
}

export interface UsuarioCrear {
  usuario: string;
  nombre: string;
  email: string | null;
  rol: Rol;
  clave: string;
}

export interface UsuarioActualizar {
  nombre: string;
  email: string | null;
  rol: Rol;
  activo: boolean;
  /** Si no se envía, la contraseña actual no cambia. */
  clave?: string;
}

@Injectable({ providedIn: 'root' })
export class UsuariosApi {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_URL}/api/usuarios`;

  listar(): Observable<UsuarioDto[]> {
    return this.http.get<UsuarioDto[]>(this.url);
  }

  crear(datos: UsuarioCrear): Observable<UsuarioDto> {
    return this.http.post<UsuarioDto>(this.url, datos);
  }

  actualizar(id: number, datos: UsuarioActualizar): Observable<UsuarioDto> {
    return this.http.put<UsuarioDto>(`${this.url}/${id}`, datos);
  }
}
