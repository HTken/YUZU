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

import com.yuzu.backend.config.SesionActual;
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
    private final PagoRepository pagos;
    private final int diasGracia;

    public SuscripcionService(RestauranteRepository restaurantes,
                              PlanRepository planes,
                              UsuarioRepository usuarios,
                              PagoRepository pagos,
                              @Value("${yuzu.suscripcion.dias-gracia:3}") int diasGracia) {
        this.restaurantes = restaurantes;
        this.planes = planes;
        this.usuarios = usuarios;
        this.pagos = pagos;
        this.diasGracia = diasGracia;
    }

    @Transactional(readOnly = true)
    public MiPlanDto miPlan() {
        Restaurante restaurante = restaurante(SesionActual.restauranteId());
        Plan plan = plan(restaurante);

        long activos = usuarios.countByRestauranteIdAndActivoTrue(restaurante.getId());
        long dias = ChronoUnit.DAYS.between(LocalDate.now(LIMA), restaurante.getVenceEn());

        return new MiPlanDto(
                plan.getNombre(),
                plan.getPrecioMensual(),
                plan.getMaxUsuarios(),
                (int) activos,
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
     */
    @Transactional
    public ResultadoVencimientosDto procesarVencimientos() {
        LocalDate hoy = LocalDate.now(LIMA);
        int aGracia = 0;
        int suspendidos = 0;

        for (Restaurante r : restaurantes.findByEstadoAndVenceEnBefore("ACTIVO", hoy)) {
            if (hoy.isAfter(r.getVenceEn().plusDays(diasGracia))) {
                r.setEstado("SUSPENDIDO");
                suspendidos++;
            } else {
                r.setEstado("EN_GRACIA");
                aGracia++;
            }
            restaurantes.save(r);
        }

        for (Restaurante r : restaurantes.findByEstadoAndVenceEnBefore("EN_GRACIA", hoy.minusDays(diasGracia))) {
            r.setEstado("SUSPENDIDO");
            restaurantes.save(r);
            suspendidos++;
        }

        return new ResultadoVencimientosDto(aGracia, suspendidos);
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
