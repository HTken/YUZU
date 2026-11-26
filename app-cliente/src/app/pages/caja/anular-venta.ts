import { HttpErrorResponse } from '@angular/common/http';
import { Component, ElementRef, computed, inject, output, signal, viewChild } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { CajaApi, ResumenTurno, Venta, etiquetaMetodo } from '../../core/caja.api';
import { soles, ticketNumero } from '../../core/formato';

/** Lo que pasó al anular: el resumen de la caja ya actualizado y qué se hizo con los insumos. */
export interface ResultadoAnulacion {
  resumen: ResumenTurno;
  ticket: number;
  devolvioInsumos: boolean;
}

/** Motivos que se repiten; tocar uno lo escribe en el campo (se puede cambiar). */
const MOTIVOS_FRECUENTES = ['Cobro duplicado', 'Se cobró por error', 'Reclamo del cliente'];

const MOTIVO_MINIMO = 3;

/**
 * La ventana para anular una venta ya cobrada. Solo el administrador la usa (el servidor lo exige) y solo
 * para ventas de la caja abierta. Pide el motivo, que queda registrado junto con quién anuló, y deja elegir
 * si los insumos de esa venta vuelven al stock.
 */
@Component({
  selector: 'app-anular-venta',
  imports: [ReactiveFormsModule],
  templateUrl: './anular-venta.html',
  styleUrl: './anular-venta.css',
})
export class AnularVenta {
  private readonly api = inject(CajaApi);
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly dialogo = viewChild.required<ElementRef<HTMLDialogElement>>('dlg');

  /** Se anuló la venta: la pantalla de caja actualiza su resumen. */
  readonly anulada = output<ResultadoAnulacion>();
  /** El servidor dijo que lo que se ve ya no es así (otra persona la anuló, se cerró la caja…): conviene recargar. */
  readonly desactualizada = output<void>();

  protected readonly soles = soles;
  protected readonly etiquetaMetodo = etiquetaMetodo;
  protected readonly ticketNumero = ticketNumero;
  protected readonly motivosFrecuentes = MOTIVOS_FRECUENTES;

  protected readonly venta = signal<Venta | null>(null);
  protected readonly ocupado = signal(false);
  protected readonly error = signal('');

  protected readonly form = this.fb.group({
    motivo: ['', [Validators.required, Validators.maxLength(200)]],
    devolverInsumos: [false],
  });

  /** Lo que se pagó en efectivo: es lo que vuelve a salir del cajón. */
  protected readonly efectivo = computed(() => {
    const v = this.venta();
    return v ? v.pagos.filter((p) => p.metodo === 'EFECTIVO').reduce((s, p) => s + p.monto, 0) : 0;
  });

  /** Lo que se pagó por otros medios: el sistema no lo devuelve, se devuelve por fuera. */
  protected readonly otrosPagos = computed(() => {
    const v = this.venta();
    return v ? v.pagos.filter((p) => p.metodo !== 'EFECTIVO') : [];
  });

  /** Abre la ventana para esa venta, con el formulario vacío. */
  abrir(venta: Venta): void {
    this.venta.set(venta);
    this.form.reset({ motivo: '', devolverInsumos: false });
    this.error.set('');
    this.ocupado.set(false);
    this.dialogo().nativeElement.showModal();
  }

  protected cancelar(): void {
    this.dialogo().nativeElement.close();
  }

  protected usarMotivo(texto: string): void {
    this.form.controls.motivo.setValue(texto);
    this.form.controls.motivo.markAsTouched();
  }

  /** El motivo es muy corto (o está vacío); espacios solos no valen. */
  protected motivoInvalido(): boolean {
    return this.motivoLimpio().length < MOTIVO_MINIMO;
  }

  protected confirmar(): void {
    const venta = this.venta();
    if (!venta || this.ocupado()) {
      return;
    }
    this.form.controls.motivo.markAsTouched();
    if (this.form.invalid || this.motivoInvalido()) {
      return;
    }
    const devolver = this.form.getRawValue().devolverInsumos;

    this.error.set('');
    this.ocupado.set(true);
    this.api.anularVenta(venta.pedidoId, this.motivoLimpio(), devolver).subscribe({
      next: (resumen) => {
        this.ocupado.set(false);
        this.dialogo().nativeElement.close();
        this.anulada.emit({ resumen, ticket: venta.ticket, devolvioInsumos: devolver });
      },
      error: (e: HttpErrorResponse) => {
        this.ocupado.set(false);
        this.error.set(this.textoError(e));
        // Si el servidor dice que la venta ya no se puede anular, lo que hay en pantalla quedó viejo.
        if (e.status === 404 || e.status === 409) {
          this.desactualizada.emit();
        }
      },
    });
  }

  private motivoLimpio(): string {
    return this.form.controls.motivo.value.trim().replace(/\s+/g, ' ');
  }

  private textoError(e: HttpErrorResponse): string {
    if (e.error?.mensaje) {
      return e.error.mensaje;
    }
    return e.status === 403
      ? 'Solo el administrador puede anular ventas.'
      : 'No se pudo anular la venta. Revisa tu conexión e inténtalo de nuevo.';
  }
}
