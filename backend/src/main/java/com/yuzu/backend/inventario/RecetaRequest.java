package com.yuzu.backend.inventario;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** La receta completa de un producto. Una lista vacía quita la receta. */
public record RecetaRequest(
        @NotNull(message = "Falta la lista de insumos de la receta")
        @Size(max = 30, message = "Una receta puede tener hasta 30 insumos")
        List<@Valid Linea> lineas) {

    public record Linea(
            @NotNull(message = "Elige el insumo en cada línea de la receta")
            Long insumoId,

            /** Lo que gasta UNA unidad del producto, en la unidad del insumo. */
            @NotNull(message = "Escribe la cantidad de cada insumo")
            @DecimalMin(value = "0", inclusive = false, message = "La cantidad de cada insumo debe ser mayor a 0")
            @Digits(integer = 5, fraction = 3, message = "La cantidad no es válida (hasta 5 enteros y 3 decimales)")
            BigDecimal cantidad) {
    }
}
