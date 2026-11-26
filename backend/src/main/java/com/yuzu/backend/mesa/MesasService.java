package com.yuzu.backend.mesa;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.yuzu.backend.config.SesionActual;
import com.yuzu.backend.pedido.Pedido;
import com.yuzu.backend.pedido.PedidoRepository;
import com.yuzu.backend.suscripcion.SuscripcionService;

/**
 * Salones y mesas del restaurante. El restaurante sale siempre del token (SesionActual) y todas las
 * búsquedas lo incluyen: lo que pertenece a otro restaurante responde "no encontrado".
 */
@Service
public class MesasService {

    /** Parte "Mesa 10" en "Mesa " y "10" para ordenar los números como personas, no como texto. */
    private static final Pattern TROZOS = Pattern.compile("(?<=\\d)(?=\\D)|(?<=\\D)(?=\\d)");

    private final SalonRepository salones;
    private final MesaRepository mesas;
    private final PedidoRepository pedidos;
    private final SuscripcionService suscripcion;

    public MesasService(SalonRepository salones,
                        MesaRepository mesas,
                        PedidoRepository pedidos,
                        SuscripcionService suscripcion) {
        this.salones = salones;
        this.mesas = mesas;
        this.pedidos = pedidos;
        this.suscripcion = suscripcion;
    }

    // ------------------------------------------------------------------- salones

    @Transactional(readOnly = true)
    public List<SalonDto> listarSalones() {
        Long restauranteId = SesionActual.restauranteId();

        Map<Long, Integer> totales = new HashMap<>();
        Map<Long, Integer> activas = new HashMap<>();
        for (Mesa m : mesas.findByRestauranteId(restauranteId)) {
            totales.merge(m.getSalonId(), 1, Integer::sum);
            if (m.isActivo()) {
                activas.merge(m.getSalonId(), 1, Integer::sum);
            }
        }

        return salones.findByRestauranteIdOrderByOrdenAscNombreAsc(restauranteId).stream()
                .map(s -> SalonDto.desde(s, totales.getOrDefault(s.getId(), 0), activas.getOrDefault(s.getId(), 0)))
                .toList();
    }

    @Transactional
    public SalonDto crearSalon(SalonRequest request) {
        Long restauranteId = SesionActual.restauranteId();
        String nombre = request.nombre().trim();

        if (salones.existsByRestauranteIdAndNombreIgnoreCase(restauranteId, nombre)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya tienes un salón con ese nombre");
        }

        Salon salon = new Salon();
        salon.setRestauranteId(restauranteId);
        salon.setNombre(nombre);
        salon.setOrden(request.orden());
        salon.setActivo(request.activo());
        return SalonDto.desde(salones.save(salon), 0, 0);
    }

    @Transactional
    public SalonDto actualizarSalon(Long id, SalonRequest request) {
        Long restauranteId = SesionActual.restauranteId();
        Salon salon = salon(id, restauranteId);
        String nombre = request.nombre().trim();

        if (salones.existsByRestauranteIdAndNombreIgnoreCaseAndIdNot(restauranteId, nombre, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya tienes un salón con ese nombre");
        }

        if (salon.isActivo() && !request.activo()) {
            long activas = mesas.countByRestauranteIdAndSalonIdAndActivoTrue(restauranteId, id);
            if (activas > 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El salón tiene " + activas + (activas == 1 ? " mesa activa" : " mesas activas")
                                + ". Desactívalas o pásalas a otro salón antes de desactivarlo.");
            }
        }

        salon.setNombre(nombre);
        salon.setOrden(request.orden());
        salon.setActivo(request.activo());
        salones.save(salon);

        List<Mesa> deSalon = mesas.findByRestauranteId(restauranteId).stream()
                .filter(m -> m.getSalonId().equals(id))
                .toList();
        int activas = (int) deSalon.stream().filter(Mesa::isActivo).count();
        return SalonDto.desde(salon, deSalon.size(), activas);
    }

    // -------------------------------------------------------------------- mesas

    @Transactional(readOnly = true)
    public List<MesaDto> listarMesas() {
        Long restauranteId = SesionActual.restauranteId();

        List<Salon> ordenados = salones.findByRestauranteIdOrderByOrdenAscNombreAsc(restauranteId);
        Map<Long, Integer> posicion = new HashMap<>();
        Map<Long, String> nombres = new HashMap<>();
        for (int i = 0; i < ordenados.size(); i++) {
            posicion.put(ordenados.get(i).getId(), i);
            nombres.put(ordenados.get(i).getId(), ordenados.get(i).getNombre());
        }

        return ordenarMesas(mesas.findByRestauranteId(restauranteId), posicion).stream()
                .map(m -> MesaDto.desde(m, nombres.get(m.getSalonId())))
                .toList();
    }

    @Transactional
    public MesaDto crearMesa(MesaRequest request) {
        Long restauranteId = SesionActual.restauranteId();
        String nombre = request.nombre().trim();

        // Si el salón es de otro restaurante, se responde "no encontrado".
        Salon salon = salon(request.salonId(), restauranteId);

        if (mesas.existsByRestauranteIdAndSalonIdAndNombreIgnoreCase(restauranteId, salon.getId(), nombre)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya hay una mesa con ese nombre en el salón " + salon.getNombre());
        }
        exigirSalonActivoSiCorresponde(salon, request.activo());
        if (request.activo()) {
            suscripcion.exigirCupoDeMesas(restauranteId, 1);
        }

        Mesa mesa = new Mesa();
        mesa.setRestauranteId(restauranteId);
        mesa.setSalonId(salon.getId());
        mesa.setNombre(nombre);
        mesa.setCapacidad(request.capacidad());
        mesa.setActivo(request.activo());
        return MesaDto.desde(mesas.save(mesa), salon.getNombre());
    }

    @Transactional
    public MesaDto actualizarMesa(Long id, MesaRequest request) {
        Long restauranteId = SesionActual.restauranteId();
        Mesa mesa = mesas.findByIdAndRestauranteId(id, restauranteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mesa no encontrada"));
        Salon salon = salon(request.salonId(), restauranteId);
        String nombre = request.nombre().trim();

        if (mesas.existsByRestauranteIdAndSalonIdAndNombreIgnoreCaseAndIdNot(restauranteId, salon.getId(), nombre, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya hay una mesa con ese nombre en el salón " + salon.getNombre());
        }
        exigirSalonActivoSiCorresponde(salon, request.activo());

        // Una mesa con la cuenta abierta no se puede desactivar: quedaría una cuenta que nadie ve.
        if (mesa.isActivo() && !request.activo()
                && pedidos.existsByRestauranteIdAndMesaIdAndEstado(restauranteId, id, Pedido.ABIERTO)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La mesa " + mesa.getNombre() + " tiene una cuenta abierta. Anúlala antes de desactivar la mesa.");
        }

        // Volver a activar una mesa también consume un cupo del plan.
        if (request.activo() && !mesa.isActivo()) {
            suscripcion.exigirCupoDeMesas(restauranteId, 1);
        }

        mesa.setSalonId(salon.getId());
        mesa.setNombre(nombre);
        mesa.setCapacidad(request.capacidad());
        mesa.setActivo(request.activo());
        return MesaDto.desde(mesas.save(mesa), salon.getNombre());
    }

    /** Crea "Mesa 1", "Mesa 2"… de una vez. Todo o nada: si algo falla, no se crea ninguna. */
    @Transactional
    public List<MesaDto> crearLote(MesaLoteRequest request) {
        Long restauranteId = SesionActual.restauranteId();
        Salon salon = salon(request.salonId(), restauranteId);
        exigirSalonActivoSiCorresponde(salon, true);

        String prefijo = request.prefijo().trim();
        List<String> nombres = new ArrayList<>();
        for (int i = 0; i < request.cantidad(); i++) {
            nombres.add(prefijo + " " + (request.desde() + i));
        }

        for (String nombre : nombres) {
            if (mesas.existsByRestauranteIdAndSalonIdAndNombreIgnoreCase(restauranteId, salon.getId(), nombre)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Ya existe \"" + nombre + "\" en el salón " + salon.getNombre()
                                + ". Cambia el número inicial o el nombre base.");
            }
        }
        suscripcion.exigirCupoDeMesas(restauranteId, nombres.size());

        List<Mesa> nuevas = new ArrayList<>();
        for (String nombre : nombres) {
            Mesa mesa = new Mesa();
            mesa.setRestauranteId(restauranteId);
            mesa.setSalonId(salon.getId());
            mesa.setNombre(nombre);
            mesa.setCapacidad(request.capacidad());
            mesa.setActivo(true);
            nuevas.add(mesa);
        }

        return ordenarMesas(mesas.saveAll(nuevas), Map.of()).stream()
                .map(m -> MesaDto.desde(m, salon.getNombre()))
                .toList();
    }

    // ------------------------------------------------------------------- ayudas

    private void exigirSalonActivoSiCorresponde(Salon salon, boolean mesaActiva) {
        if (mesaActiva && !salon.isActivo()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El salón \"" + salon.getNombre()
                            + "\" está desactivado. Actívalo o elige otro para dejar activa esta mesa.");
        }
    }

    private Salon salon(Long id, Long restauranteId) {
        return salones.findByIdAndRestauranteId(id, restauranteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Salón no encontrado"));
    }

    /** Primero por la posición de su salón y luego por nombre, con "Mesa 2" antes de "Mesa 10". */
    private static List<Mesa> ordenarMesas(List<Mesa> lista, Map<Long, Integer> posicionDeSalon) {
        Comparator<Mesa> comparador = Comparator
                .comparingInt((Mesa m) -> posicionDeSalon.getOrDefault(m.getSalonId(), 0))
                .thenComparing((a, b) -> compararNatural(a.getNombre(), b.getNombre()));
        return lista.stream().sorted(comparador).toList();
    }

    static int compararNatural(String a, String b) {
        String[] x = TROZOS.split(a.toLowerCase(Locale.ROOT));
        String[] y = TROZOS.split(b.toLowerCase(Locale.ROOT));
        int comunes = Math.min(x.length, y.length);
        for (int i = 0; i < comunes; i++) {
            int resultado = compararTrozo(x[i], y[i]);
            if (resultado != 0) {
                return resultado;
            }
        }
        return Integer.compare(x.length, y.length);
    }

    private static int compararTrozo(String a, String b) {
        boolean numeroA = !a.isEmpty() && Character.isDigit(a.charAt(0));
        boolean numeroB = !b.isEmpty() && Character.isDigit(b.charAt(0));
        if (numeroA && numeroB) {
            String sinCerosA = a.replaceFirst("^0+(?=\\d)", "");
            String sinCerosB = b.replaceFirst("^0+(?=\\d)", "");
            if (sinCerosA.length() != sinCerosB.length()) {
                return Integer.compare(sinCerosA.length(), sinCerosB.length());
            }
            return sinCerosA.compareTo(sinCerosB);
        }
        return a.compareTo(b);
    }
}
