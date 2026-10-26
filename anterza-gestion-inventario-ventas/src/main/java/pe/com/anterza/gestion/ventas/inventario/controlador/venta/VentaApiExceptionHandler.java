package pe.com.anterza.gestion.ventas.inventario.controlador.venta;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import lombok.extern.slf4j.Slf4j;
import pe.com.anterza.gestion.ventas.inventario.modelo.dto.ErrorResponse;
import pe.com.anterza.gestion.ventas.inventario.servicio.venta.StockInsuficienteException;

/** Convierte los errores de la API de ventas en respuestas JSON con un mensaje legible. */
@Slf4j
@RestControllerAdvice(assignableTypes = { VentaApiController.class, VentaController.class })
public class VentaApiExceptionHandler {

    /** Falló alguna validación (@NotNull, @Min, @Pattern...) del cuerpo de la venta. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> datosInvalidos(MethodArgumentNotValidException ex) {
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .distinct()
                .toList();
        String mensaje = detalles.isEmpty() ? "Los datos de la venta no son válidos." : detalles.get(0);
        return ResponseEntity.badRequest().body(new ErrorResponse(mensaje, detalles));
    }

    /** Falló la validación de un parámetro de la URL (búsqueda demasiado larga, etc.). */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> parametrosInvalidos(HandlerMethodValidationException ex) {
        return ResponseEntity.badRequest().body(ErrorResponse.de("Los parámetros de búsqueda no son válidos."));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> tipoIncorrecto(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(ErrorResponse.de("Un parámetro tiene un valor no válido."));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> metodoNoPermitido(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(ErrorResponse.de("Operación no permitida."));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> cuerpoIlegible(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(ErrorResponse.de("Los datos de la venta no tienen el formato esperado."));
    }

    @ExceptionHandler(StockInsuficienteException.class)
    public ResponseEntity<ErrorResponse> sinStock(StockInsuficienteException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.de(ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> argumentoInvalido(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ErrorResponse.de(ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> configuracionIncompleta(IllegalStateException ex) {
        log.error("Configuración incompleta para registrar ventas: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.de(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> inesperado(Exception ex) {
        log.error("Error inesperado en la API de ventas", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.de("No se pudo completar la operación. Inténtelo de nuevo."));
    }
}