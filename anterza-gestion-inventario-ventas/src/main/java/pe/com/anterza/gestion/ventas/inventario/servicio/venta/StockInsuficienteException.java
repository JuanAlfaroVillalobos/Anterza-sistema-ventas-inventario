package pe.com.anterza.gestion.ventas.inventario.servicio.venta;

/** Se lanza cuando se pide más unidades de las que hay. Deshace toda la venta. */
public class StockInsuficienteException extends RuntimeException {

    public StockInsuficienteException(String producto, long disponible) {
        super("Stock insuficiente de \"" + producto + "\": solo hay " + disponible
                + (disponible == 1 ? " unidad disponible." : " unidades disponibles."));
    }
}