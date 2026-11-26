import { HttpErrorResponse } from '@angular/common/http';
import { Component, ElementRef, OnInit, computed, inject, signal, viewChild } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';

import { AuthService } from '../../core/auth.service';
import { PATRON_MONTO, aNumero, fechaHora, normalizar, soles } from '../../core/formato';
import {
  Insumo,
  InsumoDatos,
  InventarioApi,
  MovimientoDatos,
  MovimientoInsumo,
  PATRON_CANTIDAD,
  TIPOS_MOVIMIENTO,
  TipoMovimiento,
  UNIDADES,
  Unidad,
  abreviatura,
  calcularMovimiento,
  cantidadConSigno,
  cantidadValida,
  conUnidad,
  etiquetaTipo,
} from '../../core/inventario.api';
import { PlanService } from '../../core/plan.service';

type Filtro = 'TODOS' | 'BAJO' | 'AGOTADO' | 'INACTIVO';

/** Lo que se muestra mientras la persona escribe la cantidad de un movimiento. */
interface VistaCalculo {
  vacio: boolean;
  error: string;
  /** "17.5 kg"; vacío si todavía no se puede calcular. */
  resultado: string;
}

/** Inventario: insumos con su stock, entradas, salidas, mermas, ajustes por conteo y alertas de stock bajo. */
@Component({
  selector: 'app-inventario',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './inventario.html',
  styleUrl: './inventario.css',
})
export class Inventario implements OnInit {
  private readonly api = inject(InventarioApi);
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly auth = inject(AuthService);
  private readonly dialogoInsumo = viewChild.required<ElementRef<HTMLDialogElement>>('dlgInsumo');
  private readonly dialogoMovimiento = viewChild.required<ElementRef<HTMLDialogElement>>('dlgMovimiento');
  private readonly dialogoHistorial = viewChild.required<ElementRef<HTMLDialogElement>>('dlgHistorial');

  protected readonly planes = inject(PlanService);

  protected readonly soles = soles;
  protected readonly fechaHora = fechaHora;
  protected readonly abreviatura = abreviatura;
  protected readonly conUnidad = conUnidad;
  protected readonly cantidadConSigno = cantidadConSigno;
  protected readonly etiquetaTipo = etiquetaTipo;
  protected readonly unidades = UNIDADES;

  /** Crear y editar insumos, y ajustar el stock después de contar, es del administrador. */
  protected readonly esAdmin = computed(() => this.auth.sesion()?.rol === 'ADMIN');

  protected readonly insumos = signal<Insumo[]>([]);
  protected readonly cargando = signal(true);
  protected readonly errorLista = signal('');
  protected readonly aviso = signal('');

  protected readonly filtro = signal<Filtro>('TODOS');
  protected readonly busqueda = signal('');

  protected readonly conteo = computed(() => {
    const lista = this.insumos();
    return {
      todos: lista.filter((i) => i.activo).length,
      bajo: lista.filter((i) => i.estado === 'BAJO').length,
      agotado: lista.filter((i) => i.estado === 'AGOTADO').length,
      inactivo: lista.filter((i) => i.estado === 'INACTIVO').length,
    };
  });

  protected readonly visibles = computed(() => {
    const filtro = this.filtro();
    const texto = normalizar(this.busqueda());

    return this.insumos().filter((i) => {
      const coincideFiltro = filtro === 'TODOS' ? i.activo : i.estado === filtro;
      return coincideFiltro && (texto === '' || normalizar(i.nombre).includes(texto));
    });
  });

  protected readonly guardando = signal(false);

  // ------------------------------------------------------------------ insumo (crear y editar)

  /** null = creando; con valor = editando ese insumo. */
  protected readonly editando = signal<Insumo | null>(null);
  protected readonly errorForm = signal('');

  protected readonly formInsumo = this.fb.group({
    nombre: ['', [Validators.required, Validators.maxLength(80)]],
    unidad: this.fb.control<Unidad>('KG'),
    stockMinimo: ['0', [Validators.required, Validators.pattern(PATRON_CANTIDAD)]],
    stockInicial: ['', Validators.pattern(PATRON_CANTIDAD)],
    activo: [true],
  });

  // ------------------------------------------------------------------ movimiento

  protected readonly movInsumo = signal<Insumo | null>(null);
  protected readonly tipo = signal<TipoMovimiento>('ENTRADA');
  protected readonly cantidadEscrita = signal('');
  protected readonly costoEscrito = signal('');
  protected readonly notaEscrita = signal('');
  protected readonly errorMovimiento = signal('');

  /** El ajuste (corregir el stock después de contar) solo lo ve el administrador. */
  protected readonly tiposDisponibles = computed(() =>
    TIPOS_MOVIMIENTO.filter((t) => t.valor !== 'AJUSTE' || this.esAdmin()),
  );

  protected readonly ayudaTipo = computed(
    () => TIPOS_MOVIMIENTO.find((t) => t.valor === this.tipo())?.ayuda ?? '',
  );

  protected readonly etiquetaCantidad = computed(() => {
    switch (this.tipo()) {
      case 'ENTRADA':
        return 'Cantidad que entra';
      case 'SALIDA':
        return 'Cantidad que sale';
      case 'MERMA':
        return 'Cantidad perdida';
      default:
        return 'Cantidad contada (el total que hay ahora)';
    }
  });

  /** Cuánto quedaría en el stock, o por qué no se puede, apenas la persona escribe la cantidad. */
  protected readonly calculo = computed<VistaCalculo>(() => {
    const insumo = this.movInsumo();
    const texto = this.cantidadEscrita().trim();
    if (!insumo || texto === '') {
      return { vacio: true, error: '', resultado: '' };
    }
    if (!cantidadValida(texto, insumo.unidad)) {
      return {
        vacio: false,
        error:
          insumo.unidad === 'UNIDAD'
            ? 'Escribe un número entero, sin decimales'
            : 'Escribe un número con hasta 3 decimales',
        resultado: '',
      };
    }
    const c = calcularMovimiento(this.tipo(), insumo.stockActual, aNumero(texto), insumo.unidad);
    return {
      vacio: false,
      error: c.error,
      resultado: c.resultado === null ? '' : conUnidad(c.resultado, insumo.unidad),
    };
  });

  // ------------------------------------------------------------------ historial

  protected readonly historialDe = signal<Insumo | null>(null);
  protected readonly movimientos = signal<MovimientoInsumo[]>([]);
  protected readonly cargandoHistorial = signal(false);
  protected readonly errorHistorial = signal('');

  ngOnInit(): void {
    this.planes.cargar();
    this.cargar();
  }

  /** Con silencioso = true no se vuelve a mostrar "Cargando…" (por ejemplo, después de guardar). */
  protected cargar(silencioso = false): void {
    if (!silencioso) {
      this.cargando.set(true);
    }
    this.errorLista.set('');

    this.api.insumos().subscribe({
      next: (lista) => {
        this.insumos.set(lista);
        this.cargando.set(false);
      },
      error: (e: HttpErrorResponse) => {
        this.cargando.set(false);
        this.errorLista.set(
          e.error?.mensaje ?? 'No se pudo cargar el inventario. Revisa tu conexión e inténtalo de nuevo.',
        );
      },
    });
  }

  protected elegirFiltro(filtro: Filtro): void {
    this.filtro.set(filtro);
  }

  protected buscar(texto: string): void {
    this.busqueda.set(texto);
  }

  protected cerrar(dialogo: HTMLDialogElement): void {
    dialogo.close();
  }

  // ------------------------------------------------------------------ insumo

  protected nuevoInsumo(): void {
    this.editando.set(null);
    this.errorForm.set('');
    this.formInsumo.reset({ nombre: '', unidad: 'KG', stockMinimo: '0', stockInicial: '', activo: true });
    this.formInsumo.controls.unidad.enable();
    this.dialogoInsumo().nativeElement.showModal();
  }

  protected editarInsumo(insumo: Insumo): void {
    this.editando.set(insumo);
    this.errorForm.set('');
    this.formInsumo.reset({
      nombre: insumo.nombre,
      unidad: insumo.unidad,
      stockMinimo: String(insumo.stockMinimo),
      stockInicial: '',
      activo: insumo.activo,
    });
    // Con movimientos, la unidad ya no se cambia: el stock guardado dejaría de tener sentido.
    if (insumo.unidadFija) {
      this.formInsumo.controls.unidad.disable();
    } else {
      this.formInsumo.controls.unidad.enable();
    }
    this.dialogoInsumo().nativeElement.showModal();
  }

  protected guardarInsumo(): void {
    if (this.guardando()) {
      return;
    }
    if (this.formInsumo.invalid) {
      this.formInsumo.markAllAsTouched();
      return;
    }

    const v = this.formInsumo.getRawValue();
    const editando = this.editando();
    const inicial = v.stockInicial.trim();

    if (
      !cantidadValida(v.stockMinimo, v.unidad) ||
      (!editando && inicial !== '' && !cantidadValida(inicial, v.unidad))
    ) {
      this.errorForm.set('Si el insumo se cuenta por unidades, escribe números enteros, sin decimales.');
      return;
    }

    const datos: InsumoDatos = {
      nombre: v.nombre.trim(),
      unidad: v.unidad,
      stockMinimo: aNumero(v.stockMinimo),
      activo: v.activo,
    };
    if (!editando) {
      datos.stockInicial = inicial === '' ? 0 : aNumero(inicial);
    }

    this.guardando.set(true);
    this.errorForm.set('');

    const peticion: Observable<Insumo> = editando
      ? this.api.actualizar(editando.id, datos)
      : this.api.crear(datos);

    peticion.subscribe({
      next: (guardado) => {
        this.guardando.set(false);
        this.dialogoInsumo().nativeElement.close();
        this.aviso.set(`${editando ? 'Cambios guardados' : 'Insumo creado'}: ${guardado.nombre}.`);
        this.cargar(true);
        // El contador "Insumos activos: X de Y" cambia con cada guardado.
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

  // ------------------------------------------------------------------ movimiento

  protected abrirMovimiento(insumo: Insumo, tipo: TipoMovimiento = 'ENTRADA'): void {
    this.movInsumo.set(insumo);
    this.tipo.set(tipo);
    this.cantidadEscrita.set('');
    this.costoEscrito.set('');
    this.notaEscrita.set('');
    this.errorMovimiento.set('');
    this.dialogoMovimiento().nativeElement.showModal();
  }

  protected elegirTipo(tipo: TipoMovimiento): void {
    this.tipo.set(tipo);
    this.errorMovimiento.set('');
  }

  protected escribirCantidad(texto: string): void {
    this.cantidadEscrita.set(texto);
    this.errorMovimiento.set('');
  }

  protected guardarMovimiento(): void {
    const insumo = this.movInsumo();
    if (!insumo || this.guardando()) {
      return;
    }

    const calculo = this.calculo();
    if (calculo.vacio) {
      this.errorMovimiento.set('Escribe la cantidad');
      return;
    }
    if (calculo.error) {
      this.errorMovimiento.set(calculo.error);
      return;
    }

    const tipo = this.tipo();
    const nota = this.notaEscrita().trim();
    if (nota.length > 150) {
      this.errorMovimiento.set('La nota no puede pasar de 150 caracteres');
      return;
    }
    if (tipo === 'MERMA' && nota === '') {
      this.errorMovimiento.set('Cuenta qué pasó con la merma (por ejemplo: se malogró, se cayó, venció)');
      return;
    }

    let costo: number | null = null;
    const costoTexto = this.costoEscrito().trim();
    if (tipo === 'ENTRADA' && costoTexto !== '') {
      if (!PATRON_MONTO.test(costoTexto)) {
        this.errorMovimiento.set('El costo no es válido: escribe un monto con hasta 2 decimales');
        return;
      }
      costo = aNumero(costoTexto);
    }

    const datos: MovimientoDatos = {
      tipo,
      cantidad: aNumero(this.cantidadEscrita()),
      costoTotal: costo,
      nota: nota === '' ? null : nota,
    };

    this.guardando.set(true);
    this.errorMovimiento.set('');

    this.api.movimiento(insumo.id, datos).subscribe({
      next: (actualizado) => {
        this.guardando.set(false);
        this.dialogoMovimiento().nativeElement.close();
        this.insumos.update((lista) => lista.map((i) => (i.id === actualizado.id ? actualizado : i)));
        this.aviso.set(this.mensajeMovimiento(datos, actualizado));
      },
      error: (e: HttpErrorResponse) => {
        this.guardando.set(false);
        this.errorMovimiento.set(
          e.error?.mensaje ?? 'No se pudo registrar. Revisa tu conexión e inténtalo de nuevo.',
        );
        if (e.status === 409) {
          this.refrescarMovimiento(insumo.id);
        }
      },
    });
  }

  /** Si otra persona movió el stock mientras tanto, se vuelve a mostrar el stock real en la ventana. */
  private refrescarMovimiento(id: number): void {
    this.api.insumos().subscribe({
      next: (lista) => {
        this.insumos.set(lista);
        const actual = lista.find((i) => i.id === id);
        if (actual && this.movInsumo()?.id === id) {
          this.movInsumo.set(actual);
        }
      },
    });
  }

  private mensajeMovimiento(datos: MovimientoDatos, insumo: Insumo): string {
    const ahora = `Ahora hay ${conUnidad(insumo.stockActual, insumo.unidad)}.`;
    const cantidad = conUnidad(datos.cantidad, insumo.unidad);
    switch (datos.tipo) {
      case 'ENTRADA':
        return `Entrada registrada: +${cantidad} de ${insumo.nombre}. ${ahora}`;
      case 'SALIDA':
        return `Salida registrada: −${cantidad} de ${insumo.nombre}. ${ahora}`;
      case 'MERMA':
        return `Merma registrada: −${cantidad} de ${insumo.nombre}. ${ahora}`;
      default:
        return `Ajuste registrado en ${insumo.nombre}. ${ahora}`;
    }
  }

  // ------------------------------------------------------------------ historial

  protected verHistorial(insumo: Insumo): void {
    this.historialDe.set(insumo);
    this.movimientos.set([]);
    this.errorHistorial.set('');
    this.cargandoHistorial.set(true);
    this.dialogoHistorial().nativeElement.showModal();

    this.api.historial(insumo.id).subscribe({
      next: (lista) => {
        // Si mientras tanto se abrió el historial de otro insumo, esta respuesta ya no corresponde.
        if (this.historialDe()?.id === insumo.id) {
          this.movimientos.set(lista);
          this.cargandoHistorial.set(false);
        }
      },
      error: (e: HttpErrorResponse) => {
        if (this.historialDe()?.id === insumo.id) {
          this.cargandoHistorial.set(false);
          this.errorHistorial.set(e.error?.mensaje ?? 'No se pudo cargar el historial.');
        }
      },
    });
  }

  protected textoEstado(insumo: Insumo): string {
    switch (insumo.estado) {
      case 'BAJO':
        return 'Stock bajo';
      case 'AGOTADO':
        return 'Agotado';
      case 'INACTIVO':
        return 'Inactivo';
      default:
        return 'Normal';
    }
  }
}
