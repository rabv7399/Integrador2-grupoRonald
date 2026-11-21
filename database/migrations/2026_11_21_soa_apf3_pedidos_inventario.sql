-- Delicias Peruanas · Migracion manual propuesta APF3 SOA
-- IMPORTANTE: NO ejecutar sobre Aiven de produccion sin respaldo y revision.
-- Uso previsto: base MySQL de desarrollo con esquema ya creado.

CREATE TABLE IF NOT EXISTS inventario_platos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    producto_id BIGINT NOT NULL,
    total_preparado INT NOT NULL DEFAULT 0,
    reservadas INT NOT NULL DEFAULT 0,
    vendidas INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uq_inventario_producto UNIQUE (producto_id),
    CONSTRAINT fk_inventario_producto FOREIGN KEY (producto_id) REFERENCES productos(id),
    CONSTRAINT chk_inventario_cantidades CHECK (
        total_preparado >= 0 AND reservadas >= 0 AND vendidas >= 0
        AND total_preparado >= reservadas + vendidas
    )
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS pedidos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    referencia_operacion VARCHAR(100) NOT NULL,
    firma_solicitud VARCHAR(64) NOT NULL,
    cliente_id BIGINT NOT NULL,
    estado VARCHAR(20) NOT NULL,
    total DECIMAL(12,2) NOT NULL,
    fecha_creacion DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_pedido_referencia UNIQUE (referencia_operacion),
    CONSTRAINT fk_pedido_cliente FOREIGN KEY (cliente_id) REFERENCES usuarios(id),
    CONSTRAINT chk_pedidos_estado CHECK (estado IN ('PENDIENTE','CONFIRMADO','CANCELADO')),
    CONSTRAINT chk_pedidos_total CHECK (total >= 0)
) ENGINE=InnoDB;

CREATE INDEX idx_pedidos_cliente_fecha ON pedidos(cliente_id, fecha_creacion);

CREATE TABLE IF NOT EXISTS detalle_pedidos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    pedido_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_detalle_pedido FOREIGN KEY (pedido_id) REFERENCES pedidos(id),
    CONSTRAINT fk_detalle_producto FOREIGN KEY (producto_id) REFERENCES productos(id),
    CONSTRAINT chk_detalle_cantidad CHECK (cantidad > 0),
    CONSTRAINT chk_detalle_precio CHECK (precio_unitario > 0),
    CONSTRAINT chk_detalle_subtotal CHECK (subtotal >= 0)
) ENGINE=InnoDB;

CREATE INDEX idx_detalle_pedido ON detalle_pedidos(pedido_id);
