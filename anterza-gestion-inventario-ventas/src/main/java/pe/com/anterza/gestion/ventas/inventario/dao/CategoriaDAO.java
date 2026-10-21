package pe.com.anterza.gestion.ventas.inventario.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.com.anterza.gestion.ventas.inventario.modelo.Categoria;

public interface CategoriaDAO extends JpaRepository<Categoria, Integer> {

    Optional<Categoria> findByNombre(String nombre);
}