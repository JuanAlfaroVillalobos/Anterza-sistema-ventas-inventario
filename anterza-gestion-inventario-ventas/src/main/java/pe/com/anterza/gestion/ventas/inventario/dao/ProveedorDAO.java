package pe.com.anterza.gestion.ventas.inventario.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.com.anterza.gestion.ventas.inventario.modelo.Proveedor;

public interface ProveedorDAO extends JpaRepository<Proveedor, Integer> {

    Optional<Proveedor> findByRuc(String ruc);
}