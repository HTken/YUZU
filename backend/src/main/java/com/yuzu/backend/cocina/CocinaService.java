package com.yuzu.backend.cocina;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yuzu.backend.config.SesionActual;
import com.yuzu.backend.config.Tiempo;
import com.yuzu.backend.mesa.Mesa;
import com.yuzu.backend.mesa.MesaRepository;
import com.yuzu.backend.mesa.Salon;
import com.yuzu.backend.mesa.SalonRepository;
import com.yuzu.backend.pedido.Pedido;
import com.yuzu.backend.pedido.PedidoItem;
import com.yuzu.backend.pedido.PedidoItemRepository;
import com.yuzu.backend.pedido.PedidoRepository;

/**
 * Pantalla de cocina. El restaurante sale siempre del token (SesionActual) y todas las búsquedas lo incluyen.
 */
@Service
public class CocinaService {

    /** Cuántos platos "listos" se muestran para poder deshacer un error. */
    private static final int MAX_LISTOS = 30;

    /** Los "listos" más viejos que esto ya no se muestran. */
    private static final int HORAS_DE_LISTOS = 3;

    private final PedidoItemRepository items;
    private final PedidoRepository pedidos;
    private final MesaRepository mesas;
    private final SalonRepository salones;

    public CocinaService(PedidoItemRepository items,
                         PedidoRepository pedidos,
                         MesaRepository mesas,
                         SalonRepository salones) {
        this.items = items;
        this.pedidos = pedidos;
        this.mesas = mesas;
        this.salones = salones;
    }

    @Transactional(readOnly = true)
    public CocinaDto estado() {
        return armar(SesionActual.restauranteId());
    }

    /** Cocina marca platos como listos. Lo que ya no esté "en cocina" (por ejemplo, anulado) se ignora. */
    @Transactional
    public CocinaDto marcarListos(List<Long> ids) {
        Long restauranteId = SesionActual.restauranteId();

        // Lo primero que se lee, y bloqueado: así no se pisa con quien anula o sirve el mismo plato a la vez.
        List<PedidoItem> platos = items.bloquearPorIds(restauranteId, ids.stream().distinct().toList());

        LocalDateTime ahora = Tiempo.ahora();
        for (PedidoItem plato : platos) {
            if (PedidoItem.ENVIADO.equals(plato.getEstado()) && plato.isPasaPorCocina()) {
                plato.setEstado(PedidoItem.LISTO);
                plato.setListoEn(ahora);
            }
        }
        items.saveAll(platos);
        return armar(restauranteId);
    }

    /** Deshace un "listo" por error: el plato vuelve a la lista de cocina. */
    @Transactional
    public CocinaDto regresar(List<Long> ids) {
        Long restauranteId = SesionActual.restauranteId();
        List<PedidoItem> platos = items.bloquearPorIds(restauranteId, ids.stream().distinct().toList());

        for (PedidoItem plato : platos) {
            if (PedidoItem.LISTO.equals(plato.getEstado()) && plato.isPasaPorCocina()) {
                plato.setEstado(PedidoItem.ENVIADO);
                plato.setListoEn(null);
            }
        }
        items.saveAll(platos);
        return armar(restauranteId);
    }

    /** Cocina confirma que vio el aviso de plato anulado: deja de mostrarse. */
    @Transactional
    public CocinaDto marcarVistos(List<Long> ids) {
        Long restauranteId = SesionActual.restauranteId();
        List<PedidoItem> platos = items.bloquearPorIds(restauranteId, ids.stream().distinct().toList());

        for (PedidoItem plato : platos) {
            if (PedidoItem.ANULADO.equals(plato.getEstado()) && plato.isAvisoCocina()) {
                plato.setAvisoCocina(false);
            }
        }
        items.saveAll(platos);
        return armar(restauranteId);
    }

    // ------------------------------------------------------------------ ayudas

    private CocinaDto armar(Long restauranteId) {
        List<PedidoItem> porPreparar = items.deCocina(restauranteId, PedidoItem.ENVIADO);
        List<PedidoItem> listos = items.listosDeCocina(restauranteId,
                Tiempo.ahora().minusHours(HORAS_DE_LISTOS), PageRequest.of(0, MAX_LISTOS));
        List<PedidoItem> anulados = items.anuladosSinVer(restauranteId);

        // Nombres de mesa y salón de las cuentas involucradas.
        List<Long> idsPedidos = Stream.of(porPreparar, listos, anulados)
                .flatMap(List::stream)
                .map(PedidoItem::getPedidoId)
                .distinct()
                .toList();
        Map<Long, Pedido> pedidosPorId = idsPedidos.isEmpty() ? Map.of()
                : pedidos.findByRestauranteIdAndIdIn(restauranteId, idsPedidos).stream()
                        .collect(Collectors.toMap(Pedido::getId, p -> p));
        Map<Long, Mesa> mesasPorId = new HashMap<>();
        for (Mesa mesa : mesas.findByRestauranteId(restauranteId)) {
            mesasPorId.put(mesa.getId(), mesa);
        }
        Map<Long, String> nombresDeSalon = new HashMap<>();
        for (Salon salon : salones.findByRestauranteIdOrderByOrdenAscNombreAsc(restauranteId)) {
            nombresDeSalon.put(salon.getId(), salon.getNombre());
        }

        // Un ticket por cada envío: los platos de la misma cuenta enviados en el mismo momento.
        Map<String, List<PedidoItem>> porTicket = new LinkedHashMap<>();
        for (PedidoItem plato : porPreparar) {
            porTicket.computeIfAbsent(clave(plato), k -> new ArrayList<>()).add(plato);
        }

        List<TicketCocinaDto> tickets = porTicket.entrySet().stream()
                .map(e -> {
                    List<PedidoItem> platos = e.getValue();
                    PedidoItem primero = platos.get(0);
                    Pedido pedido = pedidosPorId.get(primero.getPedidoId());
                    Mesa mesa = pedido == null ? null : mesasPorId.get(pedido.getMesaId());
                    return new TicketCocinaDto(
                            e.getKey(),
                            primero.getPedidoId(),
                            mesa == null ? "—" : mesa.getNombre(),
                            mesa == null ? "" : nombresDeSalon.getOrDefault(mesa.getSalonId(), ""),
                            pedido == null ? "" : pedido.getMozo(),
                            Tiempo.iso(primero.getEnviadoEn()),
                            platos.stream()
                                    .map(p -> new ItemCocinaDto(p.getId(), p.getNombre(), p.getCantidad(), p.getNota()))
                                    .toList());
                })
                .toList();

        List<ListoCocinaDto> listosDto = listos.stream()
                .map(p -> {
                    Pedido pedido = pedidosPorId.get(p.getPedidoId());
                    Mesa mesa = pedido == null ? null : mesasPorId.get(pedido.getMesaId());
                    return new ListoCocinaDto(p.getId(), p.getPedidoId(), mesa == null ? "—" : mesa.getNombre(),
                            p.getNombre(), p.getCantidad(), p.getNota(), Tiempo.iso(p.getListoEn()));
                })
                .toList();

        List<AnuladoCocinaDto> anuladosDto = anulados.stream()
                .map(p -> {
                    Pedido pedido = pedidosPorId.get(p.getPedidoId());
                    Mesa mesa = pedido == null ? null : mesasPorId.get(pedido.getMesaId());
                    return new AnuladoCocinaDto(p.getId(), p.getPedidoId(), mesa == null ? "—" : mesa.getNombre(),
                            p.getNombre(), p.getCantidad(), p.getNota(), p.getListoEn() != null,
                            Tiempo.iso(p.getAnuladoEn()));
                })
                .toList();

        return new CocinaDto(tickets, listosDto, anuladosDto);
    }

    private static String clave(PedidoItem plato) {
        return plato.getPedidoId() + "-" + Tiempo.iso(plato.getEnviadoEn());
    }
}
