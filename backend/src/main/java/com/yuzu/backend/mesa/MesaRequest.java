package com.yuzu.backend.mesa;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MesaRequest(
        @NotNull(message = "Elige un salón")
        Long salonId,

        @NotBlank(message = "El nombre de la mesa es obligatorio")
        @Size(max = 40, message = "El nombre de la mesa no puede pasar de 40 caracteres")
        String nombre,

        @NotNull(message = "La capacidad es obligatoria")
        @Min(value = 1, message = "La capacidad debe ser de al menos 1 persona")
        @Max(value = 50, message = "La capacidad no puede pasar de 50 personas")
        Integer capacidad,

        @NotNull(message = "Indica si la mesa está activa")
        Boolean activo) {
}
