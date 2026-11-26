package com.yuzu.backend.negocio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ReglasNegocioTest {

    // ---------------------------------------------------------------- texto

    @Test
    void nuloEsSinDato() {
        assertThat(ReglasNegocio.texto(null, 50, "El campo")).isNull();
    }

    @Test
    void soloEspaciosEsSinDato() {
        assertThat(ReglasNegocio.texto("   ", 50, "El campo")).isNull();
    }

    @Test
    void limpiaEspaciosDeMasYCaracteresDeControl() {
        assertThat(ReglasNegocio.texto("  Av.  Los  Pinos \t 123  ", 50, "La dirección"))
                .isEqualTo("Av. Los Pinos 123");
    }

    @Test
    void rechazaTextoMasLargoQueElMaximo() {
        String largo = "a".repeat(51);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> ReglasNegocio.texto(largo, 50, "La dirección"));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getReason()).contains("La dirección");
    }

    // ------------------------------------------------------------------ ruc

    @Test
    void rucVacioONuloEsValidoPorqueEsOpcional() {
        assertThat(ReglasNegocio.ruc(null)).isNull();
        assertThat(ReglasNegocio.ruc("")).isNull();
        assertThat(ReglasNegocio.ruc("   ")).isNull();
    }

    @Test
    void aceptaUnRucValido() {
        // 20100070970 es un RUC de ejemplo con dígito verificador correcto (empieza con 20).
        assertThat(ReglasNegocio.ruc("20100070970")).isEqualTo("20100070970");
    }

    @Test
    void aceptaUnRucConEspaciosOGuiones() {
        assertThat(ReglasNegocio.ruc("20-100070970")).isEqualTo("20100070970");
    }

    @Test
    void rechazaUnRucConElDigitoVerificadorIncorrecto() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> ReglasNegocio.ruc("20100070971"));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void rechazaUnRucQueNoEmpiezaConUnPrefijoValido() {
        // Mismos 10 primeros dígitos que un RUC válido, cambiando el prefijo a uno no permitido (30).
        assertThrows(ResponseStatusException.class, () -> ReglasNegocio.ruc("30100070970"));
    }

    @Test
    void rechazaUnRucQueNoTieneOnceDigitos() {
        assertThrows(ResponseStatusException.class, () -> ReglasNegocio.ruc("2010007097"));
        assertThrows(ResponseStatusException.class, () -> ReglasNegocio.ruc("201000709700"));
    }

    @Test
    void rechazaUnRucConLetras() {
        assertThrows(ResponseStatusException.class, () -> ReglasNegocio.ruc("2010007097A"));
    }

    // ------------------------------------------------------------- telefono

    @Test
    void telefonoVacioEsSinDato() {
        assertThat(ReglasNegocio.telefono(null)).isNull();
        assertThat(ReglasNegocio.telefono("  ")).isNull();
    }

    @Test
    void aceptaUnTelefonoConLosSignosUsuales() {
        assertThat(ReglasNegocio.telefono("+51 (01) 987-654.321")).isEqualTo("+51 (01) 987-654.321");
    }

    @Test
    void rechazaUnTelefonoConLetras() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> ReglasNegocio.telefono("987ABC321"));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ----------------------------------------------------------------- ancho

    @Test
    void sinAnchoUsaElValorPorDefecto() {
        assertThat(ReglasNegocio.ancho(null)).isEqualTo(ReglasNegocio.ANCHO_POR_DEFECTO);
    }

    @Test
    void aceptaLosAnchosValidos() {
        assertThat(ReglasNegocio.ancho(58)).isEqualTo(58);
        assertThat(ReglasNegocio.ancho(80)).isEqualTo(80);
    }

    @Test
    void rechazaUnAnchoQueNoEsDePapelDeTicket() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> ReglasNegocio.ancho(100));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
