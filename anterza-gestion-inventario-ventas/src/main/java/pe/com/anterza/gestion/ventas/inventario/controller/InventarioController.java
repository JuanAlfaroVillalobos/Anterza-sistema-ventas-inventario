package pe.com.anterza.gestion.ventas.inventario.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import pe.com.anterza.gestion.ventas.inventario.dao.ProductoDAO;
import pe.com.anterza.gestion.ventas.inventario.dao.CategoriaDAO;
import pe.com.anterza.gestion.ventas.inventario.dao.LoteDAO;
import pe.com.anterza.gestion.ventas.inventario.modelo.Producto;
import pe.com.anterza.gestion.ventas.inventario.modelo.Lote;

@Controller
@RequestMapping("/inventario")
public class InventarioController {

    @Autowired
    private ProductoDAO productoDAO;

    @Autowired
    private CategoriaDAO categoriaDAO;

    @Autowired
    private LoteDAO loteDAO;

    // ==================== LISTADO PRINCIPAL (Fig. 9) ====================

    @GetMapping
    public String listarInventario(@RequestParam(required = false) String buscar,
                                   @RequestParam(required = false) Integer categoriaId,
                                   Model model) {

        // JOIN FETCH de categoría para evitar LazyInitializationException (open-in-view=false)
        List<Producto> productos;
        if (buscar != null && !buscar.isBlank()) {
            productos = productoDAO.findByNombreConCategoria(buscar);
        } else if (categoriaId != null) {
            productos = productoDAO.findByCategoriaConCategoria(categoriaId);
        } else {
            productos = productoDAO.findAllConCategoria();
        }

        // Calcular stock total por producto + capturar primer lote para la tabla
        Map<Integer, Lote> primerLote = new HashMap<>();
        for (Producto p : productos) {
            Long stock = loteDAO.stockTotalDelProducto(p.getIdProducto());
            p.setStockActual(stock != null ? stock.intValue() : 0);

            List<Lote> lotes = loteDAO.findByProducto_IdProducto(p.getIdProducto());
            if (!lotes.isEmpty()) {
                primerLote.put(p.getIdProducto(), lotes.get(0));
            }
        }

        // Resumen de alertas (conteos)
        LocalDate hoy = LocalDate.now();
        LocalDate limite = hoy.plusMonths(3);
        long proximosVencerCount = loteDAO.countByFechaVencimientoBetween(hoy, limite);
        long stockBajoCount = productos.stream()
                .filter(p -> p.getStockActual() != null && p.getStockActual() <= p.getStockMinimo())
                .count();

        // Datos para el modal de alertas (Fig. 10) — JOIN FETCH para evitar LazyInit
        List<Lote> lotesProximosVencer = loteDAO.findLotesProximosConProducto(hoy, limite);
        List<Producto> productosStockBajoList = productos.stream()
                .filter(p -> p.getStockActual() != null && p.getStockActual() <= p.getStockMinimo())
                .toList();

        model.addAttribute("productos", productos);
        model.addAttribute("categorias", categoriaDAO.findAll());
        model.addAttribute("buscar", buscar);
        model.addAttribute("categoriaId", categoriaId);
        model.addAttribute("primerLote", primerLote);
        model.addAttribute("proximosVencer", proximosVencerCount);
        model.addAttribute("productosStockBajo", stockBajoCount);
        model.addAttribute("lotesProximosVencer", lotesProximosVencer);
        model.addAttribute("productosStockBajoList", productosStockBajoList);

        return "inventario/listar";
    }

    // ==================== REGISTRAR / EDITAR PRODUCTO ====================

    @GetMapping("/nuevo")
    public String nuevoProducto(Model model) {
        model.addAttribute("producto", new Producto());
        model.addAttribute("categorias", categoriaDAO.findAll());
        return "inventario/formulario";
    }

    @PostMapping("/guardar")
    public String guardarProducto(@ModelAttribute Producto producto,
                                  RedirectAttributes redirect) {
        productoDAO.save(producto);
        redirect.addFlashAttribute("msgExito", "Producto guardado correctamente.");
        return "redirect:/inventario";
    }

    @GetMapping("/editar/{id}")
    public String editarProducto(@PathVariable("id") Integer id, Model model) {
        Producto producto = productoDAO.findById(id).orElse(null);
        if (producto == null) return "redirect:/inventario";
        model.addAttribute("producto", producto);
        model.addAttribute("categorias", categoriaDAO.findAll());
        return "inventario/formulario";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarProducto(@PathVariable("id") Integer id, RedirectAttributes redirect) {
        productoDAO.deleteById(id);
        redirect.addFlashAttribute("msgExito", "Producto eliminado.");
        return "redirect:/inventario";
    }

    // ==================== DETALLE DE LOTES ====================

    @GetMapping("/lotes/{idProducto}")
    public String verLotes(@PathVariable("idProducto") Integer idProducto, Model model) {
        Producto producto = productoDAO.findById(idProducto).orElse(null);
        if (producto == null) return "redirect:/inventario";
        // JOIN FETCH para tener producto cargado en cada lote
        List<Lote> lotes = loteDAO.findByProductoConProducto(idProducto);
        model.addAttribute("producto", producto);
        model.addAttribute("lotes", lotes);
        return "inventario/lotes";
    }

    // ==================== ALERTAS VENCIMIENTO (Fig. 11) ====================

    @GetMapping("/alertas/vencimiento")
    public String alertasVencimiento(Model model) {
        LocalDate hoy = LocalDate.now();
        LocalDate limite = hoy.plusMonths(3);

        // JOIN FETCH para cargar producto en cada lote (evita LazyInitException)
        List<Lote> proximosVencer = loteDAO.findLotesProximosConProducto(hoy, limite);
        List<Lote> vencidos = loteDAO.findVencidosConProducto(hoy);

        model.addAttribute("proximosVencer", proximosVencer);
        model.addAttribute("vencidos", vencidos);
        model.addAttribute("totalProximos", proximosVencer.size());
        model.addAttribute("totalVencidos", vencidos.size());
        return "inventario/alertas-vencimiento";
    }

    // ==================== ALERTAS STOCK BAJO (Fig. 12) ====================

    @GetMapping("/alertas/stock")
    public String alertasStock(Model model) {
        List<Producto> todos = productoDAO.findAll();

        // Calcular stock usando la query del DAO
        for (Producto p : todos) {
            Long stock = loteDAO.stockTotalDelProducto(p.getIdProducto());
            p.setStockActual(stock != null ? stock.intValue() : 0);
        }

        List<Producto> stockBajo = todos.stream()
                .filter(p -> p.getStockActual() > 0 && p.getStockActual() <= p.getStockMinimo())
                .toList();

        List<Producto> agotados = todos.stream()
                .filter(p -> p.getStockActual() == 0)
                .toList();

        model.addAttribute("stockBajo", stockBajo);
        model.addAttribute("agotados", agotados);
        model.addAttribute("totalStockBajo", stockBajo.size());
        model.addAttribute("totalAgotados", agotados.size());
        return "inventario/alertas-stock";
    }
}
