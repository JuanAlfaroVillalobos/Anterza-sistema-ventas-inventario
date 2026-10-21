package pe.com.anterza.gestion.ventas.inventario.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.com.anterza.gestion.ventas.inventario.modelo.Empleado;

public interface EmpleadoDAO extends JpaRepository<Empleado, Integer> {

    Optional<Empleado> findByCorreo(String correo);

    Optional<Empleado> findByDni(String dni);
}