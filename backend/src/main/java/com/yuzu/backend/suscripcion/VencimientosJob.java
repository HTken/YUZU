package com.yuzu.backend.suscripcion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Revisa los vencimientos todas las noches, sin que nadie tenga que hacerlo a mano. */
@Component
public class VencimientosJob {

    private static final Logger log = LoggerFactory.getLogger(VencimientosJob.class);

    private final SuscripcionService suscripcion;

    public VencimientosJob(SuscripcionService suscripcion) {
        this.suscripcion = suscripcion;
    }

    @Scheduled(cron = "0 5 0 * * *", zone = "America/Lima")
    public void ejecutar() {
        ResultadoVencimientosDto resultado = suscripcion.procesarVencimientos();
        log.info("Revisión de vencimientos: {} pasaron a gracia, {} suspendidos",
                resultado.aGracia(), resultado.suspendidos());
    }
}
