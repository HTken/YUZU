package com.yuzu.backend.caja;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.yuzu.backend.config.SesionActual;
import com.yuzu.backend.config.Tiempo;
import com.yuzu.backend.mesa.Mesa;
import com.yuzu.backend.mesa.MesaRepository;
import com.yuzu.backend.negocio.NegocioService;
import com.yuzu.backend.pedido.Pedido;
import com.yuzu.backend.pedido.PedidoItem;
import com.yuzu.backend.pedido.PedidoItemRepository;
import com.yuzu.backend.pedido.PedidoRepository;

/**
 * Arma el ticket de una venta cobrada, para imprimirlo al cobrar o volver a imprimirlo después. Solo lectura.
 * El restaurante sale del token: pedir el ticket de una venta de otro restaurante responde "no encontrada".
 */
@Service
public class TicketService {

    private final PedidoRepository pedidos;
    private final PedidoItemRepository items;
    private final PagoPedidoRepository pagos;
    private final MesaRepository mesas;
    private final NegocioService negocio;

    public TicketService(PedidoRepository pedidos,
                         PedidoItemRepository items,
                         PagoPedidoRepository pagos,
                         MesaRepository mesas,
                         NegocioService negocio) {
        this.pedidos = pedidos;
        this.items = items;
        this.pagos = pagos;
        this.mesas = mesas;
        this.negocio = negocio;
    }

    @Transactional(readOnly = true)
    public TicketVentaDto ticket(Long pedidoId) {
        Long restauranteId = SesionActual.restauranteId();

        Pedido pedido = pedidos.findByRestauranteIdAndIdIn(restauranteId, List.of(pedidoId)).stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venta no encontrada"));
        ReglasTicket.exigirCobrada(pedido.getEstado());

        List<LineaTicketDto> lineas = new ArrayList<>();
        for (PedidoItem item : items.findByRestauranteIdAndPedidoIdOrderByIdAsc(restauranteId, pedido.getId())) {
            if (PedidoItem.ANULADO.equals(item.getEstado())) {
                continue;
            }
            BigDecimal importe = item.getPrecioUnitario().multiply(BigDecimal.valueOf(item.getCantidad()));
            lineas.add(new LineaTicketDto(item.getNombre(), item.getCantidad(), item.getPrecioUnitario(), importe,
                    item.getNota()));
        }

        List<PagoDto> pagosDeLaVenta = pagos.findByRestauranteIdAndPedidoIdIn(restauranteId, List.of(pedido.getId()))
                .stream()
                .sorted(Comparator.comparingInt(p -> CajaService.METODOS.indexOf(p.getMetodo())))
                .map(p -> new PagoDto(p.getMetodo(), p.getMonto()))
                .toList();

        String mesa = mesas.findByIdAndRestauranteId(pedido.getMesaId(), restauranteId)
                .map(Mesa::getNombre)
                .orElse("—");

        return new TicketVentaDto(
                negocio.datos(restauranteId), pedido.getNumeroTicket(), mesa, pedido.getMozo(), pedido.getCobradoPor(),
                Tiempo.iso(pedido.getCerradoEn()), lineas, pedido.getTotalCobrado(), pagosDeLaVenta);
    }
}
