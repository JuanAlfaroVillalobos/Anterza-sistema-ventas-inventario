package pe.com.anterza.gestion.ventas.inventario.servicio.compra;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.com.anterza.gestion.ventas.inventario.dao.CategoriaDAO;
import pe.com.anterza.gestion.ventas.inventario.dao.CompraDAO;
import pe.com.anterza.gestion.ventas.inventario.dao.EmpleadoDAO;
import pe.com.anterza.gestion.ventas.inventario.dao.LoteDAO;
import pe.com.anterza.gestion.ventas.inventario.dao.ProductoDAO;
import pe.com.anterza.gestion.ventas.inventario.dao.ProveedorDAO;
import pe.com.anterza.gestion.ventas.inventario.modelo.Categoria;
import pe.com.anterza.gestion.ventas.inventario.modelo.Compra;
import pe.com.anterza.gestion.ventas.inventario.modelo.Empleado;
import pe.com.anterza.gestion.ventas.inventario.modelo.Lote;
import pe.com.anterza.gestion.ventas.inventario.modelo.Producto;
import pe.com.anterza.gestion.ventas.inventario.modelo.Proveedor;

@Service
public class CompraService {

    /** Datos de un producto que se crea junto con la compra (el codigo se genera solo). */
    public record NuevoProducto(String nombre, Integer categoriaId,
                                BigDecimal precioVenta, Integer stockMinimo) { }

    /** Resultado de registrar una compra: la compra y el producto del lote. */
    public record ResultadoCompra(Compra compra, Producto producto) { }

    private static final String PREFIJO_CODIGO = "PR-";

    private final CompraDAO compras;
    private final ProveedorDAO proveedores;
    private final EmpleadoDAO empleados;
    private final ProductoDAO productos;
    private final LoteDAO lotes;
    private final CategoriaDAO categorias;

    public CompraService(CompraDAO compras, ProveedorDAO proveedores, EmpleadoDAO empleados,
                         ProductoDAO productos, LoteDAO lotes, CategoriaDAO categorias) {
        this.compras = compras;
        this.proveedores = proveedores;
        this.empleados = empleados;
        this.productos = productos;
        this.lotes = lotes;
        this.categorias = categorias;
    }

    /**
     * Registra una compra con un lote. Si {@code nuevo} no es null, primero crea el producto
     * (se ignora productoId); si es null, usa el producto existente productoId.
     * Todo ocurre en una sola transaccion: si algo falla, no se guarda nada.
     */
    @Transactional
    public ResultadoCompra registrar(Integer proveedorId, Integer empleadoId, String documento,
                            LocalDate fecha, Integer productoId, BigDecimal precioVenta, NuevoProducto nuevo,
                            String codigoLote, LocalDate vencimiento, Integer cantidad,
                            BigDecimal costo) {
        if (documento == null || documento.isBlank() || documento.length() > 20)
            throw new IllegalArgumentException("Ingrese un número de documento válido (máximo 20 caracteres).");
        if (fecha == null || fecha.isAfter(LocalDate.now()))
            throw new IllegalArgumentException("La fecha de compra no puede ser futura.");
        if (vencimiento == null || !vencimiento.isAfter(fecha))
            throw new IllegalArgumentException("El vencimiento debe ser posterior a la compra.");
        if (codigoLote == null || codigoLote.isBlank() || codigoLote.length() > 20)
            throw new IllegalArgumentException("Ingrese un código de lote válido (máximo 20 caracteres).");
        if (cantidad == null || cantidad <= 0 || costo == null || costo.signum() <= 0)
            throw new IllegalArgumentException("Cantidad y costo unitario deben ser mayores a cero.");
        if (costo.scale() > 2 || costo.compareTo(new BigDecimal("999999.99")) > 0)
            throw new IllegalArgumentException("Costo unitario inválido (máximo dos decimales).");

        Proveedor proveedor = proveedores.findById(proveedorId)
            .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado."));
        Empleado empleado = empleados.findById(empleadoId)
            .orElseThrow(() -> new IllegalArgumentException("Empleado no encontrado."));

        Producto producto;
        if (nuevo != null) {
            producto = crearProducto(nuevo);
        } else {
            if (productoId == null)
                throw new IllegalArgumentException("Seleccione un producto.");
            producto = productos.findById(productoId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado."));
            if (precioVenta == null || precioVenta.signum() <= 0
                    || precioVenta.scale() > 2 || precioVenta.compareTo(new BigDecimal("999999.99")) > 0)
                throw new IllegalArgumentException("Precio de venta inválido (mayor a cero, máximo dos decimales).");
            String codigo = codigoLote.trim();
            boolean loteRepetido = lotes.findByProducto_IdProducto(producto.getIdProducto()).stream()
                .anyMatch(l -> l.getCodigoLote().equalsIgnoreCase(codigo));
            if (loteRepetido)
                throw new IllegalArgumentException("Ese producto ya tiene un lote con el código " + codigo + ".");
        }

        // Producto existente: si el precio de venta cambio, se actualiza junto con la compra
        if (nuevo == null && producto.getPrecioVenta().compareTo(precioVenta) != 0) {
            producto.setPrecioVenta(precioVenta);
            productos.save(producto);
        }

        Compra compra = new Compra();
        compra.setProveedor(proveedor);
        compra.setEmpleado(empleado);
        compra.setNroDocumento(documento.trim());
        compra.setFechaCompra(fecha);
        compra = compras.save(compra);

        Lote lote = new Lote();
        lote.setCompra(compra);
        lote.setProducto(producto);
        lote.setCodigoLote(codigoLote.trim());
        lote.setFechaVencimiento(vencimiento);
        lote.setCantidad(cantidad);
        lote.setCostoUnitario(costo);
        lotes.save(lote);
        return new ResultadoCompra(compra, producto);
    }

    /** Ultimo costo unitario de compra de cada producto (el del lote mas reciente). */
    @Transactional(readOnly = true)
    public Map<Integer, BigDecimal> ultimoCostoPorProducto() {
        Map<Integer, Lote> ultimo = new HashMap<>();
        for (Lote l : lotes.findAll()) {
            Integer idProducto = l.getProducto().getIdProducto();
            Lote actual = ultimo.get(idProducto);
            if (actual == null || l.getIdLote() > actual.getIdLote()) {
                ultimo.put(idProducto, l);
            }
        }
        Map<Integer, BigDecimal> costos = new HashMap<>();
        ultimo.forEach((id, l) -> costos.put(id, l.getCostoUnitario()));
        return costos;
    }

    /** Siguiente codigo libre de producto: PR-001, PR-002, ... (el mayor existente + 1). */
    public String siguienteCodigoProducto() {
        int max = 0;
        for (Producto p : productos.findAll()) {
            String c = p.getCodigo();
            if (c != null && c.startsWith(PREFIJO_CODIGO)) {
                try {
                    max = Math.max(max, Integer.parseInt(c.substring(PREFIJO_CODIGO.length())));
                } catch (NumberFormatException ignorado) {
                    // codigos con otro formato no cuentan
                }
            }
        }
        int siguiente = max + 1;
        String codigo = String.format("%s%03d", PREFIJO_CODIGO, siguiente);
        while (productos.findByCodigo(codigo).isPresent()) {
            siguiente++;
            codigo = String.format("%s%03d", PREFIJO_CODIGO, siguiente);
        }
        return codigo;
    }

    private Producto crearProducto(NuevoProducto n) {
        String nombre = n.nombre() == null ? "" : n.nombre().trim();
        if (nombre.isEmpty() || nombre.length() > 80)
            throw new IllegalArgumentException("Ingrese un nombre de producto válido (máximo 80 caracteres).");
        if (n.categoriaId() == null)
            throw new IllegalArgumentException("Seleccione la categoría del producto nuevo.");
        if (n.precioVenta() == null || n.precioVenta().signum() <= 0
                || n.precioVenta().scale() > 2 || n.precioVenta().compareTo(new BigDecimal("999999.99")) > 0)
            throw new IllegalArgumentException("Precio de venta inválido (mayor a cero, máximo dos decimales).");
        if (n.stockMinimo() == null || n.stockMinimo() < 0)
            throw new IllegalArgumentException("El stock mínimo no puede ser negativo.");

        Categoria categoria = categorias.findById(n.categoriaId())
            .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada."));

        Producto p = new Producto();
        p.setCategoria(categoria);
        p.setCodigo(siguienteCodigoProducto());
        p.setNombre(nombre);
        p.setPrecioVenta(n.precioVenta());
        p.setStockMinimo(n.stockMinimo());
        p.setActivo(true);
        return productos.save(p);
    }
}
