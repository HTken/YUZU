package com.yuzu.backend.caja;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.yuzu.backend.pedido.Pedido;

/** Qué cuentas tienen ticket: solo las cobradas. */
final class ReglasTicket {

    private ReglasTicket() {
    }

    static void exigirCobrada(String estado) {
        if (Pedido.ABIERTO.equals(estado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta cuenta todavía no se cobró");
        }
        if (Pedido.ANULADO.equals(estado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta venta fue anulada, ya no tiene ticket");
        }
    }
}
