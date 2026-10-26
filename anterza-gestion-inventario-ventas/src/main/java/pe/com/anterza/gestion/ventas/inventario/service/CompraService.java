package pe.com.anterza.gestion.ventas.inventario.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.anterza.gestion.ventas.inventario.dao.*;
import pe.com.anterza.gestion.ventas.inventario.modelo.*;

@Service
public class CompraService {
    private final CompraDAO compras;
    private final ProveedorDAO proveedores;
    private final EmpleadoDAO empleados;
    private final ProductoDAO productos;
    private final LoteDAO lotes;

    public CompraService(CompraDAO compras, ProveedorDAO proveedores, EmpleadoDAO empleados,
                         ProductoDAO productos, LoteDAO lotes) {
        this.compras = compras;
        this.proveedores = proveedores;
        this.empleados = empleados;
        this.productos = productos;
        this.lotes = lotes;
    }

    @Transactional
    public Compra registrar(Integer proveedorId, Integer empleadoId, String documento,
                            LocalDate fecha, Integer productoId, String codigoLote,
                            LocalDate vencimiento, Integer cantidad, BigDecimal costo) {
        if (documento == null || documento.isBlank() || documento.length() > 20)
            throw new IllegalArgumentException("Ingrese un número de documento válido (máximo 20 caracteres).");
        if (fecha == null || fecha.isAfter(LocalDate.now()))
            throw new IllegalArgumentException("La fecha de compra no puede ser futura.");
        if (vencimiento == null || !vencimiento.isAfter(fecha))
            throw new IllegalArgumentException("El vencimiento debe ser posterior a la compra.");
        if (codigoLote == null || codigoLote.isBlank() || codigoLote.length() > 20)
            throw new IllegalArgumentException("Ingrese un código de lote válido (máximo 20 caracteres).");
        if (cantidad == null || cantidad <= 0 || costo == null || costo.signum() <= 0)
            throw new IllegalArgumentException("Cantidad y costo unitario deben ser mayores a cero.");
        if (costo.scale() > 2 || costo.compareTo(new BigDecimal("999999.99")) > 0)
            throw new IllegalArgumentException("Costo unitario inválido (máximo dos decimales).");

        Proveedor proveedor = proveedores.findById(proveedorId)
            .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado."));
        Empleado empleado = empleados.findById(empleadoId)
            .orElseThrow(() -> new IllegalArgumentException("Empleado no encontrado."));
        Producto producto = productos.findById(productoId)
            .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado."));

        Compra compra = new Compra();
        compra.setProveedor(proveedor);
        compra.setEmpleado(empleado);
        compra.setNroDocumento(documento.trim());
        compra.setFechaCompra(fecha);
        compra = compras.save(compra);

        Lote lote = new Lote();
        lote.setCompra(compra);
        lote.setProducto(producto);
        lote.setCodigoLote(codigoLote.trim());
        lote.setFechaVencimiento(vencimiento);
        lote.setCantidad(cantidad);
        lote.setCostoUnitario(costo);
        lotes.save(lote);
        return compra;
    }
}
