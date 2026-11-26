package com.yuzu.backend.pago;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

/** Habla con la API de Culqi usando la llave secreta, que solo existe en el servidor. */
@Component
public class CulqiClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAPA = new ParameterizedTypeReference<>() {
    };

    private final String publicKey;
    private final String secretKey;
    private final RestClient http;

    public CulqiClient(@Value("${yuzu.culqi.public-key:}") String publicKey,
                       @Value("${yuzu.culqi.secret-key:}") String secretKey,
                       @Value("${yuzu.culqi.base-url:https://api.culqi.com/v2}") String baseUrl) {
        this.publicKey = publicKey;
        this.secretKey = secretKey;

        HttpClient cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(cliente);
        fabrica.setReadTimeout(Duration.ofSeconds(30));

        this.http = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(fabrica)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
                .build();
    }

    public boolean estaConfigurado() {
        return !publicKey.isBlank() && !secretKey.isBlank();
    }

    public String publicKey() {
        return publicKey;
    }

    /** Crea un cargo. Devuelve la respuesta de Culqi tal cual, sea de éxito, rechazo o "necesita 3DS". */
    public RespuestaCulqi crearCargo(Map<String, Object> cuerpo) {
        return post("/charges", cuerpo);
    }

    // ------------------------------------------------------- clientes, tarjetas, planes y suscripciones

    public RespuestaCulqi crearCliente(Map<String, Object> cuerpo) {
        return post("/customers", cuerpo);
    }

    public RespuestaCulqi crearTarjeta(Map<String, Object> cuerpo) {
        return post("/cards", cuerpo);
    }

    public RespuestaCulqi crearPlanRecurrente(Map<String, Object> cuerpo) {
        return post("/recurrent/plans/create", cuerpo);
    }

    public RespuestaCulqi crearSuscripcion(Map<String, Object> cuerpo) {
        return post("/recurrent/subscriptions/create", cuerpo);
    }

    public RespuestaCulqi obtenerSuscripcion(String id) {
        return get("/recurrent/subscriptions/" + id);
    }

    public RespuestaCulqi cancelarSuscripcion(String id) {
        return delete("/recurrent/subscriptions/" + id);
    }

    /**
     * El detalle verificado de un evento, consultado con nuestra llave secreta. Nunca se usan los datos
     * que trae el webhook tal cual: solo su id, para pedirle a Culqi el evento real con este método.
     */
    public RespuestaCulqi obtenerEvento(String id) {
        return get("/events/" + id);
    }

    // ------------------------------------------------------------------------------------- ayudas HTTP

    private RespuestaCulqi post(String uri, Map<String, Object> cuerpo) {
        return peticion(HttpMethod.POST, uri, cuerpo);
    }

    private RespuestaCulqi get(String uri) {
        return peticion(HttpMethod.GET, uri, null);
    }

    private RespuestaCulqi delete(String uri) {
        return peticion(HttpMethod.DELETE, uri, null);
    }

    private RespuestaCulqi peticion(HttpMethod metodo, String uri, Map<String, Object> cuerpo) {
        try {
            RestClient.RequestBodySpec peticion = http.method(metodo).uri(uri);
            if (cuerpo != null) {
                peticion.contentType(MediaType.APPLICATION_JSON).body(cuerpo);
            }
            return peticion.exchange((req, respuesta) ->
                    new RespuestaCulqi(respuesta.getStatusCode().value(), respuesta.bodyTo(MAPA)));
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No se pudo comunicar con Culqi. Revisa en tu banco si se hizo el cobro antes de reintentar.");
        }
    }
}
