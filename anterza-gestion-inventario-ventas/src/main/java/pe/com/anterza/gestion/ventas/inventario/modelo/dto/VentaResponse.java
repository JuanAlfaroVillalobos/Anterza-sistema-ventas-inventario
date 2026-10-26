package pe.com.anterza.gestion.ventas.inventario.modelo.dto;

import java.math.BigDecimal;

/** Resumen de la venta registrada. Los precios ya incluyen IGV (18 %). */
public record VentaResponse(
        Integer idVenta,
        String metodoPago,
        BigDecimal subtotal,
        BigDecimal igv,
        BigDecimal total) {
}