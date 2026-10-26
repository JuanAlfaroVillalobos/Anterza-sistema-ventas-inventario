package pe.com.anterza.gestion.ventas.inventario.servicio.venta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.com.anterza.gestion.ventas.inventario.dao.ClienteDAO;
import pe.com.anterza.gestion.ventas.inventario.dao.DetalleVentaStockDAO;
import pe.com.anterza.gestion.ventas.inventario.dao.EmpleadoDAO;
import pe.com.anterza.gestion.ventas.inventario.dao.LoteVentaDAO;
import pe.com.anterza.gestion.ventas.inventario.dao.ProductoVentaDAO;
import pe.com.anterza.gestion.ventas.inventario.dao.VentaDAO;
import pe.com.anterza.gestion.ventas.inventario.modelo.Cliente;
import pe.com.anterza.gestion.ventas.inventario.modelo.DetalleVenta;
import pe.com.anterza.gestion.ventas.inventario.modelo.Empleado;
import pe.com.anterza.gestion.ventas.inventario.modelo.Lote;
import pe.com.anterza.gestion.ventas.inventario.modelo.Producto;
import pe.com.anterza.gestion.ventas.inventario.modelo.Venta;
import pe.com.anterza.gestion.ventas.inventario.modelo.dto.ItemVentaRequest;
import pe.com.anterza.gestion.ventas.inventario.modelo.dto.LoteDisponibleDTO;
import pe.com.anterza.gestion.ventas.inventario.modelo.dto.ProductoVentaDTO;
import pe.com.anterza.gestion.ventas.inventario.modelo.dto.VentaRequest;
import pe.com.anterza.gestion.ventas.inventario.modelo.dto.VentaResponse;

/**
 * Lógica de la pantalla de venta. Los DAO solo traen entidades; los cálculos se hacen aquí.
 *
 * El stock no se guarda en ninguna tabla: el stock de un lote es
 * (cantidad comprada) - (cantidad vendida en ventas EMITIDAS), y el de un producto es la suma de sus lotes no vencidos.
 */
@Service
public class VentaService {

    private static final BigDecimal FACTOR_IGV = new BigDecimal("1.18");
    private static final String ESTADO_EMITIDA = "EMITIDA";
    private static final int MAX_RESULTADOS = 60;

    private final ProductoVentaDAO productoVentaDAO;
    private final LoteVentaDAO loteVentaDAO;
    private final DetalleVentaStockDAO detalleVentaStockDAO;
    private final VentaDAO ventaDAO;
    private final ClienteDAO clienteDAO;
    private final EmpleadoDAO empleadoDAO;
    private final String dniClienteGenerico;
    private final String correoVendedor;

    public VentaService(ProductoVentaDAO productoVentaDAO,
                        LoteVentaDAO loteVentaDAO,
                        DetalleVentaStockDAO detalleVentaStockDAO,
                        VentaDAO ventaDAO,
                        ClienteDAO clienteDAO,
                        EmpleadoDAO empleadoDAO,
                        @Value("${anterza.ventas.dni-cliente-generico:00000000}") String dniClienteGenerico,
                        @Value("${anterza.ventas.correo-vendedor:angelica@anterza.com}") String correoVendedor) {
        this.productoVentaDAO = productoVentaDAO;
        this.loteVentaDAO = loteVentaDAO;
        this.detalleVentaStockDAO = detalleVentaStockDAO;
        this.ventaDAO = ventaDAO;
        this.clienteDAO = clienteDAO;
        this.empleadoDAO = empleadoDAO;
        this.dniClienteGenerico = dniClienteGenerico;
        this.correoVendedor = correoVendedor;
    }

    /** Búsqueda en vivo del catálogo de venta (nombre o código, y categoría opcional). */
    @Transactional(readOnly = true)
    public List<ProductoVentaDTO> buscarProductos(String texto, Integer categoriaId) {
        String filtro = texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
        int categoria = categoriaId == null ? 0 : categoriaId;
        Map<Integer, List<LoteDisponibleDTO>> lotes = lotesDisponiblesPorProducto();

        return productoVentaDAO.findByActivoTrueOrderByNombreAsc().stream()
                .filter(p -> categoria == 0 || p.getCategoria().getIdCategoria() == categoria)
                .filter(p -> filtro.isEmpty()
                        || p.getNombre().toLowerCase(Locale.ROOT).contains(filtro)
                        || p.getCodigo().toLowerCase(Locale.ROOT).contains(filtro))
                .limit(MAX_RESULTADOS)
                .map(p -> new ProductoVentaDTO(
                        p.getIdProducto(), p.getCodigo(), p.getNombre(), p.getPrecioVenta(), p.getStockMinimo(),
                        p.getCategoria().getIdCategoria(), p.getCategoria().getNombre(),
                        stockTotal(lotes.get(p.getIdProducto()))))
                .toList();
    }

    /**
     * Registra la venta y descuenta el stock. Todo ocurre en una sola transacción:
     * si algo falla (por ejemplo falta stock), no se guarda nada.
     *
     * El descuento consiste en crear filas de detalle_venta sobre los lotes que vencen
     * primero, porque el stock de un lote es "cantidad comprada - cantidad vendida".
     */
    @Transactional
    public VentaResponse procesarVenta(VentaRequest request) {
        // Juntar las cantidades por producto, por si el mismo llega dos veces.
        Map<Integer, Integer> cantidades = new TreeMap<>();
        for (ItemVentaRequest item : request.items()) {
            cantidades.merge(item.idProducto(), item.cantidad(), Integer::sum);
        }

        // Bloquear los productos ANTES de leer el stock (ver ProductoVentaDAO).
        Map<Integer, Producto> productos = new HashMap<>();
        for (Producto p : productoVentaDAO.findByIdProductoInOrderByIdProducto(cantidades.keySet())) {
            productos.put(p.getIdProducto(), p);
        }
        for (Integer idProducto : cantidades.keySet()) {
            Producto p = productos.get(idProducto);
            if (p == null || !Boolean.TRUE.equals(p.getActivo())) {
                throw new IllegalArgumentException("Un producto de la canasta ya no está disponible. Actualice la pantalla.");
            }
        }

        Cliente cliente = clienteDAO.findByDni(dniClienteGenerico)
                .orElseThrow(() -> new IllegalStateException(
                        "Falta registrar el cliente genérico (DNI " + dniClienteGenerico + "). Ejecute datos_base_venta.sql."));
        Empleado empleado = empleadoDAO.findByCorreo(correoVendedor)
                .filter(e -> Boolean.TRUE.equals(e.getActivo()))
                .orElseThrow(() -> new IllegalStateException(
                        "Falta registrar al vendedor (" + correoVendedor + "). Ejecute datos_base_venta.sql."));

        // Cabecera de la venta.
        Venta venta = new Venta();
        venta.setCliente(cliente);
        venta.setEmpleado(empleado);
        venta.setMetodoPago(request.metodoPago());
        venta = ventaDAO.save(venta);

        // Detalle: tomar de los lotes que vencen primero hasta completar lo pedido.
        Map<Integer, List<LoteDisponibleDTO>> lotes = lotesDisponiblesPorProducto();
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Integer, Integer> linea : cantidades.entrySet()) {
            Producto producto = productos.get(linea.getKey());
            int pendiente = linea.getValue();

            for (LoteDisponibleDTO lote : lotes.getOrDefault(producto.getIdProducto(), List.of())) {
                if (pendiente == 0) {
                    break;
                }
                int tomar = (int) Math.min(pendiente, lote.disponible());

                DetalleVenta detalle = new DetalleVenta();
                detalle.setVenta(venta);
                detalle.setLote(loteVentaDAO.getReferenceById(lote.idLote()));
                detalle.setCantidad(tomar);
                detalle.setPrecioUnitario(producto.getPrecioVenta());
                detalleVentaStockDAO.save(detalle);

                total = total.add(producto.getPrecioVenta().multiply(BigDecimal.valueOf(tomar)));
                pendiente -= tomar;
            }

            if (pendiente > 0) {
                throw new StockInsuficienteException(producto.getNombre(), linea.getValue() - pendiente);
            }
        }

        // Los precios ya incluyen IGV: subtotal = total / 1.18.
        BigDecimal subtotal = total.divide(FACTOR_IGV, 2, RoundingMode.HALF_UP);
        BigDecimal igv = total.subtract(subtotal);
        return new VentaResponse(venta.getIdVenta(), request.metodoPago(), subtotal, igv, total);
    }

    /**
     * Lotes no vencidos con unidades disponibles, agrupados por producto y en orden FEFO
     * (el que vence primero va primero). Disponible = cantidad comprada - cantidad vendida (ventas EMITIDAS).
     */
    private Map<Integer, List<LoteDisponibleDTO>> lotesDisponiblesPorProducto() {
        LocalDate hoy = LocalDate.now();

        Map<Integer, Long> vendidoPorLote = new HashMap<>();
        for (DetalleVenta d : detalleVentaStockDAO.findByLote_FechaVencimientoGreaterThanEqualAndVenta_Estado(hoy, ESTADO_EMITIDA)) {
            vendidoPorLote.merge(d.getLote().getIdLote(), d.getCantidad().longValue(), Long::sum);
        }

        Map<Integer, List<LoteDisponibleDTO>> porProducto = new LinkedHashMap<>();
        for (Lote lote : loteVentaDAO.findByFechaVencimientoGreaterThanEqualOrderByFechaVencimientoAscIdLoteAsc(hoy)) {
            long disponible = lote.getCantidad() - vendidoPorLote.getOrDefault(lote.getIdLote(), 0L);
            if (disponible > 0) {
                porProducto.computeIfAbsent(lote.getProducto().getIdProducto(), k -> new ArrayList<>())
                        .add(new LoteDisponibleDTO(lote.getIdLote(), disponible));
            }
        }
        return porProducto;
    }

    private long stockTotal(List<LoteDisponibleDTO> lotes) {
        return lotes == null ? 0L : lotes.stream().mapToLong(LoteDisponibleDTO::disponible).sum();
    }
}