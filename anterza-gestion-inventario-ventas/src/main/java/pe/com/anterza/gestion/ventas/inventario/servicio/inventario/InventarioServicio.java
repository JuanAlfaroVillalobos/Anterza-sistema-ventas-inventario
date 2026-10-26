package pe.com.anterza.gestion.ventas.inventario.servicio.inventario;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.com.anterza.gestion.ventas.inventario.dao.CategoriaDAO;
import pe.com.anterza.gestion.ventas.inventario.dao.LoteDAO;
import pe.com.anterza.gestion.ventas.inventario.dao.ProductoDAO;
import pe.com.anterza.gestion.ventas.inventario.modelo.Categoria;
import pe.com.anterza.gestion.ventas.inventario.modelo.Lote;
import pe.com.anterza.gestion.ventas.inventario.modelo.Producto;

/**
 * Capa de servicio para el módulo de inventario.
 * Centraliza la lógica de negocio: stock, alertas, búsquedas.
 */
@Service
@Transactional(readOnly = true)
public class InventarioServicio {

    private final ProductoDAO productoDAO;
    private final LoteDAO loteDAO;
    private final CategoriaDAO categoriaDAO;

    public InventarioServicio(ProductoDAO productoDAO,
                              LoteDAO loteDAO,
                              CategoriaDAO categoriaDAO) {
        this.productoDAO = productoDAO;
        this.loteDAO = loteDAO;
        this.categoriaDAO = categoriaDAO;
    }

    /* ====================== PRODUCTOS ====================== */

    /** Lista todos los productos activos con su categoría y stock actual calculado. */
    public List<Producto> listarProductosActivos() {
        List<Producto> productos = productoDAO.listarActivosConCategoria();
        productos.forEach(p -> p.setStockActual(
                loteDAO.stockTotalDelProducto(p.getIdProducto())));
        return productos;
    }

    /** Busca productos activos por nombre (parcial, case-insensitive). */
    public List<Producto> buscarPorNombre(String nombre) {
        List<Producto> productos = productoDAO.buscarPorNombreConFetch(nombre);
        productos.forEach(p -> p.setStockActual(
                loteDAO.stockTotalDelProducto(p.getIdProducto())));
        return productos;
    }

    /** Filtra productos activos por categoría. */
    public List<Producto> filtrarPorCategoria(Integer idCategoria) {
        List<Producto> productos = productoDAO.listarPorCategoriaConFetch(idCategoria);
        productos.forEach(p -> p.setStockActual(
                loteDAO.stockTotalDelProducto(p.getIdProducto())));
        return productos;
    }

    /** Obtiene un producto por ID con su categoría. */
    public Optional<Producto> obtenerProductoPorId(Integer id) {
        Optional<Producto> opt = productoDAO.buscarPorIdConCategoria(id);
        opt.ifPresent(p -> p.setStockActual(
                loteDAO.stockTotalDelProducto(p.getIdProducto())));
        return opt;
    }

    /** Guarda o actualiza un producto. */
    @Transactional
    public Producto guardarProducto(Producto producto) {
        return productoDAO.save(producto);
    }

    /** Desactiva un producto (borrado lógico). */
    @Transactional
    public void desactivarProducto(Integer id) {
        productoDAO.buscarPorIdConCategoria(id).ifPresent(p -> {
            p.setActivo(false);
            productoDAO.save(p);
        });
    }

    /** Verifica si un código ya existe (para otro producto distinto). */
    public boolean codigoExiste(String codigo, Integer idProductoActual) {
        Optional<Producto> existente = productoDAO.findByCodigo(codigo);
        return existente.isPresent()
                && !existente.get().getIdProducto().equals(idProductoActual);
    }

    /* ====================== CATEGORÍAS ====================== */

    /** Lista todas las categorías (para selects). */
    public List<Categoria> listarCategorias() {
        return categoriaDAO.findAll();
    }

    /* ====================== LOTES ====================== */

    /** Lista los lotes de un producto con JOIN FETCH (detalles completos). */
    public List<Lote> listarLotesDeProducto(Integer idProducto) {
        return loteDAO.listarPorProductoConDetalles(idProducto);
    }

    /** Calcula el stock disponible de un lote individual. */
    public Long stockDelLote(Integer idLote) {
        return loteDAO.stockDelLote(idLote);
    }

    /* ====================== ALERTAS ====================== */

    /** Lotes próximos a vencer (entre hoy y fecha límite). */
    public List<Lote> lotesProximosAVencer(int diasAnticipacion) {
        LocalDate fechaLimite = LocalDate.now().plusDays(diasAnticipacion);
        return loteDAO.listarProximosAVencer(fechaLimite);
    }

    /** Lotes ya vencidos (fecha < hoy). */
    public List<Lote> lotesVencidos() {
        return loteDAO.listarVencidos();
    }

    /** Productos activos cuyo stock actual está por debajo del stock mínimo. */
    public List<Producto> productosConStockBajo() {
        List<Producto> todos = listarProductosActivos();
        return todos.stream()
                .filter(p -> p.getStockActual() != null
                        && p.getStockActual() < p.getStockMinimo())
                .toList();
    }

    /* ====================== RESUMEN DASHBOARD ====================== */

    /** Total de productos activos. */
    public long contarProductosActivos() {
        return productoDAO.findByActivoTrue().size();
    }

    /** Cantidad de lotes próximos a vencer (30 días). */
    public long contarLotesProximosAVencer() {
        return lotesProximosAVencer(30).size();
    }

    /** Cantidad de productos con stock bajo. */
    public long contarProductosStockBajo() {
        return productosConStockBajo().size();
    }
}
