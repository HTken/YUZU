package com.yuzu.backend.caja;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.yuzu.backend.config.SesionActual;
import com.yuzu.backend.config.Tiempo;
import com.yuzu.backend.inventario.ConsumoProducto;
import com.yuzu.backend.inventario.ConsumoService;
import com.yuzu.backend.pedido.Pedido;
import com.yuzu.backend.pedido.PedidoItem;
import com.yuzu.backend.pedido.PedidoItemRepository;
import com.yuzu.backend.pedido.PedidoRepository;
import com.yuzu.backend.pedido.PedidosService;
import com.yuzu.backend.usuario.UsuarioRepository;

/**
 * Cobro de una cuenta. Al cobrar, la cuenta pasa a PAGADO y la mesa queda libre sola.
 *
 * El orden importa: primero se bloquea la caja abierta y luego la cuenta. Así el número de ticket
 * (el último + 1) se calcula con todos los cobros anteriores ya confirmados, y nadie puede agregar
 * un producto a la cuenta mientras se está cobrando.
 */
@Service
public class CobroService {

    private final TurnoRepository turnos;
    private final PedidoRepository pedidos;
    private final PedidoItemRepository items;
    private final PagoPedidoRepository pagos;
    private final UsuarioRepository usuarios;
    private final ConsumoService consumo;

    public CobroService(TurnoRepository turnos,
                        PedidoRepository pedidos,
                        PedidoItemRepository items,
                        PagoPedidoRepository pagos,
                        UsuarioRepository usuarios,
                        ConsumoService consumo) {
        this.turnos = turnos;
        this.pedidos = pedidos;
        this.items = items;
        this.pagos = pagos;
        this.usuarios = usuarios;
        this.consumo = consumo;
    }

    @Transactional
    public CobroDto cobrar(Long pedidoId, CobrarRequest request) {
        Long restauranteId = SesionActual.restauranteId();

        // 1) La caja primero.
        Turno turno = turnos.bloquearAbierto(restauranteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Abre la caja antes de cobrar"));

        // 2) La cuenta (si es de otro restaurante, responde "no encontrada").
        Pedido pedido = pedidos.bloquearPorId(pedidoId, restauranteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));
        if (!Pedido.ABIERTO.equals(pedido.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta cuenta ya fue cobrada o anulada");
        }

        List<PedidoItem> lista = items.findByRestauranteIdAndPedidoIdOrderByIdAsc(restauranteId, pedido.getId());
        if (lista.stream().anyMatch(i -> PedidosService.PENDIENTE.equals(i.getEstado()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Hay productos sin enviar a cocina. Envíalos o quítalos antes de cobrar.");
        }
        BigDecimal total = PedidosService.total(lista);
        if (total.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La cuenta no tiene nada que cobrar. Si no se va a cobrar, anúlala.");
        }

        // 3) Los pagos deben sumar exactamente el total.
        Map<String, BigDecimal> porMetodo = new LinkedHashMap<>();
        BigDecimal suma = BigDecimal.ZERO;
        for (PagoRequest pago : request.pagos()) {
            String metodo = pago.metodo().trim().toUpperCase(Locale.ROOT);
            if (!CajaService.METODOS.contains(metodo)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El método de pago no es válido");
            }
            porMetodo.merge(metodo, pago.monto(), BigDecimal::add);
            suma = suma.add(pago.monto());
        }
        if (suma.compareTo(total) != 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Los pagos suman S/ " + suma.setScale(2).toPlainString() + " y la cuenta es de S/ "
                            + total.setScale(2).toPlainString()
                            + ". Si alguien agregó o anuló productos, vuelve a abrir la cuenta.");
        }

        // 4) Se cobra.
        int ticket = pedidos.ultimoTicket(restauranteId) + 1;
        var ahora = Tiempo.ahora();

        pedido.setEstado(Pedido.PAGADO);
        pedido.setCerradoEn(ahora);
        pedido.setTurnoId(turno.getId());
        pedido.setNumeroTicket(ticket);
        pedido.setTotalCobrado(total);
        pedido.setCobradoPor(Operador.nombre(usuarios, restauranteId));
        pedidos.save(pedido);

        List<PagoPedido> filas = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> e : porMetodo.entrySet()) {
            PagoPedido fila = new PagoPedido();
            fila.setRestauranteId(restauranteId);
            fila.setPedidoId(pedido.getId());
            fila.setMetodo(e.getKey());
            fila.setMonto(e.getValue());
            fila.setCreadoEn(ahora);
            filas.add(fila);
        }
        pagos.saveAll(filas);

        // 5) Se descuentan del inventario los insumos de lo vendido (lo anulado no cuenta). Va en esta misma
        //    transacción: o se cobra y se descuenta todo, o no se hace nada. Si falta stock registrado la venta
        //    sigue igual; eso se anota en el inventario.
        List<ConsumoProducto> vendidos = lista.stream()
                .filter(i -> !PedidoItem.ANULADO.equals(i.getEstado()))
                .map(i -> new ConsumoProducto(i.getProductoId(), i.getCantidad()))
                .toList();
        consumo.descontarVenta(restauranteId, pedido.getId(), ticket, pedido.getCobradoPor(), vendidos);

        return new CobroDto(pedido.getId(), ticket, total, Tiempo.iso(ahora));
    }
}
