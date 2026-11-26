package com.yuzu.backend.caja;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.yuzu.backend.pedido.Pedido;

/**
 * Las reglas para anular una venta ya cobrada, sin base de datos, para poder probarlas solas:
 * solo se anulan ventas de la caja que sigue abierta, hace falta un motivo, y el efectivo que se devuelve
 * tiene que estar en el cajón.
 */
final class ReglasAnulacion {

    static final int MOTIVO_MINIMO = 3;
    static final int MOTIVO_MAXIMO = 200;

    private ReglasAnulacion() {
    }

    /** El motivo sin espacios de más. Debe tener al menos unas letras para que sirva de constancia. */
    static String motivo(String texto) {
        String limpio = texto == null ? "" : texto.trim().replaceAll("\\s+", " ");
        if (limpio.length() < MOTIVO_MINIMO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Escribe el motivo de la anulación (al menos " + MOTIVO_MINIMO + " letras)");
        }
        if (limpio.length() > MOTIVO_MAXIMO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El motivo no puede pasar de " + MOTIVO_MAXIMO + " caracteres");
        }
        return limpio;
    }

    /**
     * Revisa que la cuenta se pueda anular: tiene que estar cobrada, y haberse cobrado en la caja que está abierta.
     * Las ventas de una caja ya cerrada no se tocan: esa caja quedó contada y cerrada.
     */
    static void exigirAnulable(String estado, Integer ticket, Long turnoDeLaVenta, Long turnoAbierto) {
        if (Pedido.ABIERTO.equals(estado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esta cuenta todavía no se cobró. Si no se va a cobrar, anúlala desde las mesas.");
        }
        if (Pedido.ANULADO.equals(estado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    ticket != null ? "Esta venta ya fue anulada" : "Esta cuenta se anuló sin cobrarse");
        }
        if (!Objects.equals(turnoDeLaVenta, turnoAbierto)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esta venta es de una caja que ya se cerró. Solo se pueden anular ventas de la caja abierta.");
        }
    }

    /** Lo que se pagó en efectivo en esa venta: es lo que hay que devolver del cajón. */
    static BigDecimal efectivoDeLaVenta(List<PagoPedido> pagos) {
        BigDecimal efectivo = BigDecimal.ZERO;
        for (PagoPedido p : pagos) {
            if (PagoPedido.EFECTIVO.equals(p.getMetodo())) {
                efectivo = efectivo.add(p.getMonto());
            }
        }
        return efectivo;
    }

    /**
     * El efectivo que se devuelve sale del cajón, así que tiene que estar ahí (igual que en un egreso).
     * "esperado" es lo que debe haber ahora, incluyendo esa venta.
     */
    static void exigirEfectivo(BigDecimal esperado, BigDecimal efectivoDeLaVenta) {
        if (efectivoDeLaVenta.compareTo(esperado) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No hay tanto efectivo en la caja para devolver. Ahora hay S/ " + esperado.setScale(2).toPlainString()
                            + " y esta venta tiene S/ " + efectivoDeLaVenta.setScale(2).toPlainString()
                            + " en efectivo. Si pusieron dinero en el cajón, regístralo antes como ingreso.");
        }
    }
}
