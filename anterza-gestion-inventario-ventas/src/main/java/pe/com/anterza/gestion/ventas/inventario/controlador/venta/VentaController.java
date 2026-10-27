package pe.com.anterza.gestion.ventas.inventario.controlador.venta;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import pe.com.anterza.gestion.ventas.inventario.dao.CategoriaDAO;
import pe.com.anterza.gestion.ventas.inventario.modelo.dto.MetodoPago;
import pe.com.anterza.gestion.ventas.inventario.servicio.venta.VentaService;

/**
 * Pantalla "Registrar Venta" con Thymeleaf.
 * - /ventas/nueva  : página completa (reutiliza fragments/layout y los fragmentos de vista/venta/fragments).
 * - /ventas/buscar : devuelve SOLO el fragmento con las tarjetas de producto (búsqueda en vivo).
 */
@Controller
@RequestMapping("/ventas")
public class VentaController {

    private final CategoriaDAO categoriaDAO;
    private final VentaService ventaService;

    public VentaController(CategoriaDAO categoriaDAO, VentaService ventaService) {
        this.categoriaDAO = categoriaDAO;
        this.ventaService = ventaService;
    }

    @GetMapping("/nueva")
    public String nuevaVenta(Model model) {
        model.addAttribute("categorias", categoriaDAO.findAll());
        model.addAttribute("productos", ventaService.buscarProductos("", 0));
        model.addAttribute("metodosPago", MetodoPago.values());
        return "vista/venta/registrar";
    }

    @GetMapping("/buscar")
    public String buscar(
            @RequestParam(defaultValue = "") @Size(max = 60, message = "La búsqueda es demasiado larga.") String q,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Categoría no válida.") Integer categoriaId,
            Model model) {
        model.addAttribute("productos", ventaService.buscarProductos(q, categoriaId));
        return "vista/venta/fragments/producto :: lista";
    }
}