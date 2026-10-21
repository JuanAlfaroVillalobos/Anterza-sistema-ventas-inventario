package pe.com.anterza.gestion.ventas.inventario.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.com.anterza.gestion.ventas.inventario.modelo.Producto;

public interface ProductoDAO extends JpaRepository<Producto, Integer> {

    Optional<Producto> findByCodigo(String codigo);

    List<Producto> findByNombreContainingIgnoreCase(String nombre);

    List<Producto> findByActivoTrue();

    List<Producto> findByCategoria_IdCategoria(Integer idCategoria);
}