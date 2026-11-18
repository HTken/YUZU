import { Injectable } from '@angular/core';

interface CulqiInstancia {
  open(): void;
  close(): void;
  token?: { id: string; email?: string };
  error?: { user_message?: string; merchant_message?: string };
  culqi?: () => void;
}

declare global {
  interface Window {
    CulqiCheckout?: new (publicKey: string, config: object) => CulqiInstancia;
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
        paymentMethods: { tarjeta: true, yape: true },
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
