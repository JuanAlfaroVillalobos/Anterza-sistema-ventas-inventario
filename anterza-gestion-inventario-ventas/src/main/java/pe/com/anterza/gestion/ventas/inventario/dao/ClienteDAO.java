package pe.com.anterza.gestion.ventas.inventario.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.com.anterza.gestion.ventas.inventario.modelo.Cliente;

public interface ClienteDAO extends JpaRepository<Cliente, Integer> {

    Optional<Cliente> findByDni(String dni);

    List<Cliente> findByApellidoPaternoContainingIgnoreCase(String apellidoPaterno);
}