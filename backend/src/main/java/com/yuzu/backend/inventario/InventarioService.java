package com.yuzu.backend.inventario;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.yuzu.backend.config.SesionActual;
import com.yuzu.backend.config.Tiempo;
import com.yuzu.backend.suscripcion.SuscripcionService;
import com.yuzu.backend.usuario.UsuarioRepository;

/**
 * Insumos y su stock. El restaurante sale siempre del token (SesionActual) y todas las búsquedas lo incluyen:
 * lo que pertenece a otro restaurante responde "no encontrado".
 * El stock nunca se edita directamente: solo cambia registrando un movimiento, y cada movimiento queda guardado.
 */
@Service
public class InventarioService {

    private final InsumoRepository insumos;
    private final MovimientoInsumoRepository movimientos;
    private final RecetaRepository recetas;
    private final UsuarioRepository usuarios;
    private final SuscripcionService suscripcion;

    public InventarioService(InsumoRepository insumos,
                             MovimientoInsumoRepository movimientos,
                             RecetaRepository recetas,
                             UsuarioRepository usuarios,
                             SuscripcionService suscripcion) {
        this.insumos = insumos;
        this.movimientos = movimientos;
        this.recetas = recetas;
        this.usuarios = usuarios;
        this.suscripcion = suscripcion;
    }

    @Transactional(readOnly = true)
    public List<InsumoDto> listar() {
        Long restauranteId = SesionActual.restauranteId();
        // Con movimientos o dentro de una receta, la unidad de medida ya no se puede cambiar.
        Set<Long> unidadFija = new HashSet<>(movimientos.insumosConMovimientos(restauranteId));
        unidadFija.addAll(recetas.insumosEnRecetas(restauranteId));

        return insumos.findByRestauranteIdOrderByNombreAsc(restauranteId).stream()
                .map(i -> InsumoDto.desde(i, unidadFija.contains(i.getId())))
                .toList();
    }

    @Transactional
    public InsumoDto crear(InsumoRequest request) {
        Long restauranteId = SesionActual.restauranteId();
        String nombre = request.nombre().trim();
        String unidad = unidad(request.unidad());

        BigDecimal minimo = ReglasStock.cantidad(request.stockMinimo(), unidad, "El stock mínimo");
        BigDecimal inicial = request.stockInicial() == null
                ? BigDecimal.ZERO.setScale(3)
                : ReglasStock.cantidad(request.stockInicial(), unidad, "El stock inicial");

        if (insumos.existsByRestauranteIdAndNombreIgnoreCase(restauranteId, nombre)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya tienes un insumo con ese nombre");
        }
        if (request.activo()) {
            suscripcion.exigirCupoDeInsumos(restauranteId);
        }

        Insumo insumo = new Insumo();
        insumo.setRestauranteId(restauranteId);
        insumo.setNombre(nombre);
        insumo.setUnidad(unidad);
        insumo.setStockMinimo(minimo);
        insumo.setStockActual(inicial);
        insumo.setActivo(request.activo());
        insumos.save(insumo);

        // Lo que ya había en el local queda como el primer movimiento, para que el historial cuadre desde el inicio.
        boolean conStock = inicial.signum() > 0;
        if (conStock) {
            guardarMovimiento(insumo, MovimientoInsumo.AJUSTE, inicial, inicial, null, "Stock inicial");
        }
        return InsumoDto.desde(insumo, conStock);
    }

    @Transactional
    public InsumoDto actualizar(Long id, InsumoRequest request) {
        Long restauranteId = SesionActual.restauranteId();
        // Se bloquea primero para no cambiar la unidad mientras alguien registra un movimiento.
        Insumo insumo = insumos.bloquearPorId(id, restauranteId).orElseThrow(this::noEncontrado);

        String nombre = request.nombre().trim();
        String unidad = unidad(request.unidad());
        BigDecimal minimo = ReglasStock.cantidad(request.stockMinimo(), unidad, "El stock mínimo");

        if (insumos.existsByRestauranteIdAndNombreIgnoreCaseAndIdNot(restauranteId, nombre, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya tienes un insumo con ese nombre");
        }

        // Si ya tiene movimientos o está en una receta, cambiar la unidad (de kg a g, por ejemplo)
        // dejaría mal el stock y las recetas.
        boolean unidadFija = movimientos.existsByRestauranteIdAndInsumoId(restauranteId, id)
                || recetas.existsByRestauranteIdAndInsumoId(restauranteId, id);
        if (unidadFija && !unidad.equals(insumo.getUnidad())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede cambiar la unidad de un insumo que ya tiene movimientos o está en una receta. "
                            + "Crea otro insumo con la unidad correcta y desactiva este.");
        }

        // Volver a activar un insumo también consume un cupo del plan.
        if (request.activo() && !insumo.isActivo()) {
            suscripcion.exigirCupoDeInsumos(restauranteId);
        }

        insumo.setNombre(nombre);
        insumo.setUnidad(unidad);
        insumo.setStockMinimo(minimo);
        insumo.setActivo(request.activo());
        insumos.save(insumo);
        return InsumoDto.desde(insumo, unidadFija);
    }

    /**
     * Registra una entrada, salida, merma o ajuste y devuelve el insumo con su nuevo stock.
     * El ajuste (corregir el stock después de contar) solo lo hace el administrador.
     */
    @Transactional
    public InsumoDto registrarMovimiento(Long insumoId, MovimientoInsumoRequest request) {
        Long restauranteId = SesionActual.restauranteId();
        String tipo = tipo(request.tipo());

        if (MovimientoInsumo.AJUSTE.equals(tipo) && !SesionActual.tieneRol("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo el administrador puede ajustar el stock después de un conteo");
        }

        // Lo primero que se lee: así el stock que se ve es el definitivo, aunque otro movimiento esté en curso.
        Insumo insumo = insumos.bloquearPorId(insumoId, restauranteId).orElseThrow(this::noEncontrado);
        if (!insumo.isActivo()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Este insumo está inactivo. Actívalo para registrar movimientos.");
        }

        BigDecimal cantidad = ReglasStock.cantidad(request.cantidad(), insumo.getUnidad(), "La cantidad");
        String nota = limpiar(request.nota());
        if (MovimientoInsumo.MERMA.equals(tipo) && nota == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cuenta qué pasó con la merma (por ejemplo: se malogró, se cayó, venció)");
        }

        BigDecimal cambio = ReglasStock.cambio(tipo, insumo.getStockActual(), cantidad, insumo.getUnidad());
        BigDecimal nuevo = insumo.getStockActual().add(cambio);

        BigDecimal costo = null;
        if (MovimientoInsumo.ENTRADA.equals(tipo) && request.costoTotal() != null) {
            costo = request.costoTotal().setScale(2, RoundingMode.HALF_UP);
        }

        insumo.setStockActual(nuevo);
        insumos.save(insumo);
        guardarMovimiento(insumo, tipo, cambio, nuevo, costo, nota);
        return InsumoDto.desde(insumo, true);
    }

    @Transactional(readOnly = true)
    public List<MovimientoInsumoDto> historial(Long insumoId) {
        Long restauranteId = SesionActual.restauranteId();
        insumos.findByIdAndRestauranteId(insumoId, restauranteId).orElseThrow(this::noEncontrado);

        return movimientos.findTop30ByRestauranteIdAndInsumoIdOrderByIdDesc(restauranteId, insumoId).stream()
                .map(MovimientoInsumoDto::desde)
                .toList();
    }

    // ------------------------------------------------------------------- ayudas

    private void guardarMovimiento(Insumo insumo, String tipo, BigDecimal cambio, BigDecimal stockResultante,
                                   BigDecimal costo, String nota) {
        MovimientoInsumo m = new MovimientoInsumo();
        m.setRestauranteId(insumo.getRestauranteId());
        m.setInsumoId(insumo.getId());
        m.setTipo(tipo);
        m.setCantidad(cambio);
        m.setStockResultante(stockResultante);
        m.setCostoTotal(costo);
        m.setNota(nota);
        m.setRegistradoPor(operador(insumo.getRestauranteId()));
        m.setCreadoEn(Tiempo.ahora());
        movimientos.save(m);
    }

    private String operador(Long restauranteId) {
        return usuarios.findByIdAndRestauranteId(SesionActual.usuarioId(), restauranteId)
                .map(u -> u.getNombre())
                .orElseGet(SesionActual::usuario);
    }

    private static String unidad(String texto) {
        String unidad = texto.trim().toUpperCase(Locale.ROOT);
        if (!Insumo.UNIDADES.contains(unidad)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La unidad de medida no es válida");
        }
        return unidad;
    }

    private static String tipo(String texto) {
        String tipo = texto.trim().toUpperCase(Locale.ROOT);
        if (!MovimientoInsumo.TIPOS.contains(tipo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El tipo de movimiento no es válido");
        }
        return tipo;
    }

    /** La nota sin espacios de más; si queda vacía, no se guarda nada. */
    private static String limpiar(String nota) {
        if (nota == null || nota.isBlank()) {
            return null;
        }
        return nota.trim();
    }

    private ResponseStatusException noEncontrado() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Insumo no encontrado");
    }
}
