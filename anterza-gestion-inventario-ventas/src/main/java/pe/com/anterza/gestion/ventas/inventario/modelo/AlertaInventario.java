package pe.com.anterza.gestion.ventas.inventario.modelo;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Alerta de inventario ya mostrada al usuario. Sirve para no repetirla
 * y para armar el historial.
 *  - tipo VENCIMIENTO: id_lote obligatorio, valor = dias que faltaban (negativo si ya vencio).
 *  - tipo STOCK: id_lote nulo, valor = stock que habia al alertar.
 */
@Entity
@Table(name = "alerta_inventario")
@Getter
@Setter
@NoArgsConstructor
public class AlertaInventario {

    public static final String VENCIMIENTO = "VENCIMIENTO";
    public static final String STOCK = "STOCK";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_alerta")
    private Integer idAlerta;

    @Column(name = "tipo", nullable = false, length = 12)
    private String tipo;

    // EAGER: el historial se muestra con open-in-view=false
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_lote")
    private Lote lote;

    @Column(name = "valor", nullable = false)
    private Integer valor;

    @Column(name = "fecha_alerta", nullable = false, updatable = false)
    private LocalDateTime fechaAlerta;

    @PrePersist
    void antesDeGuardar() {
        if (fechaAlerta == null) {
            fechaAlerta = LocalDateTime.now();
        }
    }
}
