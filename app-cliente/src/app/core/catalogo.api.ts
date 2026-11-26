import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from './config';

export interface Categoria {
  id: number;
  nombre: string;
  orden: number;
  activo: boolean;
  /** false para categorías como las bebidas: no aparecen en la pantalla de cocina. */
  pasaPorCocina: boolean;
  /** Cuántos productos tiene en total. */
  productos: number;
  productosActivos: number;
}

export interface CategoriaRequest {
  nombre: string;
  orden: number;
  activo: boolean;
  pasaPorCocina: boolean;
}

export interface Producto {
  id: number;
  categoriaId: number;
  categoria: string;
  nombre: string;
  descripcion: string | null;
  precio: number;
  activo: boolean;
}

export interface ProductoRequest {
  categoriaId: number;
  nombre: string;
  descripcion: string | null;
  precio: number;
  activo: boolean;
}

/** Carta del restaurante. El restaurante lo decide el servidor según tu sesión, nunca se envía desde aquí. */
@Injectable({ providedIn: 'root' })
export class CatalogoApi {
  private readonly http = inject(HttpClient);
  private readonly urlCategorias = `${API_URL}/api/categorias`;
  private readonly urlProductos = `${API_URL}/api/productos`;

  categorias(): Observable<Categoria[]> {
    return this.http.get<Categoria[]>(this.urlCategorias);
  }

  crearCategoria(datos: CategoriaRequest): Observable<Categoria> {
    return this.http.post<Categoria>(this.urlCategorias, datos);
  }

  actualizarCategoria(id: number, datos: CategoriaRequest): Observable<Categoria> {
    return this.http.put<Categoria>(`${this.urlCategorias}/${id}`, datos);
  }

  productos(): Observable<Producto[]> {
    return this.http.get<Producto[]>(this.urlProductos);
  }

  crearProducto(datos: ProductoRequest): Observable<Producto> {
    return this.http.post<Producto>(this.urlProductos, datos);
  }

  actualizarProducto(id: number, datos: ProductoRequest): Observable<Producto> {
    return this.http.put<Producto>(`${this.urlProductos}/${id}`, datos);
  }
}
