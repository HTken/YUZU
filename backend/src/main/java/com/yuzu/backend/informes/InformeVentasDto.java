package com.yuzu.backend.informes;

import java.math.BigDecimal;
import java.util.List;

/**
 * Las ventas de un periodo. "ventas" son las cuentas cobradas y no anuladas; "cuentasAnuladas" las que se
 * anularon sin cobrarse; "ventasAnuladas" las que se cobraron y luego se anularon (no suman en el total).
 * Todo se cuenta por día de Lima.
 */
public record InformeVentasDto(
        String desde,
        String hasta,
        int dias,
        int ventas,
        BigDecimal total,
        BigDecimal ticketPromedio,
        int cuentasAnuladas,
        int ventasAnuladas,
        List<VentaDiaDto> porDia,
        List<MetodoPagoDto> porMetodo,
        List<ProductoVendidoDto> productos) {
}
