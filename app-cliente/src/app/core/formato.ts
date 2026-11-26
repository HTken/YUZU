/** Monto en soles, por ejemplo "S/ 45.00". */
export function soles(monto: number): string {
  return `S/ ${monto.toFixed(2)}`;
}

/** Hace cuánto pasó algo, por ejemplo "hace 12 min" o "hace 1 h 5 min". */
export function textoTiempo(iso: string, ahora: number = Date.now()): string {
  const minutos = Math.max(0, Math.floor((ahora - new Date(iso).getTime()) / 60000));
  if (minutos < 1) {
    return 'recién abierta';
  }
  if (minutos < 60) {
    return `hace ${minutos} min`;
  }
  const horas = Math.floor(minutos / 60);
  const resto = minutos % 60;
  return resto === 0 ? `hace ${horas} h` : `hace ${horas} h ${resto} min`;
}

/** Para buscar sin importar mayúsculas ni tildes: "Ají" y "aji" son lo mismo. */
export function normalizar(texto: string): string {
  return texto
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
    .trim();
}

/** Texto de un monto escrito por una persona ("12", "12.5", "12,50") a número. Devuelve NaN si no es válido. */
export function aNumero(texto: string): number {
  const limpio = texto.trim().replace(',', '.');
  return limpio === '' ? NaN : Number(limpio);
}

/** Un monto válido para dinero: hasta 8 enteros y 2 decimales, con punto o coma. */
export const PATRON_MONTO = /^\d{1,8}([.,]\d{1,2})?$/;

/** Para comparar dinero sin errores de decimales: 12.30 → 1230. */
export function centavos(monto: number): number {
  return Math.round(monto * 100);
}

const ZONA = 'America/Lima';

/** "18/11, 14:05" en hora de Lima. */
export function fechaHora(iso: string): string {
  return new Date(iso).toLocaleString('es-PE', {
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
    timeZone: ZONA,
  });
}

/** "14:05" en hora de Lima. */
export function hora(iso: string): string {
  return new Date(iso).toLocaleTimeString('es-PE', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
    timeZone: ZONA,
  });
}

/** Número de ticket con ceros a la izquierda: 12 → "000012". */
export function ticketNumero(numero: number): string {
  return String(numero).padStart(6, '0');
}
