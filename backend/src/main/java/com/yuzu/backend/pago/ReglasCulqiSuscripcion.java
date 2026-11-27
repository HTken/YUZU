package com.yuzu.backend.pago;

/**
 * Las decisiones de la suscripción recurrente que no dependen de Culqi ni de la base de datos,
 * para poder probarlas solas.
 */
final class ReglasCulqiSuscripcion {

    /** Qué hacer con un evento de webhook de Culqi, ya verificado, sobre una suscripción. */
    enum AccionEvento {
        REGISTRAR_COBRO,
        COBRO_FALLIDO,
        CANCELAR,
        NINGUNA
    }

    private ReglasCulqiSuscripcion() {
    }

    static AccionEvento accionParaEvento(String tipo) {
        return switch (tipo) {
            case "subscription.charge.succeeded" -> AccionEvento.REGISTRAR_COBRO;
            case "subscription.charge.failed" -> AccionEvento.COBRO_FALLIDO;
            case "subscription.cancel.succeeded" -> AccionEvento.CANCELAR;
            default -> AccionEvento.NINGUNA;
        };
    }

    /**
     * Separa un nombre completo en nombre y apellido para mandárselo a Culqi (los dos campos son
     * obligatorios y necesitan al menos 2 caracteres). Si no hay apellido, se repite el nombre completo.
     */
    static String[] partirNombre(String nombreCompleto) {
        String limpio = nombreCompleto.trim().replaceAll("\\s+", " ");
        int espacio = limpio.indexOf(' ');
        return espacio < 0
                ? new String[] {limpio, limpio}
                : new String[] {limpio.substring(0, espacio), limpio.substring(espacio + 1)};
    }

    /** Culqi solo acepta de 5 a 15 caracteres en el teléfono: se limpia lo que haya escrito el administrador. */
    static String telefonoCulqi(String telefono) {
        String limpio = telefono.replaceAll("[^0-9]", "");
        return limpio.length() > 15 ? limpio.substring(0, 15) : limpio;
    }
}
