package com.yuzu.backend.informes;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Las consultas de los informes. Cada una recibe el restaurante y lo filtra: un informe nunca mezcla
 * datos de dos restaurantes. Las fechas son el rango en UTC ya convertido (ver PeriodoInforme).
 * Solo cuentan las cuentas cobradas (PAGADO), por el momento en que se cobraron. Una venta que se cobró y luego
 * se anuló (ANULADO con número de ticket) ya no es venta: no suma en nada y se cuenta aparte.
 */
@Repository
public class InformesRepositorio {

    private static final String PAGADO = "PAGADO";
    private static final String ANULADO = "ANULADO";

    @PersistenceContext
    private EntityManager em;

    /** Cada cuenta cobrada en el periodo: cuándo y cuánto. */
    public List<VentaFila> ventas(Long restauranteId, LocalDateTime desde, LocalDateTime hasta) {
        return em.createQuery("""
                select new com.yuzu.backend.informes.VentaFila(p.cerradoEn, p.totalCobrado)
                from Pedido p
                where p.restauranteId = :restauranteId
                  and p.estado = :estado
                  and p.cerradoEn >= :desde and p.cerradoEn < :hasta
                """, VentaFila.class)
                .setParameter("restauranteId", restauranteId)
                .setParameter("estado", PAGADO)
                .setParameter("desde", desde)
                .setParameter("hasta", hasta)
                .getResultList();
    }

    /** Cuántas cuentas se anularon en el periodo sin haberse cobrado (no llegaron a tener número de ticket). */
    public long cuentasAnuladas(Long restauranteId, LocalDateTime desde, LocalDateTime hasta) {
        return em.createQuery("""
                select count(p)
                from Pedido p
                where p.restauranteId = :restauranteId
                  and p.estado = :estado
                  and p.numeroTicket is null
                  and p.cerradoEn >= :desde and p.cerradoEn < :hasta
                """, Long.class)
                .setParameter("restauranteId", restauranteId)
                .setParameter("estado", ANULADO)
                .setParameter("desde", desde)
                .setParameter("hasta", hasta)
                .getSingleResult();
    }

    /** Cuántas ventas ya cobradas se anularon en el periodo (por el momento en que se anularon). */
    public long ventasAnuladas(Long restauranteId, LocalDateTime desde, LocalDateTime hasta) {
        return em.createQuery("""
                select count(p)
                from Pedido p
                where p.restauranteId = :restauranteId
                  and p.estado = :estado
                  and p.numeroTicket is not null
                  and p.anuladoCobroEn >= :desde and p.anuladoCobroEn < :hasta
                """, Long.class)
                .setParameter("restauranteId", restauranteId)
                .setParameter("estado", ANULADO)
                .setParameter("desde", desde)
                .setParameter("hasta", hasta)
                .getSingleResult();
    }

    /** Lo cobrado con cada método de pago, de las cuentas cobradas en el periodo. */
    public List<MetodoFila> metodos(Long restauranteId, LocalDateTime desde, LocalDateTime hasta) {
        return em.createQuery("""
                select new com.yuzu.backend.informes.MetodoFila(g.metodo, sum(g.monto), count(distinct g.pedidoId))
                from PagoPedido g
                join Pedido p on p.id = g.pedidoId and p.restauranteId = g.restauranteId
                where g.restauranteId = :restauranteId
                  and p.estado = :estado
                  and p.cerradoEn >= :desde and p.cerradoEn < :hasta
                group by g.metodo
                """, MetodoFila.class)
                .setParameter("restauranteId", restauranteId)
                .setParameter("estado", PAGADO)
                .setParameter("desde", desde)
                .setParameter("hasta", hasta)
                .getResultList();
    }

    /** Lo vendido de cada producto en las cuentas cobradas del periodo, sin contar los platos anulados. */
    public List<ProductoFila> productos(Long restauranteId, LocalDateTime desde, LocalDateTime hasta) {
        return em.createQuery("""
                select new com.yuzu.backend.informes.ProductoFila(
                    pr.id, pr.nombre, c.nombre, sum(i.cantidad), sum(i.precioUnitario * i.cantidad))
                from PedidoItem i
                join Pedido p on p.id = i.pedidoId and p.restauranteId = i.restauranteId
                join Producto pr on pr.id = i.productoId and pr.restauranteId = i.restauranteId
                left join Categoria c on c.id = pr.categoriaId and c.restauranteId = pr.restauranteId
                where i.restauranteId = :restauranteId
                  and p.estado = :estado
                  and p.cerradoEn >= :desde and p.cerradoEn < :hasta
                  and i.estado <> :anulado
                group by pr.id, pr.nombre, c.nombre
                """, ProductoFila.class)
                .setParameter("restauranteId", restauranteId)
                .setParameter("estado", PAGADO)
                .setParameter("anulado", ANULADO)
                .setParameter("desde", desde)
                .setParameter("hasta", hasta)
                .getResultList();
    }

    /** Los movimientos de inventario del periodo, sumados por insumo y por tipo. */
    public List<MovimientoFila> movimientos(Long restauranteId, LocalDateTime desde, LocalDateTime hasta) {
        return em.createQuery("""
                select new com.yuzu.backend.informes.MovimientoFila(
                    m.insumoId, m.tipo, sum(m.cantidad), sum(m.faltante), sum(m.costoTotal))
                from MovimientoInsumo m
                where m.restauranteId = :restauranteId
                  and m.creadoEn >= :desde and m.creadoEn < :hasta
                group by m.insumoId, m.tipo
                """, MovimientoFila.class)
                .setParameter("restauranteId", restauranteId)
                .setParameter("desde", desde)
                .setParameter("hasta", hasta)
                .getResultList();
    }
}
