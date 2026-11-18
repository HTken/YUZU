export function formatearFecha(iso: string): string {
  const fecha = new Date(`${iso}T00:00:00`);
  return new Intl.DateTimeFormat('es-PE', { day: 'numeric', month: 'short', year: 'numeric' }).format(
    fecha,
  );
}

export function textoVencimiento(dias: number): string {
  if (dias < 0) {
    const atras = -dias;
    return `Venció hace ${atras} ${atras === 1 ? 'día' : 'días'}`;
  }
  if (dias === 0) {
    return 'Vence hoy';
  }
  if (dias === 1) {
    return 'Vence mañana';
  }
  return `Vence en ${dias} días`;
}

export function etiquetaEstado(estado: string): string {
  switch (estado) {
    case 'ACTIVO':
      return 'Activo';
    case 'EN_GRACIA':
      return 'En gracia';
    case 'SUSPENDIDO':
      return 'Suspendido';
    default:
      return estado;
  }
}
