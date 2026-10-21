package pe.com.anterza.gestion.ventas.inventario.dao;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.com.anterza.gestion.ventas.inventario.modelo.Venta;

public interface VentaDAO extends JpaRepository<Venta, Integer> {

    List<Venta> findByCliente_IdCliente(Integer idCliente);

    List<Venta> findByFechaVentaBetween(LocalDateTime desde, LocalDateTime hasta);
}