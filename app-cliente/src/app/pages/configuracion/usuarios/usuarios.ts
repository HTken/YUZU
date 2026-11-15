import { HttpErrorResponse } from '@angular/common/http';
import { Component, ElementRef, OnInit, inject, signal, viewChild } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../../core/auth.service';
import { PlanService } from '../../../core/plan.service';
import { ROLES, Rol, UsuarioDto, UsuariosApi } from './usuarios.api';

@Component({
  selector: 'app-usuarios',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './usuarios.html',
  styleUrl: './usuarios.css',
})
export class Usuarios implements OnInit {
  private readonly api = inject(UsuariosApi);
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly dialogo = viewChild.required<ElementRef<HTMLDialogElement>>('dialogo');

  protected readonly auth = inject(AuthService);
  private readonly planes = inject(PlanService);
  protected readonly roles = ROLES;

  protected readonly lista = signal<UsuarioDto[]>([]);
  protected readonly cargando = signal(true);
  protected readonly errorLista = signal('');

  /** null = creando un usuario nuevo; con valor = editando ese usuario. */
  protected readonly editando = signal<UsuarioDto | null>(null);
  protected readonly guardando = signal(false);
  protected readonly errorForm = signal('');

  protected readonly form = this.fb.group({
    nombre: ['', [Validators.required, Validators.maxLength(150)]],
    usuario: [
      '',
      [Validators.required, Validators.maxLength(60), Validators.pattern(/^[A-Za-z0-9._-]+$/)],
    ],
    email: ['', [Validators.email, Validators.maxLength(150)]],
    rol: ['MOZO' as Rol, Validators.required],
    clave: ['', [Validators.minLength(8), Validators.maxLength(64)]],
    activo: [true],
  });

  ngOnInit(): void {
    this.cargar();
  }

  protected etiquetaRol(rol: Rol): string {
    return ROLES.find((r) => r.valor === rol)?.etiqueta ?? rol;
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.errorLista.set('');

    this.api.listar().subscribe({
      next: (datos) => {
        this.lista.set(datos);
        this.cargando.set(false);
      },
      error: (e: HttpErrorResponse) => {
        this.cargando.set(false);
        this.errorLista.set(
          e.status === 403
            ? (e.error?.mensaje ?? 'No tienes permiso para administrar usuarios.')
            : 'No se pudieron cargar los usuarios. Inténtalo de nuevo.',
        );
      },
    });
  }

  protected nuevo(): void {
    this.editando.set(null);
    this.errorForm.set('');
    this.form.reset({ nombre: '', usuario: '', email: '', rol: 'MOZO', clave: '', activo: true });
    this.form.controls.usuario.enable();
    this.form.controls.clave.setValidators([
      Validators.required,
      Validators.minLength(8),
      Validators.maxLength(64),
    ]);
    this.form.controls.clave.updateValueAndValidity();
    this.dialogo().nativeElement.showModal();
  }

  protected editar(u: UsuarioDto): void {
    this.editando.set(u);
    this.errorForm.set('');
    this.form.reset({
      nombre: u.nombre,
      usuario: u.usuario,
      email: u.email ?? '',
      rol: u.rol,
      clave: '',
      activo: u.activo,
    });
    this.form.controls.usuario.disable();
    this.form.controls.clave.setValidators([Validators.minLength(8), Validators.maxLength(64)]);
    this.form.controls.clave.updateValueAndValidity();
    this.dialogo().nativeElement.showModal();
  }

  protected cerrar(): void {
    this.dialogo().nativeElement.close();
  }

  protected guardar(): void {
    if (this.guardando()) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const v = this.form.getRawValue();
    const email = v.email.trim() || null;
    const editando = this.editando();

    const peticion = editando
      ? this.api.actualizar(editando.id, {
          nombre: v.nombre.trim(),
          email,
          rol: v.rol,
          activo: v.activo,
          clave: v.clave || undefined,
        })
      : this.api.crear({
          usuario: v.usuario.trim(),
          nombre: v.nombre.trim(),
          email,
          rol: v.rol,
          clave: v.clave,
        });

    this.guardando.set(true);
    this.errorForm.set('');

    peticion.subscribe({
      next: () => {
        this.guardando.set(false);
        this.cerrar();
        this.cargar();
        this.planes.cargar();
      },
      error: (e: HttpErrorResponse) => {
        this.guardando.set(false);
        this.errorForm.set(
          e.error?.mensaje ?? 'No se pudo guardar. Revisa tu conexión e inténtalo de nuevo.',
        );
      },
    });
  }
}
