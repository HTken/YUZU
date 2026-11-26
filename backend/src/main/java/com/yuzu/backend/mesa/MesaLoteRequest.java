package com.yuzu.backend.mesa;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Crea varias mesas de una vez: "Mesa 1", "Mesa 2", "Mesa 3"… */
public record MesaLoteRequest(
        @NotNull(message = "Elige un salón")
        Long salonId,

        @NotBlank(message = "Escribe cómo se llamarán las mesas, por ejemplo \"Mesa\"")
        @Size(max = 30, message = "El nombre base no puede pasar de 30 caracteres")
        String prefijo,

        @NotNull(message = "Indica desde qué número empiezan")
        @Min(value = 1, message = "El número inicial debe ser 1 o mayor")
        @Max(value = 9999, message = "El número inicial no puede pasar de 9999")
        Integer desde,

        @NotNull(message = "Indica cuántas mesas crear")
        @Min(value = 1, message = "Crea al menos 1 mesa")
        @Max(value = 50, message = "Puedes crear hasta 50 mesas a la vez")
        Integer cantidad,

        @NotNull(message = "La capacidad es obligatoria")
        @Min(value = 1, message = "La capacidad debe ser de al menos 1 persona")
        @Max(value = 50, message = "La capacidad no puede pasar de 50 personas")
        Integer capacidad) {
}
