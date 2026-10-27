-- Historial de alertas de inventario (ejecutar una vez en la base farmacia_anterza)
USE farmacia_anterza;

CREATE TABLE IF NOT EXISTS alerta_inventario (
    id_alerta     INT          PRIMARY KEY AUTO_INCREMENT,
    tipo          VARCHAR(12)  NOT NULL,
    id_producto   INT          NOT NULL,
    id_lote       INT          NULL,
    valor         INT          NOT NULL,
    fecha_alerta  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_alerta_tipo CHECK (tipo IN ('VENCIMIENTO', 'STOCK')),
    CONSTRAINT fk_alerta_producto FOREIGN KEY (id_producto) REFERENCES producto (id_producto),
    CONSTRAINT fk_alerta_lote     FOREIGN KEY (id_lote)     REFERENCES lote (id_lote)
);

CREATE INDEX idx_alerta_fecha ON alerta_inventario (fecha_alerta);
