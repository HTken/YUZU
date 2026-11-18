package com.yuzu.backend.pago;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
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

    /** Crea un cargo. Devuelve la respuesta de Culqi tal cual, sea de éxito o de rechazo. */
    public RespuestaCulqi crearCargo(Map<String, Object> cuerpo) {
        try {
            return http.post()
                    .uri("/charges")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo)
                    .exchange((peticion, respuesta) ->
                            new RespuestaCulqi(respuesta.getStatusCode().value(), respuesta.bodyTo(MAPA)));
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No se pudo comunicar con Culqi. Revisa en tu banco si se hizo el cobro antes de reintentar.");
        }
    }
}
