import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, isDevMode, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';

import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule],
  templateUrl: './login.html',
  styleUrls: ['./login.css', './login-2fa.css'],
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

  /** true cuando el servidor ya validó usuario y clave pero falta el código de la app autenticadora. */
  protected readonly paso2fa = signal(false);

  protected readonly form = this.fb.group({
    subdominio: [this.subdominioFijo || (isDevMode() ? 'demo' : ''), Validators.required],
    usuario: ['', Validators.required],
    clave: ['', Validators.required],
    codigoTotp: [''],
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
      next: (r) => {
        if (r.requiere2fa) {
          this.cargando.set(false);
          this.paso2fa.set(true);
          return;
        }
        // El personal de cocina entra directo a su pantalla.
        const rol = this.auth.sesion()?.rol;
        void this.router.navigateByUrl(rol === 'COCINA' ? '/cocina' : '/');
      },
      error: (e: HttpErrorResponse) => {
        this.cargando.set(false);
        if (e.status === 403) {
          this.error.set(e.error?.mensaje ?? 'Demasiados intentos fallidos. Intenta de nuevo más tarde.');
        } else if (this.paso2fa()) {
          this.error.set('Código incorrecto o vencido. Vuelve a intentarlo.');
        } else {
          this.error.set(
            e.status === 401
              ? 'Usuario o contraseña incorrectos. Revísalos e inténtalo de nuevo.'
              : 'No se pudo conectar con el servidor. Revisa tu internet e inténtalo de nuevo.',
          );
        }
      },
    });
  }

  /** Vuelve a pedir usuario y clave, por si se equivocó de cuenta. */
  protected volverAEmpezar(): void {
    this.paso2fa.set(false);
    this.error.set('');
    this.form.patchValue({ clave: '', codigoTotp: '' });
  }
}
