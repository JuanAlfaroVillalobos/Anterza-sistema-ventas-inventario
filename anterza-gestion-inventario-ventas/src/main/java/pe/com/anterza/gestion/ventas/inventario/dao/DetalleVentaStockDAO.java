package pe.com.anterza.gestion.ventas.inventario.dao;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.com.anterza.gestion.ventas.inventario.modelo.DetalleVenta;

/** Consultas de DetalleVenta para calcular cuánto se vendió de cada lote (solo métodos por nombre). */
public interface DetalleVentaStockDAO extends JpaRepository<DetalleVenta, Integer> {

    /** Detalles de ventas con el estado dado (EMITIDA) que salieron de lotes no vencidos. */
    List<DetalleVenta> findByLote_FechaVencimientoGreaterThanEqualAndVenta_Estado(LocalDate hoy, String estado);
}