package pe.com.anterza.gestion.ventas.inventario.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.com.anterza.gestion.ventas.inventario.modelo.DetalleVenta;

public interface DetalleVentaDAO extends JpaRepository<DetalleVenta, Integer> {

    List<DetalleVenta> findByVenta_IdVenta(Integer idVenta);
}