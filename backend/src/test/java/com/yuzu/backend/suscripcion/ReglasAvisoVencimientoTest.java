package com.yuzu.backend.suscripcion;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.yuzu.backend.suscripcion.ReglasAvisoVencimiento.Correo;

class ReglasAvisoVencimientoTest {

    // ------------------------------------------------------------ proximoAVencer

    @Test
    void avisaCuantosDiasFaltanYLaFechaDeVencimiento() {
        Correo correo = ReglasAvisoVencimiento.proximoAVencer("Restaurante Demo", "2026-12-25", 3);

        assertThat(correo.asunto()).isEqualTo("Tu plan de Yuzu vence en 3 días");
        assertThat(correo.cuerpo()).contains("Restaurante Demo").contains("2026-12-25").contains("3 días");
    }

    @Test
    void conUnSoloDiaUsaSingular() {
        Correo correo = ReglasAvisoVencimiento.proximoAVencer("Restaurante Demo", "2026-12-25", 1);

        assertThat(correo.asunto()).isEqualTo("Tu plan de Yuzu vence en 1 día");
        assertThat(correo.cuerpo()).contains("1 día").doesNotContain("1 días");
    }

    // ------------------------------------------------------------------ enGracia

    @Test
    void avisaQueVencioYCuantosDiasDeGraciaQuedan() {
        Correo correo = ReglasAvisoVencimiento.enGracia("Restaurante Demo", "2026-11-20", 3);

        assertThat(correo.asunto()).isEqualTo("Tu plan de Yuzu venció");
        assertThat(correo.cuerpo()).contains("Restaurante Demo").contains("2026-11-20").contains("3 días");
    }

    // ----------------------------------------------------------------- suspendido

    @Test
    void avisaQueLaCuentaFueSuspendida() {
        Correo correo = ReglasAvisoVencimiento.suspendido("Restaurante Demo");

        assertThat(correo.asunto()).isEqualTo("Tu cuenta de Yuzu fue suspendida");
        assertThat(correo.cuerpo()).contains("Restaurante Demo").contains("suspendida");
    }
}
