package com.yuzu.backend.pago;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

class RespuestaCulqiTest {

    @Test
    void unCargoConHttp201YResponseCodeVentaExitosaEsExitoso() {
        RespuestaCulqi r = new RespuestaCulqi(201, Map.of("object", "charge", "response_code", "venta_exitosa"));
        assertThat(r.esExitosa()).isTrue();
        assertThat(r.ventaExitosa()).isTrue();
        assertThat(r.necesitaAutenticacion3DS()).isFalse();
    }

    @Test
    void unCargoConHttp200NecesitaAutenticacion3DS() {
        RespuestaCulqi r = new RespuestaCulqi(200, Map.of("object", "charge"));
        assertThat(r.necesitaAutenticacion3DS()).isTrue();
        assertThat(r.ventaExitosa()).isFalse();
    }

    @Test
    void unRechazoConHttp201PeroSinVentaExitosaNoEsUnaVentaExitosa() {
        RespuestaCulqi r = new RespuestaCulqi(201, Map.of(
                "object", "charge",
                "outcome", Map.of("type", "tarjeta_invalida", "user_message", "Tarjeta inválida")));
        assertThat(r.ventaExitosa()).isFalse();
        assertThat(r.mensajeUsuario()).isEqualTo("Tarjeta inválida");
    }

    @Test
    void unErrorDeCulqiNuncaEsVentaExitosaAunqueElHttpSeaExitoso() {
        RespuestaCulqi r = new RespuestaCulqi(200, Map.of("object", "error", "user_message", "Fondos insuficientes"));
        assertThat(r.ventaExitosa()).isFalse();
        assertThat(r.mensajeUsuario()).isEqualTo("Fondos insuficientes");
    }

    @Test
    void unHttpDeErrorNuncaEsExitosoAunqueElCuerpoDigaOtraCosa() {
        RespuestaCulqi r = new RespuestaCulqi(500, Map.of("object", "charge", "response_code", "venta_exitosa"));
        assertThat(r.esExitosa()).isFalse();
        assertThat(r.ventaExitosa()).isFalse();
    }

    @Test
    void mapaDevuelveUnObjetoAnidadoOVacioPorNull() {
        RespuestaCulqi r = new RespuestaCulqi(200, Map.of("data", Map.of("id", "sxn_test_123")));
        assertThat(r.mapa("data")).isNotNull();
        assertThat(r.mapa("data").get("id")).isEqualTo("sxn_test_123");
        assertThat(r.mapa("no-existe")).isNull();
    }

    @Test
    void textoDevuelveNullSiNoVinoElCampoOElCuerpoEsNulo() {
        RespuestaCulqi sinCuerpo = new RespuestaCulqi(502, null);
        assertThat(sinCuerpo.texto("id")).isNull();
        assertThat(sinCuerpo.esExitosa()).isFalse();
    }
}
