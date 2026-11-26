import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, ElementRef, OnInit, computed, inject, signal, viewChild } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { AuthService } from '../../core/auth.service';
import { CajaApi, ResumenTurno, Turno, Venta, etiquetaMetodo } from '../../core/caja.api';
import { PATRON_MONTO, aNumero, centavos, fechaHora, hora, soles, ticketNumero } from '../../core/formato';
import { imprimirTicket } from '../../core/ticket';
import { AnularVenta, ResultadoAnulacion } from './anular-venta';

type TipoMovimiento = 'INGRESO' | 'EGRESO';

/**
 * Caja: abrir el turno, ver lo cobrado, registrar ingresos y egresos y cerrar contando el efectivo.
 * El administrador también puede anular una venta ya cobrada mientras su caja siga abierta.
 */
@Component({
  selector: 'app-caja',
  imports: [ReactiveFormsModule, AnularVenta],
  templateUrl: './caja.html',
  // Lo de las anulaciones y el ticket va en archivos aparte para no tocar los ajustes que ya hiciste en caja.css.
  styleUrls: ['./caja.css', './caja-anulaciones.css', './caja-ticket.css'],
})
export class Caja implements OnInit {
  private readonly api = inject(CajaApi);
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly destruccion = inject(DestroyRef);
  private readonly auth = inject(AuthService);
  private readonly anulacion = viewChild.required(AnularVenta);
  private readonly dialogoMovimiento = viewChild.required<ElementRef<HTMLDialogElement>>('dlgMovimiento');
  private readonly dialogoCierre = viewChild.required<ElementRef<HTMLDialogElement>>('dlgCierre');

  protected readonly soles = soles;
  protected readonly fechaHora = fechaHora;
  protected readonly hora = hora;
  protected readonly etiquetaMetodo = etiquetaMetodo;
  protected readonly ticketNumero = ticketNumero;

  /** Solo el administrador anula ventas (el servidor también lo exige; esto solo esconde el botón). */
  protected readonly esAdmin = computed(() => this.auth.sesion()?.rol === 'ADMIN');

  /** El turno abierto con todo lo que pasó en él; null si la caja está cerrada. */
  protected readonly resumen = signal<ResumenTurno | null>(null);
  protected readonly historial = signal<Turno[]>([]);
  /** El turno que se acaba de cerrar, para mostrar cómo quedó. */
  protected readonly ultimoCierre = signal<Turno | null>(null);

  protected readonly cargando = signal(true);
  protected readonly errorCarga = signal('');
  protected readonly ocupado = signal(false);
  protected readonly error = signal('');
  protected readonly aviso = signal('');
  /** La venta cuyo ticket se está preparando para reimprimir. */
  protected readonly reimprimiendo = signal<number | null>(null);

  // Abrir caja
  protected readonly formApertura = this.fb.group({
    montoInicial: ['0', [Validators.required, Validators.pattern(PATRON_MONTO)]],
  });

  // Ingreso o egreso
  protected readonly tipoMovimiento = signal<TipoMovimiento>('INGRESO');
  protected readonly errorMovimiento = signal('');
  protected readonly formMovimiento = this.fb.group({
    concepto: ['', [Validators.required, Validators.maxLength(120)]],
    monto: ['', [Validators.required, Validators.pattern(PATRON_MONTO)]],
  });

  // Cerrar caja
  protected readonly errorCierre = signal('');
  protected readonly contado = signal('');
  protected readonly formCierre = this.fb.group({
    contado: ['', [Validators.required, Validators.pattern(PATRON_MONTO)]],
    nota: ['', Validators.maxLength(200)],
  });

  /** Contado menos esperado, en soles; null mientras no se escriba un monto válido. */
  protected readonly diferencia = computed<number | null>(() => {
    const r = this.resumen();
    const n = aNumero(this.contado());
    if (!r || Number.isNaN(n)) {
      return null;
    }
    return (centavos(n) - centavos(r.efectivoEsperado)) / 100;
  });

  ngOnInit(): void {
    this.cargar();

    // Otros cajeros o el administrador también cobran: la pantalla se refresca sola cada 20 segundos.
    const reloj = setInterval(() => {
      if (!this.ocupado()) {
        this.cargar(true);
      }
    }, 20000);
    this.destruccion.onDestroy(() => clearInterval(reloj));
  }

  protected cargar(silencioso = false): void {
    if (!silencioso) {
      this.cargando.set(true);
      this.errorCarga.set('');
    }

    this.api.estado().subscribe({
      next: (estado) => {
        this.resumen.set(estado.resumen);
        this.cargando.set(false);
        if (!estado.abierta) {
          this.cargarHistorial();
        }
      },
      error: (e: HttpErrorResponse) => {
        if (!silencioso) {
          this.cargando.set(false);
          this.errorCarga.set(this.textoError(e, 'No se pudo cargar la caja. Revisa tu conexión e inténtalo de nuevo.'));
        }
      },
    });
  }

  private cargarHistorial(): void {
    this.api.historial().subscribe({
      next: (turnos) => this.historial.set(turnos),
      error: () => this.historial.set([]),
    });
  }

  private textoError(e: HttpErrorResponse, porDefecto: string): string {
    if (e.error?.mensaje) {
      return e.error.mensaje;
    }
    return e.status === 403 ? 'Tu rol no tiene acceso a la caja. Solo el administrador y el cajero la usan.' : porDefecto;
  }

  private limpiarMensajes(): void {
    this.error.set('');
    this.aviso.set('');
  }

  // ----------------------------------------------------------------- abrir caja

  protected abrir(): void {
    if (this.formApertura.invalid) {
      this.formApertura.markAllAsTouched();
      return;
    }
    this.limpiarMensajes();
    this.ultimoCierre.set(null);
    this.ocupado.set(true);

    this.api.abrir(aNumero(this.formApertura.getRawValue().montoInicial)).subscribe({
      next: (resumen) => {
        this.resumen.set(resumen);
        this.ocupado.set(false);
        this.aviso.set('La caja está abierta. Ya puedes cobrar las cuentas.');
      },
      error: (e: HttpErrorResponse) => {
        this.ocupado.set(false);
        this.error.set(this.textoError(e, 'No se pudo abrir la caja. Inténtalo de nuevo.'));
        // Si alguien más ya la abrió, se muestra su caja.
        if (e.status === 409) {
          this.cargar(true);
        }
      },
    });
  }

  // ------------------------------------------------------- ingresos y egresos

  protected abrirMovimiento(tipo: TipoMovimiento): void {
    this.tipoMovimiento.set(tipo);
    this.formMovimiento.reset({ concepto: '', monto: '' });
    this.errorMovimiento.set('');
    this.dialogoMovimiento().nativeElement.showModal();
  }

  protected cerrarMovimiento(): void {
    this.dialogoMovimiento().nativeElement.close();
  }

  protected guardarMovimiento(): void {
    if (this.formMovimiento.invalid) {
      this.formMovimiento.markAllAsTouched();
      return;
    }
    const { concepto, monto } = this.formMovimiento.getRawValue();
    const importe = aNumero(monto);
    if (!(importe > 0)) {
      this.errorMovimiento.set('El monto debe ser mayor a 0');
      return;
    }

    this.errorMovimiento.set('');
    this.ocupado.set(true);
    this.api.movimiento(this.tipoMovimiento(), concepto.trim(), importe).subscribe({
      next: (resumen) => {
        this.resumen.set(resumen);
        this.ocupado.set(false);
        this.dialogoMovimiento().nativeElement.close();
        this.limpiarMensajes();
        this.aviso.set(this.tipoMovimiento() === 'INGRESO' ? 'Ingreso registrado.' : 'Egreso registrado.');
      },
      error: (e: HttpErrorResponse) => {
        this.ocupado.set(false);
        this.errorMovimiento.set(this.textoError(e, 'No se pudo registrar el movimiento. Inténtalo de nuevo.'));
      },
    });
  }

  // ----------------------------------------------------------------- cerrar caja

  protected abrirCierre(): void {
    this.formCierre.reset({ contado: '', nota: '' });
    this.contado.set('');
    this.errorCierre.set('');
    this.dialogoCierre().nativeElement.showModal();
  }

  protected cancelarCierre(): void {
    this.dialogoCierre().nativeElement.close();
  }

  protected escribirContado(texto: string): void {
    this.contado.set(texto);
  }

  protected cerrarCaja(): void {
    if (this.formCierre.invalid) {
      this.formCierre.markAllAsTouched();
      return;
    }
    const { contado, nota } = this.formCierre.getRawValue();

    this.errorCierre.set('');
    this.ocupado.set(true);
    this.api.cerrar(aNumero(contado), nota.trim() || null).subscribe({
      next: (turno) => {
        this.ocupado.set(false);
        this.dialogoCierre().nativeElement.close();
        this.limpiarMensajes();
        this.resumen.set(null);
        this.ultimoCierre.set(turno);
        this.cargarHistorial();
      },
      error: (e: HttpErrorResponse) => {
        this.ocupado.set(false);
        this.errorCierre.set(this.textoError(e, 'No se pudo cerrar la caja. Inténtalo de nuevo.'));
      },
    });
  }

  // ------------------------------------------------------------ reimprimir el ticket

  /** Vuelve a imprimir el ticket de una venta cobrada. Lo pueden hacer el administrador y el cajero. */
  protected reimprimir(venta: Venta): void {
    if (this.reimprimiendo() !== null) {
      return;
    }
    this.limpiarMensajes();
    this.reimprimiendo.set(venta.pedidoId);

    this.api.ticket(venta.pedidoId).subscribe({
      next: (datos) => {
        this.reimprimiendo.set(null);
        imprimirTicket(datos, { copia: true });
      },
      error: (e: HttpErrorResponse) => {
        this.reimprimiendo.set(null);
        this.error.set(this.textoError(e, 'No se pudo preparar el ticket. Revisa tu conexión e inténtalo de nuevo.'));
        // Si otra persona anuló la venta mientras tanto, la tabla se actualiza.
        if (e.status === 404 || e.status === 409) {
          this.cargar(true);
        }
      },
    });
  }

  // ------------------------------------------------------------ anular una venta

  protected pedirAnulacion(venta: Venta): void {
    this.anulacion().abrir(venta);
  }

  protected alAnular(resultado: ResultadoAnulacion): void {
    this.resumen.set(resultado.resumen);
    this.limpiarMensajes();
    this.aviso.set(
      `Venta ${ticketNumero(resultado.ticket)} anulada. Ya no suma en las ventas ni en el efectivo del cajón.` +
        (resultado.devolvioInsumos ? ' Los insumos volvieron al stock.' : ''),
    );
  }

  // ------------------------------------------------------------------- textos

  protected pagosTexto(venta: Venta): string {
    return venta.pagos.map((p) => `${etiquetaMetodo(p.metodo)} ${soles(p.monto)}`).join(' + ');
  }

  protected textoDiferencia(diferencia: number | null): string {
    if (diferencia === null) {
      return '';
    }
    const centimos = centavos(diferencia);
    if (centimos === 0) {
      return 'La caja cuadra exacto.';
    }
    return centimos > 0 ? `Sobran ${soles(diferencia)}.` : `Faltan ${soles(Math.abs(diferencia))}.`;
  }

  /** Para pintar de rojo lo que falta y de verde lo que cuadra. */
  protected claseDiferencia(diferencia: number | null): string {
    if (diferencia === null) {
      return '';
    }
    const centimos = centavos(diferencia);
    return centimos === 0 ? 'cuadra' : centimos > 0 ? 'sobra' : 'falta';
  }
}
