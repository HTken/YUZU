package com.yuzu.backend.pago;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lo que Culqi llama cuando pasa algo con una suscripción (un cobro automático, una cancelación...).
 * Se configura a mano en el CulqiPanel (sección Eventos, Webhooks), pegando la URL pública de este
 * endpoint. No pasa por el login (ver SecurityConfig): Culqi no manda ningún token.
 *
 * Culqi no firma sus webhooks, así que nunca se confía en lo que manda el cuerpo de la petición: solo
 * se usa el id del evento para preguntarle a la API de Culqi, con nuestra llave secreta, qué pasó de
 * verdad (ver CulqiSuscripcionService.procesarEvento).
 */
@RestController
@RequestMapping("/api/pagos/culqi")
public class CulqiWebhookController {

    private static final Logger log = LoggerFactory.getLogger(CulqiWebhookController.class);

    private final CulqiSuscripcionService servicio;

    public CulqiWebhookController(CulqiSuscripcionService servicio) {
        this.servicio = servicio;
    }

    /** Siempre responde 200: si no, Culqi reintenta de más el mismo aviso. */
    @PostMapping("/webhook")
    public Map<String, String> webhook(@RequestBody(required = false) Map<String, Object> cuerpo) {
        Object id = cuerpo == null ? null : cuerpo.get("id");
        if (id == null) {
            log.warn("Webhook de Culqi sin id de evento: {}", cuerpo);
        } else {
            servicio.procesarEvento(id.toString());
        }
        return Map.of("recibido", "ok");
    }
}
