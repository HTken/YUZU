import { HttpErrorResponse } from '@angular/common/http';
import { Component, ElementRef, OnInit, computed, inject, signal, viewChild } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Observable, forkJoin } from 'rxjs';

import { AuthService } from '../../../core/auth.service';
import { CatalogoApi, Categoria, Producto } from '../../../core/catalogo.api';
import { PlanService } from '../../../core/plan.service';

@Component({
  selector: 'app-productos',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './productos.html',
  styleUrl: './productos.css',
})
export class Productos implements OnInit {
  private readonly api = inject(CatalogoApi);
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly auth = inject(AuthService);
  private readonly dialogoCategoria = viewChild.required<ElementRef<HTMLDialogElement>>('dlgCategoria');
  private readonly dialogoProducto = viewChild.required<ElementRef<HTMLDialogElement>>('dlgProducto');

  protected readonly planes = inject(PlanService);

  /** Todos pueden ver la carta; solo el administrador la modifica. */
  protected readonly esAdmin = computed(() => this.auth.sesion()?.rol === 'ADMIN');

  protected readonly categorias = signal<Categoria[]>([]);
  protected readonly productos = signal<Producto[]>([]);
  protected readonly cargando = signal(true);
  protected readonly errorLista = signal('');

  /** null = se muestran los productos de todas las categorías. */
  protected readonly filtro = signal<number | null>(null);

  protected readonly categoriasActivas = computed(() => this.categorias().filter((c) => c.activo));

  protected readonly productosFiltrados = computed(() => {
    const id = this.filtro();
    return id === null ? this.productos() : this.productos().filter((p) => p.categoriaId === id);
  });

  protected readonly categoriaFiltrada = computed(() => {
    const id = this.filtro();
    return id === null ? null : (this.categorias().find((c) => c.id === id) ?? null);
  });

  /** null = creando; con valor = editando ese elemento. */
  protected readonly editandoCategoria = signal<Categoria | null>(null);
  protected readonly editandoProducto = signal<Producto | null>(null);
  protected readonly guardando = signal(false);
  protected readonly errorForm = signal('');

  /**
   * Categorías que se pueden elegir en el formulario de producto: las activas y, si se está
   * editando, también la categoría actual del producto aunque esté desactivada.
   */
  protected readonly opcionesCategoria = computed(() => {
    const actual = this.editandoProducto()?.categoriaId;
    return this.categorias().filter((c) => c.activo || c.id === actual);
  });

  protected readonly formCategoria = this.fb.group({
    nombre: ['', [Validators.required, Validators.maxLength(80)]],
    orden: [1, [Validators.required, Validators.min(0), Validators.max(9999), Validators.pattern(/^\d+$/)]],
    activo: [true],
    pasaPorCocina: [true],
  });

  protected readonly formProducto = this.fb.group({
    categoriaId: this.fb.control<number | null>(null, Validators.required),
    nombre: ['', [Validators.required, Validators.maxLength(120)]],
    descripcion: ['', Validators.maxLength(300)],
    precio: this.fb.control<number | null>(null, [
      Validators.required,
      Validators.min(0.01),
      Validators.pattern(/^\d{1,8}(\.\d{1,2})?$/),
    ]),
    activo: [true],
  });

  ngOnInit(): void {
    this.planes.cargar();
    this.cargar();
  }

  protected soles(monto: number): string {
    return `S/ ${monto.toFixed(2)}`;
  }

  protected filtrar(categoriaId: number | null): void {
    this.filtro.set(categoriaId);
  }

  /** Con silencioso = true no se vuelve a mostrar "Cargando…" (por ejemplo, después de guardar). */
  protected cargar(silencioso = false): void {
    if (!silencioso) {
      this.cargando.set(true);
    }
    this.errorLista.set('');

    forkJoin({ categorias: this.api.categorias(), productos: this.api.productos() }).subscribe({
      next: ({ categorias, productos }) => {
        this.categorias.set(categorias);
        this.productos.set(productos);
        this.cargando.set(false);
      },
      error: (e: HttpErrorResponse) => {
        this.cargando.set(false);
        this.errorLista.set(
          e.error?.mensaje ?? 'No se pudo cargar la carta. Revisa tu conexión e inténtalo de nuevo.',
        );
      },
    });
  }

  // ------------------------------------------------------------------ categorías

  protected nuevaCategoria(): void {
    const siguiente = Math.max(0, ...this.categorias().map((c) => c.orden)) + 1;
    this.editandoCategoria.set(null);
    this.errorForm.set('');
    this.formCategoria.reset({ nombre: '', orden: siguiente, activo: true, pasaPorCocina: true });
    this.dialogoCategoria().nativeElement.showModal();
  }

  protected editarCategoria(c: Categoria): void {
    this.editandoCategoria.set(c);
    this.errorForm.set('');
    this.formCategoria.reset({
      nombre: c.nombre,
      orden: c.orden,
      activo: c.activo,
      pasaPorCocina: c.pasaPorCocina,
    });
    this.dialogoCategoria().nativeElement.showModal();
  }

  protected guardarCategoria(): void {
    if (this.guardando()) {
      return;
    }
    if (this.formCategoria.invalid) {
      this.formCategoria.markAllAsTouched();
      return;
    }

    const v = this.formCategoria.getRawValue();
    const datos = {
      nombre: v.nombre.trim(),
      orden: Math.trunc(Number(v.orden)),
      activo: v.activo,
      pasaPorCocina: v.pasaPorCocina,
    };
    const editando = this.editandoCategoria();

    this.enviar(
      editando ? this.api.actualizarCategoria(editando.id, datos) : this.api.crearCategoria(datos),
      this.dialogoCategoria().nativeElement,
    );
  }

  // ------------------------------------------------------------------- productos

  protected nuevoProducto(): void {
    const filtrada = this.categoriaFiltrada();
    const sugerida = filtrada?.activo ? filtrada : this.categoriasActivas()[0];

    this.editandoProducto.set(null);
    this.errorForm.set('');
    this.formProducto.reset({
      categoriaId: sugerida?.id ?? null,
      nombre: '',
      descripcion: '',
      precio: null,
      activo: true,
    });
    this.dialogoProducto().nativeElement.showModal();
  }

  protected editarProducto(p: Producto): void {
    this.editandoProducto.set(p);
    this.errorForm.set('');
    this.formProducto.reset({
      categoriaId: p.categoriaId,
      nombre: p.nombre,
      descripcion: p.descripcion ?? '',
      precio: p.precio,
      activo: p.activo,
    });
    this.dialogoProducto().nativeElement.showModal();
  }

  protected guardarProducto(): void {
    if (this.guardando()) {
      return;
    }
    if (this.formProducto.invalid) {
      this.formProducto.markAllAsTouched();
      return;
    }

    const v = this.formProducto.getRawValue();
    const datos = {
      categoriaId: v.categoriaId as number,
      nombre: v.nombre.trim(),
      descripcion: v.descripcion.trim() || null,
      precio: Number(v.precio),
      activo: v.activo,
    };
    const editando = this.editandoProducto();

    this.enviar(
      editando ? this.api.actualizarProducto(editando.id, datos) : this.api.crearProducto(datos),
      this.dialogoProducto().nativeElement,
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
        // El contador "Productos activos: X de Y" cambia con cada guardado.
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
