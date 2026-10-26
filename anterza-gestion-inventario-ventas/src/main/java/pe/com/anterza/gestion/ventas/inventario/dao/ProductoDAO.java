package pe.com.anterza.gestion.ventas.inventario.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.com.anterza.gestion.ventas.inventario.modelo.Producto;

public interface ProductoDAO extends JpaRepository<Producto, Integer> {

    Optional<Producto> findByCodigo(String codigo);

    List<Producto> findByNombreContainingIgnoreCase(String nombre);

    List<Producto> findByActivoTrue();

    List<Producto> findByCategoria_IdCategoria(Integer idCategoria);

    /** JOIN FETCH categoría — evita LazyInitializationException (open-in-view=false). */
    @Query("SELECT p FROM Producto p JOIN FETCH p.categoria WHERE p.activo = true ORDER BY p.nombre")
    List<Producto> listarActivosConCategoria();

    @Query("SELECT p FROM Producto p JOIN FETCH p.categoria WHERE p.idProducto = :id")
    Optional<Producto> buscarPorIdConCategoria(@Param("id") Integer id);

    @Query("SELECT p FROM Producto p JOIN FETCH p.categoria WHERE p.activo = true AND p.categoria.idCategoria = :idCat ORDER BY p.nombre")
    List<Producto> listarPorCategoriaConFetch(@Param("idCat") Integer idCategoria);

    @Query("SELECT p FROM Producto p JOIN FETCH p.categoria WHERE p.activo = true AND LOWER(p.nombre) LIKE LOWER(CONCAT('%', :nombre, '%')) ORDER BY p.nombre")
    List<Producto> buscarPorNombreConFetch(@Param("nombre") String nombre);
}