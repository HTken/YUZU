import { HttpErrorResponse } from '@angular/common/http';
import { Component, ElementRef, OnInit, computed, inject, signal, viewChild } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Observable, forkJoin, map, of, switchMap } from 'rxjs';

import { AuthService } from '../../core/auth.service';
import { CajaApi, METODOS, MetodoPago } from '../../core/caja.api';
import { CatalogoApi, Categoria, Producto } from '../../core/catalogo.api';
import { PATRON_MONTO, aNumero, centavos, normalizar, soles, textoTiempo, ticketNumero } from '../../core/formato';
import { Mesa, MesasApi } from '../../core/mesas.api';
import { EstadoItem, ItemPedido, Pedido, PedidosApi } from '../../core/pedidos.api';
import { imprimirTicket } from '../../core/ticket';

/** Una parte del pago: cuánto y con qué método. El monto se guarda como texto porque es lo que se escribe. */
interface LineaPago {
  metodo: MetodoPago;
  monto: string;
}

/** Lo que se muestra al terminar de cobrar. */
interface TicketCobrado {
  pedidoId: number;
  ticket: number;
  total: number;
  mesa: string;
  /** Vuelto a entregar si se indicó el efectivo recibido; null si no se indicó. */
  vuelto: number | null;
}

interface Confirmacion {
  titulo: string;
  mensaje: string;
  boton: string;
  accion: () => void;
}

/** Toma de pedido de una mesa: la carta a la izquierda y la cuenta a la derecha. */
@Component({
  selector: 'app-pedido-mesa',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './pedido.html',
  styleUrl: './pedido.css',
})
export class PedidoMesa implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly mesasApi = inject(MesasApi);
  private readonly catalogo = inject(CatalogoApi);
  private readonly api = inject(PedidosApi);
  private readonly cajaApi = inject(CajaApi);
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly auth = inject(AuthService);
  private readonly dialogoNota = viewChild.required<ElementRef<HTMLDialogElement>>('dlgNota');
  private readonly dialogoConfirmar = viewChild.required<ElementRef<HTMLDialogElement>>('dlgConfirmar');
  private readonly dialogoCobro = viewChild.required<ElementRef<HTMLDialogElement>>('dlgCobro');
  private readonly dialogoTicket = viewChild.required<ElementRef<HTMLDialogElement>>('dlgTicket');

  private readonly mesaId = Number(this.route.snapshot.paramMap.get('mesaId'));

  protected readonly soles = soles;
  protected readonly ticketNumero = ticketNumero;
  protected readonly metodos = METODOS;
  protected readonly esAdmin = computed(() => this.auth.sesion()?.rol === 'ADMIN');
  /** Cobrar es del administrador y del cajero; el mozo solo toma el pedido. */
  protected readonly puedeCobrar = computed(() => {
    const rol = this.auth.sesion()?.rol;
    return rol === 'ADMIN' || rol === 'CAJERO';
  });

  protected readonly mesa = signal<Mesa | null>(null);
  protected readonly categorias = signal<Categoria[]>([]);
  protected readonly productos = signal<Producto[]>([]);
  protected readonly pedido = signal<Pedido | null>(null);

  protected readonly cargando = signal(true);
  protected readonly errorCarga = signal('');
  protected readonly ocupado = signal(false);
  protected readonly error = signal('');
  protected readonly aviso = signal('');

  protected readonly categoriaElegida = signal<number | null>(null);
  protected readonly busqueda = signal('');

  protected readonly lineas = computed(() => this.pedido()?.items ?? []);
  protected readonly total = computed(() => this.pedido()?.total ?? 0);
  protected readonly pendientes = computed(() => this.pedido()?.pendientes ?? 0);
  /** Platos que cocina ya dejó listos y que el mozo tiene que llevar a la mesa. */
  protected readonly listos = computed(() => this.lineas().filter((i) => i.estado === 'LISTO').length);

  /** Solo se ofrece lo que se puede vender: productos activos de categorías activas. */
  private readonly disponibles = computed(() => {
    const activas = new Set(this.categorias().filter((c) => c.activo).map((c) => c.id));
    return this.productos().filter((p) => p.activo && activas.has(p.categoriaId));
  });

  protected readonly categoriasConProductos = computed(() => {
    const conProductos = new Set(this.disponibles().map((p) => p.categoriaId));
    return this.categorias().filter((c) => c.activo && conProductos.has(c.id));
  });

  protected readonly productosVisibles = computed(() => {
    const categoria = this.categoriaElegida();
    const texto = normalizar(this.busqueda());
    return this.disponibles().filter(
      (p) =>
        (categoria === null || p.categoriaId === categoria) &&
        (texto === '' || normalizar(p.nombre).includes(texto)),
    );
  });

  // Nota de un producto
  protected readonly itemConNota = signal<ItemPedido | null>(null);
  protected readonly formNota = this.fb.group({
    nota: ['', Validators.maxLength(150)],
  });

  // Confirmación antes de anular algo
  protected readonly confirmacion = signal<Confirmacion | null>(null);

  // ------------------------------------------------------------ cobro
  /** Se muestra cuando se intenta cobrar con la caja cerrada. */
  protected readonly cajaCerrada = signal(false);
  protected readonly lineasPago = signal<LineaPago[]>([]);
  protected readonly efectivoRecibido = signal('');
  protected readonly errorCobro = signal('');
  protected readonly cobrando = signal(false);
  protected readonly ticket = signal<TicketCobrado | null>(null);
  protected readonly imprimiendo = signal(false);
  protected readonly errorImpresion = signal('');

  private readonly centavosPagados = computed(() =>
    this.lineasPago().reduce((suma, l) => {
      const monto = aNumero(l.monto);
      return suma + (Number.isNaN(monto) ? 0 : centavos(monto));
    }, 0),
  );

  /** Lo que falta para completar el total (positivo), o lo que sobra (negativo). */
  protected readonly faltante = computed(() => (centavos(this.total()) - this.centavosPagados()) / 100);

  /** Todos los montos son válidos y suman exactamente el total de la cuenta. */
  protected readonly pagosValidos = computed(
    () =>
      this.lineasPago().length > 0 &&
      this.lineasPago().every((l) => PATRON_MONTO.test(l.monto.trim()) && aNumero(l.monto) > 0) &&
      this.faltante() === 0,
  );

  protected readonly hayEfectivo = computed(() => this.lineasPago().some((l) => l.metodo === 'EFECTIVO'));

  private readonly centavosEfectivo = computed(() =>
    this.lineasPago()
      .filter((l) => l.metodo === 'EFECTIVO')
      .reduce((suma, l) => {
        const monto = aNumero(l.monto);
        return suma + (Number.isNaN(monto) ? 0 : centavos(monto));
      }, 0),
  );

  /** Vuelto a entregar, si se escribió el efectivo recibido y alcanza. */
  protected readonly vuelto = computed<number | null>(() => {
    const recibido = aNumero(this.efectivoRecibido());
    if (!this.hayEfectivo() || Number.isNaN(recibido)) {
      return null;
    }
    const diferencia = centavos(recibido) - this.centavosEfectivo();
    return diferencia >= 0 ? diferencia / 100 : null;
  });

  protected readonly recibidoInsuficiente = computed(() => {
    const recibido = aNumero(this.efectivoRecibido());
    return this.hayEfectivo() && !Number.isNaN(recibido) && this.vuelto() === null;
  });

  ngOnInit(): void {
    if (!Number.isInteger(this.mesaId) || this.mesaId <= 0) {
      this.cargando.set(false);
      this.errorCarga.set('Esa mesa no existe.');
      return;
    }
    this.cargar();
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.errorCarga.set('');

    forkJoin({
      mesas: this.mesasApi.mesas(),
      categorias: this.catalogo.categorias(),
      productos: this.catalogo.productos(),
      pedido: this.pedidoDeLaMesa(),
    }).subscribe({
      next: ({ mesas, categorias, productos, pedido }) => {
        const mesa = mesas.find((m) => m.id === this.mesaId && m.activo) ?? null;
        this.mesa.set(mesa);
        this.categorias.set(categorias);
        this.productos.set(productos);
        this.pedido.set(pedido);
        this.cargando.set(false);
        if (!mesa) {
          this.errorCarga.set('No encontramos esa mesa. Puede que se haya desactivado.');
        }
      },
      error: (e: HttpErrorResponse) => {
        this.cargando.set(false);
        this.errorCarga.set(this.textoError(e, 'No se pudo cargar la mesa. Revisa tu conexión e inténtalo de nuevo.'));
      },
    });
  }

  /** La cuenta abierta de esta mesa, o null si está libre. */
  private pedidoDeLaMesa(): Observable<Pedido | null> {
    return this.api.abiertos().pipe(
      switchMap((abiertos) => {
        const resumen = abiertos.find((p) => p.mesaId === this.mesaId);
        const pedido$: Observable<Pedido | null> = resumen ? this.api.obtener(resumen.id) : of(null);
        return pedido$;
      }),
    );
  }

  /** Por si otro mozo agregó algo a la misma cuenta. */
  protected actualizar(): void {
    this.limpiarMensajes();
    this.ocupado.set(true);
    this.pedidoDeLaMesa().subscribe({
      next: (pedido) => {
        this.pedido.set(pedido);
        this.ocupado.set(false);
      },
      error: (e: HttpErrorResponse) => this.fallo(e),
    });
  }

  protected tiempo(p: Pedido): string {
    return textoTiempo(p.abiertoEn);
  }

  protected etiquetaEstado(estado: EstadoItem): string {
    switch (estado) {
      case 'PENDIENTE':
        return 'Sin enviar';
      case 'ENVIADO':
        return 'En cocina';
      case 'LISTO':
        return 'Listo para servir';
      case 'SERVIDO':
        return 'Servido';
      default:
        return 'Anulado';
    }
  }

  protected elegirCategoria(id: number | null): void {
    this.categoriaElegida.set(id);
  }

  protected buscar(texto: string): void {
    this.busqueda.set(texto);
  }

  // ----------------------------------------------------------------- la cuenta

  /** Toca un producto de la carta: se agrega 1 a la cuenta (y se abre la cuenta si la mesa estaba libre). */
  protected agregar(producto: Producto): void {
    if (this.ocupado()) {
      return;
    }
    this.limpiarMensajes();
    this.ocupado.set(true);

    const actual = this.pedido();
    const idPedido$: Observable<number> = actual
      ? of(actual.id)
      : this.api.abrir(this.mesaId).pipe(map((p) => p.id));

    idPedido$
      .pipe(switchMap((id) => this.api.agregar(id, { productoId: producto.id, cantidad: 1, nota: null })))
      .subscribe({
        next: (p) => this.listo(p),
        error: (e: HttpErrorResponse) => this.fallo(e),
      });
  }

  protected cambiarCantidad(item: ItemPedido, cambio: number): void {
    const nueva = item.cantidad + cambio;
    if (nueva < 1) {
      this.quitar(item);
      return;
    }
    this.modificar((id) => this.api.actualizar(id, item.id, { cantidad: nueva, nota: item.nota }));
  }

  protected quitar(item: ItemPedido): void {
    this.modificar((id) => this.api.quitar(id, item.id));
  }

  /** El mozo lleva un plato a la mesa. */
  protected servir(item: ItemPedido): void {
    this.modificar((id) => this.api.servir(id, item.id));
  }

  /** El mozo lleva todo lo que está listo de una vez. */
  protected servirListos(): void {
    this.modificar((id) => this.api.servirListos(id), 'Marcado como servido.');
  }

  protected enviar(): void {
    this.modificar((id) => this.api.enviar(id), 'Los productos nuevos se enviaron a cocina.');
  }

  private modificar(peticion: (pedidoId: number) => Observable<Pedido>, mensajeOk = ''): void {
    const actual = this.pedido();
    if (!actual || this.ocupado()) {
      return;
    }
    this.limpiarMensajes();
    this.ocupado.set(true);

    peticion(actual.id).subscribe({
      next: (p) => this.listo(p, mensajeOk),
      error: (e: HttpErrorResponse) => this.fallo(e),
    });
  }

  private listo(pedido: Pedido, mensajeOk = ''): void {
    this.pedido.set(pedido);
    this.aviso.set(mensajeOk);
    this.ocupado.set(false);
  }

  private fallo(e: HttpErrorResponse): void {
    this.ocupado.set(false);
    this.error.set(this.textoError(e, 'No se pudo completar la acción. Revisa tu conexión e inténtalo de nuevo.'));
  }

  private limpiarMensajes(): void {
    this.error.set('');
    this.aviso.set('');
  }

  private textoError(e: HttpErrorResponse, porDefecto: string): string {
    if (e.error?.mensaje) {
      return e.error.mensaje;
    }
    return e.status === 403 ? 'Tu rol no tiene permiso para hacer esto.' : porDefecto;
  }

  // ---------------------------------------------------------------------- notas

  protected abrirNota(item: ItemPedido): void {
    this.itemConNota.set(item);
    this.formNota.reset({ nota: item.nota ?? '' });
    this.dialogoNota().nativeElement.showModal();
  }

  protected guardarNota(): void {
    const item = this.itemConNota();
    if (!item) {
      return;
    }
    if (this.formNota.invalid) {
      this.formNota.markAllAsTouched();
      return;
    }

    const nota = this.formNota.getRawValue().nota.trim() || null;
    this.dialogoNota().nativeElement.close();
    this.modificar((id) => this.api.actualizar(id, item.id, { cantidad: item.cantidad, nota }));
  }

  protected cerrarNota(): void {
    this.dialogoNota().nativeElement.close();
  }

  // --------------------------------------------------------------------- cobro

  /** Botón "Cobrar": primero se confirma que la caja esté abierta y luego se abre la ventana de pago. */
  protected cobrar(): void {
    if (!this.puedeCobrar() || this.ocupado() || this.pendientes() > 0 || this.total() <= 0) {
      return;
    }
    this.limpiarMensajes();
    this.ocupado.set(true);

    this.cajaApi.estado().subscribe({
      next: (estado) => {
        this.ocupado.set(false);
        this.cajaCerrada.set(!estado.abierta);
        if (estado.abierta) {
          this.prepararCobro();
        }
      },
      error: (e: HttpErrorResponse) => this.fallo(e),
    });
  }

  private prepararCobro(): void {
    // Por defecto, toda la cuenta en efectivo; se puede cambiar el método o repartirla en varios.
    this.lineasPago.set([{ metodo: 'EFECTIVO', monto: this.total().toFixed(2) }]);
    this.efectivoRecibido.set('');
    this.errorCobro.set('');
    this.dialogoCobro().nativeElement.showModal();
  }

  protected cambiarMetodo(indice: number, valor: string): void {
    const metodo = METODOS.find((m) => m.valor === valor)?.valor;
    if (!metodo) {
      return;
    }
    this.lineasPago.update((lineas) => lineas.map((l, i) => (i === indice ? { ...l, metodo } : l)));
  }

  protected cambiarMonto(indice: number, texto: string): void {
    this.lineasPago.update((lineas) => lineas.map((l, i) => (i === indice ? { ...l, monto: texto } : l)));
  }

  /** Agrega otra forma de pago con lo que todavía falta, para repartir la cuenta (por ejemplo, Yape + efectivo). */
  protected agregarMetodo(): void {
    const usados = new Set(this.lineasPago().map((l) => l.metodo));
    const metodo = METODOS.find((m) => !usados.has(m.valor))?.valor;
    if (!metodo) {
      return;
    }
    const falta = this.faltante();
    this.lineasPago.update((lineas) => [...lineas, { metodo, monto: falta > 0 ? falta.toFixed(2) : '' }]);
  }

  protected quitarMetodo(indice: number): void {
    this.lineasPago.update((lineas) => lineas.filter((_, i) => i !== indice));
  }

  protected escribirRecibido(texto: string): void {
    this.efectivoRecibido.set(texto);
  }

  protected cancelarCobro(): void {
    this.dialogoCobro().nativeElement.close();
  }

  protected enviarCobro(evento: Event): void {
    evento.preventDefault();
    this.confirmarCobro();
  }

  private confirmarCobro(): void {
    const actual = this.pedido();
    if (!actual || this.cobrando() || !this.pagosValidos()) {
      return;
    }

    const pagos = this.lineasPago().map((l) => ({ metodo: l.metodo, monto: aNumero(l.monto) }));
    const vuelto = this.vuelto();
    this.errorCobro.set('');
    this.cobrando.set(true);

    this.cajaApi.cobrar(actual.id, pagos).subscribe({
      next: (cobro) => {
        this.cobrando.set(false);
        this.dialogoCobro().nativeElement.close();
        this.errorImpresion.set('');
        this.ticket.set({
          pedidoId: cobro.pedidoId,
          ticket: cobro.ticket,
          total: cobro.total,
          mesa: this.mesa()?.nombre ?? '',
          vuelto,
        });
        this.dialogoTicket().nativeElement.showModal();
      },
      error: (e: HttpErrorResponse) => {
        this.cobrando.set(false);
        this.errorCobro.set(
          this.textoError(e, 'No se pudo cobrar la cuenta. Revisa tu conexión e inténtalo de nuevo.'),
        );
      },
    });
  }

  /**
   * Botón "Imprimir ticket". Trae del servidor el ticket de la venta (con los datos del negocio) y abre la
   * impresión. El vuelto solo se conoce ahora, al cobrar, por eso se agrega aquí.
   */
  protected imprimir(): void {
    const t = this.ticket();
    if (!t || this.imprimiendo()) {
      return;
    }
    this.errorImpresion.set('');
    this.imprimiendo.set(true);

    this.cajaApi.ticket(t.pedidoId).subscribe({
      next: (datos) => {
        this.imprimiendo.set(false);
        imprimirTicket(datos, { vuelto: t.vuelto });
      },
      error: (e: HttpErrorResponse) => {
        this.imprimiendo.set(false);
        this.errorImpresion.set(
          this.textoError(e, 'No se pudo preparar el ticket. Revisa tu conexión e inténtalo de nuevo.'),
        );
      },
    });
  }

  /** Botón "Listo" del ticket. Al cerrarse la ventana se vuelve al mapa de mesas. */
  protected cerrarTicket(): void {
    this.dialogoTicket().nativeElement.close();
  }

  protected terminarTicket(): void {
    if (this.ticket()) {
      this.ticket.set(null);
      void this.router.navigateByUrl('/mesas');
    }
  }

  // ------------------------------------------------------------------- anular

  protected pedirAnularPedido(): void {
    this.confirmar({
      titulo: 'Anular la cuenta',
      mensaje: `Se anulará toda la cuenta de ${this.mesa()?.nombre ?? 'la mesa'} y la mesa quedará libre. Esto no se puede deshacer.`,
      boton: 'Sí, anular la cuenta',
      accion: () => {
        const actual = this.pedido();
        if (!actual) {
          return;
        }
        this.ocupado.set(true);
        this.api.anular(actual.id).subscribe({
          next: () => void this.router.navigateByUrl('/mesas'),
          error: (e: HttpErrorResponse) => this.fallo(e),
        });
      },
    });
  }

  protected pedirAnularItem(item: ItemPedido): void {
    this.confirmar({
      titulo: 'Anular producto',
      mensaje: `"${item.nombre}" ya estaba en cocina. Se quitará de la cuenta y no se cobrará.`,
      boton: 'Sí, anular el producto',
      accion: () => this.modificar((id) => this.api.anularItem(id, item.id), 'El producto se anuló.'),
    });
  }

  private confirmar(confirmacion: Confirmacion): void {
    this.confirmacion.set(confirmacion);
    this.dialogoConfirmar().nativeElement.showModal();
  }

  protected cancelarConfirmacion(): void {
    this.dialogoConfirmar().nativeElement.close();
  }

  protected aceptarConfirmacion(): void {
    const confirmacion = this.confirmacion();
    this.dialogoConfirmar().nativeElement.close();
    this.limpiarMensajes();
    confirmacion?.accion();
  }
}
