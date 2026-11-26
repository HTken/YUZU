package com.yuzu.backend.caja;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.yuzu.backend.config.SesionActual;
import com.yuzu.backend.config.Tiempo;
import com.yuzu.backend.inventario.ConsumoService;
import com.yuzu.backend.pedido.Pedido;
import com.yuzu.backend.pedido.PedidoRepository;
import com.yuzu.backend.usuario.UsuarioRepository;

/**
 * Anulación de una venta ya cobrada. Solo el administrador (ver SecurityConfig) y solo si la venta es de la caja
 * que sigue abierta. La cuenta no se borra: pasa a ANULADO con su ticket, quién la anuló, cuándo y por qué.
 *
 * Como la venta deja de contar, el efectivo que debe haber en el cajón baja solo; no hace falta ningún egreso.
 * Los números de ticket no se reutilizan.
 *
 * El orden de los bloqueos es el mismo que al cobrar: primero la caja, luego la cuenta y luego los insumos.
 */
@Service
public class AnulacionVentaService {

    private final TurnoRepository turnos;
    private final PedidoRepository pedidos;
    private final PagoPedidoRepository pagos;
    private final UsuarioRepository usuarios;
    private final ConsumoService consumo;
    private final CajaService caja;

    public AnulacionVentaService(TurnoRepository turnos,
                                 PedidoRepository pedidos,
                                 PagoPedidoRepository pagos,
                                 UsuarioRepository usuarios,
                                 ConsumoService consumo,
                                 CajaService caja) {
        this.turnos = turnos;
        this.pedidos = pedidos;
        this.pagos = pagos;
        this.usuarios = usuarios;
        this.consumo = consumo;
        this.caja = caja;
    }

    @Transactional
    public ResumenTurnoDto anular(Long pedidoId, AnularVentaRequest request) {
        Long restauranteId = SesionActual.restauranteId();
        String motivo = ReglasAnulacion.motivo(request.motivo());

        // 1) La caja primero. Sin caja abierta no hay ventas que se puedan anular.
        Turno turno = turnos.bloquearAbierto(restauranteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "No hay una caja abierta. Solo se pueden anular ventas de la caja abierta."));

        // 2) La cuenta (si es de otro restaurante, responde "no encontrada").
        Pedido pedido = pedidos.bloquearPorId(pedidoId, restauranteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venta no encontrada"));
        ReglasAnulacion.exigirAnulable(pedido.getEstado(), pedido.getNumeroTicket(), pedido.getTurnoId(), turno.getId());

        // 3) El efectivo de esta venta sale del cajón: tiene que estar.
        List<PagoPedido> pagosDeLaVenta = pagos.findByRestauranteIdAndPedidoIdIn(restauranteId, List.of(pedido.getId()));
        BigDecimal efectivo = ReglasAnulacion.efectivoDeLaVenta(pagosDeLaVenta);
        ReglasAnulacion.exigirEfectivo(caja.resumen(turno).efectivoEsperado(), efectivo);

        // 4) Se anula. Conserva ticket, turno y total; se guarda quién, cuándo y por qué.
        String operador = Operador.nombre(usuarios, restauranteId);
        pedido.setEstado(Pedido.ANULADO);
        pedido.setAnuladoCobroPor(operador);
        pedido.setAnuladoCobroEn(Tiempo.ahora());
        pedido.setMotivoAnulacion(motivo);
        pedidos.save(pedido);

        // 5) Si se eligió, los insumos vuelven al stock (en esta misma transacción).
        if (request.devolverInsumos()) {
            consumo.devolverVenta(restauranteId, pedido.getId(), pedido.getNumeroTicket(), operador);
        }

        return caja.resumen(turno);
    }
}
