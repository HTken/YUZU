import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, isDevMode, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';

import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  /** Restaurante tomado de la dirección (demo.localhost). Vacío si no hay. */
  protected readonly subdominioFijo = this.auth.subdominioDelHost();

  /** Diez gajos, uno cada 36 grados, para dibujar la rodaja de yuzu. */
  protected readonly gajos = Array.from({ length: 10 }, (_, i) => i * 36);

  protected readonly cargando = signal(false);
  protected readonly error = signal('');
  protected readonly verClave = signal(false);

  protected readonly form = this.fb.group({
    subdominio: [this.subdominioFijo || (isDevMode() ? 'demo' : ''), Validators.required],
    usuario: ['', Validators.required],
    clave: ['', Validators.required],
  });

  protected ingresar(): void {
    if (this.cargando()) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.cargando.set(true);
    this.error.set('');

    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => {
        void this.router.navigateByUrl('/');
      },
      error: (e: HttpErrorResponse) => {
        this.cargando.set(false);
        this.error.set(
          e.status === 401
            ? 'Usuario o contraseña incorrectos. Revísalos e inténtalo de nuevo.'
            : 'No se pudo conectar con el servidor. Revisa tu internet e inténtalo de nuevo.',
        );
      },
    });
  }
}
