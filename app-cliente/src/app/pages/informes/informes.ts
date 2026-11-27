import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { etiquetaMetodo } from '../../core/caja.api';
import {
  CriterioProductos,
  InformeInsumos,
  InformeVentas,
  InformesApi,
  anchoBarra,
  csvInsumos,
  csvVentas,
  descargarCsv,
  dinero,
  ordenarProductos,
  porcentaje,
} from '../../core/informes.api';
import { cantidadTexto, conUnidad } from '../../core/inventario.api';
import {
  ANIO_MAXIMO,
  ANIO_MINIMO,
  MAXIMO_DIAS,
  PRESETS,
  PresetPeriodo,
  Rango,
  errorDeRango,
  etiquetaDia,
  hoyLima,
  rangoDe,
  rotuloRango,
} from '../../core/periodo';

/** Cuántos productos se muestran antes de pedir "Ver todos". */
const LIMITE_PRODUCTOS = 10;

/**
 * Informes: ventas, productos más vendidos e insumos de un periodo. Solo el administrador.
 * Los días son días de Lima. Las ventas y los insumos se piden por separado: si una parte falla,
 * la otra se sigue viendo.
 */
@Component({
  selector: 'app-informes',
  imports: [RouterLink],
  templateUrl: './informes.html',
  styleUrls: ['./informes.css', './informes-csv.css'],
})
export class Informes implements OnInit {
  private readonly api = inject(InformesApi);

  protected readonly dinero = dinero;
  protected readonly conUnidad = conUnidad;
  protected readonly cantidadTexto = cantidadTexto;
  protected readonly etiquetaMetodo = etiquetaMetodo;
  protected readonly presets = PRESETS;
  protected readonly maximoDias = MAXIMO_DIAS;
  protected readonly limiteProductos = LIMITE_PRODUCTOS;
  protected readonly fechaMinima = `${ANIO_MINIMO}-01-01`;
  protected readonly fechaMaxima = `${ANIO_MAXIMO}-12-31`;

  // ------------------------------------------------------------------ periodo

  protected readonly preset = signal<PresetPeriodo>('SIETE');
  /** Lo que se está escribiendo en las fechas del periodo personalizado. */
  protected readonly desde = signal('');
  protected readonly hasta = signal('');

  protected readonly errorRango = computed(() =>
    this.preset() === 'PERSONALIZADO' ? errorDeRango(this.desde(), this.hasta()) : '',
  );

  // ------------------------------------------------------------------ datos

  protected readonly ventas = signal<InformeVentas | null>(null);
  protected readonly insumos = signal<InformeInsumos | null>(null);
  protected readonly cargandoVentas = signal(false);
  protected readonly cargandoInsumos = signal(false);
  protected readonly errorVentas = signal('');
  protected readonly errorInsumos = signal('');

  /** Para ignorar la respuesta de una consulta vieja si mientras tanto se pidió otro periodo. */
  private secuencia = 0;
  /** El último periodo pedido, para poder reintentar si falló. */
  private ultimoRango: Rango | null = null;

  /** Mientras se actualiza, los números anteriores se quedan a la vista, atenuados. */
  protected readonly actualizandoVentas = computed(() => this.cargandoVentas() && this.ventas() !== null);
  protected readonly actualizandoInsumos = computed(() => this.cargandoInsumos() && this.insumos() !== null);

  /** El periodo que muestran los datos (no el que se está escribiendo). */
  protected readonly rotulo = computed(() => {
    const v = this.ventas();
    return v ? rotuloRango(v.desde, v.hasta) : '';
  });

  // ------------------------------------------------------------------ ventas por día

  protected readonly filasDias = computed(() => {
    const v = this.ventas();
    if (!v) {
      return [];
    }
    const maximo = Math.max(0, ...v.porDia.map((d) => d.total));
    return v.porDia.map((d) => ({
      ...d,
      etiqueta: etiquetaDia(d.fecha),
      ancho: anchoBarra(d.total, maximo),
    }));
  });

  // ------------------------------------------------------------------ métodos de pago

  protected readonly filasMetodos = computed(() => {
    const v = this.ventas();
    if (!v) {
      return [];
    }
    const usados = v.porMetodo.filter((m) => m.total > 0 || m.cuentas > 0).sort((a, b) => b.total - a.total);
    const maximo = usados.reduce((mayor, m) => Math.max(mayor, m.total), 0);
    const suma = usados.reduce((s, m) => s + m.total, 0);
    return usados.map((m) => ({
      ...m,
      etiqueta: etiquetaMetodo(m.metodo),
      ancho: anchoBarra(m.total, maximo),
      porcentaje: porcentaje(m.total, suma),
    }));
  });

  // ------------------------------------------------------------------ productos

  protected readonly criterio = signal<CriterioProductos>('CANTIDAD');
  protected readonly verTodos = signal(false);

  private readonly productosOrdenados = computed(() =>
    ordenarProductos(this.ventas()?.productos ?? [], this.criterio()),
  );

  protected readonly totalProductos = computed(() => this.productosOrdenados().length);

  protected readonly filasProductos = computed(() => {
    const todos = this.productosOrdenados();
    const criterio = this.criterio();
    const valor = (p: (typeof todos)[number]): number => (criterio === 'CANTIDAD' ? p.cantidad : p.importe);
    const maximo = todos.reduce((mayor, p) => Math.max(mayor, valor(p)), 0);
    const visibles = this.verTodos() ? todos : todos.slice(0, LIMITE_PRODUCTOS);
    return visibles.map((p, i) => ({ ...p, puesto: i + 1, ancho: anchoBarra(valor(p), maximo) }));
  });

  ngOnInit(): void {
    this.elegirPreset('SIETE');
  }

  // ------------------------------------------------------------------ acciones

  protected elegirPreset(preset: PresetPeriodo): void {
    const rango = rangoDe(preset, hoyLima());
    this.preset.set(preset);
    // Las fechas escritas arrancan con el periodo que se estaba viendo, para ajustarlas desde ahí.
    if (preset !== 'PERSONALIZADO') {
      this.desde.set(rango.desde);
      this.hasta.set(rango.hasta);
      this.consultar(rango);
    } else if (this.desde() === '' || this.hasta() === '') {
      this.desde.set(rango.desde);
      this.hasta.set(rango.hasta);
    }
  }

  protected aplicarPersonalizado(): void {
    if (this.preset() !== 'PERSONALIZADO' || errorDeRango(this.desde(), this.hasta()) !== '') {
      return;
    }
    this.consultar({ desde: this.desde(), hasta: this.hasta() });
  }

  protected elegirCriterio(criterio: CriterioProductos): void {
    this.criterio.set(criterio);
  }

  /** Vuelve a pedir el último periodo, por ejemplo si se cayó la conexión. */
  protected reintentar(): void {
    if (this.ultimoRango) {
      this.consultar(this.ultimoRango);
    }
  }

  protected exportarVentas(): void {
    const v = this.ventas();
    if (v) {
      descargarCsv(`ventas_${v.desde}_a_${v.hasta}.csv`, csvVentas(v));
    }
  }

  protected exportarInsumos(): void {
    const i = this.insumos();
    if (i) {
      descargarCsv(`insumos_${i.desde}_a_${i.hasta}.csv`, csvInsumos(i));
    }
  }

  private consultar(rango: Rango): void {
    const mia = ++this.secuencia;
    this.ultimoRango = rango;
    this.verTodos.set(false);

    this.cargandoVentas.set(true);
    this.errorVentas.set('');
    this.api.ventas(rango.desde, rango.hasta).subscribe({
      next: (informe) => {
        if (mia !== this.secuencia) {
          return;
        }
        this.ventas.set(informe);
        this.cargandoVentas.set(false);
      },
      error: (e: HttpErrorResponse) => {
        if (mia !== this.secuencia) {
          return;
        }
        // Si falló, no se dejan a la vista los números de otro periodo.
        this.ventas.set(null);
        this.cargandoVentas.set(false);
        this.errorVentas.set(e.error?.mensaje ?? 'No se pudo cargar el informe. Revisa tu conexión e inténtalo de nuevo.');
      },
    });

    this.cargandoInsumos.set(true);
    this.errorInsumos.set('');
    this.api.insumos(rango.desde, rango.hasta).subscribe({
      next: (informe) => {
        if (mia !== this.secuencia) {
          return;
        }
        this.insumos.set(informe);
        this.cargandoInsumos.set(false);
      },
      error: (e: HttpErrorResponse) => {
        if (mia !== this.secuencia) {
          return;
        }
        this.insumos.set(null);
        this.cargandoInsumos.set(false);
        this.errorInsumos.set(
          e.error?.mensaje ?? 'No se pudo cargar los insumos. Revisa tu conexión e inténtalo de nuevo.',
        );
      },
    });
  }
}
