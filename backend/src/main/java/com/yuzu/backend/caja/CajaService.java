package com.yuzu.backend.caja;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.yuzu.backend.config.SesionActual;
import com.yuzu.backend.config.Tiempo;
import com.yuzu.backend.mesa.Mesa;
import com.yuzu.backend.mesa.MesaRepository;
import com.yuzu.backend.pedido.Pedido;
import com.yuzu.backend.pedido.PedidoRepository;
import com.yuzu.backend.usuario.UsuarioRepository;

/**
 * Turnos de caja. El restaurante sale siempre del token (SesionActual) y todas las búsquedas lo incluyen.
 * Solo puede haber una caja abierta por restaurante (lo garantiza la base de datos).
 */
@Service
public class CajaService {

    /** Métodos de pago aceptados, en el orden en que se muestran. */
    static final List<String> METODOS = List.of(
            PagoPedido.EFECTIVO, PagoPedido.YAPE, PagoPedido.PLIN, PagoPedido.TARJETA);

    private final TurnoRepository turnos;
    private final MovimientoRepository movimientos;
    private final PagoPedidoRepository pagos;
    private final PedidoRepository pedidos;
    private final MesaRepository mesas;
    private final UsuarioRepository usuarios;

    public CajaService(TurnoRepository turnos,
                       MovimientoRepository movimientos,
                       PagoPedidoRepository pagos,
                       PedidoRepository pedidos,
                       MesaRepository mesas,
                       UsuarioRepository usuarios) {
        this.turnos = turnos;
        this.movimientos = movimientos;
        this.pagos = pagos;
        this.pedidos = pedidos;
        this.mesas = mesas;
        this.usuarios = usuarios;
    }

    // ------------------------------------------------------------------ consultas

    @Transactional(readOnly = true)
    public EstadoCajaDto estado() {
        Long restauranteId = SesionActual.restauranteId();
        return turnos.findByRestauranteIdAndEstado(restauranteId, Turno.ABIERTA)
                .map(t -> new EstadoCajaDto(true, resumen(t)))
                .orElseGet(() -> new EstadoCajaDto(false, null));
    }

    /** Los últimos turnos cerrados. */
    @Transactional(readOnly = true)
    public List<TurnoDto> historial() {
        return turnos.findTop15ByRestauranteIdAndEstadoOrderByIdDesc(SesionActual.restauranteId(), Turno.CERRADA)
                .stream().map(TurnoDto::desde).toList();
    }

    // ------------------------------------------------------------------ turno

    @Transactional
    public ResumenTurnoDto abrir(AbrirCajaRequest request) {
        Long restauranteId = SesionActual.restauranteId();

        if (turnos.findByRestauranteIdAndEstado(restauranteId, Turno.ABIERTA).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya hay una caja abierta");
        }

        Turno turno = new Turno();
        turno.setRestauranteId(restauranteId);
        turno.setAbiertaPor(Operador.nombre(usuarios, restauranteId));
        turno.setAbiertaEn(Tiempo.ahora());
        turno.setMontoInicial(request.montoInicial());
        try {
            turno = turnos.saveAndFlush(turno);
        } catch (DataIntegrityViolationException e) {
            // Dos personas abrieron la caja a la vez: la base de datos dejó pasar solo a una.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya hay una caja abierta");
        }
        return resumen(turno);
    }

    @Transactional
    public ResumenTurnoDto registrarMovimiento(MovimientoRequest request) {
        Long restauranteId = SesionActual.restauranteId();
        Turno turno = cajaAbiertaBloqueada(restauranteId);

        String tipo = request.tipo().trim().toUpperCase(Locale.ROOT);
        if (!Movimiento.INGRESO.equals(tipo) && !Movimiento.EGRESO.equals(tipo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El tipo de movimiento no es válido");
        }

        // No se puede sacar más efectivo del que hay en el cajón.
        if (Movimiento.EGRESO.equals(tipo)) {
            BigDecimal disponible = resumen(turno).efectivoEsperado();
            if (request.monto().compareTo(disponible) > 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "No hay tanto efectivo en la caja. Ahora hay S/ " + disponible.setScale(2).toPlainString());
            }
        }

        Movimiento movimiento = new Movimiento();
        movimiento.setRestauranteId(restauranteId);
        movimiento.setTurnoId(turno.getId());
        movimiento.setTipo(tipo);
        movimiento.setConcepto(request.concepto().trim());
        movimiento.setMonto(request.monto());
        movimiento.setRegistradoPor(Operador.nombre(usuarios, restauranteId));
        movimiento.setCreadoEn(Tiempo.ahora());
        movimientos.save(movimiento);

        return resumen(turno);
    }

    /** Cierra la caja: guarda cuánto se esperaba, cuánto se contó y la diferencia. */
    @Transactional
    public TurnoDto cerrar(CerrarCajaRequest request) {
        Long restauranteId = SesionActual.restauranteId();
        Turno turno = cajaAbiertaBloqueada(restauranteId);

        ResumenTurnoDto resumen = resumen(turno);
        BigDecimal contado = request.efectivoContado();

        turno.setEstado(Turno.CERRADA);
        turno.setCerradaPor(Operador.nombre(usuarios, restauranteId));
        turno.setCerradaEn(Tiempo.ahora());
        turno.setTotalVentas(resumen.totalVentas());
        turno.setNumVentas(resumen.ventas());
        turno.setEfectivoEsperado(resumen.efectivoEsperado());
        turno.setEfectivoContado(contado);
        turno.setDiferencia(contado.subtract(resumen.efectivoEsperado()));
        turno.setNotaCierre(request.nota() == null || request.nota().isBlank() ? null : request.nota().trim());
        return TurnoDto.desde(turnos.save(turno));
    }

    // ------------------------------------------------------------------ ayudas

    /** La caja abierta, bloqueada para que nadie cobre ni cierre al mismo tiempo. */
    private Turno cajaAbiertaBloqueada(Long restauranteId) {
        return turnos.bloquearAbierto(restauranteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No hay una caja abierta"));
    }

    /** Visible para AnulacionVentaService, que necesita el efectivo esperado y devolver el resumen ya actualizado. */
    ResumenTurnoDto resumen(Turno turno) {
        Long restauranteId = turno.getRestauranteId();

        // Solo las cobradas suman. Las que se cobraron y luego se anularon se listan aparte.
        List<Pedido> ventas = pedidos.findByRestauranteIdAndTurnoIdAndEstadoOrderByNumeroTicketAsc(
                restauranteId, turno.getId(), Pedido.PAGADO);
        List<Pedido> anuladas = pedidos.findByRestauranteIdAndTurnoIdAndEstadoOrderByNumeroTicketAsc(
                restauranteId, turno.getId(), Pedido.ANULADO);
        List<Long> idsVentas = ventas.stream().map(Pedido::getId).toList();
        Map<Long, List<PagoPedido>> pagosPorPedido = idsVentas.isEmpty() ? Map.of()
                : pagos.findByRestauranteIdAndPedidoIdIn(restauranteId, idsVentas).stream()
                        .collect(Collectors.groupingBy(PagoPedido::getPedidoId));

        Map<Long, String> nombresDeMesa = new HashMap<>();
        for (Mesa mesa : mesas.findByRestauranteId(restauranteId)) {
            nombresDeMesa.put(mesa.getId(), mesa.getNombre());
        }

        Map<String, BigDecimal> porMetodo = new HashMap<>();
        BigDecimal totalVentas = BigDecimal.ZERO;
        List<VentaDto> detalle = new ArrayList<>();
        for (Pedido venta : ventas) {
            List<PagoDto> pagosDeLaVenta = pagosPorPedido.getOrDefault(venta.getId(), List.of()).stream()
                    .map(p -> new PagoDto(p.getMetodo(), p.getMonto()))
                    .toList();
            for (PagoDto pago : pagosDeLaVenta) {
                porMetodo.merge(pago.metodo(), pago.monto(), BigDecimal::add);
            }
            totalVentas = totalVentas.add(venta.getTotalCobrado());
            detalle.add(new VentaDto(
                    venta.getId(), venta.getNumeroTicket(),
                    nombresDeMesa.getOrDefault(venta.getMesaId(), "—"),
                    venta.getMozo(), venta.getCobradoPor(), Tiempo.iso(venta.getCerradoEn()),
                    venta.getTotalCobrado(), pagosDeLaVenta));
        }

        List<VentaAnuladaDto> detalleAnuladas = new ArrayList<>();
        for (Pedido venta : anuladas) {
            detalleAnuladas.add(new VentaAnuladaDto(
                    venta.getId(), venta.getNumeroTicket(),
                    nombresDeMesa.getOrDefault(venta.getMesaId(), "—"),
                    venta.getTotalCobrado(), Tiempo.iso(venta.getCerradoEn()),
                    venta.getAnuladoCobroPor(), Tiempo.iso(venta.getAnuladoCobroEn()),
                    venta.getMotivoAnulacion()));
        }

        List<TotalMetodoDto> totalesPorMetodo = METODOS.stream()
                .map(m -> new TotalMetodoDto(m, porMetodo.getOrDefault(m, BigDecimal.ZERO)))
                .toList();

        List<Movimiento> lista = movimientos.findByRestauranteIdAndTurnoIdOrderByIdDesc(restauranteId, turno.getId());
        BigDecimal ingresos = sumar(lista, Movimiento.INGRESO);
        BigDecimal egresos = sumar(lista, Movimiento.EGRESO);

        BigDecimal efectivoEsperado = turno.getMontoInicial()
                .add(porMetodo.getOrDefault(PagoPedido.EFECTIVO, BigDecimal.ZERO))
                .add(ingresos)
                .subtract(egresos);

        int cuentasAbiertas = (int) pedidos.countByRestauranteIdAndEstado(restauranteId, Pedido.ABIERTO);

        return new ResumenTurnoDto(
                TurnoDto.desde(turno), ventas.size(), totalVentas, totalesPorMetodo, ingresos, egresos,
                efectivoEsperado, cuentasAbiertas, detalle, detalleAnuladas,
                lista.stream().map(MovimientoDto::desde).toList());
    }

    private static BigDecimal sumar(List<Movimiento> lista, String tipo) {
        return lista.stream()
                .filter(m -> tipo.equals(m.getTipo()))
                .map(Movimiento::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
