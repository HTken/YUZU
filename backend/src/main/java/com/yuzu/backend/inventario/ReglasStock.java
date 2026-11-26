package com.yuzu.backend.inventario;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Las cuentas del stock, sin base de datos: qué cantidad es válida y cuánto cambia el stock con cada
 * tipo de movimiento. Está aparte para poder probarlas solas.
 */
final class ReglasStock {

    /** Lo máximo que cabe en la base de datos (DECIMAL(12,3)). */
    static final BigDecimal MAXIMO = new BigDecimal("999999999.999");

    private ReglasStock() {
    }

    /**
     * Deja la cantidad con 3 decimales y revisa que quepa y que tenga sentido para la unidad:
     * lo que se cuenta por UNIDAD no puede tener decimales (no existe "media gaseosa" en el stock).
     */
    static BigDecimal cantidad(BigDecimal valor, String unidad, String etiqueta) {
        BigDecimal v = valor.setScale(3, RoundingMode.HALF_UP);
        if (v.signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, etiqueta + " no puede ser negativa");
        }
        if (v.compareTo(MAXIMO) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, etiqueta + " es demasiado grande");
        }
        if (Insumo.UNIDAD.equals(unidad) && v.stripTrailingZeros().scale() > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    etiqueta + " debe ser un número entero porque este insumo se cuenta por unidad");
        }
        return v;
    }

    /**
     * Cuánto cambia el stock con este movimiento (con signo).
     * En ENTRADA, SALIDA y MERMA la cantidad es lo que entra o sale. En AJUSTE es lo que se contó:
     * el cambio es la diferencia con lo que el sistema creía que había.
     */
    static BigDecimal cambio(String tipo, BigDecimal stockActual, BigDecimal cantidad, String unidad) {
        BigDecimal delta;

        if (MovimientoInsumo.AJUSTE.equals(tipo)) {
            delta = cantidad.subtract(stockActual);
            if (delta.signum() == 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El stock ya es " + texto(stockActual) + " " + etiquetaUnidad(unidad)
                                + ". No hay nada que ajustar.");
            }
        } else {
            if (cantidad.signum() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cantidad debe ser mayor a 0");
            }
            if (MovimientoInsumo.ENTRADA.equals(tipo)) {
                delta = cantidad;
            } else {
                if (cantidad.compareTo(stockActual) > 0) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "No hay tanto stock. Hay " + texto(stockActual) + " " + etiquetaUnidad(unidad));
                }
                delta = cantidad.negate();
            }
        }

        if (stockActual.add(delta).compareTo(MAXIMO) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El stock resultante es demasiado grande");
        }
        return delta;
    }

    /** Lo que se descontó de verdad y lo que faltó (la venta necesitaba más de lo que había registrado). */
    record Consumo(BigDecimal descontado, BigDecimal faltante) {
    }

    /**
     * Cuánto se descuenta del stock por una venta. La venta nunca se bloquea: si no alcanza, se descuenta
     * lo que haya (el stock queda en 0, nunca negativo) y la diferencia se anota como faltante.
     */
    static Consumo consumo(BigDecimal stockActual, BigDecimal requerido) {
        if (requerido.compareTo(stockActual) <= 0) {
            return new Consumo(requerido, BigDecimal.ZERO);
        }
        // El faltante tampoco puede pasar de lo que cabe en la base de datos: una venta nunca debe fallar por eso.
        return new Consumo(stockActual, requerido.subtract(stockActual).min(MAXIMO));
    }

    /**
     * Lo que descontó de verdad una venta, por insumo: la suma de sus movimientos VENTA (que van en negativo).
     * Se usa lo que se descontó y no lo que dice la receta hoy: la receta pudo cambiar, y si faltó stock se
     * descontó menos de lo que pedía. Ordenado por insumo para bloquear siempre en el mismo orden.
     */
    static Map<Long, BigDecimal> descontadoPorInsumo(List<MovimientoInsumo> ventas) {
        Map<Long, BigDecimal> descontado = new TreeMap<>();
        for (MovimientoInsumo m : ventas) {
            BigDecimal cantidad = m.getCantidad().negate();
            if (cantidad.signum() > 0) {
                descontado.merge(m.getInsumoId(), cantidad, BigDecimal::add);
            }
        }
        return descontado;
    }

    /** Cuánto vuelve de verdad al stock: todo lo descontado, salvo que el stock resultante ya no quepa en la base de datos. */
    static BigDecimal devolucion(BigDecimal stockActual, BigDecimal descontado) {
        return descontado.min(MAXIMO.subtract(stockActual)).max(BigDecimal.ZERO);
    }

    /**
     * Cuánto necesita cada insumo para todo lo vendido: por cada línea de receta,
     * (cantidad por unidad) x (unidades vendidas del producto). Ordenado por insumo para bloquear siempre en el mismo orden.
     */
    static Map<Long, BigDecimal> requeridoPorInsumo(Map<Long, Integer> vendidos, List<RecetaLinea> lineas) {
        Map<Long, BigDecimal> requerido = new TreeMap<>();
        for (RecetaLinea linea : lineas) {
            Integer unidades = vendidos.get(linea.getProductoId());
            if (unidades == null || unidades <= 0) {
                continue;
            }
            BigDecimal gasto = linea.getCantidad().multiply(BigDecimal.valueOf(unidades));
            requerido.merge(linea.getInsumoId(), gasto, BigDecimal::add);
        }
        return requerido;
    }

    /** "2.5" en vez de "2.500", y sin notación científica. */
    static String texto(BigDecimal valor) {
        BigDecimal limpio = valor.stripTrailingZeros();
        return (limpio.signum() == 0 ? BigDecimal.ZERO : limpio).toPlainString();
    }

    static String etiquetaUnidad(String unidad) {
        return switch (unidad) {
            case Insumo.KG -> "kg";
            case Insumo.G -> "g";
            case Insumo.L -> "L";
            case Insumo.ML -> "ml";
            default -> "u.";
        };
    }
}
