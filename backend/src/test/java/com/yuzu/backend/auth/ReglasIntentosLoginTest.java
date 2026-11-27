package com.yuzu.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class ReglasIntentosLoginTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 27, 10, 0, 0);

    @Test
    void cadaIntentoFallidoSumaUno() {
        assertThat(ReglasIntentosLogin.siguienteIntento(0)).isEqualTo(1);
        assertThat(ReglasIntentosLogin.siguienteIntento(4)).isEqualTo(5);
    }

    @Test
    void bloqueaSoloAlLlegarAlMaximo() {
        assertThat(ReglasIntentosLogin.debeBloquear(4, 5)).isFalse();
        assertThat(ReglasIntentosLogin.debeBloquear(5, 5)).isTrue();
        assertThat(ReglasIntentosLogin.debeBloquear(6, 5)).isTrue();
    }

    @Test
    void elBloqueoDuraLosMinutosConfigurados() {
        LocalDateTime hasta = ReglasIntentosLogin.calcularBloqueoHasta(AHORA, 15);
        assertThat(hasta).isEqualTo(AHORA.plusMinutes(15));
    }

    @Test
    void estaBloqueadoMientrasNoPaseLaFecha() {
        LocalDateTime hasta = AHORA.plusMinutes(15);
        assertThat(ReglasIntentosLogin.estaBloqueado(hasta, AHORA)).isTrue();
        assertThat(ReglasIntentosLogin.estaBloqueado(hasta, AHORA.plusMinutes(14))).isTrue();
        assertThat(ReglasIntentosLogin.estaBloqueado(hasta, AHORA.plusMinutes(15))).isFalse();
        assertThat(ReglasIntentosLogin.estaBloqueado(hasta, AHORA.plusMinutes(16))).isFalse();
    }

    @Test
    void sinBloqueoNuncaEstaBloqueado() {
        assertThat(ReglasIntentosLogin.estaBloqueado(null, AHORA)).isFalse();
    }

    @Test
    void redondeaLosMinutosRestantesHaciaArriba() {
        LocalDateTime hasta = AHORA.plusMinutes(15);
        assertThat(ReglasIntentosLogin.minutosRestantes(hasta, AHORA)).isEqualTo(15);
        assertThat(ReglasIntentosLogin.minutosRestantes(hasta, AHORA.plusSeconds(10))).isEqualTo(15);
        assertThat(ReglasIntentosLogin.minutosRestantes(hasta, AHORA.plusMinutes(14).plusSeconds(1))).isEqualTo(1);
    }
}
