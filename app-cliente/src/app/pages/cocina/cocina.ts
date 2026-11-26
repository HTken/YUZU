import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { Observable } from 'rxjs';

import { AnuladoCocina, Cocina as EstadoCocina, CocinaApi, ListoCocina, TicketCocina } from '../../core/cocina.api';
import { hora } from '../../core/formato';

type Prioridad = 'normal' | 'atencion' | 'urgente';

/** Desde cuántos minutos esperando un ticket se pinta de amarillo y de rojo. */
const MINUTOS_ATENCION = 10;
const MINUTOS_URGENTE = 20;

/** Pantalla de cocina: los pedidos por preparar, del más antiguo al más nuevo. */
@Component({
  selector: 'app-cocina',
  templateUrl: './cocina.html',
  styleUrl: './cocina.css',
})
export class Cocina implements OnInit {
  private readonly api = inject(CocinaApi);
  private readonly destruccion = inject(DestroyRef);

  protected readonly hora = hora;

  protected readonly pendientes = signal<TicketCocina[]>([]);
  protected readonly listos = signal<ListoCocina[]>([]);
  protected readonly anulados = signal<AnuladoCocina[]>([]);
  protected readonly cargando = signal(true);
  protected readonly errorCarga = signal('');
  protected readonly ocupado = signal(false);
  protected readonly error = signal('');

  /** Se actualiza con cada recarga para que los minutos de espera avancen solos. */
  protected readonly ahora = signal(Date.now());

  /** Tickets que llegaron en la última actualización, para resaltarlos. */
  protected readonly nuevos = signal<ReadonlySet<string>>(new Set());

  protected readonly sonido = signal(false);
  private audio: AudioContext | null = null;

  /** null hasta la primera carga: lo que ya estaba al abrir la pantalla no se considera "nuevo". */
  private conocidos: Set<string> | null = null;
  private anuladosConocidos: Set<number> | null = null;

  protected readonly totalPlatos = computed(() =>
    this.pendientes().reduce((suma, t) => suma + t.items.reduce((s, i) => s + i.cantidad, 0), 0),
  );

  ngOnInit(): void {
    this.cargar();

    // Los mozos envían pedidos en cualquier momento: la pantalla se refresca sola cada 8 segundos.
    const reloj = setInterval(() => {
      if (!this.ocupado()) {
        this.cargar(true);
      }
    }, 8000);
    this.destruccion.onDestroy(() => {
      clearInterval(reloj);
      void this.audio?.close();
    });
  }

  protected cargar(silencioso = false): void {
    if (!silencioso) {
      this.cargando.set(true);
      this.errorCarga.set('');
    }

    this.api.estado().subscribe({
      next: (estado) => {
        this.aplicar(estado);
        this.cargando.set(false);
      },
      error: (e: HttpErrorResponse) => {
        if (!silencioso) {
          this.cargando.set(false);
          this.errorCarga.set(
            this.textoError(e, 'No se pudo cargar la cocina. Revisa tu conexión e inténtalo de nuevo.'),
          );
        }
      },
    });
  }

  private aplicar(estado: EstadoCocina): void {
    const claves = new Set(estado.pendientes.map((t) => t.clave));
    const llegaron = new Set<string>();
    if (this.conocidos !== null) {
      for (const clave of claves) {
        if (!this.conocidos.has(clave)) {
          llegaron.add(clave);
        }
      }
    }
    this.conocidos = claves;

    // Avisos de plato anulado que no se habían visto antes en esta pantalla.
    const idsAnulados = new Set(estado.anulados.map((a) => a.id));
    const anuladosNuevos =
      this.anuladosConocidos !== null && [...idsAnulados].some((id) => !this.anuladosConocidos?.has(id));
    this.anuladosConocidos = idsAnulados;

    this.pendientes.set(estado.pendientes);
    this.listos.set(estado.listos);
    this.anulados.set(estado.anulados);
    this.nuevos.set(llegaron);
    this.ahora.set(Date.now());

    if (this.sonido()) {
      if (anuladosNuevos) {
        this.pitar(true);
      } else if (llegaron.size > 0) {
        this.pitar();
      }
    }
  }

  // ------------------------------------------------------------------ acciones

  /** Marca uno o varios platos como listos. */
  protected listo(ids: number[]): void {
    this.enviar(this.api.listo(ids));
  }

  /** Cocina confirma que vio el aviso de plato anulado. */
  protected entendido(ids: number[]): void {
    this.enviar(this.api.anuladosVistos(ids));
  }

  protected idsDeAnulados(): number[] {
    return this.anulados().map((a) => a.id);
  }

  /** Deshace un "listo" por error. */
  protected regresar(id: number): void {
    this.enviar(this.api.regresar([id]));
  }

  private enviar(peticion: Observable<EstadoCocina>): void {
    if (this.ocupado()) {
      return;
    }
    this.error.set('');
    this.ocupado.set(true);

    peticion.subscribe({
      next: (estado) => {
        // Lo que ya estaba en pantalla no vuelve a sonar como nuevo.
        this.conocidos = new Set(estado.pendientes.map((t) => t.clave));
        this.anuladosConocidos = new Set(estado.anulados.map((a) => a.id));
        this.pendientes.set(estado.pendientes);
        this.listos.set(estado.listos);
        this.anulados.set(estado.anulados);
        this.nuevos.set(new Set());
        this.ahora.set(Date.now());
        this.ocupado.set(false);
      },
      error: (e: HttpErrorResponse) => {
        this.ocupado.set(false);
        this.error.set(this.textoError(e, 'No se pudo completar la acción. Revisa tu conexión e inténtalo de nuevo.'));
      },
    });
  }

  private textoError(e: HttpErrorResponse, porDefecto: string): string {
    if (e.error?.mensaje) {
      return e.error.mensaje;
    }
    return e.status === 403
      ? 'Tu rol no tiene acceso a la pantalla de cocina. Solo el administrador y el personal de cocina la usan.'
      : porDefecto;
  }

  // ------------------------------------------------------------------- tiempos

  protected minutos(iso: string): number {
    return Math.max(0, Math.floor((this.ahora() - new Date(iso).getTime()) / 60000));
  }

  protected espera(iso: string): string {
    const minutos = this.minutos(iso);
    if (minutos < 1) {
      return 'Recién llegó';
    }
    if (minutos < 60) {
      return `${minutos} min`;
    }
    const horas = Math.floor(minutos / 60);
    return `${horas} h ${minutos % 60} min`;
  }

  protected prioridad(iso: string): Prioridad {
    const minutos = this.minutos(iso);
    if (minutos >= MINUTOS_URGENTE) {
      return 'urgente';
    }
    return minutos >= MINUTOS_ATENCION ? 'atencion' : 'normal';
  }

  protected haceCuanto(iso: string): string {
    const minutos = this.minutos(iso);
    return minutos < 1 ? 'hace un momento' : `hace ${minutos} min`;
  }

  protected idsDelTicket(ticket: TicketCocina): number[] {
    return ticket.items.map((i) => i.id);
  }

  // -------------------------------------------------------------------- sonido

  /** El navegador solo deja sonar el audio después de que la persona toca algo; por eso es un botón. */
  protected alternarSonido(): void {
    const activar = !this.sonido();
    this.sonido.set(activar);
    if (activar) {
      this.pitar();
    }
  }

  /** Un pitido para pedidos nuevos; dos pitidos más graves para un plato anulado. */
  private pitar(anulado = false): void {
    try {
      this.audio ??= new AudioContext();
      const contexto = this.audio;
      if (contexto.state === 'suspended') {
        void contexto.resume();
      }
      const oscilador = contexto.createOscillator();
      const volumen = contexto.createGain();
      oscilador.type = 'sine';
      oscilador.frequency.value = anulado ? 440 : 880;
      volumen.gain.value = 0.2;
      oscilador.connect(volumen);
      volumen.connect(contexto.destination);
      const inicio = contexto.currentTime;
      oscilador.start(inicio);
      oscilador.stop(inicio + 0.3);

      if (anulado) {
        const segundo = contexto.createOscillator();
        segundo.type = 'sine';
        segundo.frequency.value = 440;
        segundo.connect(volumen);
        segundo.start(inicio + 0.45);
        segundo.stop(inicio + 0.75);
      }
    } catch {
      // Sin audio disponible: la pantalla funciona igual, solo que sin aviso sonoro.
    }
  }
}
