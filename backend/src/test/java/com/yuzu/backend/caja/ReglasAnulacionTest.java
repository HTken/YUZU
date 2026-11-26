package com.yuzu.backend.caja;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.yuzu.backend.pedido.Pedido;

class ReglasAnulacionTest {

    // --------------------------------------------------------------- motivo

    @Test
    void limpiaEspaciosDeMasDelMotivo() {
        assertThat(ReglasAnulacion.motivo("  el cliente   se arrepintió  ")).isEqualTo("el cliente se arrepintió");
    }

    @Test
    void rechazaMotivoMuyCorto() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> ReglasAnulacion.motivo("ab"));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void rechazaMotivoVacioONulo() {
        assertThrows(ResponseStatusException.class, () -> ReglasAnulacion.motivo(null));
        assertThrows(ResponseStatusException.class, () -> ReglasAnulacion.motivo("   "));
    }

    @Test
    void rechazaMotivoMuyLargo() {
        String largo = "a".repeat(ReglasAnulacion.MOTIVO_MAXIMO + 1);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> ReglasAnulacion.motivo(largo));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void aceptaMotivoEnElLimite() {
        String limite = "a".repeat(ReglasAnulacion.MOTIVO_MAXIMO);
        assertDoesNotThrow(() -> ReglasAnulacion.motivo(limite));
    }

    // ---------------------------------------------------------- exigirAnulable

    @Test
    void noSePuedeAnularUnaCuentaAbierta() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> ReglasAnulacion.exigirAnulable(Pedido.ABIERTO, null, 1L, 1L));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void noSePuedeAnularUnaVentaYaAnuladaConTicket() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> ReglasAnulacion.exigirAnulable(Pedido.ANULADO, 5, 1L, 1L));
        assertThat(ex.getReason()).contains("ya fue anulada");
    }

    @Test
    void noSePuedeAnularUnaCuentaAnuladaSinTicket() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> ReglasAnulacion.exigirAnulable(Pedido.ANULADO, null, 1L, 1L));
        assertThat(ex.getReason()).contains("sin cobrarse");
    }

    @Test
    void noSePuedeAnularUnaVentaDeUnaCajaYaCerrada() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> ReglasAnulacion.exigirAnulable(Pedido.PAGADO, 5, 1L, 2L));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getReason()).contains("ya se cerró");
    }

    @Test
    void siSeCobroEnLaCajaAbiertaSiSePuedeAnular() {
        assertDoesNotThrow(() -> ReglasAnulacion.exigirAnulable(Pedido.PAGADO, 5, 1L, 1L));
    }

    // ------------------------------------------------------- efectivoDeLaVenta

    @Test
    void sumaSoloLosPagosEnEfectivo() {
        PagoPedido efectivo1 = pago(PagoPedido.EFECTIVO, "20.00");
        PagoPedido efectivo2 = pago(PagoPedido.EFECTIVO, "15.50");
        PagoPedido tarjeta = pago(PagoPedido.TARJETA, "100.00");
        PagoPedido yape = pago(PagoPedido.YAPE, "30.00");

        BigDecimal total = ReglasAnulacion.efectivoDeLaVenta(List.of(efectivo1, efectivo2, tarjeta, yape));

        assertThat(total).isEqualByComparingTo("35.50");
    }

    @Test
    void sinPagosEnEfectivoElTotalEsCero() {
        BigDecimal total = ReglasAnulacion.efectivoDeLaVenta(List.of(pago(PagoPedido.TARJETA, "100.00")));
        assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ---------------------------------------------------------- exigirEfectivo

    @Test
    void noSePuedeDevolverMasEfectivoDelQueHayEnLaCaja() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> ReglasAnulacion.exigirEfectivo(new BigDecimal("10.00"), new BigDecimal("20.00")));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void siAlcanzaElEfectivoDeLaCajaSiSePuedeDevolver() {
        assertDoesNotThrow(() -> ReglasAnulacion.exigirEfectivo(new BigDecimal("20.00"), new BigDecimal("20.00")));
        assertDoesNotThrow(() -> ReglasAnulacion.exigirEfectivo(new BigDecimal("50.00"), new BigDecimal("20.00")));
    }

    private static PagoPedido pago(String metodo, String monto) {
        PagoPedido p = new PagoPedido();
        p.setMetodo(metodo);
        p.setMonto(new BigDecimal(monto));
        p.setCreadoEn(LocalDateTime.now());
        return p;
    }
}
