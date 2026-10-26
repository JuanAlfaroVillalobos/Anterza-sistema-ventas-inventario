package pe.com.anterza.gestion.ventas.inventario.dao;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;
import pe.com.anterza.gestion.ventas.inventario.modelo.Producto;

/** Consultas de Producto para la pantalla de venta (solo métodos por nombre, sin escribir consultas). */
public interface ProductoVentaDAO extends JpaRepository<Producto, Integer> {

    /** Catálogo para vender: solo productos activos, ordenados por nombre. */
    List<Producto> findByActivoTrueOrderByNombreAsc();

    /**
     * Trae los productos pedidos y los bloquea hasta terminar la transacción, así dos ventas
     * simultáneas del mismo producto se atienden una tras otra y no se vende dos veces la misma unidad.
     * Se piden siempre ordenados por id para evitar bloqueos cruzados.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Producto> findByIdProductoInOrderByIdProducto(Collection<Integer> ids);
}