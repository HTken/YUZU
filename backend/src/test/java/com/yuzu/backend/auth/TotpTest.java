package com.yuzu.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Random;

import org.junit.jupiter.api.Test;

class TotpTest {

    /** Clave y resultados oficiales de ejemplo del estándar (RFC 4226, anexo D), contadores 0 a 9. */
    private static final byte[] CLAVE_RFC = "12345678901234567890".getBytes(StandardCharsets.US_ASCII);
    private static final String[] CODIGOS_RFC = {
        "755224", "287082", "359152", "969429", "338314", "254676", "287922", "162583", "399871", "520489",
    };

    @Test
    void elCalculoDeHotpCoincideConLosValoresOficialesDelEstandar() {
        for (int contador = 0; contador < CODIGOS_RFC.length; contador++) {
            assertThat(Totp.hotp(CLAVE_RFC, contador)).isEqualTo(CODIGOS_RFC[contador]);
        }
    }

    @Test
    void base32CodificaYDecodificaSinPerderNada() {
        byte[] datos = new byte[20];
        new Random(42).nextBytes(datos);

        String codificado = Totp.base32Encode(datos);
        byte[] decodificado = Totp.base32Decode(codificado);

        assertThat(decodificado).isEqualTo(datos);
    }

    @Test
    void unSecretoNuevoGeneraUnCodigoDeSeisDigitosValidoEnEseMomento() {
        String secreto = Totp.generarSecreto();
        long ahora = 1_732_000_000L;
        String codigo = Totp.hotp(Totp.base32Decode(secreto), ahora / 30);

        assertThat(codigo).matches("\\d{6}");
        assertThat(Totp.validar(secreto, codigo, ahora)).isTrue();
    }

    @Test
    void aceptaUnPasoDeTreintaSegundosDeDiferenciaDeReloj() {
        String secreto = Totp.generarSecreto();
        long ahora = 1_732_000_000L;
        String codigoDelPasoAnterior = Totp.hotp(Totp.base32Decode(secreto), ahora / 30 - 1);

        assertThat(Totp.validar(secreto, codigoDelPasoAnterior, ahora)).isTrue();
    }

    @Test
    void rechazaUnCodigoDeHaceDosPasosOMas() {
        String secreto = Totp.generarSecreto();
        long ahora = 1_732_000_000L;
        String codigoViejo = Totp.hotp(Totp.base32Decode(secreto), ahora / 30 - 2);

        assertThat(Totp.validar(secreto, codigoViejo, ahora)).isFalse();
    }

    @Test
    void rechazaUnCodigoQueNoSonSeisDigitos() {
        String secreto = Totp.generarSecreto();
        assertThat(Totp.validar(secreto, "12345", 1_732_000_000L)).isFalse();
        assertThat(Totp.validar(secreto, "abcdef", 1_732_000_000L)).isFalse();
        assertThat(Totp.validar(secreto, null, 1_732_000_000L)).isFalse();
    }
}
