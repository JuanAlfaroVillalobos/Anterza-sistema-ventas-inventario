package pe.com.anterza.gestion.ventas.inventario.servicio.inventario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pe.com.anterza.gestion.ventas.inventario.dao.AlertaInventarioDAO;
import pe.com.anterza.gestion.ventas.inventario.modelo.AlertaInventario;
import pe.com.anterza.gestion.ventas.inventario.modelo.Lote;
import pe.com.anterza.gestion.ventas.inventario.modelo.Producto;
import pe.com.anterza.gestion.ventas.inventario.servicio.inventario.AlertaInventarioServicio.AlertasNuevas;

/** Pruebas unitarias de las alertas: cada alerta se genera una sola vez. */
@ExtendWith(MockitoExtension.class)
class AlertaInventarioServicioTest {

    @Mock AlertaInventarioDAO alertaDAO;
    @Mock InventarioServicio inventario;

    AlertaInventarioServicio servicio;

    @BeforeEach
    void preparar() {
        servicio = new AlertaInventarioServicio(alertaDAO, inventario);
        lenient().when(alertaDAO.saveAll(any())).thenAnswer(i -> i.getArgument(0));
    }

    private Producto producto(int id, Long stock) {
        Producto p = new Producto();
        p.setIdProducto(id);
        p.setStockActual(stock);
        return p;
    }

    private Lote lote(int id, Producto p, int dias) {
        Lote l = new Lote();
        l.setIdLote(id);
        l.setProducto(p);
        l.setFechaVencimiento(LocalDate.now().plusDays(dias));
        return l;
    }

    @Test
    @DisplayName("Un lote que vence en 30 días con stock genera alerta de vencimiento")
    void alertaDeLoteProximoAVencer() {
        Producto p = producto(1, 50L);
        Lote l = lote(1, p, 30);
        when(inventario.lotesProximosAVencer(AlertaInventarioServicio.DIAS_VENCIMIENTO)).thenReturn(List.of(l));
        when(inventario.stockDelLote(1)).thenReturn(8L);

        AlertasNuevas r = servicio.generarNuevas();

        assertThat(r.vencimiento()).hasSize(1);
        assertThat(r.vencimiento().get(0).getValor()).isEqualTo(30);
        assertThat(r.vencimiento().get(0).getTipo()).isEqualTo(AlertaInventario.VENCIMIENTO);
    }

    @Test
    @DisplayName("Un lote ya alertado no vuelve a generar alerta")
    void noRepiteAlertaDeVencimiento() {
        Producto p = producto(1, 50L);
        Lote l = lote(1, p, 30);
        AlertaInventario previa = new AlertaInventario();
        previa.setLote(l);
        lenient().when(alertaDAO.findByTipo(AlertaInventario.VENCIMIENTO)).thenReturn(List.of(previa));
        when(inventario.lotesProximosAVencer(AlertaInventarioServicio.DIAS_VENCIMIENTO)).thenReturn(List.of(l));

        assertThat(servicio.generarNuevas().vencimiento()).isEmpty();
    }

    @Test
    @DisplayName("Un lote sin unidades por vender no genera alerta")
    void loteAgotadoNoAlerta() {
        Lote l = lote(1, producto(1, 0L), 10);
        when(inventario.lotesProximosAVencer(AlertaInventarioServicio.DIAS_VENCIMIENTO)).thenReturn(List.of(l));
        when(inventario.stockDelLote(1)).thenReturn(0L);

        assertThat(servicio.generarNuevas().vencimiento()).isEmpty();
    }

    @Test
    @DisplayName("Solo los productos con stock menor a 5 generan alerta de stock")
    void alertaDeStockBajo() {
        when(inventario.listarProductosActivos()).thenReturn(List.of(producto(1, 3L), producto(2, 10L), producto(3, 5L)));

        AlertasNuevas r = servicio.generarNuevas();

        assertThat(r.stock()).hasSize(1);
        assertThat(r.stock().get(0).getProducto().getIdProducto()).isEqualTo(1);
        assertThat(r.stock().get(0).getValor()).isEqualTo(3);
    }

    @Test
    @DisplayName("Un producto con stock bajo ya alertado no vuelve a alertar")
    void noRepiteAlertaDeStock() {
        Producto p = producto(1, 2L);
        AlertaInventario previa = new AlertaInventario();
        previa.setProducto(p);
        lenient().when(alertaDAO.findByTipo(AlertaInventario.STOCK)).thenReturn(List.of(previa));
        when(inventario.listarProductosActivos()).thenReturn(List.of(p));

        assertThat(servicio.generarNuevas().stock()).isEmpty();
    }
}
