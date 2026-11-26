package com.yuzu.backend.pago;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.yuzu.backend.pago.ReglasCulqiSuscripcion.AccionEvento;

class ReglasCulqiSuscripcionTest {

    // ----------------------------------------------------------- accionParaEvento

    @Test
    void unCobroExitosoSeRegistra() {
        assertThat(ReglasCulqiSuscripcion.accionParaEvento("subscription.charge.succeeded"))
                .isEqualTo(AccionEvento.REGISTRAR_COBRO);
    }

    @Test
    void unCobroFallidoNoSeRegistraPeroSeAvisa() {
        assertThat(ReglasCulqiSuscripcion.accionParaEvento("subscription.charge.failed"))
                .isEqualTo(AccionEvento.COBRO_FALLIDO);
    }

    @Test
    void unaCancelacionMarcaLaSuscripcionInactiva() {
        assertThat(ReglasCulqiSuscripcion.accionParaEvento("subscription.cancel.succeeded"))
                .isEqualTo(AccionEvento.CANCELAR);
    }

    @Test
    void otrosEventosNoHacenNada() {
        assertThat(ReglasCulqiSuscripcion.accionParaEvento("subscription.creation.succeeded"))
                .isEqualTo(AccionEvento.NINGUNA);
        assertThat(ReglasCulqiSuscripcion.accionParaEvento("subscription.trial.end"))
                .isEqualTo(AccionEvento.NINGUNA);
        assertThat(ReglasCulqiSuscripcion.accionParaEvento("charge.creation.succeeded"))
                .isEqualTo(AccionEvento.NINGUNA);
        assertThat(ReglasCulqiSuscripcion.accionParaEvento("algo-que-no-existe"))
                .isEqualTo(AccionEvento.NINGUNA);
    }

    // ----------------------------------------------------------------- partirNombre

    @Test
    void separaNombreYApellido() {
        assertThat(ReglasCulqiSuscripcion.partirNombre("Juan Pérez")).containsExactly("Juan", "Pérez");
    }

    @Test
    void conVariosApellidosElRestoQuedaComoApellido() {
        assertThat(ReglasCulqiSuscripcion.partirNombre("María Luisa Gómez Torres"))
                .containsExactly("María", "Luisa Gómez Torres");
    }

    @Test
    void sinApellidoRepiteElNombreCompleto() {
        assertThat(ReglasCulqiSuscripcion.partirNombre("Admin")).containsExactly("Admin", "Admin");
    }

    @Test
    void limpiaEspaciosDeMasYAlRededor() {
        assertThat(ReglasCulqiSuscripcion.partirNombre("  Juan   Pérez  ")).containsExactly("Juan", "Pérez");
    }
}
