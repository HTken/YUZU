package com.yuzu.backend.caja;

import java.math.BigDecimal;
import java.util.List;

/**
 * Todo lo que pasó en un turno. El efectivo esperado es lo que debería haber en el cajón:
 * monto inicial + ventas en efectivo + ingresos - egresos. Las ventas anuladas no suman en nada de esto.
 */
public record ResumenTurnoDto(
        TurnoDto turno,
        int ventas,
        BigDecimal totalVentas,
        List<TotalMetodoDto> porMetodo,
        BigDecimal ingresos,
        BigDecimal egresos,
        BigDecimal efectivoEsperado,
        int cuentasAbiertas,
        List<VentaDto> detalleVentas,
        List<VentaAnuladaDto> ventasAnuladas,
        List<MovimientoDto> movimientos) {
}
