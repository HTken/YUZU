import { Component, inject } from '@angular/core';

import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-inicio',
  template: `
    <h1>Hola, {{ auth.sesion()?.nombre }}</h1>
    <p class="descripcion">{{ auth.sesion()?.restaurante }}</p>
    <p class="pendiente">Aquí irá el resumen del día: ventas, mesas abiertas y pedidos pendientes.</p>
  `,
  styles: [
    `
      h1 {
        margin: 0 0 6px;
        font-size: 2rem;
        letter-spacing: -0.02em;
      }

      .descripcion {
        margin: 0;
        color: var(--texto-suave);
      }

      .pendiente {
        max-width: 520px;
        margin: 32px 0 0;
        padding: 14px 16px;
        border-left: 4px solid var(--yuzu);
        border-radius: 0 10px 10px 0;
        background: #fff;
      }
    `,
  ],
})
export class Inicio {
  protected readonly auth = inject(AuthService);
}
