package pe.com.anterza.gestion.ventas.inventario.dao;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.com.anterza.gestion.ventas.inventario.modelo.Compra;

public interface CompraDAO extends JpaRepository<Compra, Integer> {

    List<Compra> findByProveedor_IdProveedor(Integer idProveedor);

    List<Compra> findByFechaCompraBetween(LocalDate desde, LocalDate hasta);
}