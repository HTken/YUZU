package com.yuzu.backend.informes;

import java.math.BigDecimal;
import java.util.List;

/** Los insumos del periodo: solo aparecen los que tuvieron consumo, merma, entradas o faltante. */
public record InformeInsumosDto(
        String desde,
        String hasta,
        BigDecimal compras,
        int insumosConFaltante,
        List<InsumoPeriodoDto> insumos) {
}
