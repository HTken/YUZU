package com.yuzu.backend.pago;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.yuzu.backend.config.SesionActual;
import com.yuzu.backend.suscripcion.MiPlanDto;
import com.yuzu.backend.suscripcion.SuscripcionService;

/**
 * Cobra la suscripción con Culqi y, si el cobro sale bien, renueva al restaurante.
 * Este servicio no abre una transacción de base de datos: la llamada a Culqi puede tardar.
 */
@Service
public class PagoCulqiService {

    private static final Logger log = LoggerFactory.getLogger(PagoCulqiService.class);

    private final CulqiClient culqi;
    private final SuscripcionService suscripcion;

    public PagoCulqiService(CulqiClient culqi, SuscripcionService suscripcion) {
        this.culqi = culqi;
        this.suscripcion = suscripcion;
    }

    public CulqiConfigDto configuracion() {
        exigirConfigurado();
        return new CulqiConfigDto(culqi.publicKey());
    }

    public ResultadoCobroDto cobrar(CobrarRequest request) {
        exigirConfigurado();

        // Todo sale del token ya verificado, nunca de lo que diga el navegador.
        Long restauranteId = SesionActual.restauranteId();
        MiPlanDto plan = suscripcion.miPlan();

        long centimos = plan.precioMensual()
                .multiply(BigDecimal.valueOf(request.meses()))
                .movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();

        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("amount", centimos);
        cuerpo.put("currency_code", "PEN");
        cuerpo.put("email", request.email());
        cuerpo.put("source_id", request.tokenId());
        cuerpo.put("capture", true);
        cuerpo.put("description", "Suscripción Yuzu, plan " + plan.plan() + " (" + request.meses() + " mes/es)");
        cuerpo.put("metadata", Map.of(
                "restaurante_id", String.valueOf(restauranteId),
                "meses", String.valueOf(request.meses()),
                "plan", plan.plan()));
        if (request.deviceFingerPrintId() != null && !request.deviceFingerPrintId().isBlank()) {
            cuerpo.put("antifraud_details", Map.of("device_finger_print_id", request.deviceFingerPrintId()));
        }
        if (request.authentication3DS() != null) {
            Autenticacion3DSRequest a = request.authentication3DS();
            cuerpo.put("authentication_3DS", Map.of(
                    "eci", a.eci(),
                    "xid", a.xid(),
                    "cavv", a.cavv(),
                    "protocolVersion", a.protocolVersion(),
                    "directoryServerTransactionId", a.directoryServerTransactionId()));
        }

        RespuestaCulqi respuesta = culqi.crearCargo(cuerpo);
        log.info("Culqi respondió al cargo: {}", respuesta.resumen());

        if (respuesta.necesitaAutenticacion3DS()) {
            throw new Requiere3DSException();
        }
        if (!respuesta.ventaExitosa()) {
            throw rechazo(respuesta);
        }

        String cargoId = respuesta.texto("id");
        String metodo = request.tokenId().startsWith("ype_") ? "YAPE" : "TARJETA";

        try {
            suscripcion.registrarPago(restauranteId, request.meses(), metodo, cargoId,
                    limitar("cliente:" + SesionActual.usuario(), 60));
        } catch (RuntimeException e) {
            // El cliente ya pagó en Culqi: hay que dejar rastro para renovarlo a mano si hace falta.
            log.error("COBRO_SIN_REGISTRAR cargo={} restaurante={} meses={} motivo={}",
                    cargoId, restauranteId, request.meses(), e.toString());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Tu pago se recibió, pero no pudimos registrarlo. Guarda este código y avísanos: " + cargoId);
        }

        MiPlanDto actualizado = suscripcion.miPlan();
        return new ResultadoCobroDto(cargoId, actualizado.venceEn(), actualizado.estado());
    }

    private ResponseStatusException rechazo(RespuestaCulqi respuesta) {
        String motivo = respuesta.mensajeUsuario();
        return new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED,
                motivo != null
                        ? "El pago fue rechazado: " + motivo
                        : "El pago no se pudo completar. Prueba con otra tarjeta.");
    }

    private void exigirConfigurado() {
        if (!culqi.estaConfigurado()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "El pago en línea todavía no está disponible.");
        }
    }

    private static String limitar(String texto, int maximo) {
        return texto.length() <= maximo ? texto : texto.substring(0, maximo);
    }
}
