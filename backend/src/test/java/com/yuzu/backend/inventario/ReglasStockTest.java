package com.yuzu.backend.inventario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ReglasStockTest {

    // -------------------------------------------------------------- cantidad

    @Test
    void redondeaATresDecimales() {
        BigDecimal valor = ReglasStock.cantidad(new BigDecimal("2.5"), Insumo.KG, "El stock");
        assertThat(valor).isEqualByComparingTo("2.500");
    }

    @Test
    void rechazaCantidadNegativa() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> ReglasStock.cantidad(new BigDecimal("-1"), Insumo.KG, "El stock"));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void rechazaCantidadQueNoCabeEnLaBaseDeDatos() {
        assertThrows(ResponseStatusException.class,
                () -> ReglasStock.cantidad(ReglasStock.MAXIMO.add(BigDecimal.ONE), Insumo.KG, "El stock"));
    }

    @Test
    void loQueSeCuentaPorUnidadNoAceptaDecimales() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> ReglasStock.cantidad(new BigDecimal("2.5"), Insumo.UNIDAD, "El stock"));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void loQueSeCuentaPorUnidadSiAceptaEnteros() {
        BigDecimal valor = ReglasStock.cantidad(new BigDecimal("4"), Insumo.UNIDAD, "El stock");
        assertThat(valor).isEqualByComparingTo("4");
    }

    // ----------------------------------------------------------------- cambio

    @Test
    void entradaSumaAlStock() {
        BigDecimal delta = ReglasStock.cambio(MovimientoInsumo.ENTRADA, new BigDecimal("10"), new BigDecimal("5"), Insumo.KG);
        assertThat(delta).isEqualByComparingTo("5");
    }

    @Test
    void salidaRestaDelStock() {
        BigDecimal delta = ReglasStock.cambio(MovimientoInsumo.SALIDA, new BigDecimal("10"), new BigDecimal("5"), Insumo.KG);
        assertThat(delta).isEqualByComparingTo("-5");
    }

    @Test
    void mermaRestaDelStock() {
        BigDecimal delta = ReglasStock.cambio(MovimientoInsumo.MERMA, new BigDecimal("10"), new BigDecimal("3"), Insumo.KG);
        assertThat(delta).isEqualByComparingTo("-3");
    }

    @Test
    void noSePuedeSacarMasStockDelQueHay() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> ReglasStock.cambio(MovimientoInsumo.SALIDA, new BigDecimal("10"), new BigDecimal("20"), Insumo.KG));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void entradaOSalidaDeCeroOMenosSeRechaza() {
        assertThrows(ResponseStatusException.class,
                () -> ReglasStock.cambio(MovimientoInsumo.ENTRADA, new BigDecimal("10"), BigDecimal.ZERO, Insumo.KG));
        assertThrows(ResponseStatusException.class,
                () -> ReglasStock.cambio(MovimientoInsumo.ENTRADA, new BigDecimal("10"), new BigDecimal("-1"), Insumo.KG));
    }

    @Test
    void ajusteCalculaLaDiferenciaConLoContado() {
        BigDecimal delta = ReglasStock.cambio(MovimientoInsumo.AJUSTE, new BigDecimal("10"), new BigDecimal("7"), Insumo.KG);
        assertThat(delta).isEqualByComparingTo("-3");

        BigDecimal delta2 = ReglasStock.cambio(MovimientoInsumo.AJUSTE, new BigDecimal("10"), new BigDecimal("15"), Insumo.KG);
        assertThat(delta2).isEqualByComparingTo("5");
    }

    @Test
    void ajusteSinCambioSeRechaza() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> ReglasStock.cambio(MovimientoInsumo.AJUSTE, new BigDecimal("10"), new BigDecimal("10"), Insumo.KG));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void elStockResultanteNoPuedePasarDelMaximo() {
        assertThrows(ResponseStatusException.class,
                () -> ReglasStock.cambio(MovimientoInsumo.ENTRADA, ReglasStock.MAXIMO, new BigDecimal("1"), Insumo.KG));
    }

    // ---------------------------------------------------------------- consumo

    @Test
    void siAlcanzaElStockSeDescuentaTodoYNoFaltaNada() {
        ReglasStock.Consumo consumo = ReglasStock.consumo(new BigDecimal("10"), new BigDecimal("4"));
        assertThat(consumo.descontado()).isEqualByComparingTo("4");
        assertThat(consumo.faltante()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void siNoAlcanzaElStockSeDescuentaLoQueHayYElRestoEsFaltante() {
        ReglasStock.Consumo consumo = ReglasStock.consumo(new BigDecimal("3"), new BigDecimal("10"));
        assertThat(consumo.descontado()).isEqualByComparingTo("3");
        assertThat(consumo.faltante()).isEqualByComparingTo("7");
    }

    @Test
    void laVentaNuncaDejaElStockNegativo() {
        ReglasStock.Consumo consumo = ReglasStock.consumo(BigDecimal.ZERO, new BigDecimal("5"));
        assertThat(consumo.descontado()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(consumo.faltante()).isEqualByComparingTo("5");
    }

    // ------------------------------------------------------------- devolucion

    @Test
    void devuelveTodoLoDescontadoSiCabe() {
        BigDecimal devuelto = ReglasStock.devolucion(new BigDecimal("10"), new BigDecimal("5"));
        assertThat(devuelto).isEqualByComparingTo("5");
    }

    @Test
    void noDevuelveMasDeLoQueCabeEnLaBaseDeDatos() {
        BigDecimal devuelto = ReglasStock.devolucion(ReglasStock.MAXIMO.subtract(new BigDecimal("2")), new BigDecimal("5"));
        assertThat(devuelto).isEqualByComparingTo("2");
    }

    @Test
    void laDevolucionNuncaEsNegativa() {
        BigDecimal devuelto = ReglasStock.devolucion(ReglasStock.MAXIMO.add(new BigDecimal("3")), new BigDecimal("5"));
        assertThat(devuelto).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // --------------------------------------------------- descontadoPorInsumo

    @Test
    void sumaLoDescontadoDeLasVentasPorInsumo() {
        MovimientoInsumo venta1 = movimientoVenta(1L, "-2.000");
        MovimientoInsumo venta2 = movimientoVenta(1L, "-3.000");
        MovimientoInsumo venta3 = movimientoVenta(2L, "-1.000");

        Map<Long, BigDecimal> descontado = ReglasStock.descontadoPorInsumo(List.of(venta1, venta2, venta3));

        assertThat(descontado.get(1L)).isEqualByComparingTo("5.000");
        assertThat(descontado.get(2L)).isEqualByComparingTo("1.000");
    }

    @Test
    void ignoraMovimientosQueNoDescontaronNada() {
        MovimientoInsumo sinDescuento = movimientoVenta(1L, "0.000");
        Map<Long, BigDecimal> descontado = ReglasStock.descontadoPorInsumo(List.of(sinDescuento));
        assertThat(descontado).isEmpty();
    }

    // --------------------------------------------------- requeridoPorInsumo

    @Test
    void calculaLoQueNecesitaCadaInsumoSegunLoVendido() {
        RecetaLinea pollo = recetaLinea(10L, 100L, "0.200");
        RecetaLinea arroz = recetaLinea(10L, 101L, "0.150");
        RecetaLinea pollo2 = recetaLinea(11L, 100L, "0.300");

        Map<Long, Integer> vendidos = Map.of(10L, 3, 11L, 2);
        Map<Long, BigDecimal> requerido = ReglasStock.requeridoPorInsumo(vendidos, List.of(pollo, arroz, pollo2));

        // producto 10: 3 unidades x 0.200 = 0.600; producto 11: 2 unidades x 0.300 = 0.600 -> insumo 100 = 1.200
        assertThat(requerido.get(100L)).isEqualByComparingTo("1.200");
        assertThat(requerido.get(101L)).isEqualByComparingTo("0.450");
    }

    @Test
    void ignoraProductosQueNoSeVendieron() {
        RecetaLinea linea = recetaLinea(99L, 100L, "0.200");
        Map<Long, BigDecimal> requerido = ReglasStock.requeridoPorInsumo(Map.of(), List.of(linea));
        assertThat(requerido).isEmpty();
    }

    // ---------------------------------------------------------------- texto

    @Test
    void elTextoNoMuestraDecimalesDeMas() {
        assertThat(ReglasStock.texto(new BigDecimal("2.500"))).isEqualTo("2.5");
        assertThat(ReglasStock.texto(new BigDecimal("3.000"))).isEqualTo("3");
        assertThat(ReglasStock.texto(BigDecimal.ZERO)).isEqualTo("0");
    }

    @Test
    void etiquetaUnidadDevuelveElTextoCorto() {
        assertThat(ReglasStock.etiquetaUnidad(Insumo.KG)).isEqualTo("kg");
        assertThat(ReglasStock.etiquetaUnidad(Insumo.G)).isEqualTo("g");
        assertThat(ReglasStock.etiquetaUnidad(Insumo.L)).isEqualTo("L");
        assertThat(ReglasStock.etiquetaUnidad(Insumo.ML)).isEqualTo("ml");
        assertThat(ReglasStock.etiquetaUnidad(Insumo.UNIDAD)).isEqualTo("u.");
    }

    private static MovimientoInsumo movimientoVenta(Long insumoId, String cantidad) {
        MovimientoInsumo m = new MovimientoInsumo();
        m.setInsumoId(insumoId);
        m.setTipo(MovimientoInsumo.VENTA);
        m.setCantidad(new BigDecimal(cantidad));
        return m;
    }

    private static RecetaLinea recetaLinea(Long productoId, Long insumoId, String cantidad) {
        RecetaLinea linea = new RecetaLinea();
        linea.setProductoId(productoId);
        linea.setInsumoId(insumoId);
        linea.setCantidad(new BigDecimal(cantidad));
        return linea;
    }
}
