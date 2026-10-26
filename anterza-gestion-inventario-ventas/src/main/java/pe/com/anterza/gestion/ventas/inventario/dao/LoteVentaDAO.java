package pe.com.anterza.gestion.ventas.inventario.dao;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.com.anterza.gestion.ventas.inventario.modelo.Lote;

/** Consultas de Lote para la pantalla de venta (solo métodos por nombre). */
public interface LoteVentaDAO extends JpaRepository<Lote, Integer> {

    /** Lotes no vencidos, el que vence primero va primero (FEFO). */
    List<Lote> findByFechaVencimientoGreaterThanEqualOrderByFechaVencimientoAscIdLoteAsc(LocalDate hoy);
}