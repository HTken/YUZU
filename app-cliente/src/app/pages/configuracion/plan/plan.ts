import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { AuthService } from '../../../core/auth.service';
import { Culqi3dsService, CulqiService, Parametros3DS, TokenCulqi } from '../../../core/culqi.service';
import { PagosApi, SuscripcionCulqi } from '../../../core/pagos.api';
import {
  PlanService,
  etiquetaEstado,
  formatearFecha,
  textoVencimiento,
} from '../../../core/plan.service';

/** Lo que Culqi cobra (y devuelve de inmediato) para validar una tarjeta nueva. */
const CENTIMOS_VALIDACION_TARJETA = 300;

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

      @if (esAdmin()) {
        <section class="pago cobro-automatico">
          <h2>Cobro automático</h2>

          @if (suscripcionCulqi(); as s) {
            @if (s.activa) {
              <p class="nota">
                Tu plan se renueva solo cada mes con la tarjeta
                {{ s.marcaTarjeta ?? 'guardada' }} terminada en {{ s.ultimosCuatro ?? '····' }}.
              </p>
              <button
                type="button"
                class="pagar cancelar"
                [disabled]="ocupadoSuscripcion()"
                (click)="cancelarCobroAutomatico()"
              >
                {{ ocupadoSuscripcion() ? 'Procesando…' : 'Cancelar el cobro automático' }}
              </button>
            } @else {
              <p class="nota">
                Guarda una tarjeta y tu plan se va a renovar solo cada mes, sin que tengas que volver a
                pagar a mano. Culqi hace una validación de S/ 3.00 que se devuelve de inmediato.
              </p>
              <button
                type="button"
                class="pagar"
                [disabled]="ocupadoSuscripcion()"
                (click)="activarCobroAutomatico()"
              >
                {{ ocupadoSuscripcion() ? 'Procesando…' : 'Activar el cobro automático' }}
              </button>
            }
          }

          @if (mensajeOkSuscripcion()) {
            <p class="aviso-ok" role="status">{{ mensajeOkSuscripcion() }}</p>
          }
          @if (mensajeErrorSuscripcion()) {
            <p class="aviso-error" role="alert">{{ mensajeErrorSuscripcion() }}</p>
          }
        </section>
      }
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

      .cobro-automatico {
        margin-top: 20px;
      }

      .cancelar {
        background: #fff;
        color: var(--error);
        border: 1.5px solid var(--error);
      }

      .cancelar:hover:not(:disabled) {
        background: #fdecea;
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
  private readonly culqi3ds = inject(Culqi3dsService);

  protected readonly etiquetaEstado = etiquetaEstado;
  protected readonly formatearFecha = formatearFecha;
  protected readonly textoVencimiento = textoVencimiento;

  protected readonly opcionesMeses = [1, 3, 6, 12];
  protected readonly meses = signal(1);
  protected readonly ocupado = signal(false);
  protected readonly mensajeOk = signal('');
  protected readonly mensajeError = signal('');

  protected readonly suscripcionCulqi = signal<SuscripcionCulqi | null>(null);
  protected readonly ocupadoSuscripcion = signal(false);
  protected readonly mensajeOkSuscripcion = signal('');
  protected readonly mensajeErrorSuscripcion = signal('');

  protected readonly esAdmin = computed(() => this.auth.sesion()?.rol === 'ADMIN');

  protected readonly total = computed(() => {
    const plan = this.planes.plan();
    return plan ? plan.precioMensual * this.meses() : 0;
  });

  ngOnInit(): void {
    this.planes.cargar();
    if (this.esAdmin()) {
      this.cargarSuscripcion();
    }
  }

  private cargarSuscripcion(): void {
    this.pagos.suscripcion().subscribe({
      next: (s) => this.suscripcionCulqi.set(s),
      error: () => this.mensajeErrorSuscripcion.set('No se pudo cargar el estado del cobro automático.'),
    });
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
          onToken: (token) => void this.cobrar(token, meses, config.publicKey),
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

  private async cobrar(token: TokenCulqi, meses: number, publicKey: string): Promise<void> {
    this.ocupado.set(true);
    this.mensajeError.set('');

    const centimos = Math.round((this.planes.plan()?.precioMensual ?? 0) * meses * 100);
    const deviceFingerPrintId = await this.culqi3ds.generarDispositivo(publicKey);

    try {
      await this.intentarCobro(token, meses, publicKey, centimos, deviceFingerPrintId);
    } catch (e) {
      this.mensajeError.set(this.textoError(e));
    } finally {
      this.ocupado.set(false);
    }
  }

  private async intentarCobro(
    token: TokenCulqi,
    meses: number,
    publicKey: string,
    centimos: number,
    deviceFingerPrintId: string | null,
    authentication3DS?: Parametros3DS,
  ): Promise<void> {
    try {
      const resultado = await firstValueFrom(
        this.pagos.cobrar({ tokenId: token.id, meses, email: token.email, deviceFingerPrintId, authentication3DS }),
      );
      this.mensajeOk.set(
        `Pago recibido. Tu suscripción está activa hasta el ${formatearFecha(resultado.venceEn)}.`,
      );
      this.planes.cargar();
    } catch (e) {
      if (!(e instanceof HttpErrorResponse) || e.status !== 428) {
        throw e;
      }
      const parametros = await this.culqi3ds.autenticar({
        publicKey,
        tokenId: token.id,
        email: token.email,
        totalAmount: centimos,
      });
      await this.intentarCobro(token, meses, publicKey, centimos, deviceFingerPrintId, parametros);
    }
  }

  protected async activarCobroAutomatico(): Promise<void> {
    if (this.ocupadoSuscripcion()) {
      return;
    }

    this.mensajeOkSuscripcion.set('');
    this.mensajeErrorSuscripcion.set('');
    this.ocupadoSuscripcion.set(true);

    try {
      const config = await firstValueFrom(this.pagos.config());
      await this.culqi.abrir(
        {
          publicKey: config.publicKey,
          titulo: 'Validar tarjeta (S/ 3.00, se devuelve)',
          centimos: CENTIMOS_VALIDACION_TARJETA,
          soloTarjeta: true,
        },
        {
          onToken: (token) => void this.activarSuscripcion(token, config.publicKey),
          onError: (mensaje) => this.mensajeErrorSuscripcion.set(mensaje),
        },
      );
    } catch (e) {
      this.mensajeErrorSuscripcion.set(this.textoError(e));
    } finally {
      this.ocupadoSuscripcion.set(false);
    }
  }

  private async activarSuscripcion(token: TokenCulqi, publicKey: string): Promise<void> {
    this.ocupadoSuscripcion.set(true);
    this.mensajeErrorSuscripcion.set('');

    try {
      await this.intentarActivarSuscripcion(token, publicKey);
    } catch (e) {
      this.mensajeErrorSuscripcion.set(this.textoError(e));
    } finally {
      this.ocupadoSuscripcion.set(false);
    }
  }

  private async intentarActivarSuscripcion(
    token: TokenCulqi,
    publicKey: string,
    authentication3DS?: Parametros3DS,
  ): Promise<void> {
    try {
      const resultado = await firstValueFrom(
        this.pagos.activarSuscripcion({ tokenId: token.id, email: token.email, authentication3DS }),
      );
      this.suscripcionCulqi.set(resultado);
      this.mensajeOkSuscripcion.set('Cobro automático activado. Tu plan se va a renovar solo cada mes.');
    } catch (e) {
      if (!(e instanceof HttpErrorResponse) || e.status !== 428) {
        throw e;
      }
      const parametros = await this.culqi3ds.autenticar({
        publicKey,
        tokenId: token.id,
        email: token.email,
        totalAmount: CENTIMOS_VALIDACION_TARJETA,
      });
      await this.intentarActivarSuscripcion(token, publicKey, parametros);
    }
  }

  protected async cancelarCobroAutomatico(): Promise<void> {
    if (this.ocupadoSuscripcion() || !confirm('¿Cancelar el cobro automático? Tendrás que pagar a mano cada mes.')) {
      return;
    }

    this.mensajeOkSuscripcion.set('');
    this.mensajeErrorSuscripcion.set('');
    this.ocupadoSuscripcion.set(true);

    try {
      const resultado = await firstValueFrom(this.pagos.cancelarSuscripcion());
      this.suscripcionCulqi.set(resultado);
      this.mensajeOkSuscripcion.set('Cobro automático cancelado.');
    } catch (e) {
      this.mensajeErrorSuscripcion.set(this.textoError(e));
    } finally {
      this.ocupadoSuscripcion.set(false);
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
