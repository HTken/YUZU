package com.yuzu.backend.inventario;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.yuzu.backend.catalogo.ProductoRepository;
import com.yuzu.backend.config.SesionActual;

/**
 * Recetas: qué insumos gasta cada producto. El restaurante sale siempre del token (SesionActual) y el producto y
 * los insumos se buscan dentro de ese restaurante: lo que sea de otro responde "no encontrado".
 */
@Service
public class RecetasService {

    private final RecetaRepository recetas;
    private final InsumoRepository insumos;
    private final ProductoRepository productos;

    public RecetasService(RecetaRepository recetas, InsumoRepository insumos, ProductoRepository productos) {
        this.recetas = recetas;
        this.insumos = insumos;
        this.productos = productos;
    }

    /** Cuántos insumos tiene la receta de cada producto. */
    @Transactional(readOnly = true)
    public List<ResumenReceta> resumen() {
        return recetas.resumen(SesionActual.restauranteId());
    }

    @Transactional(readOnly = true)
    public RecetaDto obtener(Long productoId) {
        Long restauranteId = SesionActual.restauranteId();
        exigirProducto(productoId, restauranteId);

        List<RecetaLinea> lineas = recetas.findByRestauranteIdAndProductoIdOrderByIdAsc(restauranteId, productoId);
        Set<Long> ids = new HashSet<>();
        for (RecetaLinea l : lineas) {
            ids.add(l.getInsumoId());
        }
        Map<Long, Insumo> porId = porId(restauranteId, ids);

        List<RecetaDto.Linea> resultado = new ArrayList<>();
        for (RecetaLinea l : lineas) {
            Insumo insumo = porId.get(l.getInsumoId());
            resultado.add(new RecetaDto.Linea(l.getInsumoId(), insumo.getNombre(), insumo.getUnidad(),
                    l.getCantidad()));
        }
        return new RecetaDto(productoId, resultado);
    }

    /** Reemplaza la receta completa del producto. Una lista vacía la quita. */
    @Transactional
    public RecetaDto guardar(Long productoId, RecetaRequest request) {
        Long restauranteId = SesionActual.restauranteId();
        exigirProducto(productoId, restauranteId);

        Set<Long> ids = new HashSet<>();
        for (RecetaRequest.Linea linea : request.lineas()) {
            if (!ids.add(linea.insumoId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Hay un insumo repetido en la receta. Cada insumo va una sola vez.");
            }
        }
        Map<Long, Insumo> porId = porId(restauranteId, ids);
        if (porId.size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Alguno de los insumos no existe");
        }

        // Se arma todo antes de tocar la base de datos.
        List<RecetaLinea> nuevas = new ArrayList<>();
        List<RecetaDto.Linea> resultado = new ArrayList<>();
        for (RecetaRequest.Linea linea : request.lineas()) {
            Insumo insumo = porId.get(linea.insumoId());
            BigDecimal cantidad = linea.cantidad().setScale(3, RoundingMode.HALF_UP);
            if (cantidad.signum() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La cantidad de cada insumo debe ser mayor a 0");
            }

            RecetaLinea fila = new RecetaLinea();
            fila.setRestauranteId(restauranteId);
            fila.setProductoId(productoId);
            fila.setInsumoId(insumo.getId());
            fila.setCantidad(cantidad);
            nuevas.add(fila);
            resultado.add(new RecetaDto.Linea(insumo.getId(), insumo.getNombre(), insumo.getUnidad(), cantidad));
        }

        recetas.borrarDeProducto(restauranteId, productoId);
        recetas.saveAll(nuevas);
        return new RecetaDto(productoId, resultado);
    }

    private void exigirProducto(Long productoId, Long restauranteId) {
        productos.findByIdAndRestauranteId(productoId, restauranteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
    }

    private Map<Long, Insumo> porId(Long restauranteId, Set<Long> ids) {
        Map<Long, Insumo> porId = new HashMap<>();
        if (ids.isEmpty()) {
            return porId;
        }
        for (Insumo i : insumos.findByRestauranteIdAndIdIn(restauranteId, ids)) {
            porId.put(i.getId(), i);
        }
        return porId;
    }
}
