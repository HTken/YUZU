import { HttpErrorResponse } from '@angular/common/http';
import { Component, ElementRef, OnInit, computed, inject, signal, viewChild } from '@angular/core';
import { RouterLink } from '@angular/router';

import { CatalogoApi, Producto } from '../../core/catalogo.api';
import { aNumero, normalizar } from '../../core/formato';
import {
  Insumo,
  InventarioApi,
  LineaRecetaDatos,
  MAXIMO_LINEAS_RECETA,
  PATRON_RECETA,
  abreviatura,
} from '../../core/inventario.api';

type Filtro = 'TODOS' | 'SIN' | 'CON';

/** Una fila del editor: lo que la persona está escribiendo, todavía sin validar. */
interface LineaEditable {
  insumoId: number | null;
  cantidad: string;
}

/**
 * Recetas: qué insumos gasta cada producto. Al cobrar una cuenta, el sistema descuenta del inventario
 * los insumos de lo que se vendió. Un producto sin receta no descuenta nada.
 */
@Component({
  selector: 'app-recetas',
  imports: [RouterLink],
  templateUrl: './recetas.html',
  styleUrl: './recetas.css',
})
export class Recetas implements OnInit {
  private readonly api = inject(InventarioApi);
  private readonly catalogo = inject(CatalogoApi);
  private readonly dialogo = viewChild.required<ElementRef<HTMLDialogElement>>('dlgReceta');

  protected readonly abreviatura = abreviatura;
  protected readonly maximoLineas = MAXIMO_LINEAS_RECETA;

  protected readonly productos = signal<Producto[]>([]);
  protected readonly insumos = signal<Insumo[]>([]);
  /** Cuántos insumos tiene la receta de cada producto (los que no aparecen no tienen receta). */
  protected readonly lineasPorProducto = signal<Record<number, number>>({});

  protected readonly cargando = signal(true);
  protected readonly errorLista = signal('');
  protected readonly aviso = signal('');

  protected readonly filtro = signal<Filtro>('TODOS');
  protected readonly busqueda = signal('');

  /** Solo los productos que se venden; los desactivados no necesitan receta. */
  private readonly activos = computed(() => this.productos().filter((p) => p.activo));

  protected readonly conteo = computed(() => {
    const lineas = this.lineasPorProducto();
    const lista = this.activos();
    const con = lista.filter((p) => (lineas[p.id] ?? 0) > 0).length;
    return { todos: lista.length, con, sin: lista.length - con };
  });

  protected readonly visibles = computed(() => {
    const filtro = this.filtro();
    const texto = normalizar(this.busqueda());
    const lineas = this.lineasPorProducto();

    return this.activos().filter((p) => {
      const tiene = (lineas[p.id] ?? 0) > 0;
      const coincideFiltro = filtro === 'TODOS' || (filtro === 'CON' ? tiene : !tiene);
      return (
        coincideFiltro &&
        (texto === '' || normalizar(p.nombre).includes(texto) || normalizar(p.categoria).includes(texto))
      );
    });
  });

  // ------------------------------------------------------------------ editor

  protected readonly producto = signal<Producto | null>(null);
  protected readonly lineas = signal<LineaEditable[]>([]);
  protected readonly cargandoReceta = signal(false);
  protected readonly guardando = signal(false);
  protected readonly errorForm = signal('');

  /** Los insumos que se pueden elegir: los activos, y los que ya estén en esta receta aunque se hayan desactivado. */
  private readonly opciones = computed(() => {
    const usados = new Set(this.lineas().map((l) => l.insumoId));
    return this.insumos().filter((i) => i.activo || usados.has(i.id));
  });

  protected readonly quedanInsumos = computed(
    () => this.opciones().length > this.lineas().filter((l) => l.insumoId !== null).length,
  );

  ngOnInit(): void {
    this.cargar();
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.errorLista.set('');

    let pendientes = 3;
    let fallo = false;
    const listo = (): void => {
      pendientes -= 1;
      if (pendientes === 0 && !fallo) {
        this.cargando.set(false);
      }
    };
    const error = (e: HttpErrorResponse): void => {
      if (fallo) {
        return;
      }
      fallo = true;
      this.cargando.set(false);
      this.errorLista.set(
        e.error?.mensaje ?? 'No se pudieron cargar las recetas. Revisa tu conexión e inténtalo de nuevo.',
      );
    };

    this.catalogo.productos().subscribe({
      next: (lista) => {
        this.productos.set(lista);
        listo();
      },
      error,
    });
    this.api.insumos().subscribe({
      next: (lista) => {
        this.insumos.set(lista);
        listo();
      },
      error,
    });
    this.api.resumenRecetas().subscribe({
      next: (resumen) => {
        const mapa: Record<number, number> = {};
        for (const r of resumen) {
          mapa[r.productoId] = r.lineas;
        }
        this.lineasPorProducto.set(mapa);
        listo();
      },
      error,
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

  protected textoReceta(producto: Producto): string {
    const n = this.lineasPorProducto()[producto.id] ?? 0;
    if (n === 0) {
      return 'Sin receta';
    }
    return n === 1 ? '1 insumo' : `${n} insumos`;
  }

  // ------------------------------------------------------------------ editor

  protected editar(producto: Producto): void {
    this.producto.set(producto);
    this.lineas.set([]);
    this.errorForm.set('');
    this.cargandoReceta.set(true);
    this.dialogo().nativeElement.showModal();

    this.api.receta(producto.id).subscribe({
      next: (receta) => {
        // Si mientras tanto se abrió la receta de otro producto, esta respuesta ya no corresponde.
        if (this.producto()?.id === producto.id) {
          this.lineas.set(receta.lineas.map((l) => ({ insumoId: l.insumoId, cantidad: String(l.cantidad) })));
          this.cargandoReceta.set(false);
        }
      },
      error: (e: HttpErrorResponse) => {
        if (this.producto()?.id === producto.id) {
          this.cargandoReceta.set(false);
          this.errorForm.set(e.error?.mensaje ?? 'No se pudo cargar la receta.');
        }
      },
    });
  }

  protected agregarLinea(): void {
    if (this.lineas().length >= MAXIMO_LINEAS_RECETA) {
      return;
    }
    this.lineas.update((lista) => [...lista, { insumoId: null, cantidad: '' }]);
    this.errorForm.set('');
  }

  protected quitarLinea(indice: number): void {
    this.lineas.update((lista) => lista.filter((_, i) => i !== indice));
    this.errorForm.set('');
  }

  protected elegirInsumo(indice: number, valor: string): void {
    const id = valor === '' ? null : Number(valor);
    this.lineas.update((lista) => lista.map((l, i) => (i === indice ? { ...l, insumoId: id } : l)));
    this.errorForm.set('');
  }

  protected escribirCantidad(indice: number, texto: string): void {
    this.lineas.update((lista) => lista.map((l, i) => (i === indice ? { ...l, cantidad: texto } : l)));
    this.errorForm.set('');
  }

  /** Los insumos que puede elegir esta fila: los que no están ya en otra fila. */
  protected opcionesPara(indice: number): Insumo[] {
    const enOtras = new Set(
      this.lineas()
        .filter((_, i) => i !== indice)
        .map((l) => l.insumoId),
    );
    return this.opciones().filter((i) => !enOtras.has(i.id));
  }

  protected unidadDe(insumoId: number | null): string {
    const insumo = this.insumos().find((i) => i.id === insumoId);
    return insumo ? abreviatura(insumo.unidad) : '';
  }

  protected guardar(): void {
    const producto = this.producto();
    if (!producto || this.guardando() || this.cargandoReceta()) {
      return;
    }

    const datos: LineaRecetaDatos[] = [];
    for (const l of this.lineas()) {
      const texto = l.cantidad.trim();
      // Una fila completamente vacía se ignora: no hace falta que la quiten a mano.
      if (l.insumoId === null && texto === '') {
        continue;
      }
      if (l.insumoId === null) {
        this.errorForm.set('Elige el insumo en cada fila, o quita la fila que no uses.');
        return;
      }
      const nombre = this.insumos().find((i) => i.id === l.insumoId)?.nombre ?? 'el insumo';
      if (texto === '' || !PATRON_RECETA.test(texto) || aNumero(texto) <= 0) {
        this.errorForm.set(
          `Escribe cuánto gasta una unidad del producto de ${nombre}: un número mayor a 0 (hasta 5 enteros y 3 decimales).`,
        );
        return;
      }
      datos.push({ insumoId: l.insumoId, cantidad: aNumero(texto) });
    }

    this.guardando.set(true);
    this.errorForm.set('');

    this.api.guardarReceta(producto.id, datos).subscribe({
      next: (receta) => {
        this.guardando.set(false);
        this.dialogo().nativeElement.close();
        this.lineasPorProducto.update((mapa) => {
          const nuevo = { ...mapa };
          if (receta.lineas.length === 0) {
            delete nuevo[producto.id];
          } else {
            nuevo[producto.id] = receta.lineas.length;
          }
          return nuevo;
        });
        this.aviso.set(
          receta.lineas.length === 0
            ? `Se quitó la receta de ${producto.nombre}. Ya no descuenta insumos al venderse.`
            : `Receta guardada: ${producto.nombre} (${this.textoReceta(producto)}). Se descontará al cobrar.`,
        );
        // Los insumos usados en una receta ya no pueden cambiar de unidad: se refresca la lista.
        this.api.insumos().subscribe({ next: (lista) => this.insumos.set(lista) });
      },
      error: (e: HttpErrorResponse) => {
        this.guardando.set(false);
        this.errorForm.set(e.error?.mensaje ?? 'No se pudo guardar. Revisa tu conexión e inténtalo de nuevo.');
      },
    });
  }
}
