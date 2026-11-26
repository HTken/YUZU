import { HttpErrorResponse } from '@angular/common/http';
import { Component, ElementRef, OnInit, computed, inject, signal, viewChild } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Observable, forkJoin } from 'rxjs';

import { AuthService } from '../../../core/auth.service';
import { Mesa, MesasApi, Salon } from '../../../core/mesas.api';
import { PlanService } from '../../../core/plan.service';

@Component({
  selector: 'app-mesas',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './mesas.html',
  styleUrl: './mesas.css',
})
export class Mesas implements OnInit {
  private readonly api = inject(MesasApi);
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly auth = inject(AuthService);
  private readonly dialogoSalon = viewChild.required<ElementRef<HTMLDialogElement>>('dlgSalon');
  private readonly dialogoMesa = viewChild.required<ElementRef<HTMLDialogElement>>('dlgMesa');
  private readonly dialogoLote = viewChild.required<ElementRef<HTMLDialogElement>>('dlgLote');

  protected readonly planes = inject(PlanService);

  /** Todos pueden ver los salones y mesas; solo el administrador los modifica. */
  protected readonly esAdmin = computed(() => this.auth.sesion()?.rol === 'ADMIN');

  protected readonly salones = signal<Salon[]>([]);
  protected readonly mesas = signal<Mesa[]>([]);
  protected readonly cargando = signal(true);
  protected readonly errorLista = signal('');

  /** null = se muestran las mesas de todos los salones. */
  protected readonly filtro = signal<number | null>(null);

  protected readonly salonesActivos = computed(() => this.salones().filter((s) => s.activo));

  protected readonly mesasFiltradas = computed(() => {
    const id = this.filtro();
    return id === null ? this.mesas() : this.mesas().filter((m) => m.salonId === id);
  });

  protected readonly salonFiltrado = computed(() => {
    const id = this.filtro();
    return id === null ? null : (this.salones().find((s) => s.id === id) ?? null);
  });

  /** Cuántas mesas más permite todavía el plan. */
  protected readonly cupoLibre = computed(() => {
    const p = this.planes.plan();
    return p ? Math.max(0, p.maxMesas - p.mesasActivas) : null;
  });

  /** null = creando; con valor = editando ese elemento. */
  protected readonly editandoSalon = signal<Salon | null>(null);
  protected readonly editandoMesa = signal<Mesa | null>(null);
  protected readonly guardando = signal(false);
  protected readonly errorForm = signal('');

  /**
   * Salones que se pueden elegir en el formulario de mesa: los activos y, si se está editando,
   * también el salón actual de la mesa aunque esté desactivado.
   */
  protected readonly opcionesSalon = computed(() => {
    const actual = this.editandoMesa()?.salonId;
    return this.salones().filter((s) => s.activo || s.id === actual);
  });

  private readonly capacidad = [
    Validators.required,
    Validators.min(1),
    Validators.max(50),
    Validators.pattern(/^\d+$/),
  ];

  protected readonly formSalon = this.fb.group({
    nombre: ['', [Validators.required, Validators.maxLength(60)]],
    orden: [1, [Validators.required, Validators.min(0), Validators.max(9999), Validators.pattern(/^\d+$/)]],
    activo: [true],
  });

  protected readonly formMesa = this.fb.group({
    salonId: this.fb.control<number | null>(null, Validators.required),
    nombre: ['', [Validators.required, Validators.maxLength(40)]],
    capacidad: [4, this.capacidad],
    activo: [true],
  });

  protected readonly formLote = this.fb.group({
    salonId: this.fb.control<number | null>(null, Validators.required),
    prefijo: ['Mesa', [Validators.required, Validators.maxLength(30)]],
    desde: [1, [Validators.required, Validators.min(1), Validators.max(9999), Validators.pattern(/^\d+$/)]],
    cantidad: [10, [Validators.required, Validators.min(1), Validators.max(50), Validators.pattern(/^\d+$/)]],
    capacidad: [4, this.capacidad],
  });

  ngOnInit(): void {
    this.planes.cargar();
    this.cargar();
  }

  protected etiquetaCapacidad(n: number): string {
    return `${n} ${n === 1 ? 'persona' : 'personas'}`;
  }

  protected filtrar(salonId: number | null): void {
    this.filtro.set(salonId);
  }

  /** Con silencioso = true no se vuelve a mostrar "Cargando…" (por ejemplo, después de guardar). */
  protected cargar(silencioso = false): void {
    if (!silencioso) {
      this.cargando.set(true);
    }
    this.errorLista.set('');

    forkJoin({ salones: this.api.salones(), mesas: this.api.mesas() }).subscribe({
      next: ({ salones, mesas }) => {
        this.salones.set(salones);
        this.mesas.set(mesas);
        this.cargando.set(false);
      },
      error: (e: HttpErrorResponse) => {
        this.cargando.set(false);
        this.errorLista.set(
          e.error?.mensaje ?? 'No se pudieron cargar las mesas. Revisa tu conexión e inténtalo de nuevo.',
        );
      },
    });
  }

  // --------------------------------------------------------------------- salones

  protected nuevoSalon(): void {
    const siguiente = Math.max(0, ...this.salones().map((s) => s.orden)) + 1;
    this.editandoSalon.set(null);
    this.errorForm.set('');
    this.formSalon.reset({ nombre: '', orden: siguiente, activo: true });
    this.dialogoSalon().nativeElement.showModal();
  }

  protected editarSalon(s: Salon): void {
    this.editandoSalon.set(s);
    this.errorForm.set('');
    this.formSalon.reset({ nombre: s.nombre, orden: s.orden, activo: s.activo });
    this.dialogoSalon().nativeElement.showModal();
  }

  protected guardarSalon(): void {
    if (this.guardando()) {
      return;
    }
    if (this.formSalon.invalid) {
      this.formSalon.markAllAsTouched();
      return;
    }

    const v = this.formSalon.getRawValue();
    const datos = { nombre: v.nombre.trim(), orden: Math.trunc(Number(v.orden)), activo: v.activo };
    const editando = this.editandoSalon();

    this.enviar(
      editando ? this.api.actualizarSalon(editando.id, datos) : this.api.crearSalon(datos),
      this.dialogoSalon().nativeElement,
    );
  }

  // ----------------------------------------------------------------------- mesas

  private salonSugerido(): Salon | undefined {
    const filtrado = this.salonFiltrado();
    return filtrado?.activo ? filtrado : this.salonesActivos()[0];
  }

  protected nuevaMesa(): void {
    this.editandoMesa.set(null);
    this.errorForm.set('');
    this.formMesa.reset({
      salonId: this.salonSugerido()?.id ?? null,
      nombre: '',
      capacidad: 4,
      activo: true,
    });
    this.dialogoMesa().nativeElement.showModal();
  }

  protected editarMesa(m: Mesa): void {
    this.editandoMesa.set(m);
    this.errorForm.set('');
    this.formMesa.reset({ salonId: m.salonId, nombre: m.nombre, capacidad: m.capacidad, activo: m.activo });
    this.dialogoMesa().nativeElement.showModal();
  }

  protected guardarMesa(): void {
    if (this.guardando()) {
      return;
    }
    if (this.formMesa.invalid) {
      this.formMesa.markAllAsTouched();
      return;
    }

    const v = this.formMesa.getRawValue();
    const datos = {
      salonId: v.salonId as number,
      nombre: v.nombre.trim(),
      capacidad: Math.trunc(Number(v.capacidad)),
      activo: v.activo,
    };
    const editando = this.editandoMesa();

    this.enviar(
      editando ? this.api.actualizarMesa(editando.id, datos) : this.api.crearMesa(datos),
      this.dialogoMesa().nativeElement,
    );
  }

  // ----------------------------------------------------------------------- lote

  protected nuevoLote(): void {
    this.errorForm.set('');
    this.formLote.reset({
      salonId: this.salonSugerido()?.id ?? null,
      prefijo: 'Mesa',
      desde: 1,
      cantidad: 10,
      capacidad: 4,
    });
    this.dialogoLote().nativeElement.showModal();
  }

  /** Ejemplo de lo que se va a crear, para que se vea antes de guardar. */
  protected vistaPreviaLote(): string {
    const v = this.formLote.getRawValue();
    const prefijo = v.prefijo.trim();
    const desde = Number(v.desde);
    const cantidad = Number(v.cantidad);

    if (!prefijo || !Number.isInteger(desde) || !Number.isInteger(cantidad) || desde < 1 || cantidad < 1) {
      return '';
    }
    const primera = `${prefijo} ${desde}`;
    if (cantidad === 1) {
      return `Se creará: ${primera}`;
    }
    return `Se crearán ${cantidad} mesas: ${primera} … ${prefijo} ${desde + cantidad - 1}`;
  }

  protected guardarLote(): void {
    if (this.guardando()) {
      return;
    }
    if (this.formLote.invalid) {
      this.formLote.markAllAsTouched();
      return;
    }

    const v = this.formLote.getRawValue();
    this.enviar(
      this.api.crearLote({
        salonId: v.salonId as number,
        prefijo: v.prefijo.trim(),
        desde: Math.trunc(Number(v.desde)),
        cantidad: Math.trunc(Number(v.cantidad)),
        capacidad: Math.trunc(Number(v.capacidad)),
      }),
      this.dialogoLote().nativeElement,
    );
  }

  protected cerrar(dialogo: HTMLDialogElement): void {
    dialogo.close();
  }

  // ---------------------------------------------------------------------- común

  private enviar(peticion: Observable<unknown>, dialogo: HTMLDialogElement): void {
    this.guardando.set(true);
    this.errorForm.set('');

    peticion.subscribe({
      next: () => {
        this.guardando.set(false);
        dialogo.close();
        this.cargar(true);
        // El contador "Mesas activas: X de Y" cambia con cada guardado.
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
