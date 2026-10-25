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

    // JOIN FETCH para cargar categoría y evitar LazyInitializationException
    @Query("SELECT p FROM Producto p JOIN FETCH p.categoria")
    List<Producto> findAllConCategoria();

    @Query("SELECT p FROM Producto p JOIN FETCH p.categoria WHERE LOWER(p.nombre) LIKE LOWER(CONCAT('%', :nombre, '%'))")
    List<Producto> findByNombreConCategoria(@Param("nombre") String nombre);

    @Query("SELECT p FROM Producto p JOIN FETCH p.categoria WHERE p.categoria.idCategoria = :idCategoria")
    List<Producto> findByCategoriaConCategoria(@Param("idCategoria") Integer idCategoria);
}