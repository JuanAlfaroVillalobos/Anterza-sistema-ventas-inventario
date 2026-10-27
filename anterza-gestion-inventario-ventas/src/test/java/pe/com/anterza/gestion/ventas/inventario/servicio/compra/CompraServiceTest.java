package pe.com.anterza.gestion.ventas.inventario.servicio.compra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pe.com.anterza.gestion.ventas.inventario.dao.CategoriaDAO;
import pe.com.anterza.gestion.ventas.inventario.dao.CompraDAO;
import pe.com.anterza.gestion.ventas.inventario.dao.EmpleadoDAO;
import pe.com.anterza.gestion.ventas.inventario.dao.LoteDAO;
import pe.com.anterza.gestion.ventas.inventario.dao.ProductoDAO;
import pe.com.anterza.gestion.ventas.inventario.dao.ProveedorDAO;
import pe.com.anterza.gestion.ventas.inventario.modelo.Categoria;
import pe.com.anterza.gestion.ventas.inventario.modelo.Empleado;
import pe.com.anterza.gestion.ventas.inventario.modelo.Lote;
import pe.com.anterza.gestion.ventas.inventario.modelo.Producto;
import pe.com.anterza.gestion.ventas.inventario.modelo.Proveedor;
import pe.com.anterza.gestion.ventas.inventario.servicio.compra.CompraService.NuevoProducto;
import pe.com.anterza.gestion.ventas.inventario.servicio.compra.CompraService.ResultadoCompra;

/** Pruebas unitarias de CompraService: los DAO se simulan con Mockito (sin base de datos). */
@ExtendWith(MockitoExtension.class)
class CompraServiceTest {

    @Mock CompraDAO compras;
    @Mock ProveedorDAO proveedores;
    @Mock EmpleadoDAO empleados;
    @Mock ProductoDAO productos;
    @Mock LoteDAO lotes;
    @Mock CategoriaDAO categorias;

    CompraService servicio;
    final LocalDate hoy = LocalDate.now();

    @BeforeEach
    void preparar() {
        servicio = new CompraService(compras, proveedores, empleados, productos, lotes, categorias);
    }

    private Producto producto(int id, String codigo, String precio) {
        Producto p = new Producto();
        p.setIdProducto(id);
        p.setCodigo(codigo);
        p.setNombre("Producto " + id);
        p.setPrecioVenta(new BigDecimal(precio));
        return p;
    }

    private void proveedorYEmpleadoExisten() {
        when(proveedores.findById(1)).thenReturn(Optional.of(mock(Proveedor.class)));
        when(empleados.findById(1)).thenReturn(Optional.of(mock(Empleado.class)));
        lenient().when(compras.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("Rechaza una compra sin número de documento")
    void rechazaDocumentoVacio() {
        assertThatThrownBy(() -> servicio.registrar(1, 1, " ", hoy, 1, new BigDecimal("10.00"), null,
                "L1", hoy.plusDays(100), 5, new BigDecimal("5.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("documento");
    }

    @Test
    @DisplayName("Rechaza una fecha de compra futura")
    void rechazaFechaFutura() {
        assertThatThrownBy(() -> servicio.registrar(1, 1, "F001-1", hoy.plusDays(1), 1, new BigDecimal("10.00"), null,
                "L1", hoy.plusDays(100), 5, new BigDecimal("5.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("futura");
    }

    @Test
    @DisplayName("Rechaza un vencimiento que no es posterior a la compra")
    void rechazaVencimientoAnterior() {
        assertThatThrownBy(() -> servicio.registrar(1, 1, "F001-1", hoy, 1, new BigDecimal("10.00"), null,
                "L1", hoy, 5, new BigDecimal("5.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vencimiento");
    }

    @Test
    @DisplayName("Rechaza cantidad cero o costo negativo")
    void rechazaCantidadYCostoInvalidos() {
        assertThatThrownBy(() -> servicio.registrar(1, 1, "F001-1", hoy, 1, new BigDecimal("10.00"), null,
                "L1", hoy.plusDays(100), 0, new BigDecimal("5.00")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> servicio.registrar(1, 1, "F001-1", hoy, 1, new BigDecimal("10.00"), null,
                "L1", hoy.plusDays(100), 5, new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Rechaza un costo con más de dos decimales")
    void rechazaCostoConTresDecimales() {
        assertThatThrownBy(() -> servicio.registrar(1, 1, "F001-1", hoy, 1, new BigDecimal("10.00"), null,
                "L1", hoy.plusDays(100), 5, new BigDecimal("1.234")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dos decimales");
    }

    @Test
    @DisplayName("El código de producto empieza en PR-001 si no hay productos")
    void siguienteCodigoSinProductos() {
        when(productos.findAll()).thenReturn(List.of());
        assertThat(servicio.siguienteCodigoProducto()).isEqualTo("PR-001");
    }

    @Test
    @DisplayName("El código de producto es el mayor existente + 1 e ignora otros formatos")
    void siguienteCodigoConExistentes() {
        when(productos.findAll()).thenReturn(List.of(
                producto(1, "PR-003", "1.00"), producto(2, "PR-010", "1.00"), producto(3, "XYZ", "1.00")));
        assertThat(servicio.siguienteCodigoProducto()).isEqualTo("PR-011");
    }

    @Test
    @DisplayName("Producto existente: si cambia el precio de venta, se actualiza")
    void actualizaPrecioDeProductoExistente() {
        proveedorYEmpleadoExisten();
        Producto p = producto(1, "PR-001", "10.00");
        when(productos.findById(1)).thenReturn(Optional.of(p));

        ResultadoCompra r = servicio.registrar(1, 1, "F001-1", hoy, 1, new BigDecimal("12.50"), null,
                "L1", hoy.plusDays(200), 20, new BigDecimal("6.00"));

        assertThat(r.producto().getPrecioVenta()).isEqualByComparingTo("12.50");
        verify(productos).save(p);
        verify(lotes).save(any(Lote.class));
    }

    @Test
    @DisplayName("Rechaza un código de lote repetido para el mismo producto")
    void rechazaLoteRepetido() {
        proveedorYEmpleadoExisten();
        Producto p = producto(1, "PR-001", "10.00");
        Lote existente = new Lote();
        existente.setCodigoLote("L1");
        when(productos.findById(1)).thenReturn(Optional.of(p));
        when(lotes.findByProducto_IdProducto(1)).thenReturn(List.of(existente));

        assertThatThrownBy(() -> servicio.registrar(1, 1, "F001-1", hoy, 1, new BigDecimal("10.00"), null,
                "l1", hoy.plusDays(200), 20, new BigDecimal("6.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya tiene un lote");
    }

    @Test
    @DisplayName("Producto nuevo: se crea activo y con código automático")
    void creaProductoNuevoConCodigoAutomatico() {
        proveedorYEmpleadoExisten();
        when(categorias.findById(2)).thenReturn(Optional.of(new Categoria()));
        when(productos.findAll()).thenReturn(List.of());
        when(productos.save(any(Producto.class))).thenAnswer(i -> i.getArgument(0));

        ResultadoCompra r = servicio.registrar(1, 1, "F001-2", hoy, null, null,
                new NuevoProducto("  Amoxicilina 500 mg ", 2, new BigDecimal("8.50"), 5),
                "L9", hoy.plusDays(300), 40, new BigDecimal("4.00"));

        assertThat(r.producto().getCodigo()).isEqualTo("PR-001");
        assertThat(r.producto().getNombre()).isEqualTo("Amoxicilina 500 mg");
        assertThat(r.producto().getActivo()).isTrue();
    }
}
