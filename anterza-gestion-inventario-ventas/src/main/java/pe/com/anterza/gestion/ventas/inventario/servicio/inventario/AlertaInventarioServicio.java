package pe.com.anterza.gestion.ventas.inventario.servicio.inventario;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.com.anterza.gestion.ventas.inventario.dao.AlertaInventarioDAO;
import pe.com.anterza.gestion.ventas.inventario.modelo.AlertaInventario;
import pe.com.anterza.gestion.ventas.inventario.modelo.Lote;
import pe.com.anterza.gestion.ventas.inventario.modelo.Producto;

/**
 * Alertas de inventario que se muestran una sola vez:
 *  - Lotes que vencen en 3 meses (90 dias) o menos, o ya vencidos, con unidades por vender.
 *  - Productos con stock menor a 5.
 * Cada alerta mostrada se guarda y pasa al historial.
 */
@Service
public class AlertaInventarioServicio {

    public static final int DIAS_VENCIMIENTO = 90;
    public static final int STOCK_ALERTA = 5;

    private final AlertaInventarioDAO alertaDAO;
    private final InventarioServicio inventario;

    public AlertaInventarioServicio(AlertaInventarioDAO alertaDAO, InventarioServicio inventario) {
        this.alertaDAO = alertaDAO;
        this.inventario = inventario;
    }

    /** Alertas que se muestran ahora en el modal. */
    public record AlertasNuevas(List<AlertaInventario> vencimiento, List<AlertaInventario> stock) { }

    /** Detecta las alertas que aun no se mostraron, las guarda y las devuelve. */
    @Transactional
    public AlertasNuevas generarNuevas() {
        return new AlertasNuevas(nuevasDeVencimiento(), nuevasDeStock());
    }

    private List<AlertaInventario> nuevasDeVencimiento() {
        Set<Integer> lotesYaAlertados = alertaDAO.findByTipo(AlertaInventario.VENCIMIENTO).stream()
                .map(a -> a.getLote().getIdLote())
                .collect(Collectors.toSet());

        List<Lote> candidatos = new ArrayList<>(inventario.lotesVencidos());
        candidatos.addAll(inventario.lotesProximosAVencer(DIAS_VENCIMIENTO));

        LocalDate hoy = LocalDate.now();
        List<AlertaInventario> nuevas = new ArrayList<>();
        for (Lote lote : candidatos) {
            if (lotesYaAlertados.contains(lote.getIdLote())) {
                continue;
            }
            Long restante = inventario.stockDelLote(lote.getIdLote());
            if (restante == null || restante <= 0) {
                continue; // lote sin unidades: no hay nada que alertar
            }
            AlertaInventario a = new AlertaInventario();
            a.setTipo(AlertaInventario.VENCIMIENTO);
            a.setProducto(lote.getProducto());
            a.setLote(lote);
            a.setValor((int) ChronoUnit.DAYS.between(hoy, lote.getFechaVencimiento()));
            nuevas.add(a);
        }
        return alertaDAO.saveAll(nuevas);
    }

    private List<AlertaInventario> nuevasDeStock() {
        Set<Integer> productosYaAlertados = alertaDAO.findByTipo(AlertaInventario.STOCK).stream()
                .map(a -> a.getProducto().getIdProducto())
                .collect(Collectors.toSet());

        List<AlertaInventario> nuevas = new ArrayList<>();
        for (Producto p : inventario.listarProductosActivos()) {
            long stock = p.getStockActual() != null ? p.getStockActual() : 0L;
            if (stock < STOCK_ALERTA && !productosYaAlertados.contains(p.getIdProducto())) {
                AlertaInventario a = new AlertaInventario();
                a.setTipo(AlertaInventario.STOCK);
                a.setProducto(p);
                a.setValor((int) stock);
                nuevas.add(a);
            }
        }
        return alertaDAO.saveAll(nuevas);
    }

    @Transactional(readOnly = true)
    public List<AlertaInventario> historialVencimiento() {
        return alertaDAO.findByTipoOrderByFechaAlertaDescIdAlertaDesc(AlertaInventario.VENCIMIENTO);
    }

    @Transactional(readOnly = true)
    public List<AlertaInventario> historialStock() {
        return alertaDAO.findByTipoOrderByFechaAlertaDescIdAlertaDesc(AlertaInventario.STOCK);
    }
}
