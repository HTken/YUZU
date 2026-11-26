package com.yuzu.backend.caja;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.yuzu.backend.pedido.Pedido;

class ReglasTicketTest {

    @Test
    void unaCuentaAbiertaNoTieneTicket() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> ReglasTicket.exigirCobrada(Pedido.ABIERTO));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void unaVentaAnuladaYaNoTieneTicket() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> ReglasTicket.exigirCobrada(Pedido.ANULADO));
        assertThat(ex.getReason()).contains("anulada");
    }

    @Test
    void unaCuentaPagadaSiTieneTicket() {
        assertDoesNotThrow(() -> ReglasTicket.exigirCobrada(Pedido.PAGADO));
    }
}
