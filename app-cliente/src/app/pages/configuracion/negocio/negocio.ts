import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { AnchoTicket, Negocio, NegocioApi, NegocioRequest } from '../../../core/negocio.api';
import { imprimirTicket, ticketDePrueba } from '../../../core/ticket';

/** 11 números, con espacios o guiones sueltos; vacío también vale. El dígito verificador lo revisa el servidor. */
const PATRON_RUC = /^(\s*|\s*(\d[\s-]*){11})$/;
const PATRON_TELEFONO = /^[0-9+()\-./ ]*$/;

/**
 * Datos del negocio que salen en los tickets: razón social, RUC, dirección, teléfono, mensaje final y ancho
 * del papel. Solo el administrador entra aquí.
 */
@Component({
  selector: 'app-datos-negocio',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './negocio.html',
  styleUrl: './negocio.css',
})
export class DatosNegocio implements OnInit {
  private readonly api = inject(NegocioApi);
  private readonly fb = inject(NonNullableFormBuilder);

  protected readonly cargando = signal(true);
  protected readonly errorCarga = signal('');
  protected readonly guardando = signal(false);
  protected readonly errorForm = signal('');
  protected readonly aviso = signal('');

  /** El nombre del restaurante en el sistema; sale en el ticket y no se cambia aquí. */
  protected readonly nombre = signal('');

  protected readonly form = this.fb.group({
    razonSocial: ['', [Validators.maxLength(150)]],
    ruc: ['', [Validators.pattern(PATRON_RUC)]],
    direccion: ['', [Validators.maxLength(200)]],
    telefono: ['', [Validators.maxLength(40), Validators.pattern(PATRON_TELEFONO)]],
    mensajeTicket: ['', [Validators.maxLength(120)]],
    ancho: [80 as AnchoTicket],
  });

  ngOnInit(): void {
    this.cargar();
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.errorCarga.set('');
    this.api.obtener().subscribe({
      next: (negocio) => {
        this.ponerEnFormulario(negocio);
        this.cargando.set(false);
      },
      error: (e: HttpErrorResponse) => {
        this.cargando.set(false);
        this.errorCarga.set(
          e.error?.mensaje ??
            (e.status === 403
              ? 'Solo el administrador puede ver los datos del negocio.'
              : 'No se pudieron cargar los datos. Revisa tu conexión e inténtalo de nuevo.'),
        );
      },
    });
  }

  private ponerEnFormulario(negocio: Negocio): void {
    this.nombre.set(negocio.nombre);
    this.form.reset({
      razonSocial: negocio.razonSocial ?? '',
      ruc: negocio.ruc ?? '',
      direccion: negocio.direccion ?? '',
      telefono: negocio.telefono ?? '',
      mensajeTicket: negocio.mensajeTicket ?? '',
      ancho: negocio.anchoTicketMm,
    });
  }

  /** Lo que hay escrito ahora en el formulario, con los espacios de los bordes recortados. */
  private datos(): NegocioRequest {
    const v = this.form.getRawValue();
    return {
      razonSocial: v.razonSocial.trim(),
      ruc: v.ruc.replace(/[\s-]/g, ''),
      direccion: v.direccion.trim(),
      telefono: v.telefono.trim(),
      mensajeTicket: v.mensajeTicket.trim(),
      anchoTicketMm: v.ancho,
    };
  }

  protected guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.errorForm.set('');
    this.aviso.set('');
    this.guardando.set(true);

    this.api.guardar(this.datos()).subscribe({
      next: (negocio) => {
        this.guardando.set(false);
        this.ponerEnFormulario(negocio);
        this.aviso.set('Datos guardados. Los próximos tickets saldrán con estos datos.');
      },
      error: (e: HttpErrorResponse) => {
        this.guardando.set(false);
        this.errorForm.set(e.error?.mensaje ?? 'No se pudieron guardar los datos. Inténtalo de nuevo.');
      },
    });
  }

  /** Imprime un ticket de ejemplo con lo que está escrito ahora, aunque todavía no se haya guardado. */
  protected imprimirPrueba(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const d = this.datos();
    const negocio: Negocio = {
      nombre: this.nombre(),
      razonSocial: d.razonSocial || null,
      ruc: d.ruc || null,
      direccion: d.direccion || null,
      telefono: d.telefono || null,
      mensajeTicket: d.mensajeTicket || null,
      anchoTicketMm: d.anchoTicketMm,
    };
    imprimirTicket(ticketDePrueba(negocio), { prueba: true });
  }
}
