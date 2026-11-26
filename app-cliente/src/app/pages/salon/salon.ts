import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { textoTiempo, soles } from '../../core/formato';
import { Mesa, MesasApi, Salon } from '../../core/mesas.api';
import { PedidoResumen, PedidosApi } from '../../core/pedidos.api';

interface MesaEnSalon {
  mesa: Mesa;
  /** La cuenta abierta de la mesa, o null si está libre. */
  pedido: PedidoResumen | null;
}

interface GrupoSalon {
  salon: Salon;
  mesas: MesaEnSalon[];
}

/** Mapa de mesas: cuáles están libres, cuáles ocupadas y cuánto lleva cada cuenta. */
@Component({
  selector: 'app-salon-mesas',
  imports: [RouterLink],
  templateUrl: './salon.html',
  styleUrl: './salon.css',
})
export class SalonMesas implements OnInit {
  private readonly mesasApi = inject(MesasApi);
  private readonly pedidosApi = inject(PedidosApi);
  private readonly destruccion = inject(DestroyRef);

  protected readonly soles = soles;

  protected readonly salones = signal<Salon[]>([]);
  protected readonly mesas = signal<Mesa[]>([]);
  protected readonly abiertos = signal<PedidoResumen[]>([]);
  protected readonly cargando = signal(true);
  protected readonly errorLista = signal('');

  /** Se actualiza con cada recarga para que "hace 12 min" avance solo. */
  protected readonly ahora = signal(Date.now());

  protected readonly grupos = computed<GrupoSalon[]>(() => {
    const porMesa = new Map(this.abiertos().map((p) => [p.mesaId, p]));
    return this.salones()
      .filter((s) => s.activo)
      .map((salon) => ({
        salon,
        mesas: this.mesas()
          .filter((m) => m.activo && m.salonId === salon.id)
          .map((mesa) => ({ mesa, pedido: porMesa.get(mesa.id) ?? null })),
      }))
      .filter((g) => g.mesas.length > 0);
  });

  protected readonly ocupadas = computed(() =>
    this.grupos().reduce((suma, g) => suma + g.mesas.filter((m) => m.pedido !== null).length, 0),
  );

  protected readonly libres = computed(() =>
    this.grupos().reduce((suma, g) => suma + g.mesas.filter((m) => m.pedido === null).length, 0),
  );

  ngOnInit(): void {
    this.cargar();

    // Otros mozos también toman pedidos: el mapa se refresca solo cada 15 segundos.
    const reloj = setInterval(() => this.cargar(true), 15000);
    this.destruccion.onDestroy(() => clearInterval(reloj));
  }

  protected tiempo(p: PedidoResumen): string {
    return textoTiempo(p.abiertoEn, this.ahora());
  }

  protected etiquetaMesa(m: MesaEnSalon): string {
    const estado = m.pedido ? 'ocupada' : 'libre';
    return `${m.mesa.nombre}, ${estado}`;
  }

  /** Con silencioso = true no se muestra "Cargando…" y, si falla, se conserva lo que ya había. */
  protected cargar(silencioso = false): void {
    if (!silencioso) {
      this.cargando.set(true);
      this.errorLista.set('');
    }

    forkJoin({
      salones: this.mesasApi.salones(),
      mesas: this.mesasApi.mesas(),
      abiertos: this.pedidosApi.abiertos(),
    }).subscribe({
      next: ({ salones, mesas, abiertos }) => {
        this.salones.set(salones);
        this.mesas.set(mesas);
        this.abiertos.set(abiertos);
        this.ahora.set(Date.now());
        this.cargando.set(false);
        this.errorLista.set('');
      },
      error: (e: HttpErrorResponse) => {
        this.cargando.set(false);
        if (silencioso) {
          return;
        }
        this.errorLista.set(
          e.error?.mensaje ??
            (e.status === 403
              ? 'Tu rol no tiene acceso a la toma de pedidos.'
              : 'No se pudo cargar el mapa de mesas. Revisa tu conexión e inténtalo de nuevo.'),
        );
      },
    });
  }
}
