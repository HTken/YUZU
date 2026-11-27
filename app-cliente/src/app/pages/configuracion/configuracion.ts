import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-configuracion',
  imports: [RouterLink],
  template: `
    <h1>Configuración</h1>
    <p class="descripcion">Datos del restaurante, usuarios, productos y mesas.</p>

    <ul class="lista">
      <li>
        <a routerLink="/configuracion/usuarios">
          <span class="titulo">Usuarios</span>
          <span class="detalle">Quién entra al sistema y qué puede hacer.</span>
        </a>
      </li>
      <li>
        <a routerLink="/configuracion/plan">
          <span class="titulo">Mi plan</span>
          <span class="detalle">Tu plan, vencimiento y lo que incluye.</span>
        </a>
      </li>
      <li>
        <a routerLink="/configuracion/mesas">
          <span class="titulo">Mesas</span>
          <span class="detalle">Salones y mesas del local.</span>
        </a>
      </li>
      <li>
        <a routerLink="/configuracion/productos">
          <span class="titulo">Productos</span>
          <span class="detalle">Carta, categorías y precios.</span>
        </a>
      </li>
      @if (esAdmin()) {
        <li>
          <a routerLink="/configuracion/negocio">
            <span class="titulo">Datos del negocio</span>
            <span class="detalle">RUC, dirección y mensaje que salen en el ticket, y el ancho del papel.</span>
          </a>
        </li>
        <li>
          <a routerLink="/configuracion/seguridad">
            <span class="titulo">Seguridad</span>
            <span class="detalle">Verificación en dos pasos con una app autenticadora.</span>
          </a>
        </li>
      }
    </ul>
  `,
  styles: [
    `
      h1 {
        margin: 0 0 6px;
        font-size: 2rem;
        letter-spacing: -0.02em;
      }

      .descripcion {
        margin: 0 0 28px;
        color: var(--texto-suave);
      }

      .lista {
        max-width: 640px;
        margin: 0;
        padding: 0;
        list-style: none;
        border-radius: 12px;
        background: #fff;
      }

      li {
        border-bottom: 1px solid #e3e8e2;
      }

      li:last-child {
        border-bottom: 0;
      }

      a {
        display: grid;
        grid-template-columns: 1fr auto;
        gap: 2px 16px;
        padding: 16px 20px;
        color: inherit;
        text-decoration: none;
      }

      a:hover {
        background: #f1f5ee;
      }

      a:focus-visible {
        outline: 3px solid var(--yuzu);
        outline-offset: -3px;
      }

      .titulo {
        grid-column: 1;
        font-weight: 700;
      }

      .detalle {
        grid-column: 1;
        color: var(--texto-suave);
        font-size: 0.95rem;
      }
    `,
  ],
})
export class Configuracion {
  private readonly auth = inject(AuthService);

  /** Los datos del negocio los cambia solo el administrador (el servidor también lo exige). */
  protected readonly esAdmin = computed(() => this.auth.sesion()?.rol === 'ADMIN');
}
