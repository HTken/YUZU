import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { AuthService } from '../../../core/auth.service';
import { CulqiService, TokenCulqi } from '../../../core/culqi.service';
import { PagosApi } from '../../../core/pagos.api';
import {
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
              <div class="relleno" [style.width.%]="porcentaje(p.usuariosActivos, p.maxUsuarios)"></div>
            </div>
          </dd>
        </div>
        <div class="fila">
          <dt>Productos activos</dt>
          <dd>
            {{ p.productosActivos }} de {{ p.maxProductos }}
            <div
              class="barra"
              role="progressbar"
              aria-label="Productos activos del plan"
              aria-valuemin="0"
              [attr.aria-valuemax]="p.maxProductos"
              [attr.aria-valuenow]="p.productosActivos"
            >
              <div class="relleno" [style.width.%]="porcentaje(p.productosActivos, p.maxProductos)"></div>
            </div>
          </dd>
        </div>
        <div class="fila">
          <dt>Mesas activas</dt>
          <dd>
            {{ p.mesasActivas }} de {{ p.maxMesas }}
            <div
              class="barra"
              role="progressbar"
              aria-label="Mesas activas del plan"
              aria-valuemin="0"
              [attr.aria-valuemax]="p.maxMesas"
              [attr.aria-valuenow]="p.mesasActivas"
            >
              <div class="relleno" [style.width.%]="porcentaje(p.mesasActivas, p.maxMesas)"></div>
            </div>
          </dd>
        </div>
        <div class="fila">
          <dt>Insumos activos</dt>
          <dd>
            {{ p.insumosActivos }} de {{ p.maxInsumos }}
            <div
              class="barra"
              role="progressbar"
              aria-label="Insumos activos del plan"
              aria-valuemin="0"
              [attr.aria-valuemax]="p.maxInsumos"
              [attr.aria-valuenow]="p.insumosActivos"
            >
              <div class="relleno" [style.width.%]="porcentaje(p.insumosActivos, p.maxInsumos)"></div>
            </div>
          </dd>
        </div>
      </dl>

      <section class="pago">
        <h2>Pagar o renovar</h2>

        @if (esAdmin()) {
          <div class="campo">
            <label for="meses">Meses a pagar</label>
            <select id="meses" (change)="cambiarMeses($any($event.target).value)">
              @for (m of opcionesMeses; track m) {
                <option [value]="m" [selected]="m === meses()">{{ m }} {{ m === 1 ? 'mes' : 'meses' }}</option>
              }
            </select>
          </div>

          <p class="total">Total: <strong>S/ {{ total().toFixed(2) }}</strong></p>

          <button type="button" class="pagar" [disabled]="ocupado()" (click)="pagar()">
            {{ ocupado() ? 'Procesando…' : 'Pagar con tarjeta o Yape' }}
          </button>

          <p class="nota">
            El pago lo procesa Culqi y tus datos de tarjeta no pasan por Yuzu. Si la suscripción ya venció,
            los meses se cuentan desde hoy.
          </p>
        } @else {
          <p class="nota">Solo el administrador del restaurante puede pagar la suscripción.</p>
        }

        @if (mensajeOk()) {
          <p class="aviso-ok" role="status">{{ mensajeOk() }}</p>
        }
        @if (mensajeError()) {
          <p class="aviso-error" role="alert">{{ mensajeError() }}</p>
        }
      </section>
    } @else if (planes.fallo()) {
      <p class="aviso-error" role="alert">No se pudo cargar tu plan. Revisa tu conexión e inténtalo de nuevo.</p>
      <button type="button" class="pagar" (click)="planes.cargar()">Reintentar</button>
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

      h2 {
        margin: 0 0 16px;
        font-size: 1.25rem;
        letter-spacing: -0.01em;
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

      .pago {
        max-width: 560px;
        padding: 20px;
        border-radius: 12px;
        background: #fff;
      }

      .campo {
        margin-bottom: 12px;
      }

      label {
        display: block;
        margin-bottom: 6px;
        font-size: 0.95rem;
        font-weight: 600;
      }

      select {
        width: 100%;
        max-width: 240px;
        height: 46px;
        padding: 0 14px;
        border: 1.5px solid var(--borde);
        border-radius: 10px;
        background: #fff;
        color: var(--texto);
        font-size: 1rem;
      }

      select:focus-visible {
        outline: none;
        border-color: var(--hoja);
        box-shadow: 0 0 0 4px rgba(245, 196, 0, 0.45);
      }

      .total {
        margin: 12px 0 16px;
        font-size: 1.15rem;
      }

      .nota {
        margin: 12px 0 0;
        color: var(--texto-suave);
        font-size: 0.95rem;
      }

      .pagar {
        height: 46px;
        padding: 0 20px;
        border: 0;
        border-radius: 10px;
        background: var(--hoja);
        color: var(--yuzu);
        font-size: 1rem;
        font-weight: 700;
        cursor: pointer;
      }

      .pagar:hover:not(:disabled) {
        background: #0e3322;
      }

      .pagar:focus-visible {
        outline: 3px solid var(--yuzu);
        outline-offset: 3px;
      }

      .pagar:disabled {
        opacity: 0.7;
        cursor: progress;
      }

      .aviso-ok,
      .aviso-error {
        margin: 16px 0 0;
        padding: 12px 14px;
        border-radius: 10px;
      }

      .aviso-ok {
        background: var(--hoja-suave);
        color: var(--hoja);
        font-weight: 600;
      }

      .aviso-error {
        background: #fdecea;
        color: var(--error);
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
  private readonly auth = inject(AuthService);
  private readonly pagos = inject(PagosApi);
  private readonly culqi = inject(CulqiService);

  protected readonly etiquetaEstado = etiquetaEstado;
  protected readonly formatearFecha = formatearFecha;
  protected readonly textoVencimiento = textoVencimiento;

  protected readonly opcionesMeses = [1, 3, 6, 12];
  protected readonly meses = signal(1);
  protected readonly ocupado = signal(false);
  protected readonly mensajeOk = signal('');
  protected readonly mensajeError = signal('');

  protected readonly esAdmin = computed(() => this.auth.sesion()?.rol === 'ADMIN');

  protected readonly total = computed(() => {
    const plan = this.planes.plan();
    return plan ? plan.precioMensual * this.meses() : 0;
  });

  ngOnInit(): void {
    this.planes.cargar();
  }

  protected porcentaje(usados: number, maximo: number): number {
    return maximo > 0 ? Math.min(100, Math.round((usados / maximo) * 100)) : 0;
  }

  protected cambiarMeses(valor: string): void {
    this.meses.set(Number(valor));
  }

  protected async pagar(): Promise<void> {
    const plan = this.planes.plan();
    if (!plan || this.ocupado()) {
      return;
    }

    this.mensajeOk.set('');
    this.mensajeError.set('');
    this.ocupado.set(true);

    const meses = this.meses();

    try {
      const config = await firstValueFrom(this.pagos.config());
      await this.culqi.abrir(
        {
          publicKey: config.publicKey,
          titulo: `Plan ${plan.plan}`,
          centimos: Math.round(plan.precioMensual * meses * 100),
        },
        {
          onToken: (token) => void this.cobrar(token, meses),
          onError: (mensaje) => this.mensajeError.set(mensaje),
        },
      );
    } catch (e) {
      this.mensajeError.set(this.textoError(e));
    } finally {
      // El formulario de Culqi ya está abierto: el botón vuelve a quedar libre.
      this.ocupado.set(false);
    }
  }

  private async cobrar(token: TokenCulqi, meses: number): Promise<void> {
    this.ocupado.set(true);
    this.mensajeError.set('');

    try {
      const resultado = await firstValueFrom(
        this.pagos.cobrar({ tokenId: token.id, meses, email: token.email }),
      );
      this.mensajeOk.set(
        `Pago recibido. Tu suscripción está activa hasta el ${formatearFecha(resultado.venceEn)}.`,
      );
      this.planes.cargar();
    } catch (e) {
      this.mensajeError.set(this.textoError(e));
    } finally {
      this.ocupado.set(false);
    }
  }

  private textoError(e: unknown): string {
    if (e instanceof HttpErrorResponse) {
      if (e.status === 0) {
        return 'No se pudo conectar con el servidor. Revisa tu conexión.';
      }
      return e.error?.mensaje ?? 'No se pudo completar el pago.';
    }
    return e instanceof Error ? e.message : 'No se pudo completar el pago.';
  }
}
