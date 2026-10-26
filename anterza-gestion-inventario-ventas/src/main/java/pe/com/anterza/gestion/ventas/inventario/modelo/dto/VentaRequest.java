package pe.com.anterza.gestion.ventas.inventario.modelo.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Datos que envía la pantalla al pulsar "Procesar venta". */
public record VentaRequest(

        @NotBlank(message = "Seleccione el método de pago.")
        @Pattern(regexp = "EFECTIVO|TARJETA|YAPE|PLIN|TRANSFERENCIA",
                 message = "El método de pago no es válido.")
        String metodoPago,

        @NotEmpty(message = "La canasta está vacía.")
        @Size(max = 100, message = "La venta no puede tener más de 100 productos distintos.")
        @Valid
        List<ItemVentaRequest> items) {
}