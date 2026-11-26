package com.yuzu.backend.pedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.yuzu.backend.catalogo.Categoria;
import com.yuzu.backend.catalogo.CategoriaRepository;
import com.yuzu.backend.catalogo.Producto;
import com.yuzu.backend.catalogo.ProductoRepository;
import com.yuzu.backend.config.SesionActual;
import com.yuzu.backend.mesa.Mesa;
import com.yuzu.backend.mesa.MesaRepository;
import com.yuzu.backend.mesa.Salon;
import com.yuzu.backend.mesa.SalonRepository;
import com.yuzu.backend.usuario.UsuarioRepository;

/**
 * Pedidos de salón. El restaurante sale siempre del token (SesionActual) y todas las búsquedas lo
 * incluyen: lo que pertenece a otro restaurante responde "no encontrado".
 */
@Service
public class PedidosService {

    static final String ABIERTO = Pedido.ABIERTO;
    static final String ANULADO = Pedido.ANULADO;
    public static final String PENDIENTE = "PENDIENTE";
    static final String ENVIADO = PedidoItem.ENVIADO;
    static final String LISTO = PedidoItem.LISTO;
    static final String SERVIDO = PedidoItem.SERVIDO;

    private static final int MAX_POR_PRODUCTO = 99;

    private final PedidoRepository pedidos;
    private final PedidoItemRepository items;
    private final MesaRepository mesas;
    private final SalonRepository salones;
    private final ProductoRepository productos;
    private final CategoriaRepository categorias;
    private final UsuarioRepository usuarios;

    public PedidosService(PedidoRepository pedidos,
                          PedidoItemRepository items,
                          MesaRepository mesas,
                          SalonRepository salones,
                          ProductoRepository productos,
                          CategoriaRepository categorias,
                          UsuarioRepository usuarios) {
        this.pedidos = pedidos;
        this.items = items;
        this.mesas = mesas;
        this.salones = salones;
        this.productos = productos;
        this.categorias = categorias;
        this.usuarios = usuarios;
    }

    // ------------------------------------------------------------------ consultas

    /** Las cuentas abiertas del restaurante, para saber qué mesas están ocupadas. */
    @Transactional(readOnly = true)
    public List<PedidoResumenDto> abiertos() {
        Long restauranteId = SesionActual.restauranteId();

        List<Pedido> abiertos = pedidos.findByRestauranteIdAndEstado(restauranteId, ABIERTO);
        if (abiertos.isEmpty()) {
            return List.of();
        }

        List<Long> ids = abiertos.stream().map(Pedido::getId).toList();
        Map<Long, List<PedidoItem>> porPedido = items.findByRestauranteIdAndPedidoIdIn(restauranteId, ids).stream()
                .collect(Collectors.groupingBy(PedidoItem::getPedidoId));

        return abiertos.stream()
                .map(p -> {
                    List<PedidoItem> lista = porPedido.getOrDefault(p.getId(), List.of());
                    return new PedidoResumenDto(
                            p.getId(), p.getMesaId(), p.getMozo(), iso(p.getAbiertoEn()),
                            total(lista), unidades(lista), pendientes(lista),
                            cuantos(lista, ENVIADO), cuantos(lista, LISTO));
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public PedidoDto obtener(Long id) {
        return armar(pedido(id, SesionActual.restauranteId()));
    }

    // ------------------------------------------------------------------ pedido

    /** Abre la cuenta de una mesa. Si ya había una abierta, devuelve esa misma. */
    @Transactional
    public PedidoDto abrir(Long mesaId) {
        Long restauranteId = SesionActual.restauranteId();

        // Si la mesa es de otro restaurante, se responde "no encontrada".
        Mesa mesa = mesas.findByIdAndRestauranteId(mesaId, restauranteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mesa no encontrada"));
        if (!mesa.isActivo()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La mesa " + mesa.getNombre() + " está desactivada");
        }

        Pedido existente = pedidos.findByRestauranteIdAndMesaIdAndEstado(restauranteId, mesaId, ABIERTO).orElse(null);
        if (existente != null) {
            return armar(existente);
        }

        Pedido pedido = new Pedido();
        pedido.setRestauranteId(restauranteId);
        pedido.setMesaId(mesaId);
        pedido.setMozo(nombreDeQuienAtiende(restauranteId));
        pedido.setAbiertoEn(ahora());
        return armar(pedidos.save(pedido));
    }

    /** Anula toda la cuenta y libera la mesa. Solo el administrador (ver SecurityConfig). */
    @Transactional
    public PedidoDto anular(Long id) {
        Long restauranteId = SesionActual.restauranteId();
        Pedido pedido = abierto(id);
        LocalDateTime ahora = ahora();

        pedido.setEstado(ANULADO);
        pedido.setCerradoEn(ahora);

        // Lo que ya estaba en cocina se anula también, y cocina recibe el aviso para no prepararlo.
        for (PedidoItem item : items.findByRestauranteIdAndPedidoIdOrderByIdAsc(restauranteId, pedido.getId())) {
            if (ENVIADO.equals(item.getEstado()) || LISTO.equals(item.getEstado())) {
                anularPlato(item, ahora);
                items.save(item);
            }
        }
        return armar(pedidos.save(pedido));
    }

    // ------------------------------------------------------------------ productos del pedido

    @Transactional
    public PedidoDto agregarItem(Long pedidoId, ItemPedidoRequest request) {
        Long restauranteId = SesionActual.restauranteId();
        Pedido pedido = abierto(pedidoId);

        Producto producto = productos.findByIdAndRestauranteId(request.productoId(), restauranteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
        if (!producto.isActivo()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El producto \"" + producto.getNombre() + "\" no está disponible");
        }

        String nota = limpiar(request.nota());

        // Si ya hay el mismo producto sin enviar y con la misma nota, se suma a esa línea.
        PedidoItem igual = items.findByRestauranteIdAndPedidoIdOrderByIdAsc(restauranteId, pedido.getId()).stream()
                .filter(i -> PENDIENTE.equals(i.getEstado()))
                .filter(i -> i.getProductoId().equals(producto.getId()))
                .filter(i -> Objects.equals(i.getNota(), nota))
                .findFirst()
                .orElse(null);

        if (igual != null) {
            int nueva = igual.getCantidad() + request.cantidad();
            if (nueva > MAX_POR_PRODUCTO) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No puedes pedir más de " + MAX_POR_PRODUCTO + " unidades del mismo producto en una línea");
            }
            igual.setCantidad(nueva);
            items.save(igual);
        } else {
            PedidoItem item = new PedidoItem();
            item.setRestauranteId(restauranteId);
            item.setPedidoId(pedido.getId());
            item.setProductoId(producto.getId());
            item.setNombre(producto.getNombre());
            item.setPrecioUnitario(producto.getPrecio());
            item.setCantidad(request.cantidad());
            item.setNota(nota);
            items.save(item);
        }
        return armar(pedido);
    }

    @Transactional
    public PedidoDto actualizarItem(Long pedidoId, Long itemId, ActualizarItemRequest request) {
        Long restauranteId = SesionActual.restauranteId();
        Pedido pedido = abierto(pedidoId);
        PedidoItem item = item(itemId, pedido.getId(), restauranteId);
        exigirPendiente(item);

        item.setCantidad(request.cantidad());
        item.setNota(limpiar(request.nota()));
        items.save(item);
        return armar(pedido);
    }

    @Transactional
    public PedidoDto quitarItem(Long pedidoId, Long itemId) {
        Long restauranteId = SesionActual.restauranteId();
        Pedido pedido = abierto(pedidoId);
        PedidoItem item = item(itemId, pedido.getId(), restauranteId);
        exigirPendiente(item);

        items.delete(item);
        return armar(pedido);
    }

    /**
     * Manda a cocina todo lo que está pendiente. Lo enviado ya no se puede cambiar.
     * Los productos de categorías que no pasan por cocina (por ejemplo, las bebidas) quedan listos para servir
     * de inmediato y no aparecen en la pantalla de cocina.
     */
    @Transactional
    public PedidoDto enviar(Long pedidoId) {
        Long restauranteId = SesionActual.restauranteId();
        Pedido pedido = abierto(pedidoId);

        List<PedidoItem> pendientes = items.findByRestauranteIdAndPedidoIdOrderByIdAsc(restauranteId, pedido.getId())
                .stream()
                .filter(i -> PENDIENTE.equals(i.getEstado()))
                .toList();
        if (pendientes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No hay productos nuevos para enviar a cocina");
        }

        Map<Long, Boolean> pasaPorCocina = categoriasQuePasanPorCocina(restauranteId);

        LocalDateTime ahora = ahora();
        for (PedidoItem item : pendientes) {
            boolean cocina = pasaPorCocina.getOrDefault(item.getProductoId(), true);
            item.setEnviadoEn(ahora);
            item.setPasaPorCocina(cocina);
            if (cocina) {
                item.setEstado(ENVIADO);
            } else {
                item.setEstado(LISTO);
                item.setListoEn(ahora);
            }
        }
        items.saveAll(pendientes);
        return armar(pedido);
    }

    /** Anula un producto que ya estaba en cocina. Solo el administrador (ver SecurityConfig). */
    @Transactional
    public PedidoDto anularItem(Long pedidoId, Long itemId) {
        Long restauranteId = SesionActual.restauranteId();
        Pedido pedido = abierto(pedidoId);
        PedidoItem item = item(itemId, pedido.getId(), restauranteId);

        if (!ENVIADO.equals(item.getEstado()) && !LISTO.equals(item.getEstado()) && !SERVIDO.equals(item.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se anulan los productos que ya se enviaron a cocina. Los demás se pueden quitar directamente.");
        }
        anularPlato(item, ahora());
        items.save(item);
        return armar(pedido);
    }

    /**
     * Anula un plato. Si cocina todavía lo tenía en pantalla (en preparación o ya listo, pero sin servir),
     * queda marcado para avisarle.
     */
    private void anularPlato(PedidoItem item, LocalDateTime ahora) {
        boolean cocinaDebeSaberlo = item.isPasaPorCocina()
                && (ENVIADO.equals(item.getEstado()) || LISTO.equals(item.getEstado()));
        item.setEstado(ANULADO);
        item.setAnuladoEn(ahora);
        item.setAvisoCocina(cocinaDebeSaberlo);
    }

    /** El mozo marca un plato como servido en la mesa. */
    @Transactional
    public PedidoDto servir(Long pedidoId, Long itemId) {
        Long restauranteId = SesionActual.restauranteId();
        Pedido pedido = pedidoParaServir(pedidoId, restauranteId);
        PedidoItem item = item(itemId, pedido.getId(), restauranteId);

        if (SERVIDO.equals(item.getEstado())) {
            return armar(pedido);
        }
        if (!ENVIADO.equals(item.getEstado()) && !LISTO.equals(item.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este producto no se puede marcar como servido");
        }
        marcarServido(item, ahora());
        return armar(pedido);
    }

    /** El mozo se lleva todo lo que está listo y lo marca como servido de una vez. */
    @Transactional
    public PedidoDto servirListos(Long pedidoId) {
        Long restauranteId = SesionActual.restauranteId();
        Pedido pedido = pedidoParaServir(pedidoId, restauranteId);

        LocalDateTime ahora = ahora();
        for (PedidoItem item : items.findByRestauranteIdAndPedidoIdOrderByIdAsc(restauranteId, pedido.getId())) {
            if (LISTO.equals(item.getEstado())) {
                marcarServido(item, ahora);
            }
        }
        return armar(pedido);
    }

    // ------------------------------------------------------------------ ayudas

    private Pedido pedido(Long id, Long restauranteId) {
        return pedidos.findByIdAndRestauranteId(id, restauranteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));
    }

    /**
     * El pedido, pero solo si todavía se puede modificar. Queda bloqueado hasta que termine la operación:
     * así no se le puede agregar un producto a una cuenta que en ese mismo instante se está cobrando.
     */
    private Pedido abierto(Long id) {
        Pedido pedido = pedidos.bloquearPorId(id, SesionActual.restauranteId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));
        if (!ABIERTO.equals(pedido.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este pedido ya está cerrado");
        }
        return pedido;
    }

    /** La cuenta bloqueada para servir. Se puede servir aunque ya esté cobrada, pero no si está anulada. */
    private Pedido pedidoParaServir(Long pedidoId, Long restauranteId) {
        Pedido pedido = pedidos.bloquearPorId(pedidoId, restauranteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));
        if (ANULADO.equals(pedido.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este pedido está anulado");
        }
        return pedido;
    }

    private void marcarServido(PedidoItem item, LocalDateTime ahora) {
        item.setEstado(SERVIDO);
        item.setServidoEn(ahora);
        items.save(item);
    }

    /** Para cada producto de la carta, si su categoría pasa por cocina. */
    private Map<Long, Boolean> categoriasQuePasanPorCocina(Long restauranteId) {
        Map<Long, Boolean> porCategoria = new HashMap<>();
        for (Categoria c : categorias.findByRestauranteIdOrderByOrdenAscNombreAsc(restauranteId)) {
            porCategoria.put(c.getId(), c.isPasaPorCocina());
        }
        Map<Long, Boolean> porProducto = new HashMap<>();
        for (Producto p : productos.findByRestauranteIdOrderByNombreAsc(restauranteId)) {
            porProducto.put(p.getId(), porCategoria.getOrDefault(p.getCategoriaId(), true));
        }
        return porProducto;
    }

    private PedidoItem item(Long itemId, Long pedidoId, Long restauranteId) {
        return items.findByIdAndPedidoIdAndRestauranteId(itemId, pedidoId, restauranteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto del pedido no encontrado"));
    }

    private void exigirPendiente(PedidoItem item) {
        if (!PENDIENTE.equals(item.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Este producto ya se envió a cocina y no se puede cambiar");
        }
    }

    private String nombreDeQuienAtiende(Long restauranteId) {
        return usuarios.findByIdAndRestauranteId(SesionActual.usuarioId(), restauranteId)
                .map(u -> u.getNombre())
                .orElseGet(SesionActual::usuario);
    }

    private PedidoDto armar(Pedido pedido) {
        Long restauranteId = pedido.getRestauranteId();

        Mesa mesa = mesas.findByIdAndRestauranteId(pedido.getMesaId(), restauranteId).orElse(null);
        Salon salon = mesa == null ? null
                : salones.findByIdAndRestauranteId(mesa.getSalonId(), restauranteId).orElse(null);

        List<PedidoItem> lista = items.findByRestauranteIdAndPedidoIdOrderByIdAsc(restauranteId, pedido.getId());

        return new PedidoDto(
                pedido.getId(),
                pedido.getMesaId(),
                mesa == null ? "" : mesa.getNombre(),
                salon == null ? "" : salon.getNombre(),
                pedido.getEstado(),
                pedido.getMozo(),
                iso(pedido.getAbiertoEn()),
                lista.stream().map(ItemDto::desde).toList(),
                total(lista),
                pendientes(lista));
    }

    /** Suma de lo pedido, sin contar lo anulado. */
    public static BigDecimal total(List<PedidoItem> lista) {
        return lista.stream()
                .filter(i -> !ANULADO.equals(i.getEstado()))
                .map(i -> i.getPrecioUnitario().multiply(BigDecimal.valueOf(i.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static int unidades(List<PedidoItem> lista) {
        return lista.stream().filter(i -> !ANULADO.equals(i.getEstado())).mapToInt(PedidoItem::getCantidad).sum();
    }

    private static int cuantos(List<PedidoItem> lista, String estado) {
        return (int) lista.stream().filter(i -> estado.equals(i.getEstado())).count();
    }

    private static int pendientes(List<PedidoItem> lista) {
        return (int) lista.stream().filter(i -> PENDIENTE.equals(i.getEstado())).count();
    }

    private static LocalDateTime ahora() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private static String iso(LocalDateTime fecha) {
        return fecha == null ? null : fecha.toInstant(ZoneOffset.UTC).toString();
    }

    private static String limpiar(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim();
    }
}
