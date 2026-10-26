package pe.com.anterza.gestion.ventas.inventario.modelo.dto;

import java.math.BigDecimal;

/**
 * Producto tal como se muestra en la pantalla de venta.
 * "stock" es el stock vendible: lotes no vencidos menos lo ya vendido.
 */
public record ProductoVentaDTO(
        Integer idProducto,
        String codigo,
        String nombre,
        BigDecimal precioVenta,
        Integer stockMinimo,
        Integer idCategoria,
        String categoria,
        Long stock) {
}