package pe.com.anterza.gestion.ventas.inventario.modelo.dto;

import java.util.List;

/** Error devuelto a la pantalla: "mensaje" es lo que se muestra al usuario. */
public record ErrorResponse(String mensaje, List<String> detalles) {

    public static ErrorResponse de(String mensaje) {
        return new ErrorResponse(mensaje, List.of(mensaje));
    }
}