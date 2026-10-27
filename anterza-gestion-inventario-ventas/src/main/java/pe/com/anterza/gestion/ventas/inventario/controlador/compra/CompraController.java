package pe.com.anterza.gestion.ventas.inventario.controlador.compra;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import pe.com.anterza.gestion.ventas.inventario.dao.CategoriaDAO;
import pe.com.anterza.gestion.ventas.inventario.dao.EmpleadoDAO;
import pe.com.anterza.gestion.ventas.inventario.dao.ProductoDAO;
import pe.com.anterza.gestion.ventas.inventario.dao.ProveedorDAO;
import pe.com.anterza.gestion.ventas.inventario.servicio.compra.CompraService;
import pe.com.anterza.gestion.ventas.inventario.servicio.compra.CompraService.NuevoProducto;
import pe.com.anterza.gestion.ventas.inventario.servicio.compra.CompraService.ResultadoCompra;

@Controller
@RequestMapping("/compras")
public class CompraController {

    private final CompraService servicio;
    private final ProveedorDAO proveedores;
    private final EmpleadoDAO empleados;
    private final ProductoDAO productos;
    private final CategoriaDAO categorias;

    public CompraController(CompraService servicio, ProveedorDAO proveedores,
                            EmpleadoDAO empleados, ProductoDAO productos,
                            CategoriaDAO categorias) {
        this.servicio = servicio;
        this.proveedores = proveedores;
        this.empleados = empleados;
        this.productos = productos;
        this.categorias = categorias;
    }

    @GetMapping
    public String formulario(Model model) {
        model.addAttribute("proveedores", proveedores.findAll());
        model.addAttribute("empleados", empleados.findAll());
        model.addAttribute("productos", productos.findByActivoTrue());
        model.addAttribute("categorias", categorias.findAll());
        model.addAttribute("siguienteCodigo", servicio.siguienteCodigoProducto());
        model.addAttribute("costosPorProducto", servicio.ultimoCostoPorProducto());
        model.addAttribute("hoy", LocalDate.now());
        return "vista/compra/registrar";
    }

    @PostMapping("/registrar")
    public String registrar(@RequestParam Integer proveedorId,
                            @RequestParam Integer empleadoId,
                            @RequestParam String documento,
                            @RequestParam LocalDate fecha,
                            @RequestParam(defaultValue = "EXISTENTE") String tipoProducto,
                            @RequestParam(required = false) Integer productoId,
                            @RequestParam(required = false) BigDecimal precioVenta,
                            @RequestParam(required = false) String nuevoNombre,
                            @RequestParam(required = false) Integer nuevaCategoriaId,
                            @RequestParam(required = false) BigDecimal nuevoPrecioVenta,
                            @RequestParam(required = false) Integer nuevoStockMinimo,
                            @RequestParam String codigoLote,
                            @RequestParam LocalDate vencimiento,
                            @RequestParam Integer cantidad,
                            @RequestParam BigDecimal costo,
                            RedirectAttributes redirect) {
        boolean esNuevo = "NUEVO".equals(tipoProducto);
        NuevoProducto nuevo = esNuevo
            ? new NuevoProducto(nuevoNombre, nuevaCategoriaId, nuevoPrecioVenta, nuevoStockMinimo)
            : null;
        try {
            ResultadoCompra r = servicio.registrar(proveedorId, empleadoId, documento, fecha,
                productoId, precioVenta, nuevo, codigoLote, vencimiento, cantidad, costo);
            redirect.addFlashAttribute("exito", "Compra #" + r.compra().getIdCompra() + " registrada correctamente"
                + (esNuevo ? " y producto nuevo creado con el código " + r.producto().getCodigo() + "." : "."));
        } catch (IllegalArgumentException ex) {
            devolverConError(redirect, ex.getMessage(), esNuevo, proveedorId, empleadoId, documento, fecha,
                tipoProducto, productoId, precioVenta, nuevoNombre, nuevaCategoriaId, nuevoPrecioVenta,
                nuevoStockMinimo, codigoLote, vencimiento, cantidad, costo);
        } catch (Exception ex) {
            devolverConError(redirect,
                "No se pudo registrar la compra. Verifique que el comprobante no esté repetido para ese proveedor.",
                esNuevo, proveedorId, empleadoId, documento, fecha, tipoProducto, productoId, precioVenta,
                nuevoNombre, nuevaCategoriaId, nuevoPrecioVenta, nuevoStockMinimo, codigoLote, vencimiento,
                cantidad, costo);
        }
        return "redirect:/compras";
    }

    /**
     * Si hay un error, la pantalla vuelve con lo que el usuario ya escribió (para no llenar todo de nuevo)
     * y con los campos que fallaron marcados en rojo.
     */
    private void devolverConError(RedirectAttributes redirect, String mensaje, boolean esNuevo,
                                  Integer proveedorId, Integer empleadoId, String documento, LocalDate fecha,
                                  String tipoProducto, Integer productoId, BigDecimal precioVenta,
                                  String nuevoNombre, Integer nuevaCategoriaId, BigDecimal nuevoPrecioVenta,
                                  Integer nuevoStockMinimo, String codigoLote, LocalDate vencimiento,
                                  Integer cantidad, BigDecimal costo) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("proveedorId", texto(proveedorId));
        form.put("empleadoId", texto(empleadoId));
        form.put("documento", texto(documento));
        form.put("fecha", texto(fecha));
        form.put("tipoProducto", texto(tipoProducto));
        form.put("productoId", texto(productoId));
        form.put("precioVenta", texto(precioVenta));
        form.put("nuevoNombre", texto(nuevoNombre));
        form.put("nuevaCategoriaId", texto(nuevaCategoriaId));
        form.put("nuevoPrecioVenta", texto(nuevoPrecioVenta));
        form.put("nuevoStockMinimo", texto(nuevoStockMinimo));
        form.put("codigoLote", texto(codigoLote));
        form.put("vencimiento", texto(vencimiento));
        form.put("cantidad", texto(cantidad));
        form.put("costo", texto(costo));
        redirect.addFlashAttribute("error", mensaje);
        redirect.addFlashAttribute("form", form);
        redirect.addFlashAttribute("camposError", camposConError(mensaje, esNuevo));
    }

    private String texto(Object valor) {
        return valor == null ? "" : valor.toString();
    }

    /** Ids de los campos que se marcan en rojo, según el mensaje de error. */
    private List<String> camposConError(String mensaje, boolean esNuevo) {
        String m = mensaje == null ? "" : mensaje.toLowerCase(Locale.ROOT);
        List<String> campos = new ArrayList<>();
        if (m.contains("proveedor")) campos.add("proveedorId");
        if (m.contains("empleado")) campos.add("empleadoId");
        if (m.contains("documento") || m.contains("comprobante")) campos.add("documento");
        if (m.contains("fecha de compra")) campos.add("fecha");
        if (m.contains("vencimiento")) campos.add("vencimiento");
        if (m.contains("lote")) campos.add("codigoLote");
        if (m.contains("cantidad")) campos.add("cantidad");
        if (m.contains("costo")) campos.add("costo");
        if (m.contains("precio de venta")) campos.add(esNuevo ? "nuevoPrecioVenta" : "precioVentaActual");
        if (m.contains("seleccione un producto") || m.contains("producto no encontrado")) campos.add("productoId");
        if (m.contains("nombre de producto")) campos.add("nuevoNombre");
        if (m.contains("categor")) campos.add("nuevaCategoriaId");
        if (m.contains("stock mínimo")) campos.add("nuevoStockMinimo");
        return campos;
    }
}
