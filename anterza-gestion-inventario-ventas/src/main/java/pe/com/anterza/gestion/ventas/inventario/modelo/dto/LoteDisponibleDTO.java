package pe.com.anterza.gestion.ventas.inventario.modelo.dto;

/** Lote con unidades por vender (cantidad comprada menos cantidad ya vendida). */
public record LoteDisponibleDTO(Integer idLote, Long disponible) {
}