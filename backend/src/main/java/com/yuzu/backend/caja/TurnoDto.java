package com.yuzu.backend.caja;

import java.math.BigDecimal;

import com.yuzu.backend.config.Tiempo;

/** Un turno de caja. Las fechas van en formato 2026-11-18T07:05:12Z (hora UTC). */
public record TurnoDto(
        Long id,
        String estado,
        String abiertaPor,
        String abiertaEn,
        BigDecimal montoInicial,
        String cerradaPor,
        String cerradaEn,
        BigDecimal totalVentas,
        Integer numVentas,
        BigDecimal efectivoEsperado,
        BigDecimal efectivoContado,
        BigDecimal diferencia,
        String notaCierre) {

    public static TurnoDto desde(Turno t) {
        return new TurnoDto(t.getId(), t.getEstado(), t.getAbiertaPor(), Tiempo.iso(t.getAbiertaEn()),
                t.getMontoInicial(), t.getCerradaPor(), Tiempo.iso(t.getCerradaEn()), t.getTotalVentas(),
                t.getNumVentas(), t.getEfectivoEsperado(), t.getEfectivoContado(), t.getDiferencia(),
                t.getNotaCierre());
    }
}
