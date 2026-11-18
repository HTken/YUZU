package com.yuzu.backend.plataforma;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.yuzu.backend.config.SesionActual;
import com.yuzu.backend.plan.Plan;
import com.yuzu.backend.plan.PlanRepository;
import com.yuzu.backend.restaurante.Restaurante;
import com.yuzu.backend.restaurante.RestauranteRepository;
import com.yuzu.backend.suscripcion.ResultadoVencimientosDto;
import com.yuzu.backend.suscripcion.SuscripcionService;
import com.yuzu.backend.usuario.Usuario;
import com.yuzu.backend.usuario.UsuarioRepository;

/** Operaciones del equipo de Yuzu sobre los restaurantes clientes. */
@Service
public class PlataformaService {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final Pattern SUBDOMINIO = Pattern.compile("^[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?$");
    private static final Set<String> RESERVADOS =
            Set.of("www", "api", "app", "admin", "plataforma", "mail", "soporte", "yuzu");

    private final RestauranteRepository restaurantes;
    private final PlanRepository planes;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final SuscripcionService suscripcion;

    public PlataformaService(RestauranteRepository restaurantes,
                             PlanRepository planes,
                             UsuarioRepository usuarios,
                             PasswordEncoder passwordEncoder,
                             SuscripcionService suscripcion) {
        this.restaurantes = restaurantes;
        this.planes = planes;
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.suscripcion = suscripcion;
    }

    @Transactional(readOnly = true)
    public List<PlanDto> listarPlanes() {
        return planes.findAll(Sort.by("precioMensual")).stream().map(PlanDto::desde).toList();
    }

    @Transactional(readOnly = true)
    public List<RestauranteResumenDto> listarRestaurantes() {
        Map<Long, Plan> planesPorId = planes.findAll().stream()
                .collect(Collectors.toMap(Plan::getId, Function.identity()));

        return restaurantes.findAll(Sort.by("nombre")).stream()
                .map(r -> resumen(r, planesPorId.get(r.getPlanId())))
                .toList();
    }

    @Transactional
    public RestauranteResumenDto crear(RestauranteCrearRequest request) {
        String subdominio = request.subdominio().trim().toLowerCase();
        if (!SUBDOMINIO.matcher(subdominio).matches() || RESERVADOS.contains(subdominio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El subdominio no es válido. Usa letras minúsculas, números y guiones, sin espacios.");
        }
        if (restaurantes.findBySubdominio(subdominio).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese subdominio ya está en uso");
        }

        Plan plan = plan(request.planId());
        if (!plan.isActivo()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ese plan ya no se vende");
        }

        Restaurante restaurante = new Restaurante();
        restaurante.setNombre(request.nombre().trim());
        restaurante.setSubdominio(subdominio);
        restaurante.setPlanId(plan.getId());
        restaurante.setVenceEn(LocalDate.now(LIMA).plusDays(request.diasDePrueba()));
        restaurante = restaurantes.save(restaurante);

        Usuario admin = new Usuario();
        admin.setRestauranteId(restaurante.getId());
        admin.setUsuario(request.adminUsuario().trim().toLowerCase());
        admin.setNombre(request.adminNombre().trim());
        admin.setRol("ADMIN");
        admin.setClaveHash(passwordEncoder.encode(request.adminClave()));
        usuarios.save(admin);

        return resumen(restaurante, plan);
    }

    @Transactional
    public RestauranteResumenDto cambiarPlan(Long id, Long planId) {
        Restaurante restaurante = restaurante(id);
        Plan nuevo = plan(planId);

        if (!nuevo.isActivo()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ese plan ya no se vende");
        }

        long activos = usuarios.countByRestauranteIdAndActivoTrue(id);
        if (activos > nuevo.getMaxUsuarios()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El restaurante tiene " + activos + " usuarios activos y el plan " + nuevo.getNombre()
                            + " permite " + nuevo.getMaxUsuarios()
                            + ". Debe desactivar usuarios antes de bajar de plan.");
        }

        restaurante.setPlanId(nuevo.getId());
        restaurantes.save(restaurante);
        return resumen(restaurante, nuevo);
    }

    @Transactional
    public RestauranteResumenDto suspender(Long id) {
        Restaurante restaurante = restaurante(id);
        restaurante.setEstado("SUSPENDIDO");
        restaurantes.save(restaurante);
        return resumen(restaurante, plan(restaurante.getPlanId()));
    }

    @Transactional
    public RestauranteResumenDto reactivar(Long id) {
        Restaurante restaurante = restaurante(id);
        restaurante.setEstado("ACTIVO");
        restaurantes.save(restaurante);
        return resumen(restaurante, plan(restaurante.getPlanId()));
    }

    /** Registra un pago hecho fuera del sistema (efectivo, transferencia, Yape...) y renueva. */
    @Transactional
    public RestauranteResumenDto renovar(Long id, int meses, String metodo, String referencia) {
        suscripcion.registrarPago(id, meses, metodo, referencia, SesionActual.usuario());

        Restaurante restaurante = restaurante(id);
        return resumen(restaurante, plan(restaurante.getPlanId()));
    }

    /** Lo mismo que hace la revisión automática de cada noche, pero a pedido. */
    public ResultadoVencimientosDto procesarVencimientos() {
        return suscripcion.procesarVencimientos();
    }

    private RestauranteResumenDto resumen(Restaurante r, Plan p) {
        long activos = usuarios.countByRestauranteIdAndActivoTrue(r.getId());
        long dias = ChronoUnit.DAYS.between(LocalDate.now(LIMA), r.getVenceEn());
        return new RestauranteResumenDto(
                r.getId(),
                r.getNombre(),
                r.getSubdominio(),
                r.getEstado(),
                p.getId(),
                p.getNombre(),
                r.getVenceEn().toString(),
                (int) dias,
                (int) activos,
                p.getMaxUsuarios());
    }

    private Restaurante restaurante(Long id) {
        return restaurantes.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurante no encontrado"));
    }

    private Plan plan(Long id) {
        return planes.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan no encontrado"));
    }
}
