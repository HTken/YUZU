package com.yuzu.backend.suscripcion;

/** El texto de los avisos de vencimiento, sin base de datos ni correo, para poder probarlo solo. */
final class ReglasAvisoVencimiento {

    /** El asunto y el cuerpo de un correo. */
    record Correo(String asunto, String cuerpo) {
    }

    private ReglasAvisoVencimiento() {
    }

    /** El plan todavía está activo, pero vence pronto. */
    static Correo proximoAVencer(String nombreRestaurante, String fechaVencimiento, int diasRestantes) {
        String dias = texto(diasRestantes);
        return new Correo(
                "Tu plan de Yuzu vence en " + dias,
                "Hola,\n\n"
                        + "El plan de " + nombreRestaurante + " vence el " + fechaVencimiento + " (en " + dias + ").\n\n"
                        + "Para seguir usando Yuzu sin interrupciones, activa el cobro automático o renueva tu plan "
                        + "desde Configuración > Mi Plan. Si ya tienes el cobro automático activado, no necesitas "
                        + "hacer nada.\n\n"
                        + "Equipo Yuzu");
    }

    /** El plan ya venció, pero todavía hay unos días de gracia antes de que se suspenda. */
    static Correo enGracia(String nombreRestaurante, String fechaVencimiento, int diasGracia) {
        String dias = texto(diasGracia);
        return new Correo(
                "Tu plan de Yuzu venció",
                "Hola,\n\n"
                        + "El plan de " + nombreRestaurante + " venció el " + fechaVencimiento + ".\n\n"
                        + "Tienes " + dias + " para renovarlo antes de que tu cuenta se suspenda. Ingresa a "
                        + "Configuración > Mi Plan para renovar.\n\n"
                        + "Equipo Yuzu");
    }

    /** La cuenta ya quedó suspendida. */
    static Correo suspendido(String nombreRestaurante) {
        return new Correo(
                "Tu cuenta de Yuzu fue suspendida",
                "Hola,\n\n"
                        + "La cuenta de " + nombreRestaurante + " fue suspendida por falta de pago.\n\n"
                        + "Renueva tu plan desde Configuración > Mi Plan para reactivarla.\n\n"
                        + "Equipo Yuzu");
    }

    private static String texto(int dias) {
        return dias == 1 ? "1 día" : dias + " días";
    }
}
