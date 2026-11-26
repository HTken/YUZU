package com.yuzu.backend.informes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.yuzu.backend.caja.PagoPedido;
import com.yuzu.backend.config.SesionActual;
import com.yuzu.backend.inventario.Insumo;
import com.yuzu.backend.inventario.InsumoRepository;

/**
 * Informes de ventas e insumos. Solo lectura. El restaurante sale siempre del token, nunca de lo que
 * envía el navegador; el acceso es solo del administrador (ver SecurityConfig).
 */
@Service
public class InformesService {

    /** El orden en que se muestran los métodos de pago. */
    private static final List<String> METODOS = List.of(
            PagoPedido.EFECTIVO, PagoPedido.YAPE, PagoPedido.PLIN, PagoPedido.TARJETA);

    private final InformesRepositorio consultas;
    private final InsumoRepository insumos;

    public InformesService(InformesRepositorio consultas, InsumoRepository insumos) {
        this.consultas = consultas;
        this.insumos = insumos;
    }

    @Transactional(readOnly = true)
    public InformeVentasDto ventas(String desde, String hasta) {
        PeriodoInforme periodo = periodo(desde, hasta);
        Long restauranteId = SesionActual.restauranteId();

        List<VentaFila> filas = consultas.ventas(restauranteId, periodo.inicioUtc(), periodo.finUtc());
        BigDecimal total = CalculosInforme.total(filas);
        int ventas = filas.size();

        return new InformeVentasDto(
                periodo.desde().toString(),
                periodo.hasta().toString(),
                periodo.dias(),
                ventas,
                total,
                CalculosInforme.ticketPromedio(total, ventas),
                (int) consultas.cuentasAnuladas(restauranteId, periodo.inicioUtc(), periodo.finUtc()),
                (int) consultas.ventasAnuladas(restauranteId, periodo.inicioUtc(), periodo.finUtc()),
                CalculosInforme.porDia(filas, periodo),
                CalculosInforme.porMetodo(consultas.metodos(restauranteId, periodo.inicioUtc(), periodo.finUtc()), METODOS),
                CalculosInforme.productos(consultas.productos(restauranteId, periodo.inicioUtc(), periodo.finUtc())));
    }

    @Transactional(readOnly = true)
    public InformeInsumosDto insumos(String desde, String hasta) {
        PeriodoInforme periodo = periodo(desde, hasta);
        Long restauranteId = SesionActual.restauranteId();

        List<MovimientoFila> movimientos = consultas.movimientos(restauranteId, periodo.inicioUtc(), periodo.finUtc());

        Map<Long, InsumoInfo> info = new HashMap<>();
        for (Insumo i : insumos.findByRestauranteIdOrderByNombreAsc(restauranteId)) {
            info.put(i.getId(), new InsumoInfo(i.getNombre(), i.getUnidad()));
        }

        List<InsumoPeriodoDto> lista = CalculosInforme.insumos(movimientos, info);
        return new InformeInsumosDto(
                periodo.desde().toString(),
                periodo.hasta().toString(),
                CalculosInforme.compras(lista),
                CalculosInforme.conFaltante(lista),
                lista);
    }

    private static PeriodoInforme periodo(String desde, String hasta) {
        try {
            return PeriodoInforme.de(desde, hasta);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
