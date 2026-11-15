package com.yuzu.backend.suscripcion;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.yuzu.backend.config.SesionActual;
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

    public SuscripcionService(RestauranteRepository restaurantes,
                              PlanRepository planes,
                              UsuarioRepository usuarios) {
        this.restaurantes = restaurantes;
        this.planes = planes;
        this.usuarios = usuarios;
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

    private Restaurante restaurante(Long id) {
        return restaurantes.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurante no encontrado"));
    }

    private Plan plan(Restaurante restaurante) {
        return planes.findById(restaurante.getPlanId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan no encontrado"));
    }
}
