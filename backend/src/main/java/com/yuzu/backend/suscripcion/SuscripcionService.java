package com.yuzu.backend.suscripcion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.yuzu.backend.catalogo.ProductoRepository;
import com.yuzu.backend.config.SesionActual;
import com.yuzu.backend.inventario.InsumoRepository;
import com.yuzu.backend.mesa.MesaRepository;
import com.yuzu.backend.pago.Pago;
import com.yuzu.backend.pago.PagoRepository;
import com.yuzu.backend.plan.Plan;
import com.yuzu.backend.plan.PlanRepository;
import com.yuzu.backend.restaurante.Restaurante;
import com.yuzu.backend.restaurante.RestauranteRepository;
import com.yuzu.backend.usuario.UsuarioRepository;

@Service
public class SuscripcionService {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    private final RestauranteRepository restaurantes;
    private final PlanRepository planes;
    private final UsuarioRepository usuarios;
    private final ProductoRepository productos;
    private final MesaRepository mesas;
    private final InsumoRepository insumos;
    private final PagoRepository pagos;
    private final AvisoVencimientoService avisoVencimiento;
    private final int diasGracia;
    private final int diasAviso;

    public SuscripcionService(RestauranteRepository restaurantes,
                              PlanRepository planes,
                              UsuarioRepository usuarios,
                              ProductoRepository productos,
                              MesaRepository mesas,
                              InsumoRepository insumos,
                              PagoRepository pagos,
                              AvisoVencimientoService avisoVencimiento,
                              @Value("${yuzu.suscripcion.dias-gracia:3}") int diasGracia,
                              @Value("${yuzu.suscripcion.dias-aviso:3}") int diasAviso) {
        this.restaurantes = restaurantes;
        this.planes = planes;
        this.usuarios = usuarios;
        this.productos = productos;
        this.mesas = mesas;
        this.insumos = insumos;
        this.pagos = pagos;
        this.avisoVencimiento = avisoVencimiento;
        this.diasGracia = diasGracia;
        this.diasAviso = diasAviso;
    }

    @Transactional(readOnly = true)
    public MiPlanDto miPlan() {
        Restaurante restaurante = restaurante(SesionActual.restauranteId());
        Plan plan = plan(restaurante);

        long activos = usuarios.countByRestauranteIdAndActivoTrue(restaurante.getId());
        long productosActivos = productos.countByRestauranteIdAndActivoTrue(restaurante.getId());
        long mesasActivas = mesas.countByRestauranteIdAndActivoTrue(restaurante.getId());
        long insumosActivos = insumos.countByRestauranteIdAndActivoTrue(restaurante.getId());
        long dias = ChronoUnit.DAYS.between(LocalDate.now(LIMA), restaurante.getVenceEn());

        return new MiPlanDto(
                plan.getNombre(),
                plan.getPrecioMensual(),
                plan.getMaxUsuarios(),
                (int) activos,
                plan.getMaxProductos(),
                (int) productosActivos,
                plan.getMaxMesas(),
                (int) mesasActivas,
                plan.getMaxInsumos(),
                (int) insumosActivos,
                restaurante.getVenceEn().toString(),
                (int) dias,
                restaurante.getEstado());
    }

    /** Falla si el restaurante ya usa todos los usuarios que permite su plan. */
    public void exigirCupoDeUsuario(Long restauranteId) {
        Restaurante restaurante = restaurante(restauranteId);
        Plan plan = plan(restaurante);

        long activos = usuarios.countByRestauranteIdAndActivoTrue(restauranteId);
        if (activos >= plan.getMaxUsuarios()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Tu plan " + plan.getNombre() + " permite hasta " + plan.getMaxUsuarios()
                            + " usuarios activos. Desactiva a alguien o mejora tu plan.");
        }
    }

    /** Falla si el restaurante ya usa todos los productos activos que permite su plan. */
    public void exigirCupoDeProductos(Long restauranteId) {
        Restaurante restaurante = restaurante(restauranteId);
        Plan plan = plan(restaurante);

        long activos = productos.countByRestauranteIdAndActivoTrue(restauranteId);
        if (activos >= plan.getMaxProductos()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Tu plan " + plan.getNombre() + " permite hasta " + plan.getMaxProductos()
                            + " productos activos. Desactiva alguno o mejora tu plan.");
        }
    }

    /**
     * Falla si agregar {@code adicionales} mesas activas pasaría el límite del plan.
     * Se usa con 1 para una mesa y con N para crear varias de una vez.
     */
    public void exigirCupoDeMesas(Long restauranteId, int adicionales) {
        Restaurante restaurante = restaurante(restauranteId);
        Plan plan = plan(restaurante);

        long activas = mesas.countByRestauranteIdAndActivoTrue(restauranteId);
        if (activas + adicionales > plan.getMaxMesas()) {
            long libres = Math.max(0, plan.getMaxMesas() - activas);
            String mensaje = adicionales == 1
                    ? "Tu plan " + plan.getNombre() + " permite hasta " + plan.getMaxMesas()
                            + " mesas activas. Desactiva alguna o mejora tu plan."
                    : "Tu plan " + plan.getNombre() + " permite hasta " + plan.getMaxMesas()
                            + " mesas activas y ya tienes " + activas + ": solo puedes agregar " + libres
                            + " más. Desactiva alguna o mejora tu plan.";
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, mensaje);
        }
    }

    /** Falla si el restaurante ya usa todos los insumos activos que permite su plan. */
    public void exigirCupoDeInsumos(Long restauranteId) {
        Restaurante restaurante = restaurante(restauranteId);
        Plan plan = plan(restaurante);

        long activos = insumos.countByRestauranteIdAndActivoTrue(restauranteId);
        if (activos >= plan.getMaxInsumos()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Tu plan " + plan.getNombre() + " permite hasta " + plan.getMaxInsumos()
                            + " insumos activos. Desactiva alguno o mejora tu plan.");
        }
    }

    /** Falla si la suscripción del restaurante está suspendida. */
    public void exigirActiva(Long restauranteId) {
        Restaurante restaurante = restaurante(restauranteId);
        if ("SUSPENDIDO".equals(restaurante.getEstado())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Tu suscripción está suspendida. Renueva tu plan para seguir usando el sistema.");
        }
    }

    /**
     * Registra un pago, extiende el vencimiento y deja el restaurante activo.
     * Si ya había vencido, los meses se cuentan desde hoy.
     * Es la operación que usará también el cobro automático: si llega dos veces
     * la misma referencia del mismo método, la segunda se rechaza y no se cobra doble.
     */
    @Transactional
    public void registrarPago(Long restauranteId, int meses, String metodo, String referencia, String registradoPor) {
        Restaurante restaurante = restaurante(restauranteId);
        Plan plan = plan(restaurante);

        String ref = (referencia == null || referencia.isBlank()) ? null : referencia.trim();
        if (ref != null && pagos.existsByMetodoAndReferencia(metodo, ref)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese pago ya fue registrado");
        }

        LocalDate hoy = LocalDate.now(LIMA);
        LocalDate antes = restaurante.getVenceEn();
        LocalDate base = antes.isAfter(hoy) ? antes : hoy;
        LocalDate despues = base.plusMonths(meses);

        restaurante.setVenceEn(despues);
        restaurante.setEstado("ACTIVO");
        restaurantes.save(restaurante);

        Pago pago = new Pago();
        pago.setRestauranteId(restauranteId);
        pago.setPlanId(plan.getId());
        pago.setMonto(plan.getPrecioMensual().multiply(BigDecimal.valueOf(meses)));
        pago.setMeses(meses);
        pago.setMetodo(metodo);
        pago.setReferencia(ref);
        pago.setRegistradoPor(registradoPor);
        pago.setVenceAntes(antes);
        pago.setVenceDespues(despues);
        pagos.save(pago);
    }

    /**
     * Revisa los vencimientos:
     * activo con fecha pasada pasa a "en gracia" y, cuando la gracia termina, a "suspendido".
     * Los restaurantes suspendidos a mano no se tocan.
     * De paso, avisa por correo al administrador: cuando cambia de estado, y cuando el plan todavía está
     * activo pero vence pronto (sin que eso cambie ningún estado).
     */
    @Transactional
    public ResultadoVencimientosDto procesarVencimientos() {
        LocalDate hoy = LocalDate.now(LIMA);
        int aGracia = 0;
        int suspendidos = 0;

        for (Restaurante r : restaurantes.findByEstadoAndVenceEnBefore("ACTIVO", hoy)) {
            if (hoy.isAfter(r.getVenceEn().plusDays(diasGracia))) {
                r.setEstado("SUSPENDIDO");
                restaurantes.save(r);
                suspendidos++;
                avisoVencimiento.avisarSuspendido(r);
            } else {
                r.setEstado("EN_GRACIA");
                restaurantes.save(r);
                aGracia++;
                avisoVencimiento.avisarEnGracia(r, diasGracia);
            }
        }

        for (Restaurante r : restaurantes.findByEstadoAndVenceEnBefore("EN_GRACIA", hoy.minusDays(diasGracia))) {
            r.setEstado("SUSPENDIDO");
            restaurantes.save(r);
            suspendidos++;
            avisoVencimiento.avisarSuspendido(r);
        }

        int avisados = 0;
        for (Restaurante r : restaurantes.findByEstadoAndVenceEn("ACTIVO", hoy.plusDays(diasAviso))) {
            avisoVencimiento.avisarProximoAVencer(r, diasAviso);
            avisados++;
        }

        return new ResultadoVencimientosDto(aGracia, suspendidos, avisados);
    }

    private Restaurante restaurante(Long id) {
        return restaurantes.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurante no encontrado"));
    }

    private Plan plan(Restaurante restaurante) {
        return planes.findById(restaurante.getPlanId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan no encontrado"));
    }
}
