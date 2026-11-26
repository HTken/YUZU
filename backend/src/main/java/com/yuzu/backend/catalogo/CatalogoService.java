package com.yuzu.backend.catalogo;

import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.yuzu.backend.config.SesionActual;
import com.yuzu.backend.suscripcion.SuscripcionService;

/**
 * Carta del restaurante. El restaurante sale siempre del token (SesionActual) y todas las
 * búsquedas lo incluyen: lo que pertenece a otro restaurante responde "no encontrado".
 */
@Service
public class CatalogoService {

    private final CategoriaRepository categorias;
    private final ProductoRepository productos;
    private final SuscripcionService suscripcion;

    public CatalogoService(CategoriaRepository categorias,
                           ProductoRepository productos,
                           SuscripcionService suscripcion) {
        this.categorias = categorias;
        this.productos = productos;
        this.suscripcion = suscripcion;
    }

    // ---------------------------------------------------------------- categorías

    @Transactional(readOnly = true)
    public List<CategoriaDto> listarCategorias() {
        Long restauranteId = SesionActual.restauranteId();

        Map<Long, Integer> totales = new HashMap<>();
        Map<Long, Integer> activos = new HashMap<>();
        for (Producto p : productos.findByRestauranteIdOrderByNombreAsc(restauranteId)) {
            totales.merge(p.getCategoriaId(), 1, Integer::sum);
            if (p.isActivo()) {
                activos.merge(p.getCategoriaId(), 1, Integer::sum);
            }
        }

        return categorias.findByRestauranteIdOrderByOrdenAscNombreAsc(restauranteId).stream()
                .map(c -> CategoriaDto.desde(c, totales.getOrDefault(c.getId(), 0), activos.getOrDefault(c.getId(), 0)))
                .toList();
    }

    @Transactional
    public CategoriaDto crearCategoria(CategoriaRequest request) {
        Long restauranteId = SesionActual.restauranteId();
        String nombre = request.nombre().trim();

        if (categorias.existsByRestauranteIdAndNombreIgnoreCase(restauranteId, nombre)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya tienes una categoría con ese nombre");
        }

        Categoria categoria = new Categoria();
        categoria.setRestauranteId(restauranteId);
        categoria.setNombre(nombre);
        categoria.setOrden(request.orden());
        categoria.setActivo(request.activo());
        categoria.setPasaPorCocina(request.pasaPorCocina() == null || request.pasaPorCocina());
        return CategoriaDto.desde(categorias.save(categoria), 0, 0);
    }

    @Transactional
    public CategoriaDto actualizarCategoria(Long id, CategoriaRequest request) {
        Long restauranteId = SesionActual.restauranteId();
        Categoria categoria = categoria(id, restauranteId);
        String nombre = request.nombre().trim();

        if (categorias.existsByRestauranteIdAndNombreIgnoreCaseAndIdNot(restauranteId, nombre, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya tienes una categoría con ese nombre");
        }

        if (categoria.isActivo() && !request.activo()) {
            long activos = productos.countByRestauranteIdAndCategoriaIdAndActivoTrue(restauranteId, id);
            if (activos > 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "La categoría tiene " + activos + (activos == 1 ? " producto activo" : " productos activos")
                                + ". Desactívalos o pásalos a otra categoría antes de desactivarla.");
            }
        }

        categoria.setNombre(nombre);
        categoria.setOrden(request.orden());
        categoria.setActivo(request.activo());
        if (request.pasaPorCocina() != null) {
            categoria.setPasaPorCocina(request.pasaPorCocina());
        }
        categorias.save(categoria);

        List<Producto> deLaCategoria = productos.findByRestauranteIdOrderByNombreAsc(restauranteId).stream()
                .filter(p -> p.getCategoriaId().equals(id))
                .toList();
        int activos = (int) deLaCategoria.stream().filter(Producto::isActivo).count();
        return CategoriaDto.desde(categoria, deLaCategoria.size(), activos);
    }

    // ----------------------------------------------------------------- productos

    @Transactional(readOnly = true)
    public List<ProductoDto> listarProductos() {
        Long restauranteId = SesionActual.restauranteId();
        Map<Long, String> nombres = nombresDeCategorias(restauranteId);

        return productos.findByRestauranteIdOrderByNombreAsc(restauranteId).stream()
                .map(p -> ProductoDto.desde(p, nombres.get(p.getCategoriaId())))
                .toList();
    }

    @Transactional
    public ProductoDto crearProducto(ProductoRequest request) {
        Long restauranteId = SesionActual.restauranteId();
        String nombre = request.nombre().trim();

        // Si la categoría es de otro restaurante, se responde "no encontrada".
        Categoria categoria = categoria(request.categoriaId(), restauranteId);

        if (productos.existsByRestauranteIdAndNombreIgnoreCase(restauranteId, nombre)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya tienes un producto con ese nombre");
        }
        exigirCategoriaActivaSiCorresponde(categoria, request.activo());
        if (request.activo()) {
            suscripcion.exigirCupoDeProductos(restauranteId);
        }

        Producto producto = new Producto();
        producto.setRestauranteId(restauranteId);
        aplicar(producto, request, nombre);
        return ProductoDto.desde(productos.save(producto), categoria.getNombre());
    }

    @Transactional
    public ProductoDto actualizarProducto(Long id, ProductoRequest request) {
        Long restauranteId = SesionActual.restauranteId();
        Producto producto = productos.findByIdAndRestauranteId(id, restauranteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
        Categoria categoria = categoria(request.categoriaId(), restauranteId);
        String nombre = request.nombre().trim();

        if (productos.existsByRestauranteIdAndNombreIgnoreCaseAndIdNot(restauranteId, nombre, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya tienes un producto con ese nombre");
        }
        exigirCategoriaActivaSiCorresponde(categoria, request.activo());

        // Volver a activar un producto también consume un cupo del plan.
        if (request.activo() && !producto.isActivo()) {
            suscripcion.exigirCupoDeProductos(restauranteId);
        }

        aplicar(producto, request, nombre);
        return ProductoDto.desde(productos.save(producto), categoria.getNombre());
    }

    // ------------------------------------------------------------------- ayudas

    private void aplicar(Producto producto, ProductoRequest request, String nombre) {
        producto.setCategoriaId(request.categoriaId());
        producto.setNombre(nombre);
        producto.setDescripcion(limpiar(request.descripcion()));
        producto.setPrecio(request.precio().setScale(2, RoundingMode.HALF_UP));
        producto.setActivo(request.activo());
    }

    private void exigirCategoriaActivaSiCorresponde(Categoria categoria, boolean productoActivo) {
        if (productoActivo && !categoria.isActivo()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La categoría \"" + categoria.getNombre()
                            + "\" está desactivada. Actívala o elige otra para dejar activo este producto.");
        }
    }

    private Categoria categoria(Long id, Long restauranteId) {
        return categorias.findByIdAndRestauranteId(id, restauranteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoría no encontrada"));
    }

    private Map<Long, String> nombresDeCategorias(Long restauranteId) {
        return categorias.findByRestauranteIdOrderByOrdenAscNombreAsc(restauranteId).stream()
                .collect(Collectors.toMap(Categoria::getId, Categoria::getNombre, (a, b) -> a, HashMap::new));
    }

    private static String limpiar(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim();
    }
}
