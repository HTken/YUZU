package com.yuzu.backend.inventario;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yuzu.backend.config.Tiempo;

/**
 * Descuenta del inventario los insumos de lo que se vendió. Se llama desde el cobro, DENTRO de su misma
 * transacción: o se cobra y se descuenta todo, o no se hace nada.
 *
 * La venta nunca se bloquea por falta de stock registrado: se descuenta lo que haya, el stock queda en 0
 * y lo que faltó se anota en el movimiento.
 */
@Service
public class ConsumoService {

    private final RecetaRepository recetas;
    private final InsumoRepository insumos;
    private final MovimientoInsumoRepository movimientos;

    public ConsumoService(RecetaRepository recetas, InsumoRepository insumos, MovimientoInsumoRepository movimientos) {
        this.recetas = recetas;
        this.insumos = insumos;
        this.movimientos = movimientos;
    }

    /**
     * @param vendidos lo que se cobró (sin lo anulado)
     * @param operador quién cobró, para el historial
     */
    @Transactional
    public void descontarVenta(Long restauranteId, Long pedidoId, int ticket, String operador,
                               List<ConsumoProducto> vendidos) {
        Map<Long, Integer> porProducto = new HashMap<>();
        for (ConsumoProducto v : vendidos) {
            porProducto.merge(v.productoId(), v.cantidad(), Integer::sum);
        }
        if (porProducto.isEmpty()) {
            return;
        }

        List<RecetaLinea> lineas = recetas.findByRestauranteIdAndProductoIdIn(restauranteId, porProducto.keySet());
        if (lineas.isEmpty()) {
            return;
        }
        Map<Long, BigDecimal> requerido = ReglasStock.requeridoPorInsumo(porProducto, lineas);

        // Todos los insumos de la cuenta se bloquean juntos y en orden: así el stock que se lee es el definitivo.
        List<Insumo> bloqueados = insumos.bloquearPorIds(restauranteId, requerido.keySet());
        LocalDateTime ahora = Tiempo.ahora();
        String nota = "Venta · ticket N° " + String.format(Locale.ROOT, "%06d", ticket);

        for (Insumo insumo : bloqueados) {
            // Un insumo desactivado ya no se controla: no se le descuenta nada.
            if (!insumo.isActivo()) {
                continue;
            }

            ReglasStock.Consumo consumo = ReglasStock.consumo(insumo.getStockActual(), requerido.get(insumo.getId()));
            BigDecimal nuevo = insumo.getStockActual().subtract(consumo.descontado());
            insumo.setStockActual(nuevo);
            insumos.save(insumo);

            MovimientoInsumo m = new MovimientoInsumo();
            m.setRestauranteId(restauranteId);
            m.setInsumoId(insumo.getId());
            m.setTipo(MovimientoInsumo.VENTA);
            m.setCantidad(consumo.descontado().negate());
            m.setStockResultante(nuevo);
            m.setFaltante(consumo.faltante().signum() > 0 ? consumo.faltante() : null);
            m.setNota(nota);
            m.setPedidoId(pedidoId);
            m.setRegistradoPor(operador);
            m.setCreadoEn(ahora);
            movimientos.save(m);
        }
    }

    /**
     * Devuelve al stock lo que descontó una venta que se anuló. Se llama desde la anulación, DENTRO de su misma
     * transacción. Devuelve lo que se descontó de verdad (sus movimientos VENTA), no lo que diga la receta hoy.
     * Un insumo desactivado ya no se controla y no se toca.
     *
     * @param operador quién anuló, para el historial
     */
    @Transactional
    public void devolverVenta(Long restauranteId, Long pedidoId, int ticket, String operador) {
        List<MovimientoInsumo> ventas = movimientos.findByRestauranteIdAndPedidoIdAndTipo(
                restauranteId, pedidoId, MovimientoInsumo.VENTA);
        Map<Long, BigDecimal> descontado = ReglasStock.descontadoPorInsumo(ventas);
        if (descontado.isEmpty()) {
            return;
        }

        List<Insumo> bloqueados = insumos.bloquearPorIds(restauranteId, descontado.keySet());
        LocalDateTime ahora = Tiempo.ahora();
        String nota = "Venta anulada · ticket N° " + String.format(Locale.ROOT, "%06d", ticket);

        for (Insumo insumo : bloqueados) {
            if (!insumo.isActivo()) {
                continue;
            }
            BigDecimal vuelve = ReglasStock.devolucion(insumo.getStockActual(), descontado.get(insumo.getId()));
            if (vuelve.signum() <= 0) {
                continue;
            }

            BigDecimal nuevo = insumo.getStockActual().add(vuelve);
            insumo.setStockActual(nuevo);
            insumos.save(insumo);

            MovimientoInsumo m = new MovimientoInsumo();
            m.setRestauranteId(restauranteId);
            m.setInsumoId(insumo.getId());
            m.setTipo(MovimientoInsumo.DEVOLUCION);
            m.setCantidad(vuelve);
            m.setStockResultante(nuevo);
            m.setNota(nota);
            m.setPedidoId(pedidoId);
            m.setRegistradoPor(operador);
            m.setCreadoEn(ahora);
            movimientos.save(m);
        }
    }
}
