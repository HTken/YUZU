package com.yuzu.backend.inventario;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Todos los métodos reciben el restaurante: nunca se busca una receta "a secas". */
public interface RecetaRepository extends JpaRepository<RecetaLinea, Long> {

    List<RecetaLinea> findByRestauranteIdAndProductoIdOrderByIdAsc(Long restauranteId, Long productoId);

    List<RecetaLinea> findByRestauranteIdAndProductoIdIn(Long restauranteId, Collection<Long> productoIds);

    boolean existsByRestauranteIdAndInsumoId(Long restauranteId, Long insumoId);

    /** Cuántos insumos tiene la receta de cada producto (solo los que tienen receta). */
    @Query("select new com.yuzu.backend.inventario.ResumenReceta(r.productoId, count(r)) "
            + "from RecetaLinea r where r.restauranteId = :restauranteId group by r.productoId")
    List<ResumenReceta> resumen(@Param("restauranteId") Long restauranteId);

    /** Los insumos que se usan en alguna receta: su unidad de medida ya no se puede cambiar. */
    @Query("select distinct r.insumoId from RecetaLinea r where r.restauranteId = :restauranteId")
    List<Long> insumosEnRecetas(@Param("restauranteId") Long restauranteId);

    /**
     * Borra la receta de un producto al instante (no al final de la operación). Así, al guardar una receta
     * nueva con los mismos insumos no choca con las líneas que se acaban de borrar.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from RecetaLinea r where r.restauranteId = :restauranteId and r.productoId = :productoId")
    void borrarDeProducto(@Param("restauranteId") Long restauranteId, @Param("productoId") Long productoId);
}
