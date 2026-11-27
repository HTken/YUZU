import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { DosFactoresApi, TotpSetup } from '../../core/dos-factores.api';

/** Verificación en dos pasos (2FA) con una app autenticadora, para tu propia cuenta de SUPERADMIN. */
@Component({
  selector: 'app-cuenta',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './cuenta.html',
  styleUrl: './cuenta.css',
})
export class Cuenta implements OnInit {
  private readonly api = inject(DosFactoresApi);
  private readonly fb = inject(NonNullableFormBuilder);

  protected readonly cargando = signal(true);
  protected readonly errorCarga = signal('');
  protected readonly activo = signal(false);

  protected readonly dialogoAbierto = signal(false);
  protected readonly generando = signal(false);
  protected readonly confirmando = signal(false);
  protected readonly errorConfirmar = signal('');
  protected readonly setup = signal<TotpSetup | null>(null);
  protected readonly desactivando = signal(false);

  protected readonly secretoFormateado = computed(() => {
    const secreto = this.setup()?.secreto ?? '';
    return secreto.match(/.{1,4}/g)?.join(' ') ?? secreto;
  });

  protected readonly formCodigo = this.fb.group({
    codigo: ['', [Validators.required]],
  });

  ngOnInit(): void {
    this.cargando.set(true);
    this.api.estado().subscribe({
      next: (r) => {
        this.activo.set(r.activo);
        this.cargando.set(false);
      },
      error: (e: HttpErrorResponse) => {
        this.cargando.set(false);
        this.errorCarga.set(e.error?.mensaje ?? 'No se pudo cargar el estado del 2FA.');
      },
    });
  }

  protected activarDialogo(): void {
    this.generando.set(true);
    this.errorConfirmar.set('');
    this.formCodigo.reset({ codigo: '' });

    this.api.iniciar().subscribe({
      next: (setup) => {
        this.generando.set(false);
        this.setup.set(setup);
        this.dialogoAbierto.set(true);
      },
      error: () => {
        this.generando.set(false);
        this.errorCarga.set('No se pudo generar el código secreto. Inténtalo de nuevo.');
      },
    });
  }

  protected confirmar(): void {
    const setup = this.setup();
    if (!setup || this.formCodigo.invalid || this.confirmando()) {
      return;
    }

    this.confirmando.set(true);
    this.errorConfirmar.set('');

    this.api.activar(setup.secreto, this.formCodigo.getRawValue().codigo.trim()).subscribe({
      next: () => {
        this.confirmando.set(false);
        this.activo.set(true);
        this.cerrarDialogo();
      },
      error: (e: HttpErrorResponse) => {
        this.confirmando.set(false);
        this.errorConfirmar.set(e.error?.mensaje ?? 'El código no coincide. Inténtalo de nuevo.');
      },
    });
  }

  protected cerrarDialogo(): void {
    this.dialogoAbierto.set(false);
    this.setup.set(null);
  }

  protected desactivar(): void {
    if (this.desactivando()) {
      return;
    }
    this.desactivando.set(true);
    this.api.desactivar().subscribe({
      next: () => {
        this.desactivando.set(false);
        this.activo.set(false);
      },
      error: () => {
        this.desactivando.set(false);
        this.errorCarga.set('No se pudo desactivar. Inténtalo de nuevo.');
      },
    });
  }
}
