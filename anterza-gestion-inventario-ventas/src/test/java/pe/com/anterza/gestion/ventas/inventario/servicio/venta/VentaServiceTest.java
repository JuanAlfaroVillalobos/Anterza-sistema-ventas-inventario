package pe.com.anterza.gestion.ventas.inventario.servicio.venta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pe.com.anterza.gestion.ventas.inventario.dao.ClienteDAO;
import pe.com.anterza.gestion.ventas.inventario.dao.DetalleVentaStockDAO;
import pe.com.anterza.gestion.ventas.inventario.dao.EmpleadoDAO;
import pe.com.anterza.gestion.ventas.inventario.dao.LoteVentaDAO;
import pe.com.anterza.gestion.ventas.inventario.dao.ProductoVentaDAO;
import pe.com.anterza.gestion.ventas.inventario.dao.VentaDAO;
import pe.com.anterza.gestion.ventas.inventario.modelo.Categoria;
import pe.com.anterza.gestion.ventas.inventario.modelo.Cliente;
import pe.com.anterza.gestion.ventas.inventario.modelo.DetalleVenta;
import pe.com.anterza.gestion.ventas.inventario.modelo.Empleado;
import pe.com.anterza.gestion.ventas.inventario.modelo.Lote;
import pe.com.anterza.gestion.ventas.inventario.modelo.Producto;
import pe.com.anterza.gestion.ventas.inventario.modelo.dto.ItemVentaRequest;
import pe.com.anterza.gestion.ventas.inventario.modelo.dto.ProductoVentaDTO;
import pe.com.anterza.gestion.ventas.inventario.modelo.dto.VentaRequest;
import pe.com.anterza.gestion.ventas.inventario.modelo.dto.VentaResponse;

/** Pruebas unitarias de VentaService: cálculo de IGV, descuento FEFO, stock y búsqueda. */
@ExtendWith(MockitoExtension.class)
class VentaServiceTest {

    static final String DNI = "00000000";
    static final String CORREO = "angelica@anterza.com";

    @Mock ProductoVentaDAO productoVentaDAO;
    @Mock LoteVentaDAO loteVentaDAO;
    @Mock DetalleVentaStockDAO detalleVentaStockDAO;
    @Mock VentaDAO ventaDAO;
    @Mock ClienteDAO clienteDAO;
    @Mock EmpleadoDAO empleadoDAO;

    VentaService servicio;

    @BeforeEach
    void preparar() {
        servicio = new VentaService(productoVentaDAO, loteVentaDAO, detalleVentaStockDAO,
                ventaDAO, clienteDAO, empleadoDAO, DNI, CORREO);
    }

    private Producto producto(int id, String nombre, String precio, boolean activo) {
        Categoria c = new Categoria();
        c.setIdCategoria(1);
        c.setNombre("Analgésicos");
        Producto p = new Producto();
        p.setIdProducto(id);
        p.setCodigo("PR-00" + id);
        p.setNombre(nombre);
        p.setPrecioVenta(new BigDecimal(precio));
        p.setStockMinimo(5);
        p.setActivo(activo);
        p.setCategoria(c);
        return p;
    }

    private Lote lote(int id, Producto p, int cantidad, int diasParaVencer) {
        Lote l = new Lote();
        l.setIdLote(id);
        l.setProducto(p);
        l.setCantidad(cantidad);
        l.setFechaVencimiento(LocalDate.now().plusDays(diasParaVencer));
        return l;
    }

    /** Prepara cliente, vendedor y guardado de la venta para los casos que sí llegan a registrar. */
    private void clienteVendedorYGuardado() {
        Empleado vendedor = mock(Empleado.class);
        when(vendedor.getActivo()).thenReturn(true);
        when(clienteDAO.findByDni(DNI)).thenReturn(Optional.of(mock(Cliente.class)));
        when(empleadoDAO.findByCorreo(CORREO)).thenReturn(Optional.of(vendedor));
        when(ventaDAO.save(any())).thenAnswer(i -> {
            var v = (pe.com.anterza.gestion.ventas.inventario.modelo.Venta) i.getArgument(0);
            v.setIdVenta(1);
            return v;
        });
    }

    private VentaRequest pedido(int idProducto, int cantidad) {
        return new VentaRequest("EFECTIVO", List.of(new ItemVentaRequest(idProducto, cantidad)));
    }

    @Test
    @DisplayName("La venta calcula subtotal e IGV a partir de precios que ya incluyen IGV")
    void calculaIgv() {
        Producto p = producto(1, "Paracetamol 500 mg", "11.80", true);
        Lote l = lote(5, p, 10, 200);
        when(productoVentaDAO.findByIdProductoInOrderByIdProducto(any())).thenReturn(List.of(p));
        clienteVendedorYGuardado();
        when(loteVentaDAO.findByFechaVencimientoGreaterThanEqualOrderByFechaVencimientoAscIdLoteAsc(any()))
                .thenReturn(List.of(l));
        when(loteVentaDAO.getReferenceById(5)).thenReturn(l);

        VentaResponse r = servicio.procesarVenta(pedido(1, 2));

        assertThat(r.total()).isEqualByComparingTo("23.60");
        assertThat(r.subtotal()).isEqualByComparingTo("20.00");
        assertThat(r.igv()).isEqualByComparingTo("3.60");
    }

    @Test
    @DisplayName("El descuento de stock toma primero el lote que vence antes (FEFO)")
    void descuentaPrimeroElLoteQueVenceAntes() {
        Producto p = producto(1, "Paracetamol 500 mg", "10.00", true);
        Lote proximo = lote(5, p, 3, 30);
        Lote lejano = lote(6, p, 10, 300);
        when(productoVentaDAO.findByIdProductoInOrderByIdProducto(any())).thenReturn(List.of(p));
        clienteVendedorYGuardado();
        when(loteVentaDAO.findByFechaVencimientoGreaterThanEqualOrderByFechaVencimientoAscIdLoteAsc(any()))
                .thenReturn(List.of(proximo, lejano));
        when(loteVentaDAO.getReferenceById(5)).thenReturn(proximo);
        when(loteVentaDAO.getReferenceById(6)).thenReturn(lejano);

        servicio.procesarVenta(pedido(1, 5));

        ArgumentCaptor<DetalleVenta> captor = ArgumentCaptor.forClass(DetalleVenta.class);
        verify(detalleVentaStockDAO, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(DetalleVenta::getCantidad).containsExactly(3, 2);
    }

    @Test
    @DisplayName("Lanza StockInsuficienteException si se pide más de lo disponible")
    void rechazaPorStockInsuficiente() {
        Producto p = producto(1, "Paracetamol 500 mg", "10.00", true);
        Lote l = lote(5, p, 10, 200);
        when(productoVentaDAO.findByIdProductoInOrderByIdProducto(any())).thenReturn(List.of(p));
        clienteVendedorYGuardado();
        when(loteVentaDAO.findByFechaVencimientoGreaterThanEqualOrderByFechaVencimientoAscIdLoteAsc(any()))
                .thenReturn(List.of(l));
        when(loteVentaDAO.getReferenceById(5)).thenReturn(l);

        assertThatThrownBy(() -> servicio.procesarVenta(pedido(1, 50)))
                .isInstanceOf(StockInsuficienteException.class);
    }

    @Test
    @DisplayName("No vende un producto inactivo")
    void rechazaProductoInactivo() {
        Producto p = producto(1, "Paracetamol 500 mg", "10.00", false);
        when(productoVentaDAO.findByIdProductoInOrderByIdProducto(any())).thenReturn(List.of(p));

        assertThatThrownBy(() -> servicio.procesarVenta(pedido(1, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya no está disponible");
    }

    @Test
    @DisplayName("Falla con un mensaje claro si falta el cliente genérico")
    void fallaSinClienteGenerico() {
        Producto p = producto(1, "Paracetamol 500 mg", "10.00", true);
        when(productoVentaDAO.findByIdProductoInOrderByIdProducto(any())).thenReturn(List.of(p));
        when(clienteDAO.findByDni(DNI)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.procesarVenta(pedido(1, 1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cliente genérico");
    }

    @Test
    @DisplayName("La búsqueda filtra por nombre y calcula el stock sumando lotes no vencidos")
    void buscaProductosPorTexto() {
        Producto para = producto(1, "Paracetamol 500 mg", "10.00", true);
        Producto ibu = producto(2, "Ibuprofeno 400 mg", "12.00", true);
        when(productoVentaDAO.findByActivoTrueOrderByNombreAsc()).thenReturn(List.of(ibu, para));
        when(loteVentaDAO.findByFechaVencimientoGreaterThanEqualOrderByFechaVencimientoAscIdLoteAsc(any()))
                .thenReturn(List.of(lote(5, para, 20, 100), lote(6, para, 5, 200)));
        when(detalleVentaStockDAO.findByLote_FechaVencimientoGreaterThanEqualAndVenta_Estado(any(), eq("EMITIDA")))
                .thenReturn(List.of());

        List<ProductoVentaDTO> r = servicio.buscarProductos("PARA", null);

        assertThat(r).hasSize(1);
        assertThat(r.get(0).nombre()).isEqualTo("Paracetamol 500 mg");
        assertThat(r.get(0).stock()).isEqualTo(25L);
    }
}
