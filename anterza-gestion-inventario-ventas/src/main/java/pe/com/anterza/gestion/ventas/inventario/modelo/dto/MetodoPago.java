package pe.com.anterza.gestion.ventas.inventario.modelo.dto;

/**
 * Métodos de pago que muestra la pantalla de venta. El código (EFECTIVO, TARJETA...)
 * es el texto que se guarda en venta.metodo_pago.
 */
public enum MetodoPago {

    EFECTIVO("Efectivo", "bi-cash-coin"),
    TARJETA("Tarjeta", "bi-credit-card"),
    YAPE("Yape", "bi-phone"),
    PLIN("Plin", "bi-qr-code");

    private final String nombre;
    private final String icono;

    MetodoPago(String nombre, String icono) {
        this.nombre = nombre;
        this.icono = icono;
    }

    public String getCodigo() {
        return name();
    }

    public String getNombre() {
        return nombre;
    }

    public String getIcono() {
        return icono;
    }
}