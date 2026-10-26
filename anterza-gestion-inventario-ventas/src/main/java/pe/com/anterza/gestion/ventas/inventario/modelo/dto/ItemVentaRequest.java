package pe.com.anterza.gestion.ventas.inventario.modelo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Una línea de la canasta: qué producto y cuántas unidades. */
public record ItemVentaRequest(

        @NotNull(message = "Falta indicar el producto.")
        Integer idProducto,

        @NotNull(message = "Falta indicar la cantidad.")
        @Min(value = 1, message = "La cantidad mínima por producto es 1.")
        @Max(value = 999, message = "La cantidad máxima por producto es 999.")
        Integer cantidad) {
}