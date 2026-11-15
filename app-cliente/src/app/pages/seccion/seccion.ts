import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-seccion',
  template: `
    <h1>{{ titulo }}</h1>
    <p class="descripcion">{{ descripcion }}</p>
    <p class="pendiente">Esta sección todavía no está lista.</p>
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
        max-width: 420px;
        margin: 32px 0 0;
        padding: 14px 16px;
        border-left: 4px solid var(--yuzu);
        border-radius: 0 10px 10px 0;
        background: #fff;
      }
    `,
  ],
})
export class Seccion {
  private readonly ruta = inject(ActivatedRoute);

  protected readonly titulo = this.ruta.snapshot.data['titulo'] as string;
  protected readonly descripcion = this.ruta.snapshot.data['descripcion'] as string;
}
