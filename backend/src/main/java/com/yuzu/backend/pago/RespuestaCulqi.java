package com.yuzu.backend.pago;

import java.util.Map;

/** Lo que respondió Culqi: el código HTTP y el cuerpo ya convertido a un mapa. */
public record RespuestaCulqi(int estado, Map<String, Object> cuerpo) {

    public boolean esExitosa() {
        return estado >= 200 && estado < 300;
    }

    public String texto(String clave) {
        Object valor = cuerpo == null ? null : cuerpo.get(clave);
        return valor == null ? null : valor.toString();
    }

    /** Culqi puede informar el resultado en "response_code" o dentro del objeto "outcome". */
    public boolean ventaExitosa() {
        if (!esExitosa() || !"charge".equals(texto("object"))) {
            return false;
        }
        if ("venta_exitosa".equals(texto("response_code"))) {
            return true;
        }
        Map<?, ?> outcome = outcome();
        return outcome != null && "venta_exitosa".equals(String.valueOf(outcome.get("type")));
    }

    /** Mensaje pensado para mostrarle al cliente, venga donde venga. */
    public String mensajeUsuario() {
        String directo = texto("user_message");
        if (directo != null) {
            return directo;
        }
        Map<?, ?> outcome = outcome();
        Object anidado = outcome == null ? null : outcome.get("user_message");
        return anidado == null ? null : anidado.toString();
    }

    /** Resumen sin datos sensibles, para dejar en el log y poder diagnosticar. */
    public String resumen() {
        Map<?, ?> outcome = outcome();
        return "http=" + estado
                + " object=" + texto("object")
                + " id=" + texto("id")
                + " response_code=" + texto("response_code")
                + " outcome=" + (outcome == null ? null : outcome.get("type"))
                + " type=" + texto("type")
                + " code=" + texto("code")
                + " decline_code=" + texto("decline_code");
    }

    private Map<?, ?> outcome() {
        Object valor = cuerpo == null ? null : cuerpo.get("outcome");
        return valor instanceof Map<?, ?> mapa ? mapa : null;
    }
}
