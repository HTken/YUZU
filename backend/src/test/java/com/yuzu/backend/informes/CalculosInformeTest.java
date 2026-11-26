package com.yuzu.backend.informes;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class CalculosInformeTest {

    // ----------------------------------------------------------------- total

    @Test
    void sumaElTotalDeTodasLasVentas() {
        List<VentaFila> filas = List.of(
                new VentaFila(LocalDateTime.now(), new BigDecimal("50.00")),
                new VentaFila(LocalDateTime.now(), new BigDecimal("30.25")));

        assertThat(CalculosInforme.total(filas)).isEqualByComparingTo("80.25");
    }

    @Test
    void sinVentasElTotalEsCero() {
        assertThat(CalculosInforme.total(List.of())).isEqualByComparingTo("0.00");
    }

    // ----------------------------------------------------------- ticketPromedio

    @Test
    void elTicketPromedioSeRedondeaHaciaArribaEnElEmpate() {
        assertThat(CalculosInforme.ticketPromedio(new BigDecimal("80.25"), 2)).isEqualByComparingTo("40.13");
    }

    @Test
    void sinVentasElTicketPromedioEsCero() {
        assertThat(CalculosInforme.ticketPromedio(new BigDecimal("80.25"), 0)).isEqualByComparingTo("0.00");
    }

    // ------------------------------------------------------------------ porDia

    @Test
    void agrupaLasVentasPorDiaDeLimaYRellenaLosDiasSinVentas() {
        PeriodoInforme periodo = new PeriodoInforme(LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 22));

        // 15:00 UTC del día 20 son las 10:00 en Lima (UTC-5): mismo día.
        VentaFila ventaDia20 = new VentaFila(LocalDateTime.of(2026, 11, 20, 15, 0), new BigDecimal("50.00"));
        // 02:00 UTC del día 22 son las 21:00 del día 21 en Lima: cuenta para el día 21, no el 22.
        VentaFila ventaDia21 = new VentaFila(LocalDateTime.of(2026, 11, 22, 2, 0), new BigDecimal("30.00"));
        // Fuera del periodo (antes de "desde"): se ignora, no debe romper nada.
        VentaFila fueraDelPeriodo = new VentaFila(LocalDateTime.of(2026, 11, 19, 12, 0), new BigDecimal("999.00"));

        List<VentaDiaDto> resultado = CalculosInforme.porDia(List.of(ventaDia20, ventaDia21, fueraDelPeriodo), periodo);

        assertThat(resultado).hasSize(3);
        assertThat(resultado.get(0).fecha()).isEqualTo("2026-11-20");
        assertThat(resultado.get(0).ventas()).isEqualTo(1);
        assertThat(resultado.get(0).total()).isEqualByComparingTo("50.00");

        assertThat(resultado.get(1).fecha()).isEqualTo("2026-11-21");
        assertThat(resultado.get(1).ventas()).isEqualTo(1);
        assertThat(resultado.get(1).total()).isEqualByComparingTo("30.00");

        // El día 22 no tuvo ventas pero igual aparece, en cero.
        assertThat(resultado.get(2).fecha()).isEqualTo("2026-11-22");
        assertThat(resultado.get(2).ventas()).isEqualTo(0);
        assertThat(resultado.get(2).total()).isEqualByComparingTo("0.00");
    }

    // ---------------------------------------------------------------- porMetodo

    @Test
    void ponePrimeroLosMetodosEnElOrdenIndicadoYLosQueFaltanEnCero() {
        List<MetodoFila> filas = List.of(
                new MetodoFila(PagoFijo.YAPE, new BigDecimal("40.00"), 2L),
                new MetodoFila(PagoFijo.EFECTIVO, new BigDecimal("100.00"), 5L));
        List<String> orden = List.of(PagoFijo.EFECTIVO, PagoFijo.YAPE, PagoFijo.PLIN, PagoFijo.TARJETA);

        List<MetodoPagoDto> resultado = CalculosInforme.porMetodo(filas, orden);

        assertThat(resultado).extracting(MetodoPagoDto::metodo)
                .containsExactly(PagoFijo.EFECTIVO, PagoFijo.YAPE, PagoFijo.PLIN, PagoFijo.TARJETA);
        assertThat(resultado.get(2).cuentas()).isZero();
        assertThat(resultado.get(2).total()).isEqualByComparingTo("0.00");
    }

    @Test
    void unMetodoQueNoEstaEnElOrdenVaAlFinalOrdenadoAlfabeticamente() {
        List<MetodoFila> filas = List.of(new MetodoFila("BONO", new BigDecimal("10.00"), 1L));
        List<String> orden = List.of(PagoFijo.EFECTIVO);

        List<MetodoPagoDto> resultado = CalculosInforme.porMetodo(filas, orden);

        assertThat(resultado).extracting(MetodoPagoDto::metodo).containsExactly(PagoFijo.EFECTIVO, "BONO");
    }

    // ---------------------------------------------------------------- productos

    @Test
    void ordenaLosProductosPorCantidadLuegoImporteLuegoNombre() {
        ProductoFila chaufa = new ProductoFila(1L, "Chaufa", "Platos", 10L, new BigDecimal("100.00"));
        ProductoFila lomo = new ProductoFila(2L, "Lomo saltado", "Platos", 10L, new BigDecimal("150.00"));
        ProductoFila arroz = new ProductoFila(3L, "arroz con pollo", "Platos", 10L, new BigDecimal("150.00"));
        ProductoFila gaseosa = new ProductoFila(4L, "Gaseosa", "Bebidas", 5L, new BigDecimal("50.00"));

        List<ProductoVendidoDto> resultado = CalculosInforme.productos(List.of(chaufa, lomo, arroz, gaseosa));

        assertThat(resultado).extracting(ProductoVendidoDto::productoId).containsExactly(3L, 2L, 1L, 4L);
    }

    // ----------------------------------------------------------------- insumos

    @Test
    void calculaLoConsumidoLaMermaYLasEntradasPorInsumo() {
        List<MovimientoFila> filas = List.of(
                new MovimientoFila(100L, "VENTA", new BigDecimal("-5.000"), new BigDecimal("1.000"), null),
                new MovimientoFila(100L, "VENTA", new BigDecimal("-3.000"), null, null),
                new MovimientoFila(100L, "DEVOLUCION", new BigDecimal("2.000"), null, null),
                new MovimientoFila(101L, "MERMA", new BigDecimal("-1.500"), null, null),
                new MovimientoFila(101L, "ENTRADA", new BigDecimal("10.000"), null, new BigDecimal("25.50")),
                new MovimientoFila(102L, "ENTRADA", new BigDecimal("4.000"), null, null),
                new MovimientoFila(102L, "SALIDA", new BigDecimal("-2.000"), null, null),
                new MovimientoFila(103L, "AJUSTE", new BigDecimal("1.000"), null, null));

        Map<Long, InsumoInfo> info = Map.of(
                100L, new InsumoInfo("Arroz", "KG"),
                101L, new InsumoInfo("Pollo", "KG"));

        List<InsumoPeriodoDto> resultado = CalculosInforme.insumos(filas, info);

        assertThat(resultado).extracting(InsumoPeriodoDto::insumoId).containsExactly(100L, 101L, 102L);

        InsumoPeriodoDto arroz = resultado.get(0);
        assertThat(arroz.nombre()).isEqualTo("Arroz");
        assertThat(arroz.consumo()).isEqualByComparingTo("6.000");
        assertThat(arroz.faltante()).isEqualByComparingTo("1.000");

        InsumoPeriodoDto pollo = resultado.get(1);
        assertThat(pollo.merma()).isEqualByComparingTo("1.500");
        assertThat(pollo.entradas()).isEqualByComparingTo("10.000");
        assertThat(pollo.compras()).isEqualByComparingTo("25.50");

        // Sin info en el mapa: se usa un nombre de respaldo y la unidad por defecto.
        InsumoPeriodoDto sinInfo = resultado.get(2);
        assertThat(sinInfo.nombre()).isEqualTo("Insumo 102");
        assertThat(sinInfo.unidad()).isEqualTo("UNIDAD");
        assertThat(sinInfo.entradas()).isEqualByComparingTo("4.000");
    }

    @Test
    void unInsumoSoloConSalidasOAjustesNoApareceEnElInforme() {
        List<MovimientoFila> filas = List.of(
                new MovimientoFila(200L, "SALIDA", new BigDecimal("-1.000"), null, null),
                new MovimientoFila(200L, "AJUSTE", new BigDecimal("3.000"), null, null));

        List<InsumoPeriodoDto> resultado = CalculosInforme.insumos(filas, Map.of());

        assertThat(resultado).isEmpty();
    }

    @Test
    void comprasSumaLoQueCostaronLasEntradas() {
        List<InsumoPeriodoDto> insumos = List.of(
                new InsumoPeriodoDto(1L, "Arroz", "KG", BigDecimal.ZERO, BigDecimal.ZERO,
                        new BigDecimal("10.000"), new BigDecimal("25.50"), BigDecimal.ZERO),
                new InsumoPeriodoDto(2L, "Pollo", "KG", BigDecimal.ZERO, BigDecimal.ZERO,
                        new BigDecimal("5.000"), new BigDecimal("40.00"), BigDecimal.ZERO));

        assertThat(CalculosInforme.compras(insumos)).isEqualByComparingTo("65.50");
    }

    @Test
    void conFaltanteCuentaSoloLosInsumosConFaltantePositivo() {
        List<InsumoPeriodoDto> insumos = List.of(
                new InsumoPeriodoDto(1L, "Arroz", "KG", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, new BigDecimal("1.000")),
                new InsumoPeriodoDto(2L, "Pollo", "KG", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO));

        assertThat(CalculosInforme.conFaltante(insumos)).isEqualTo(1);
    }

    /** Los códigos de método de pago, sin depender del módulo de caja (solo para no escribir cadenas sueltas). */
    private static final class PagoFijo {
        static final String EFECTIVO = "EFECTIVO";
        static final String YAPE = "YAPE";
        static final String PLIN = "PLIN";
        static final String TARJETA = "TARJETA";
    }
}
