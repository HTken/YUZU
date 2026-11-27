import { HttpErrorResponse } from '@angular/common/http';
import { Component, ElementRef, OnInit, computed, inject, signal, viewChild } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Observable } from 'rxjs';

import { etiquetaEstado, formatearFecha, textoVencimiento } from '../../core/formato';
import { PlanInfo, PlataformaApi, ResultadoVencimientos, RestauranteResumen } from '../../core/plataforma.api';

type Modo = 'nuevo' | 'renovar' | 'plan' | 'suspender' | 'reactivar';

@Component({
  selector: 'app-restaurantes',
  imports: [ReactiveFormsModule],
  templateUrl: './restaurantes.html',
  styleUrl: './restaurantes.css',
})
export class Restaurantes implements OnInit {
  private readonly api = inject(PlataformaApi);
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly dialogo = viewChild.required<ElementRef<HTMLDialogElement>>('dialogo');

  protected readonly etiquetaEstado = etiquetaEstado;
  protected readonly formatearFecha = formatearFecha;
  protected readonly textoVencimiento = textoVencimiento;

  protected readonly lista = signal<RestauranteResumen[]>([]);
  protected readonly planes = signal<PlanInfo[]>([]);
  protected readonly cargando = signal(true);
  protected readonly errorLista = signal('');

  protected readonly busqueda = signal('');
  protected readonly filtroEstado = signal('');

  protected readonly planesActivos = computed(() => this.planes().filter((p) => p.activo));

  protected readonly filtrados = computed(() => {
    const texto = this.busqueda().trim().toLowerCase();
    const estado = this.filtroEstado();
    return this.lista().filter(
      (r) =>
        (!estado || r.estado === estado) &&
        (!texto ||
          r.nombre.toLowerCase().includes(texto) ||
          r.subdominio.toLowerCase().includes(texto)),
    );
  });

  protected readonly resumen = computed(() => {
    const todos = this.lista();
    return {
      total: todos.length,
      activos: todos.filter((r) => r.estado === 'ACTIVO').length,
      enGracia: todos.filter((r) => r.estado === 'EN_GRACIA').length,
      suspendidos: todos.filter((r) => r.estado === 'SUSPENDIDO').length,
      porVencer: todos.filter(
        (r) => r.estado !== 'SUSPENDIDO' && r.diasRestantes >= 0 && r.diasRestantes <= 7,
      ).length,
    };
  });

  protected readonly modo = signal<Modo | null>(null);
  protected readonly seleccionado = signal<RestauranteResumen | null>(null);
  protected readonly guardando = signal(false);
  protected readonly errorForm = signal('');

  protected readonly formNuevo = this.fb.group({
    nombre: ['', [Validators.required, Validators.maxLength(150)]],
    subdominio: [
      '',
      [Validators.required, Validators.maxLength(63), Validators.pattern(/^[a-z0-9]([a-z0-9-]*[a-z0-9])?$/i)],
    ],
    planId: [0, [Validators.required, Validators.min(1)]],
    diasDePrueba: [14, [Validators.required, Validators.min(0), Validators.max(365)]],
    adminUsuario: [
      '',
      [Validators.required, Validators.maxLength(60), Validators.pattern(/^[A-Za-z0-9._-]+$/)],
    ],
    adminNombre: ['', [Validators.required, Validators.maxLength(150)]],
    adminClave: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(64)]],
  });

  protected readonly formRenovar = this.fb.group({
    meses: [1, [Validators.required, Validators.min(1), Validators.max(12)]],
    metodo: ['EFECTIVO', Validators.required],
    referencia: ['', Validators.maxLength(100)],
  });

  protected readonly formPlan = this.fb.group({
    planId: [0, [Validators.required, Validators.min(1)]],
  });

  protected readonly opcionesMeses = [1, 2, 3, 6, 12];

  protected readonly opcionesMetodo = [
    { valor: 'EFECTIVO', etiqueta: 'Efectivo' },
    { valor: 'TRANSFERENCIA', etiqueta: 'Transferencia' },
    { valor: 'YAPE', etiqueta: 'Yape' },
    { valor: 'PLIN', etiqueta: 'Plin' },
    { valor: 'OTRO', etiqueta: 'Otro' },
  ];

  protected readonly revisando = signal(false);
  protected readonly resultadoRevision = signal('');

  ngOnInit(): void {
    this.api.planes().subscribe({ next: (p) => this.planes.set(p) });
    this.cargar();
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.errorLista.set('');

    this.api.restaurantes().subscribe({
      next: (datos) => {
        this.lista.set(datos);
        this.cargando.set(false);
      },
      error: (e: HttpErrorResponse) => {
        this.cargando.set(false);
        this.errorLista.set(
          e.error?.mensaje ?? 'No se pudieron cargar los restaurantes. Inténtalo de nuevo.',
        );
      },
    });
  }

  protected revisar(): void {
    if (this.revisando()) {
      return;
    }
    this.revisando.set(true);
    this.resultadoRevision.set('');

    this.api.revisarVencimientos().subscribe({
      next: (r) => {
        this.revisando.set(false);
        this.resultadoRevision.set(this.mensajeRevision(r));
        this.cargar();
      },
      error: (e: HttpErrorResponse) => {
        this.revisando.set(false);
        this.resultadoRevision.set(e.error?.mensaje ?? 'No se pudo revisar los vencimientos.');
      },
    });
  }

  private mensajeRevision(r: ResultadoVencimientos): string {
    if (r.aGracia === 0 && r.suspendidos === 0 && r.avisados === 0) {
      return 'Todo al día: ningún restaurante cambió de estado ni necesitaba un aviso.';
    }

    const partes: string[] = [];
    if (r.aGracia > 0) {
      partes.push(`${r.aGracia} pasaron a gracia`);
    }
    if (r.suspendidos > 0) {
      partes.push(`${r.suspendidos} fueron suspendidos`);
    }
    if (r.avisados > 0) {
      partes.push(`se avisó por correo a ${r.avisados} que su plan vence pronto`);
    }
    return partes.join(', ') + '.';
  }

  protected claseVencimiento(r: RestauranteResumen): string {
    if (r.estado === 'SUSPENDIDO' || r.diasRestantes < 0) {
      return 'critico';
    }
    return r.diasRestantes <= 7 ? 'aviso' : '';
  }

  protected abrir(modo: Modo, restaurante: RestauranteResumen | null = null): void {
    this.modo.set(modo);
    this.seleccionado.set(restaurante);
    this.errorForm.set('');

    if (modo === 'nuevo') {
      this.formNuevo.reset({
        nombre: '',
        subdominio: '',
        planId: this.planesActivos()[0]?.id ?? 0,
        diasDePrueba: 14,
        adminUsuario: '',
        adminNombre: '',
        adminClave: '',
      });
    } else if (modo === 'renovar') {
      this.formRenovar.reset({ meses: 1, metodo: 'EFECTIVO', referencia: '' });
    } else if (modo === 'plan' && restaurante) {
      this.formPlan.reset({ planId: restaurante.planId });
    }

    this.dialogo().nativeElement.showModal();
  }

  protected cerrar(): void {
    this.dialogo().nativeElement.close();
  }

  protected confirmar(): void {
    if (this.guardando()) {
      return;
    }

    const modo = this.modo();
    const r = this.seleccionado();
    let peticion: Observable<unknown> | null = null;

    switch (modo) {
      case 'nuevo': {
        if (this.formNuevo.invalid) {
          this.formNuevo.markAllAsTouched();
          return;
        }
        const v = this.formNuevo.getRawValue();
        peticion = this.api.crear({
          nombre: v.nombre.trim(),
          subdominio: v.subdominio.trim().toLowerCase(),
          planId: v.planId,
          diasDePrueba: v.diasDePrueba,
          adminUsuario: v.adminUsuario.trim(),
          adminNombre: v.adminNombre.trim(),
          adminClave: v.adminClave,
        });
        break;
      }
      case 'renovar':
        if (r) {
          if (this.formRenovar.invalid) {
            this.formRenovar.markAllAsTouched();
            return;
          }
          const v = this.formRenovar.getRawValue();
          peticion = this.api.renovar(r.id, v.meses, v.metodo, v.referencia.trim() || null);
        }
        break;
      case 'plan':
        if (r) {
          if (this.formPlan.invalid) {
            this.formPlan.markAllAsTouched();
            return;
          }
          peticion = this.api.cambiarPlan(r.id, this.formPlan.getRawValue().planId);
        }
        break;
      case 'suspender':
        if (r) {
          peticion = this.api.suspender(r.id);
        }
        break;
      case 'reactivar':
        if (r) {
          peticion = this.api.reactivar(r.id);
        }
        break;
    }

    if (!peticion) {
      return;
    }

    this.guardando.set(true);
    this.errorForm.set('');

    peticion.subscribe({
      next: () => {
        this.guardando.set(false);
        this.cerrar();
        this.cargar();
      },
      error: (e: HttpErrorResponse) => {
        this.guardando.set(false);
        this.errorForm.set(
          e.error?.mensaje ?? 'No se pudo completar la acción. Revisa tu conexión e inténtalo de nuevo.',
        );
      },
    });
  }
}
