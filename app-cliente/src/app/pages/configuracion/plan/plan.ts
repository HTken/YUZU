import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import {
  MiPlan,
  PlanService,
  etiquetaEstado,
  formatearFecha,
  textoVencimiento,
} from '../../../core/plan.service';

@Component({
  selector: 'app-plan',
  imports: [RouterLink],
  template: `
    <a routerLink="/configuracion" class="volver">Configuración</a>
    <h1>Mi plan</h1>

    @if (planes.plan(); as p) {
      <section class="resumen">
        <p class="nombre-plan">Plan {{ p.plan }}</p>
        <p class="precio">S/ {{ p.precioMensual.toFixed(2) }} al mes</p>
      </section>

      <dl class="datos">
        <div class="fila">
          <dt>Estado</dt>
          <dd>{{ etiquetaEstado(p.estado) }}</dd>
        </div>
        <div class="fila">
          <dt>Vencimiento</dt>
          <dd>{{ formatearFecha(p.venceEn) }} ({{ textoVencimiento(p.diasRestantes) }})</dd>
        </div>
        <div class="fila">
          <dt>Usuarios activos</dt>
          <dd>
            {{ p.usuariosActivos }} de {{ p.maxUsuarios }}
            <div
              class="barra"
              role="progressbar"
              aria-label="Usuarios activos del plan"
              aria-valuemin="0"
              [attr.aria-valuemax]="p.maxUsuarios"
              [attr.aria-valuenow]="p.usuariosActivos"
            >
              <div class="relleno" [style.width.%]="porcentaje(p)"></div>
            </div>
          </dd>
        </div>
      </dl>

      <div class="pago">
        <button type="button" disabled>Pagar o renovar</button>
        <p>El pago en línea estará disponible pronto.</p>
      </div>
    } @else if (planes.fallo()) {
      <p class="aviso-error" role="alert">No se pudo cargar tu plan. Revisa tu conexión e inténtalo de nuevo.</p>
      <button type="button" (click)="planes.cargar()">Reintentar</button>
    } @else {
      <p class="cargando">Cargando tu plan…</p>
    }
  `,
  styles: [
    `
      .volver {
        color: var(--hoja);
        font-size: 0.95rem;
        font-weight: 600;
      }

      .volver:focus-visible {
        outline: 3px solid var(--yuzu);
        outline-offset: 2px;
      }

      h1 {
        margin: 4px 0 24px;
        font-size: 2rem;
        letter-spacing: -0.02em;
      }

      .resumen {
        margin-bottom: 24px;
      }

      .nombre-plan {
        margin: 0;
        font-size: 1.5rem;
        font-weight: 800;
        letter-spacing: -0.02em;
      }

      .precio {
        margin: 4px 0 0;
        color: var(--texto-suave);
      }

      .datos {
        max-width: 560px;
        margin: 0 0 28px;
        border-radius: 12px;
        background: #fff;
      }

      .fila {
        display: grid;
        grid-template-columns: 160px 1fr;
        gap: 12px;
        padding: 16px 20px;
        border-bottom: 1px solid #e3e8e2;
      }

      .fila:last-child {
        border-bottom: 0;
      }

      dt {
        color: var(--texto-suave);
      }

      dd {
        margin: 0;
        font-weight: 600;
      }

      .barra {
        height: 10px;
        margin-top: 8px;
        overflow: hidden;
        border-radius: 999px;
        background: var(--hoja-suave);
      }

      .relleno {
        height: 100%;
        border-radius: 999px;
        background: var(--hoja);
      }

      .aviso-error {
        max-width: 480px;
        margin: 0 0 16px;
        padding: 12px 14px;
        border-radius: 10px;
        background: #fdecea;
        color: var(--error);
      }

      .pago p {
        margin: 8px 0 0;
        color: var(--texto-suave);
      }

      button {
        height: 44px;
        padding: 0 18px;
        border: 0;
        border-radius: 10px;
        background: var(--hoja);
        color: var(--yuzu);
        font-size: 1rem;
        font-weight: 700;
      }

      button:disabled {
        opacity: 0.5;
      }

      @media (max-width: 880px) {
        .fila {
          grid-template-columns: 1fr;
          gap: 2px;
        }
      }
    `,
  ],
})
export class Plan implements OnInit {
  protected readonly planes = inject(PlanService);

  protected readonly etiquetaEstado = etiquetaEstado;
  protected readonly formatearFecha = formatearFecha;
  protected readonly textoVencimiento = textoVencimiento;

  ngOnInit(): void {
    this.planes.cargar();
  }

  protected porcentaje(p: MiPlan): number {
    return p.maxUsuarios > 0 ? Math.min(100, Math.round((p.usuariosActivos / p.maxUsuarios) * 100)) : 0;
  }
}
