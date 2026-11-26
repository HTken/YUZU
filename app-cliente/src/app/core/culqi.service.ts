import { Injectable } from '@angular/core';

interface CulqiInstancia {
  open(): void;
  close(): void;
  token?: { id: string; email?: string };
  error?: { user_message?: string; merchant_message?: string };
  culqi?: () => void;
}

export interface Parametros3DS {
  eci: string;
  xid: string;
  cavv: string;
  protocolVersion: string;
  directoryServerTransactionId: string;
}

interface Culqi3DSGlobal {
  publicKey: string;
  settings: { charge: { totalAmount: number; returnUrl: string; currency?: string }; card: { email: string } };
  generateDevice(): Promise<string | null>;
  initAuthentication(tokenId: string): Promise<void>;
  reset(): void;
}

declare global {
  interface Window {
    CulqiCheckout?: new (publicKey: string, config: object) => CulqiInstancia;
    Culqi3DS?: Culqi3DSGlobal;
  }
}

export interface TokenCulqi {
  id: string;
  email: string;
}

export interface OpcionesCheckout {
  publicKey: string;
  titulo: string;
  /** Monto en céntimos: S/ 129.00 se envía como 12900. */
  centimos: number;
  /** Para guardar una tarjeta (cobro automático): Yape no se puede guardar, así que no se muestra. */
  soloTarjeta?: boolean;
}

export interface RespuestaCheckout {
  onToken: (token: TokenCulqi) => void;
  onError: (mensaje: string) => void;
}

const URL_SCRIPT = 'https://js.culqi.com/checkout-js';

/** Abre el formulario de pago de Culqi. Los datos de la tarjeta nunca pasan por Yuzu. */
@Injectable({ providedIn: 'root' })
export class CulqiService {
  private carga: Promise<void> | null = null;

  async abrir(opciones: OpcionesCheckout, respuesta: RespuestaCheckout): Promise<void> {
    await this.cargarScript();

    const Checkout = window.CulqiCheckout;
    if (!Checkout) {
      throw new Error('No se pudo cargar el formulario de pago.');
    }

    const checkout = new Checkout(opciones.publicKey, {
      settings: {
        title: opciones.titulo,
        currency: 'PEN',
        amount: opciones.centimos,
      },
      options: {
        lang: 'es',
        modal: true,
        installments: false,
        paymentMethods: { tarjeta: true, yape: !opciones.soloTarjeta },
      },
      appearance: {
        theme: 'default',
        menuType: 'sidebar',
        buttonCardPayText: 'Pagar',
        defaultStyle: {
          bannerColor: '#14452f',
          buttonBackground: '#14452f',
          menuColor: '#14452f',
          linksColor: '#14452f',
          buttonTextColor: '#f5c400',
          priceColor: '#14452f',
        },
      },
    });

    checkout.culqi = () => {
      if (checkout.token) {
        const token: TokenCulqi = { id: checkout.token.id, email: checkout.token.email ?? '' };
        checkout.close();
        respuesta.onToken(token);
      } else if (checkout.error) {
        respuesta.onError(
          checkout.error.user_message ?? 'No se pudo procesar el pago. Inténtalo de nuevo.',
        );
      }
    };

    checkout.open();
  }

  private cargarScript(): Promise<void> {
    if (window.CulqiCheckout) {
      return Promise.resolve();
    }

    this.carga ??= new Promise<void>((resolve, reject) => {
      const script = document.createElement('script');
      script.src = URL_SCRIPT;
      script.async = true;
      script.onload = () =>
        window.CulqiCheckout ? resolve() : reject(new Error('El formulario de pago no se inició.'));
      script.onerror = () => {
        this.carga = null;
        reject(new Error('No se pudo cargar el formulario de pago. Revisa tu conexión.'));
      };
      document.head.appendChild(script);
    });

    return this.carga;
  }
}

export interface OpcionesAutenticacion3DS {
  publicKey: string;
  tokenId: string;
  email: string;
  /** Monto en céntimos del cargo que se está autenticando. */
  totalAmount: number;
}

const URL_SCRIPT_3DS = 'https://3ds.culqi.com';

/**
 * La autenticación 3D Secure (Culqi3DS): solo se usa cuando el backend responde que un cargo o una
 * tarjeta la necesitan. El banco muestra su propio formulario; esto solo recoge el resultado.
 */
@Injectable({ providedIn: 'root' })
export class Culqi3dsService {
  private carga: Promise<void> | null = null;

  /** El identificador del dispositivo, para mandarlo siempre junto con el cargo (puede fallar; no es obligatorio). */
  async generarDispositivo(publicKey: string): Promise<string | null> {
    await this.cargarScript();
    window.Culqi3DS!.publicKey = publicKey;
    try {
      return await window.Culqi3DS!.generateDevice();
    } catch {
      return null;
    }
  }

  /** Abre la autenticación del banco y espera su resultado. */
  async autenticar(opciones: OpcionesAutenticacion3DS): Promise<Parametros3DS> {
    await this.cargarScript();
    const culqi3ds = window.Culqi3DS!;
    culqi3ds.publicKey = opciones.publicKey;
    culqi3ds.settings = {
      charge: { totalAmount: opciones.totalAmount, returnUrl: window.location.href, currency: 'PEN' },
      card: { email: opciones.email },
    };

    return new Promise<Parametros3DS>((resolve, reject) => {
      const escuchar = (evento: MessageEvent) => {
        if (evento.origin !== window.location.origin) {
          return;
        }
        const respuesta = evento.data as { parameters3DS?: Parametros3DS; error?: string; loading?: boolean };
        if (respuesta.loading) {
          return;
        }
        if (respuesta.parameters3DS) {
          window.removeEventListener('message', escuchar);
          resolve(respuesta.parameters3DS);
        } else if (respuesta.error) {
          window.removeEventListener('message', escuchar);
          reject(new Error(respuesta.error));
        }
      };

      window.addEventListener('message', escuchar);
      culqi3ds.initAuthentication(opciones.tokenId).catch((e: unknown) => {
        window.removeEventListener('message', escuchar);
        reject(e instanceof Error ? e : new Error('No se pudo iniciar la autenticación 3D Secure.'));
      });
    });
  }

  /** Hay que llamarlo antes de un nuevo cargo: Culqi3DS solo admite configurarse una vez por cargo. */
  reiniciar(): void {
    window.Culqi3DS?.reset();
  }

  private cargarScript(): Promise<void> {
    if (window.Culqi3DS) {
      return Promise.resolve();
    }

    this.carga ??= new Promise<void>((resolve, reject) => {
      const script = document.createElement('script');
      script.src = URL_SCRIPT_3DS;
      script.defer = true;
      script.onload = () =>
        window.Culqi3DS ? resolve() : reject(new Error('No se pudo cargar la autenticación 3D Secure.'));
      script.onerror = () => {
        this.carga = null;
        reject(new Error('No se pudo cargar la autenticación 3D Secure. Revisa tu conexión.'));
      };
      document.head.appendChild(script);
    });

    return this.carga;
  }
}
