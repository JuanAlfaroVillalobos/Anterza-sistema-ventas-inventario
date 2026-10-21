package pe.com.anterza.gestion.ventas.inventario.dao;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.com.anterza.gestion.ventas.inventario.modelo.Lote;

public interface LoteDAO extends JpaRepository<Lote, Integer> {

    List<Lote> findByProducto_IdProducto(Integer idProducto);

    List<Lote> findByFechaVencimientoBefore(LocalDate fecha);

    @Query("""
            SELECT l.cantidad - COALESCE(
                (SELECT SUM(d.cantidad) FROM DetalleVenta d
                  WHERE d.lote = l AND d.venta.estado = 'EMITIDA'), 0)
              FROM Lote l
             WHERE l.idLote = :idLote
            """)
    Long stockDelLote(@Param("idLote") Integer idLote);

    @Query("""
            SELECT l FROM Lote l
             WHERE l.producto.idProducto = :idProducto
               AND l.fechaVencimiento >= CURRENT_DATE
               AND l.cantidad > COALESCE(
                   (SELECT SUM(d.cantidad) FROM DetalleVenta d
                     WHERE d.lote = l AND d.venta.estado = 'EMITIDA'), 0)
             ORDER BY l.fechaVencimiento ASC
            """)
    List<Lote> listarDisponiblesPorProducto(@Param("idProducto") Integer idProducto);

    @Query("""
            SELECT COALESCE(SUM(l.cantidad), 0) - COALESCE(
                (SELECT SUM(d.cantidad) FROM DetalleVenta d
                  WHERE d.lote.producto.idProducto = :idProducto
                    AND d.venta.estado = 'EMITIDA'), 0)
              FROM Lote l
             WHERE l.producto.idProducto = :idProducto
            """)
    Long stockTotalDelProducto(@Param("idProducto") Integer idProducto);
}