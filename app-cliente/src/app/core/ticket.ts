import { TicketVenta, etiquetaMetodo } from './caja.api';
import { AnchoTicket, Negocio } from './negocio.api';
import { ticketNumero } from './formato';

/** Lo que cambia entre el ticket que se imprime al cobrar y los demás. */
export interface OpcionesTicket {
  /** Vuelto entregado al cobrar. Solo se conoce en el momento del cobro: el servidor no lo guarda. */
  vuelto?: number | null;
  /** Ticket de prueba de Configuración: lleva un letrero para que nadie lo confunda con una venta. */
  prueba?: boolean;
  /** Reimpresión desde Caja: lleva un letrero para distinguirla del ticket original. */
  copia?: boolean;
}

const ZONA = 'America/Lima';

/** Un texto escrito por una persona (nombre de plato, nota, dirección) listo para ir dentro de HTML. */
export function escaparHtml(texto: string): string {
  return texto
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

/** "21/11/2026 19:30" en hora de Lima. Texto vacío si la fecha no es válida. */
export function fechaTicket(iso: string): string {
  const fecha = new Date(iso);
  if (Number.isNaN(fecha.getTime())) {
    return '';
  }
  const partes = new Intl.DateTimeFormat('es-PE', {
    timeZone: ZONA,
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  }).formatToParts(fecha);
  const dato = (tipo: string): string => partes.find((p) => p.type === tipo)?.value ?? '';
  return `${dato('day')}/${dato('month')}/${dato('year')} ${dato('hour')}:${dato('minute')}`;
}

/** Ancho del papel de un ticket: 58 o 80 mm. Cualquier otro valor (un dato dañado) se trata como 80. */
function anchoPapel(negocio: Negocio): AnchoTicket {
  return negocio.anchoTicketMm === 58 ? 58 : 80;
}

/** Alto de la hoja en mm para un contenido de tantos píxeles, con 3 mm de margen para que la última línea no pase a otra hoja. */
export function altoPaginaMm(altoPx: number): number {
  return Math.ceil((altoPx * 25.4) / 96) + 3;
}

function monto(valor: number): string {
  return valor.toFixed(2);
}

/** Medidas según el papel: lo que la impresora realmente imprime es unos 72 mm en el de 80 y unos 48 mm en el de 58. */
function medidas(ancho: AnchoTicket): { lado: number; letra: number } {
  return ancho === 58 ? { lado: 5, letra: 11 } : { lado: 4, letra: 12.5 };
}

function estilos(ancho: AnchoTicket): string {
  const { lado, letra } = medidas(ancho);
  return `
@page { margin: 0; }
* { box-sizing: border-box; }
html, body { margin: 0; padding: 0; background: #fff; color: #000; }
body { width: ${ancho}mm; padding: 4mm ${lado}mm 8mm; font-family: Arial, Helvetica, sans-serif; font-size: ${letra}px; line-height: 1.35; }
.centro { text-align: center; }
.nombre { font-size: 1.45em; font-weight: 700; line-height: 1.2; overflow-wrap: anywhere; }
.dato { overflow-wrap: anywhere; }
.prueba { margin-bottom: 8px; padding: 3px 0; border: 1.5px solid #000; text-align: center; font-weight: 700; letter-spacing: 0.04em; }
.raya { margin: 7px 0; border: 0; border-top: 1px dashed #000; }
.fuerte { font-weight: 700; }
table { width: 100%; border-collapse: collapse; }
th { padding: 0 0 3px; font-size: 0.9em; font-weight: 700; text-align: left; }
td { padding: 2px 0; vertical-align: top; }
.cant { width: 2.4em; }
.imp { padding-left: 6px; text-align: right; white-space: nowrap; }
th.imp { text-align: right; }
.plato { overflow-wrap: anywhere; }
.detalle { font-size: 0.88em; }
.fila { display: flex; justify-content: space-between; gap: 8px; }
.total { margin: 2px 0 4px; font-size: 1.4em; font-weight: 700; }
.pie { margin-top: 10px; text-align: center; overflow-wrap: anywhere; }
.aviso { margin-top: 6px; font-size: 0.82em; text-align: center; }
`;
}

function cabecera(negocio: Negocio): string {
  const filas: string[] = [`<div class="nombre">${escaparHtml(negocio.nombre)}</div>`];
  const razon = negocio.razonSocial?.trim();
  if (razon && razon.toLowerCase() !== negocio.nombre.trim().toLowerCase()) {
    filas.push(`<div class="dato">${escaparHtml(razon)}</div>`);
  }
  if (negocio.ruc) {
    filas.push(`<div class="dato">RUC ${escaparHtml(negocio.ruc)}</div>`);
  }
  if (negocio.direccion) {
    filas.push(`<div class="dato">${escaparHtml(negocio.direccion)}</div>`);
  }
  if (negocio.telefono) {
    filas.push(`<div class="dato">Tel. ${escaparHtml(negocio.telefono)}</div>`);
  }
  return `<div class="centro">${filas.join('')}</div>`;
}

function lineas(ticket: TicketVenta): string {
  const filas = ticket.lineas.map((l) => {
    const detalle: string[] = [];
    if (l.cantidad > 1) {
      detalle.push(`${l.cantidad} x ${monto(l.precioUnitario)}`);
    }
    if (l.nota) {
      detalle.push(escaparHtml(l.nota));
    }
    const extra = detalle.length > 0 ? `<div class="detalle">${detalle.join(' · ')}</div>` : '';
    return (
      `<tr><td class="cant">${l.cantidad}</td>` +
      `<td class="plato">${escaparHtml(l.nombre)}${extra}</td>` +
      `<td class="imp">${monto(l.importe)}</td></tr>`
    );
  });
  return (
    `<table><thead><tr><th class="cant">Cant.</th><th>Descripción</th><th class="imp">Importe</th></tr></thead>` +
    `<tbody>${filas.join('')}</tbody></table>`
  );
}

/**
 * El ticket de una venta como un documento HTML completo, listo para imprimir. Los textos que escribió una persona
 * (nombres de platos, notas, dirección) se escapan: nada de lo guardado puede meter HTML en lo que se imprime.
 * No es un comprobante electrónico de SUNAT y el ticket lo dice.
 */
export function construirTicketHtml(ticket: TicketVenta, opciones: OpcionesTicket = {}): string {
  const ancho = anchoPapel(ticket.negocio);
  const numero = ticketNumero(ticket.ticket);

  const partes: string[] = [];
  const rotulo = opciones.prueba ? 'TICKET DE PRUEBA' : opciones.copia ? 'REIMPRESIÓN' : '';
  if (rotulo) {
    partes.push(`<div class="prueba">${rotulo}</div>`);
  }
  partes.push(cabecera(ticket.negocio));
  partes.push('<hr class="raya">');

  const fecha = fechaTicket(ticket.cobradoEn);
  partes.push(`<div class="fuerte">Ticket N.° ${numero}</div>`);
  if (fecha) {
    partes.push(`<div>Fecha: ${fecha}</div>`);
  }
  partes.push(`<div class="dato">Mesa: ${escaparHtml(ticket.mesa)}</div>`);
  partes.push(`<div class="dato">Atendió: ${escaparHtml(ticket.mozo)}</div>`);
  partes.push(`<div class="dato">Cobró: ${escaparHtml(ticket.cobradoPor)}</div>`);
  partes.push('<hr class="raya">');

  partes.push(lineas(ticket));
  partes.push('<hr class="raya">');

  partes.push(`<div class="fila total"><span>TOTAL</span><span>S/ ${monto(ticket.total)}</span></div>`);
  for (const pago of ticket.pagos) {
    partes.push(`<div class="fila"><span>${escaparHtml(etiquetaMetodo(pago.metodo))}</span><span>S/ ${monto(pago.monto)}</span></div>`);
  }
  if (opciones.vuelto != null && opciones.vuelto > 0) {
    partes.push(`<div class="fila fuerte"><span>Vuelto</span><span>S/ ${monto(opciones.vuelto)}</span></div>`);
  }
  partes.push('<hr class="raya">');

  const mensaje = ticket.negocio.mensajeTicket?.trim();
  if (mensaje) {
    partes.push(`<div class="pie fuerte">${escaparHtml(mensaje)}</div>`);
  }
  partes.push('<div class="aviso">Ticket interno · no es comprobante de pago</div>');

  return (
    `<!doctype html><html lang="es"><head><meta charset="utf-8">` +
    `<title>Ticket ${numero}</title><style>${estilos(ancho)}</style></head>` +
    `<body>${partes.join('')}</body></html>`
  );
}

/** Un ticket de ejemplo con los datos del negocio tal como están escritos (aunque todavía no se hayan guardado). */
export function ticketDePrueba(negocio: Negocio): TicketVenta {
  return {
    negocio,
    ticket: 1,
    mesa: 'Mesa 1',
    mozo: 'Nombre del mozo',
    cobradoPor: 'Nombre del cajero',
    cobradoEn: new Date().toISOString(),
    lineas: [
      { nombre: 'Lomo saltado', cantidad: 1, precioUnitario: 32, importe: 32, nota: 'Sin cebolla' },
      { nombre: 'Chicha morada (jarra)', cantidad: 1, precioUnitario: 12, importe: 12, nota: null },
      { nombre: 'Inca Kola 500 ml', cantidad: 2, precioUnitario: 5, importe: 10, nota: null },
    ],
    total: 54,
    pagos: [{ metodo: 'EFECTIVO', monto: 54 }],
  };
}

/**
 * Abre el cuadro de impresión del navegador con el ticket. Se imprime desde un marco fuera de la pantalla para no
 * tocar donde se está trabajando; el marco se retira cuando termina la impresión. La hoja se ajusta al ancho del
 * papel y al largo justo del ticket, para que la impresora de rollo no deje papel en blanco.
 */
export function imprimirTicket(ticket: TicketVenta, opciones: OpcionesTicket = {}): void {
  const ancho = anchoPapel(ticket.negocio);
  const marco = document.createElement('iframe');
  marco.setAttribute('aria-hidden', 'true');
  marco.setAttribute('tabindex', '-1');
  marco.style.cssText = `position:fixed;left:-10000px;top:0;width:${ancho}mm;height:100px;border:0;`;

  const quitar = (): void => marco.remove();
  const respaldo = setTimeout(quitar, 5 * 60 * 1000);

  marco.onload = () => {
    const ventana = marco.contentWindow;
    if (!ventana) {
      clearTimeout(respaldo);
      quitar();
      return;
    }
    try {
      const documento = ventana.document;
      const hoja = documento.createElement('style');
      hoja.textContent = `@page { size: ${ancho}mm ${altoPaginaMm(documento.body.offsetHeight)}mm; margin: 0; }`;
      documento.head.appendChild(hoja);
    } catch {
      // Si no se puede medir, se imprime igual con la hoja que elija la impresora.
    }
    ventana.addEventListener('afterprint', () => {
      clearTimeout(respaldo);
      quitar();
    });
    ventana.focus();
    ventana.print();
  };

  marco.srcdoc = construirTicketHtml(ticket, opciones);
  document.body.appendChild(marco);
}
