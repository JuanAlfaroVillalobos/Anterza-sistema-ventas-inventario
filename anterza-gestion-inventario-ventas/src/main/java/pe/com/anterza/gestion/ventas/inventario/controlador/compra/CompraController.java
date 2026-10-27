package pe.com.anterza.gestion.ventas.inventario.controlador.compra;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.com.anterza.gestion.ventas.inventario.dao.*;
import pe.com.anterza.gestion.ventas.inventario.servicio.compra.CompraService;

@Controller
@RequestMapping("/compras")
public class CompraController {
    private final CompraService servicio;
    private final ProveedorDAO proveedores;
    private final EmpleadoDAO empleados;
    private final ProductoDAO productos;

    public CompraController(CompraService servicio, ProveedorDAO proveedores,
                            EmpleadoDAO empleados, ProductoDAO productos) {
        this.servicio = servicio;
        this.proveedores = proveedores;
        this.empleados = empleados;
        this.productos = productos;
    }

    @GetMapping
    public String formulario(Model model) {
        model.addAttribute("proveedores", proveedores.findAll());
        model.addAttribute("empleados", empleados.findAll());
        model.addAttribute("productos", productos.findAll());
        model.addAttribute("hoy", LocalDate.now());
        return "vista/compra/registrar";
    }

    @PostMapping("/registrar")
    public String registrar(@RequestParam Integer proveedorId,
                            @RequestParam Integer empleadoId,
                            @RequestParam String documento,
                            @RequestParam LocalDate fecha,
                            @RequestParam Integer productoId,
                            @RequestParam String codigoLote,
                            @RequestParam LocalDate vencimiento,
                            @RequestParam Integer cantidad,
                            @RequestParam BigDecimal costo,
                            RedirectAttributes redirect) {
        try {
            var compra = servicio.registrar(proveedorId, empleadoId, documento, fecha,
                productoId, codigoLote, vencimiento, cantidad, costo);
            redirect.addFlashAttribute("exito", "Compra #" + compra.getIdCompra() + " registrada en MySQL.");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        } catch (Exception ex) {
            redirect.addFlashAttribute("error", "No se pudo guardar. Verifique las tablas y conexión MySQL.");
        }
        return "redirect:/compras";
    }
}
