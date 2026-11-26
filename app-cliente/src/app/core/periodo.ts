/**
 * Periodos de los informes. Las fechas viajan como texto AAAA-MM-DD y son días de Lima
 * (de 00:00 a 23:59), sin importar en qué zona esté el navegador.
 */

const ZONA = 'America/Lima';

/** Lo mismo que acepta el servidor: un año completo, bisiesto incluido. */
export const MAXIMO_DIAS = 366;
export const ANIO_MINIMO = 2020;
export const ANIO_MAXIMO = 2100;

export type PresetPeriodo = 'HOY' | 'AYER' | 'SIETE' | 'MES' | 'MES_PASADO' | 'PERSONALIZADO';

export const PRESETS: { valor: PresetPeriodo; etiqueta: string }[] = [
  { valor: 'HOY', etiqueta: 'Hoy' },
  { valor: 'AYER', etiqueta: 'Ayer' },
  { valor: 'SIETE', etiqueta: 'Últimos 7 días' },
  { valor: 'MES', etiqueta: 'Este mes' },
  { valor: 'MES_PASADO', etiqueta: 'Mes pasado' },
  { valor: 'PERSONALIZADO', etiqueta: 'Personalizado' },
];

export interface Rango {
  desde: string;
  hasta: string;
}

const PATRON_FECHA = /^(\d{4})-(\d{2})-(\d{2})$/;

/** Convierte "2026-11-18" en milisegundos (a las 00:00 UTC), o NaN si no es una fecha real (por ejemplo 2026-02-30). */
function aMilisegundos(fecha: string): number {
  const partes = PATRON_FECHA.exec(fecha);
  if (!partes) {
    return NaN;
  }
  const [anio, mes, dia] = [Number(partes[1]), Number(partes[2]), Number(partes[3])];
  const ms = Date.UTC(anio, mes - 1, dia);
  const control = new Date(ms);
  return control.getUTCFullYear() === anio && control.getUTCMonth() === mes - 1 && control.getUTCDate() === dia
    ? ms
    : NaN;
}

function aTexto(ms: number): string {
  return new Date(ms).toISOString().slice(0, 10);
}

export function esFechaValida(fecha: string): boolean {
  return !Number.isNaN(aMilisegundos(fecha));
}

/** El día de hoy en Lima, por ejemplo "2026-11-20". */
export function hoyLima(ahora: Date = new Date()): string {
  const partes = new Intl.DateTimeFormat('en-US', {
    timeZone: ZONA,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(ahora);
  const valor = (tipo: string): string => partes.find((p) => p.type === tipo)?.value ?? '';
  return `${valor('year')}-${valor('month')}-${valor('day')}`;
}

/** Una fecha más (o menos) días: sumarDias('2026-11-30', 1) → '2026-12-01'. */
export function sumarDias(fecha: string, dias: number): string {
  return aTexto(aMilisegundos(fecha) + dias * 86_400_000);
}

/** Cuántos días abarca el rango, contando el primero y el último. */
export function diasEnRango(desde: string, hasta: string): number {
  return Math.round((aMilisegundos(hasta) - aMilisegundos(desde)) / 86_400_000) + 1;
}

/** El rango que corresponde a un periodo rápido. "Personalizado" empieza siendo el día de hoy. */
export function rangoDe(preset: PresetPeriodo, hoy: string): Rango {
  switch (preset) {
    case 'AYER': {
      const ayer = sumarDias(hoy, -1);
      return { desde: ayer, hasta: ayer };
    }
    case 'SIETE':
      return { desde: sumarDias(hoy, -6), hasta: hoy };
    case 'MES':
      return { desde: `${hoy.slice(0, 8)}01`, hasta: hoy };
    case 'MES_PASADO': {
      const primeroDeEsteMes = `${hoy.slice(0, 8)}01`;
      const ultimoDelPasado = sumarDias(primeroDeEsteMes, -1);
      return { desde: `${ultimoDelPasado.slice(0, 8)}01`, hasta: ultimoDelPasado };
    }
    default:
      return { desde: hoy, hasta: hoy };
  }
}

/** Qué está mal en un rango escrito por una persona; vacío si es válido. Mismas reglas que el servidor. */
export function errorDeRango(desde: string, hasta: string): string {
  if (!esFechaValida(desde) || !esFechaValida(hasta)) {
    return 'Elige la fecha de inicio y la de fin.';
  }
  if (desde > hasta) {
    return 'La fecha de inicio no puede ser posterior a la de fin.';
  }
  if (Number(desde.slice(0, 4)) < ANIO_MINIMO || Number(hasta.slice(0, 4)) > ANIO_MAXIMO) {
    return 'Las fechas están fuera de lo permitido.';
  }
  if (diasEnRango(desde, hasta) > MAXIMO_DIAS) {
    return `El periodo puede abarcar como máximo ${MAXIMO_DIAS} días (un año).`;
  }
  return '';
}

/** "18/11/2026" */
export function fechaCorta(fecha: string): string {
  const [anio, mes, dia] = fecha.split('-');
  return `${dia}/${mes}/${anio}`;
}

/** "mié 18/11" */
export function etiquetaDia(fecha: string): string {
  const ms = aMilisegundos(fecha);
  if (Number.isNaN(ms)) {
    return fecha;
  }
  const dia = new Date(ms).toLocaleDateString('es-PE', { weekday: 'short', timeZone: 'UTC' }).replace('.', '');
  const [, mes, numero] = fecha.split('-');
  return `${dia} ${numero}/${mes}`;
}

/** "20/11/2026" o "Del 14/11/2026 al 20/11/2026 (7 días)" */
export function rotuloRango(desde: string, hasta: string): string {
  if (desde === hasta) {
    return fechaCorta(desde);
  }
  return `Del ${fechaCorta(desde)} al ${fechaCorta(hasta)} (${diasEnRango(desde, hasta)} días)`;
}
