package pe.com.anterza.gestion.ventas.inventario.controlador.inventario;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import pe.com.anterza.gestion.ventas.inventario.modelo.Lote;
import pe.com.anterza.gestion.ventas.inventario.modelo.Producto;
import pe.com.anterza.gestion.ventas.inventario.servicio.inventario.AlertaInventarioServicio;
import pe.com.anterza.gestion.ventas.inventario.servicio.inventario.AlertaInventarioServicio.AlertasNuevas;
import pe.com.anterza.gestion.ventas.inventario.servicio.inventario.InventarioServicio;

/**
 * Controlador del módulo de inventario.
 * Maneja: listado, formulario (crear/editar), lotes, alertas de vencimiento y stock bajo.
 */
@Controller
@RequestMapping("/inventario")
public class InventarioControlador {

    private final InventarioServicio servicio;
    private final AlertaInventarioServicio alertaServicio;

    public InventarioControlador(InventarioServicio servicio,
                                 AlertaInventarioServicio alertaServicio) {
        this.servicio = servicio;
        this.alertaServicio = alertaServicio;
    }

    /* ====================== LISTADO PRINCIPAL ====================== */

    @GetMapping
    public String listar(@RequestParam(required = false) String buscar,
                         @RequestParam(required = false) Integer categoria,
                         Model model) {

        List<Producto> productos;

        if (buscar != null && !buscar.isBlank()) {
            productos = servicio.buscarPorNombre(buscar.trim());
            model.addAttribute("buscar", buscar.trim());
        } else if (categoria != null) {
            productos = servicio.filtrarPorCategoria(categoria);
            model.addAttribute("categoriaSeleccionada", categoria);
        } else {
            productos = servicio.listarProductosActivos();
        }

        model.addAttribute("productos", productos);
        model.addAttribute("categorias", servicio.listarCategorias());

        // Contadores para tarjetas de resumen
        model.addAttribute("totalProductos", servicio.contarProductosActivos());
        model.addAttribute("lotesProxVencer", servicio.contarLotesProximosAVencer());
        model.addAttribute("productosStockBajo", servicio.contarProductosStockBajo());

        // Alertas nuevas (se muestran en un modal una sola vez y pasan al historial)
        AlertasNuevas alertas = alertaServicio.generarNuevas();
        model.addAttribute("alertasVencimiento", alertas.vencimiento());
        model.addAttribute("alertasStock", alertas.stock());

        return "vista/inventario/listar";
    }

    /* ====================== FORMULARIO (CREAR / EDITAR) ====================== */

    @GetMapping("/nuevo")
    public String nuevoProducto(Model model) {
        model.addAttribute("producto", new Producto());
        model.addAttribute("categorias", servicio.listarCategorias());
        model.addAttribute("esEdicion", false);
        return "vista/inventario/formulario";
    }

    @GetMapping("/editar/{id}")
    public String editarProducto(@PathVariable Integer id, Model model,
                                 RedirectAttributes ra) {
        return servicio.obtenerProductoPorId(id)
                .map(p -> {
                    model.addAttribute("producto", p);
                    model.addAttribute("categorias", servicio.listarCategorias());
                    model.addAttribute("esEdicion", true);
                    return "vista/inventario/formulario";
                })
                .orElseGet(() -> {
                    ra.addFlashAttribute("error", "Producto no encontrado.");
                    return "redirect:/inventario";
                });
    }

    @PostMapping("/guardar")
    public String guardarProducto(@Valid @ModelAttribute("producto") Producto producto,
                                  BindingResult result,
                                  Model model,
                                  RedirectAttributes ra) {

        // Validar código duplicado
        if (!result.hasFieldErrors("codigo")
                && servicio.codigoExiste(producto.getCodigo(), producto.getIdProducto())) {
            result.rejectValue("codigo", "duplicado", "Este código ya está registrado");
        }

        if (result.hasErrors()) {
            model.addAttribute("categorias", servicio.listarCategorias());
            model.addAttribute("esEdicion", producto.getIdProducto() != null);
            return "vista/inventario/formulario";
        }

        if (producto.getActivo() == null) {
            producto.setActivo(true);
        }

        servicio.guardarProducto(producto);
        ra.addFlashAttribute("exito",
                producto.getIdProducto() != null
                        ? "Producto actualizado correctamente."
                        : "Producto registrado correctamente.");
        return "redirect:/inventario";
    }

    /* ====================== DESACTIVAR PRODUCTO ====================== */

    @PostMapping("/desactivar/{id}")
    public String desactivar(@PathVariable Integer id, RedirectAttributes ra) {
        servicio.desactivarProducto(id);
        ra.addFlashAttribute("exito", "Producto desactivado correctamente.");
        return "redirect:/inventario";
    }

    /* ====================== LOTES DE UN PRODUCTO ====================== */

    @GetMapping("/lotes/{idProducto}")
    public String verLotes(@PathVariable Integer idProducto, Model model,
                           RedirectAttributes ra) {
        return servicio.obtenerProductoPorId(idProducto)
                .map(producto -> {
                    List<Lote> lotes = servicio.listarLotesDeProducto(idProducto);
                    // Calcular stock disponible por lote
                    lotes.forEach(l -> {
                        Long stock = servicio.stockDelLote(l.getIdLote());
                        l.setCantidad(stock != null ? stock.intValue() : 0);
                    });

                    model.addAttribute("producto", producto);
                    model.addAttribute("lotes", lotes);
                    return "vista/inventario/lotes";
                })
                .orElseGet(() -> {
                    ra.addFlashAttribute("error", "Producto no encontrado.");
                    return "redirect:/inventario";
                });
    }

    /* ====================== HISTORIAL DE ALERTAS ====================== */

    @GetMapping("/historial/vencimiento")
    public String historialVencimiento(Model model) {
        model.addAttribute("alertas", alertaServicio.historialVencimiento());
        return "vista/inventario/historial-vencimiento";
    }

    @GetMapping("/historial/stock")
    public String historialStock(Model model) {
        model.addAttribute("alertas", alertaServicio.historialStock());
        return "vista/inventario/historial-stock";
    }
}
