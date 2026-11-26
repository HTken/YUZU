package com.yuzu.backend.informes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Las cuentas del informe, separadas de la base de datos para poder probarlas solas:
 * reciben las filas ya leídas y devuelven lo que se muestra.
 */
public final class CalculosInforme {

    private CalculosInforme() {
    }

    /** Lo que se cobró: suma de todas las cuentas, con dos decimales. */
    public static BigDecimal total(List<VentaFila> filas) {
        BigDecimal suma = BigDecimal.ZERO;
        for (VentaFila f : filas) {
            suma = suma.add(f.total());
        }
        return dinero(suma);
    }

    /** Lo vendido por cada cuenta, en promedio. Sin ventas es 0. */
    public static BigDecimal ticketPromedio(BigDecimal total, int ventas) {
        if (ventas <= 0) {
            return dinero(BigDecimal.ZERO);
        }
        return total.divide(BigDecimal.valueOf(ventas), 2, RoundingMode.HALF_UP);
    }

    /**
     * Las ventas día por día, en días de Lima. Aparecen todos los días del periodo, también los que no
     * tuvieron ventas (con 0), para que se vea cuándo no se vendió.
     */
    public static List<VentaDiaDto> porDia(List<VentaFila> filas, PeriodoInforme periodo) {
        Map<LocalDate, Acumulado> dias = new TreeMap<>();
        for (LocalDate dia = periodo.desde(); !dia.isAfter(periodo.hasta()); dia = dia.plusDays(1)) {
            dias.put(dia, new Acumulado());
        }

        for (VentaFila f : filas) {
            Acumulado dia = dias.get(PeriodoInforme.diaLima(f.cobradoEn()));
            if (dia == null) {
                continue; // fuera del periodo: la consulta no debería traerlo
            }
            dia.ventas++;
            dia.total = dia.total.add(f.total());
        }

        List<VentaDiaDto> resultado = new ArrayList<>(dias.size());
        for (Map.Entry<LocalDate, Acumulado> e : dias.entrySet()) {
            resultado.add(new VentaDiaDto(e.getKey().toString(), e.getValue().ventas, dinero(e.getValue().total)));
        }
        return resultado;
    }

    /**
     * Lo cobrado con cada método de pago, en el orden indicado. Los métodos sin pagos aparecen con 0;
     * si hubiera uno que no está en la lista, va al final.
     */
    public static List<MetodoPagoDto> porMetodo(List<MetodoFila> filas, List<String> orden) {
        Map<String, MetodoFila> porNombre = new HashMap<>();
        for (MetodoFila f : filas) {
            porNombre.put(f.metodo(), f);
        }

        List<MetodoPagoDto> resultado = new ArrayList<>();
        for (String metodo : orden) {
            MetodoFila f = porNombre.remove(metodo);
            resultado.add(f == null
                    ? new MetodoPagoDto(metodo, 0, dinero(BigDecimal.ZERO))
                    : new MetodoPagoDto(metodo, f.cuentas().intValue(), dinero(f.total())));
        }
        porNombre.values().stream()
                .sorted(Comparator.comparing(MetodoFila::metodo))
                .forEach(f -> resultado.add(new MetodoPagoDto(f.metodo(), f.cuentas().intValue(), dinero(f.total()))));
        return resultado;
    }

    /** Los productos vendidos, del que más salió al que menos (a igualdad: más importe, luego por nombre). */
    public static List<ProductoVendidoDto> productos(List<ProductoFila> filas) {
        return filas.stream()
                .sorted(Comparator.comparing(ProductoFila::cantidad).reversed()
                        .thenComparing(Comparator.comparing(ProductoFila::importe).reversed())
                        .thenComparing(ProductoFila::nombre, String.CASE_INSENSITIVE_ORDER))
                .map(f -> new ProductoVendidoDto(f.productoId(), f.nombre(), f.categoria(), f.cantidad(), dinero(f.importe())))
                .toList();
    }

    /**
     * Lo que pasó con cada insumo. Las ventas y mermas se guardan con signo negativo (restan del stock);
     * aquí se muestran como cantidades positivas. Las devoluciones por ventas anuladas (positivas) se restan
     * del consumo, así el consumo es lo que quedó descontado de verdad. Solo aparecen los insumos que tuvieron
     * consumo, merma, entradas o faltante; los que solo tuvieron salidas manuales o ajustes no se listan.
     */
    public static List<InsumoPeriodoDto> insumos(List<MovimientoFila> filas, Map<Long, InsumoInfo> info) {
        Map<Long, Totales> porInsumo = new LinkedHashMap<>();

        for (MovimientoFila f : filas) {
            Totales t = porInsumo.computeIfAbsent(f.insumoId(), id -> new Totales());
            switch (f.tipo()) {
                case "VENTA" -> {
                    t.consumo = t.consumo.add(f.cantidad().abs());
                    t.faltante = t.faltante.add(f.faltante());
                }
                case "DEVOLUCION" -> t.consumo = t.consumo.subtract(f.cantidad().abs());
                case "MERMA" -> t.merma = t.merma.add(f.cantidad().abs());
                case "ENTRADA" -> {
                    t.entradas = t.entradas.add(f.cantidad().abs());
                    t.compras = t.compras.add(f.costo());
                }
                default -> {
                    // SALIDA y AJUSTE no son parte de este informe
                }
            }
        }

        List<InsumoPeriodoDto> resultado = new ArrayList<>();
        for (Map.Entry<Long, Totales> e : porInsumo.entrySet()) {
            Totales t = e.getValue();
            if (t.consumo.signum() == 0 && t.merma.signum() == 0 && t.entradas.signum() == 0
                    && t.faltante.signum() == 0) {
                continue;
            }
            InsumoInfo i = info.getOrDefault(e.getKey(), new InsumoInfo("Insumo " + e.getKey(), "UNIDAD"));
            resultado.add(new InsumoPeriodoDto(e.getKey(), i.nombre(), i.unidad(),
                    cantidad(t.consumo), cantidad(t.merma), cantidad(t.entradas), dinero(t.compras),
                    cantidad(t.faltante)));
        }

        resultado.sort(Comparator.comparing(InsumoPeriodoDto::consumo).reversed()
                .thenComparing(Comparator.comparing(InsumoPeriodoDto::merma).reversed())
                .thenComparing(Comparator.comparing(InsumoPeriodoDto::entradas).reversed())
                .thenComparing(InsumoPeriodoDto::nombre, String.CASE_INSENSITIVE_ORDER));
        return resultado;
    }

    /** Lo que costaron todas las entradas del periodo. */
    public static BigDecimal compras(List<InsumoPeriodoDto> insumos) {
        BigDecimal suma = BigDecimal.ZERO;
        for (InsumoPeriodoDto i : insumos) {
            suma = suma.add(i.compras());
        }
        return dinero(suma);
    }

    public static int conFaltante(List<InsumoPeriodoDto> insumos) {
        return (int) insumos.stream().filter(i -> i.faltante().signum() > 0).count();
    }

    private static BigDecimal dinero(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal cantidad(BigDecimal valor) {
        return valor.setScale(3, RoundingMode.HALF_UP);
    }

    private static final class Acumulado {
        int ventas;
        BigDecimal total = BigDecimal.ZERO;
    }

    private static final class Totales {
        BigDecimal consumo = BigDecimal.ZERO;
        BigDecimal merma = BigDecimal.ZERO;
        BigDecimal entradas = BigDecimal.ZERO;
        BigDecimal compras = BigDecimal.ZERO;
        BigDecimal faltante = BigDecimal.ZERO;
    }
}
