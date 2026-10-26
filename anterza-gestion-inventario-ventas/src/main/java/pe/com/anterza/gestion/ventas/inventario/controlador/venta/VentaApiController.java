package pe.com.anterza.gestion.ventas.inventario.controlador.venta;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pe.com.anterza.gestion.ventas.inventario.modelo.dto.VentaRequest;
import pe.com.anterza.gestion.ventas.inventario.modelo.dto.VentaResponse;
import pe.com.anterza.gestion.ventas.inventario.servicio.venta.VentaService;

/** Registro de la venta (JSON). Las validaciones de VentaRequest se aplican con @Valid. */
@RestController
@RequestMapping("/ventas/api")
public class VentaApiController {

    private final VentaService ventaService;

    public VentaApiController(VentaService ventaService) {
        this.ventaService = ventaService;
    }

    @PostMapping("/procesar")
    @ResponseStatus(HttpStatus.CREATED)
    public VentaResponse procesar(@Valid @RequestBody VentaRequest request) {
        return ventaService.procesarVenta(request);
    }
}