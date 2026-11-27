package com.yuzu.backend.pago;

import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.yuzu.backend.config.SesionActual;
import com.yuzu.backend.negocio.NegocioDto;
import com.yuzu.backend.negocio.NegocioService;
import com.yuzu.backend.plan.Plan;
import com.yuzu.backend.plan.PlanRepository;
import com.yuzu.backend.restaurante.Restaurante;
import com.yuzu.backend.restaurante.RestauranteRepository;
import com.yuzu.backend.suscripcion.SuscripcionService;
import com.yuzu.backend.usuario.Usuario;
import com.yuzu.backend.usuario.UsuarioRepository;

/**
 * El cobro automático del plan con una tarjeta guardada: Cliente + Tarjeta + Plan recurrente +
 * Suscripción de Culqi. Culqi cobra solo, cada mes, y avisa el resultado por webhook
 * (ver {@link #procesarEvento}): el webhook es la única forma en que una renovación automática
 * llega a reflejarse en la suscripción del restaurante, también la primera vez.
 *
 * El webhook nunca se confía tal cual: solo se usa su id para preguntarle a la API de Culqi
 * (con la llave secreta) el evento y la suscripción reales.
 */
@Service
public class CulqiSuscripcionService {

    private static final Logger log = LoggerFactory.getLogger(CulqiSuscripcionService.class);

    /** interval_unit_time de Culqi: 3 = Mensual. */
    private static final int INTERVALO_MENSUAL = 3;

    private final CulqiClient culqi;
    private final CulqiSuscripcionRepository culqiSuscripciones;
    private final PlanRepository planes;
    private final RestauranteRepository restaurantes;
    private final UsuarioRepository usuarios;
    private final SuscripcionService suscripcion;
    private final NegocioService negocio;

    public CulqiSuscripcionService(CulqiClient culqi,
                                   CulqiSuscripcionRepository culqiSuscripciones,
                                   PlanRepository planes,
                                   RestauranteRepository restaurantes,
                                   UsuarioRepository usuarios,
                                   SuscripcionService suscripcion,
                                   NegocioService negocio) {
        this.culqi = culqi;
        this.culqiSuscripciones = culqiSuscripciones;
        this.planes = planes;
        this.restaurantes = restaurantes;
        this.usuarios = usuarios;
        this.suscripcion = suscripcion;
        this.negocio = negocio;
    }

    @Transactional(readOnly = true)
    public SuscripcionCulqiDto estado() {
        Long restauranteId = SesionActual.restauranteId();
        return culqiSuscripciones.findById(restauranteId)
                .map(f -> new SuscripcionCulqiDto(f.isActiva(), f.getMarcaTarjeta(), f.getUltimosCuatro()))
                .orElseGet(() -> new SuscripcionCulqiDto(false, null, null));
    }

    /**
     * Guarda la tarjeta y activa el cobro automático. Culqi valida la tarjeta con un cargo mínimo de
     * S/ 3.00 que devuelve de inmediato (no es un cobro real). Si el banco pide 3D Secure, lanza
     * {@link Requiere3DSException} y hay que reintentar con el resultado de Culqi3DS.
     *
     * Ojo: a propósito este método NO es una sola transacción. Lo que se crea en Culqi (el plan, la
     * tarjeta, la suscripción) no se puede deshacer si algo falla después, así que cada cosa se guarda
     * en nuestra base apenas se confirma en Culqi, para no perder esa referencia en un reintento.
     */
    public SuscripcionCulqiDto activar(ActivarSuscripcionRequest request) {
        if (!culqi.estaConfigurado()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "El pago en línea todavía no está disponible.");
        }

        Long restauranteId = SesionActual.restauranteId();
        Restaurante restaurante = restaurantes.findById(restauranteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurante no encontrado"));
        Plan plan = planes.findById(restaurante.getPlanId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan no encontrado"));

        String culqiPlanId = planCulqiId(plan);
        String customerId = clienteCulqiId(restauranteId, request.email());

        Map<String, Object> cuerpoTarjeta = new LinkedHashMap<>();
        cuerpoTarjeta.put("customer_id", customerId);
        cuerpoTarjeta.put("token_id", request.tokenId());
        cuerpoTarjeta.put("validate", true);
        if (request.authentication3DS() != null) {
            cuerpoTarjeta.put("authentication_3DS", mapaAutenticacion3DS(request.authentication3DS()));
        }

        RespuestaCulqi tarjetaResp = culqi.crearTarjeta(cuerpoTarjeta);
        if (tarjetaResp.necesitaAutenticacion3DS()) {
            throw new Requiere3DSException();
        }
        if (!"card".equals(tarjetaResp.texto("object"))) {
            log.warn("Culqi no guardó la tarjeta (http={}): {}", tarjetaResp.estado(), tarjetaResp.cuerpo());
            String motivo = tarjetaResp.mensajeUsuario();
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED,
                    motivo != null ? motivo : "No se pudo guardar la tarjeta. Prueba de nuevo o usa otra tarjeta.");
        }
        String cardId = tarjetaResp.texto("id");
        // El objeto se llama "source" en la respuesta de Culqi (no "token", como dice su propia guía).
        Map<?, ?> source = tarjetaResp.mapa("source");
        String ultimosCuatro = campo(source, "last_four");
        String marca = campo(source != null ? comoMapa(source.get("iin")) : null, "card_brand");

        Map<String, Object> cuerpoSuscripcion = new LinkedHashMap<>();
        cuerpoSuscripcion.put("card_id", cardId);
        cuerpoSuscripcion.put("plan_id", culqiPlanId);
        cuerpoSuscripcion.put("tyc", true);
        cuerpoSuscripcion.put("metadata", Map.of("restauranteId", String.valueOf(restauranteId)));

        RespuestaCulqi suscripcionResp = culqi.crearSuscripcion(cuerpoSuscripcion);
        if (!suscripcionResp.esExitosa()) {
            log.warn("Culqi no creó la suscripción (http={}): {}", suscripcionResp.estado(), suscripcionResp.cuerpo());
            String motivo = suscripcionResp.mensajeUsuario();
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED,
                    motivo != null ? motivo : "No se pudo activar el cobro automático. Inténtalo de nuevo.");
        }
        String suscripcionId = suscripcionResp.texto("id");
        log.info("Suscripción de Culqi creada: restaurante={} suscripcion={}", restauranteId, suscripcionId);

        CulqiSuscripcion fila = culqiSuscripciones.findById(restauranteId).orElseGet(() -> {
            CulqiSuscripcion nueva = new CulqiSuscripcion();
            nueva.setRestauranteId(restauranteId);
            return nueva;
        });
        fila.setCulqiCustomerId(customerId);
        fila.setCulqiCardId(cardId);
        fila.setMarcaTarjeta(marca);
        fila.setUltimosCuatro(ultimosCuatro);
        fila.setCulqiPlanId(culqiPlanId);
        fila.setCulqiSuscripcionId(suscripcionId);
        fila.setActiva(true);
        culqiSuscripciones.save(fila);

        return new SuscripcionCulqiDto(true, marca, ultimosCuatro);
    }

    /** Cancela el cobro automático. Culqi no vuelve a cobrar a partir de este momento. */
    @Transactional
    public SuscripcionCulqiDto cancelar() {
        Long restauranteId = SesionActual.restauranteId();
        CulqiSuscripcion fila = culqiSuscripciones.findById(restauranteId)
                .filter(CulqiSuscripcion::isActiva)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No tienes el cobro automático activado"));

        RespuestaCulqi resp = culqi.cancelarSuscripcion(fila.getCulqiSuscripcionId());
        if (!resp.esExitosa()) {
            log.warn("Culqi no canceló la suscripción {} (http={}): {}",
                    fila.getCulqiSuscripcionId(), resp.estado(), resp.cuerpo());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No se pudo cancelar el cobro automático en Culqi. Inténtalo de nuevo.");
        }
        fila.setActiva(false);
        culqiSuscripciones.save(fila);
        return new SuscripcionCulqiDto(false, fila.getMarcaTarjeta(), fila.getUltimosCuatro());
    }

    /**
     * Procesa un aviso del webhook: solo se usa el id para pedirle a Culqi el evento real, y nunca se
     * confía en lo que haya llegado en el cuerpo del webhook. Nunca lanza una excepción hacia afuera:
     * el webhook siempre debe responder 200 para que Culqi no reintente de más.
     */
    @Transactional
    public void procesarEvento(String eventoId) {
        try {
            RespuestaCulqi evento = culqi.obtenerEvento(eventoId);
            if (!evento.esExitosa()) {
                log.warn("No se pudo verificar el evento de Culqi {} (http={}): {}",
                        eventoId, evento.estado(), evento.cuerpo());
                return;
            }
            String tipo = evento.texto("type");
            if (tipo == null) {
                log.warn("El evento de Culqi {} no tiene tipo", eventoId);
            } else if (tipo.startsWith("subscription.")) {
                procesarEventoSuscripcion(eventoId, tipo, evento);
            } else {
                log.info("Evento de Culqi {} ({}) no requiere acción", eventoId, tipo);
            }
        } catch (RuntimeException e) {
            log.error("No se pudo procesar el evento de Culqi {}: {}", eventoId, e.toString());
        }
    }

    private void procesarEventoSuscripcion(String eventoId, String tipo, RespuestaCulqi evento) {
        Map<?, ?> data = evento.mapa("data");
        String suscripcionId = campo(data, "id");
        if (suscripcionId == null) {
            log.warn("El evento {} ({}) no trae el id de la suscripción", eventoId, tipo);
            return;
        }

        switch (ReglasCulqiSuscripcion.accionParaEvento(tipo)) {
            case REGISTRAR_COBRO -> registrarCobroExitoso(suscripcionId);
            case COBRO_FALLIDO -> log.warn("Falló el cobro automático de la suscripción {}", suscripcionId);
            case CANCELAR -> marcarInactiva(suscripcionId);
            case NINGUNA -> log.info("Evento de suscripción {} ({}) no requiere acción", suscripcionId, tipo);
        }
    }

    private void registrarCobroExitoso(String suscripcionId) {
        RespuestaCulqi suscripcionResp = culqi.obtenerSuscripcion(suscripcionId);
        if (!suscripcionResp.esExitosa()) {
            log.error("No se pudo consultar la suscripción {} de Culqi", suscripcionId);
            return;
        }

        Map<?, ?> metadata = suscripcionResp.mapa("metadata");
        String restauranteIdTexto = campo(metadata, "restauranteId");
        if (restauranteIdTexto == null) {
            log.error("La suscripción {} no tiene restauranteId en sus metadatos", suscripcionId);
            return;
        }

        Map<?, ?> periodos = suscripcionResp.mapa("periods");
        Map<?, ?> cargo = periodos != null ? comoMapa(periodos.get("charges")) : null;
        String chargeId = campo(cargo, "charge_id");
        if (chargeId == null) {
            log.error("La suscripción {} no trae el id del cargo cobrado", suscripcionId);
            return;
        }

        Long restauranteId = Long.valueOf(restauranteIdTexto);
        suscripcion.registrarPago(restauranteId, 1, "TARJETA", chargeId, "webhook:culqi");
        log.info("Cobro automático registrado: restaurante={} suscripcion={} cargo={}",
                restauranteId, suscripcionId, chargeId);
    }

    private void marcarInactiva(String suscripcionId) {
        culqiSuscripciones.findByCulqiSuscripcionId(suscripcionId).ifPresent(f -> {
            f.setActiva(false);
            culqiSuscripciones.save(f);
            log.info("Suscripción de Culqi {} quedó cancelada (restaurante={})", suscripcionId, f.getRestauranteId());
        });
    }

    /** El plan recurrente de Culqi para este plan interno, creándolo la primera vez que hace falta. */
    private String planCulqiId(Plan plan) {
        if (plan.getCulqiPlanId() != null) {
            return plan.getCulqiPlanId();
        }

        String shortName = "yuzu-" + plan.getCodigo().toLowerCase();
        long centimos = plan.getPrecioMensual().movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();

        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("name", "Yuzu - " + plan.getNombre());
        cuerpo.put("short_name", shortName);
        cuerpo.put("description", "Suscripción mensual al plan " + plan.getNombre() + " de Yuzu");
        cuerpo.put("amount", centimos);
        cuerpo.put("currency", "PEN");
        cuerpo.put("interval_unit_time", INTERVALO_MENSUAL);
        cuerpo.put("interval_count", 1);
        cuerpo.put("initial_cycles", Map.of(
                "count", 0, "has_initial_charge", false, "amount", 0, "interval_unit_time", INTERVALO_MENSUAL));

        RespuestaCulqi resp = culqi.crearPlanRecurrente(cuerpo);
        String id = resp.texto("id");

        // Puede haber quedado creado en Culqi de un intento anterior que no se alcanzó a guardar aquí
        // (por ejemplo, si después falló la tarjeta): en vez de fallar de nuevo, lo recuperamos.
        if (!resp.esExitosa() && mensajeIndicaQueYaExiste(resp)) {
            id = buscarPlanPorShortName(shortName);
        }

        if (id == null) {
            log.warn("Culqi no creó el plan recurrente (http={}): {}", resp.estado(), resp.cuerpo());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No se pudo preparar el cobro automático en Culqi. Inténtalo de nuevo.");
        }

        plan.setCulqiPlanId(id);
        planes.save(plan);
        return id;
    }

    private static boolean mensajeIndicaQueYaExiste(RespuestaCulqi resp) {
        String mensaje = resp.texto("merchant_message");
        return mensaje != null && mensaje.toLowerCase().contains("ya existe");
    }

    private String buscarPlanPorShortName(String shortName) {
        RespuestaCulqi resp = culqi.listarPlanesRecurrentes();
        if (!resp.esExitosa()) {
            return null;
        }
        Object datos = resp.cuerpo() == null ? null : resp.cuerpo().get("data");
        List<?> listaPlanes = datos instanceof List<?> lista ? lista : datos instanceof Map<?, ?> uno ? List.of(uno) : List.of();
        for (Object item : listaPlanes) {
            if (item instanceof Map<?, ?> mapa && shortName.equals(campo(mapa, "short_name"))) {
                return campo(mapa, "id");
            }
        }
        return null;
    }

    /** El cliente de Culqi del restaurante: reusa el que ya tenía guardado, o crea uno nuevo. */
    private String clienteCulqiId(Long restauranteId, String email) {
        return culqiSuscripciones.findById(restauranteId)
                .map(CulqiSuscripcion::getCulqiCustomerId)
                .orElseGet(() -> crearClienteCulqi(restauranteId, email));
    }

    private String crearClienteCulqi(Long restauranteId, String email) {
        Usuario admin = usuarios.findByIdAndRestauranteId(SesionActual.usuarioId(), restauranteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión no válida"));
        String[] nombre = ReglasCulqiSuscripcion.partirNombre(admin.getNombre());

        // Culqi exige dirección y teléfono para crear el cliente: sin esto no se puede activar el cobro automático.
        NegocioDto datosNegocio = negocio.datos(restauranteId);
        if (datosNegocio.direccion() == null || datosNegocio.telefono() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Para activar el cobro automático, primero completa la dirección y el teléfono en "
                            + "Configuración > Datos del negocio.");
        }

        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("first_name", nombre[0]);
        cuerpo.put("last_name", nombre[1]);
        cuerpo.put("email", email);
        cuerpo.put("address", datosNegocio.direccion());
        cuerpo.put("address_city", "Lima");
        cuerpo.put("country_code", "PE");
        cuerpo.put("phone_number", ReglasCulqiSuscripcion.telefonoCulqi(datosNegocio.telefono()));

        RespuestaCulqi resp = culqi.crearCliente(cuerpo);
        String id = resp.texto("id");
        if (!"customer".equals(resp.texto("object")) || id == null) {
            log.warn("Culqi no creó el cliente (http={}): {}", resp.estado(), resp.cuerpo());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No se pudo registrar tus datos de pago en Culqi. Inténtalo de nuevo.");
        }
        return id;
    }

    private static Map<String, Object> mapaAutenticacion3DS(Autenticacion3DSRequest a) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("eci", a.eci());
        mapa.put("xid", a.xid());
        mapa.put("cavv", a.cavv());
        mapa.put("protocolVersion", a.protocolVersion());
        mapa.put("directoryServerTransactionId", a.directoryServerTransactionId());
        return mapa;
    }

    private static String campo(Map<?, ?> mapa, String clave) {
        if (mapa == null) {
            return null;
        }
        Object valor = mapa.get(clave);
        return valor == null ? null : valor.toString();
    }

    private static Map<?, ?> comoMapa(Object valor) {
        return valor instanceof Map<?, ?> mapa ? mapa : null;
    }
}
