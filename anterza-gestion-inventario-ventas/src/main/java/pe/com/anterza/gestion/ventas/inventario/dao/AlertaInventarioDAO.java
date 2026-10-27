package pe.com.anterza.gestion.ventas.inventario.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.com.anterza.gestion.ventas.inventario.modelo.AlertaInventario;

public interface AlertaInventarioDAO extends JpaRepository<AlertaInventario, Integer> {

    List<AlertaInventario> findByTipo(String tipo);

    List<AlertaInventario> findByTipoOrderByFechaAlertaDescIdAlertaDesc(String tipo);
}
