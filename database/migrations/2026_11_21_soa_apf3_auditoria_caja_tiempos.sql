-- Delicias Peruanas · SOA APF3 · Ampliación de operación y caja (MySQL 8.4)
-- ATENCIÓN: NO EJECUTADO EN AIVEN NI RENDER. Solo revisión/ensayo con respaldo.
-- Aplicar DESPUÉS de las dos migraciones APF3 previas y solo una vez.
-- DDL de MySQL puede hacer COMMIT implícito. Verificar esquema existente antes de aplicar.

ALTER TABLE detalle_pedidos
  ADD COLUMN fecha_inicio DATETIME(6) NULL,
  ADD COLUMN fecha_listo DATETIME(6) NULL,
  ADD COLUMN fecha_entrega DATETIME(6) NULL;

-- Conservar historia: para detalles antiguos, el inicio se aproxima
-- con la fecha de creación del pedido.
UPDATE detalle_pedidos d
  JOIN pedidos p ON d.pedido_id = p.id
  SET d.fecha_inicio = p.fecha_creacion
  WHERE d.fecha_inicio IS NULL;

ALTER TABLE detalle_pedidos
  MODIFY COLUMN fecha_inicio DATETIME(6) NOT NULL;

ALTER TABLE ventas
  ADD COLUMN referencia_pago VARCHAR(80) NULL,
  ADD COLUMN cajero_correo VARCHAR(150) NOT NULL DEFAULT 'sistema';

-- Evitar comprobantes digitales duplicados por medio y referencia.
-- Requiere limpieza previa si ya existen duplicados históricos.
ALTER TABLE ventas
  ADD CONSTRAINT uq_ventas_metodo_referencia UNIQUE (medio_pago, referencia_pago);

CREATE TABLE movimientos_inventario (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  producto_id BIGINT NOT NULL,
  tipo VARCHAR(15) NOT NULL,
  cantidad INT NOT NULL,
  comentario VARCHAR(500) NOT NULL,
  usuario_correo VARCHAR(150) NOT NULL,
  fecha DATETIME(6) NOT NULL,
  CONSTRAINT fk_movimientos_producto FOREIGN KEY (producto_id)
    REFERENCES productos(id),
  CONSTRAINT chk_movimiento_cantidad CHECK (cantidad > 0)
) ENGINE=InnoDB;
CREATE INDEX idx_movimientos_fecha ON movimientos_inventario(fecha);

CREATE TABLE adiciones_pedido (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  clave VARCHAR(100) NOT NULL,
  firma VARCHAR(64) NOT NULL,
  pedido_id BIGINT NOT NULL,
  CONSTRAINT uq_adicion_clave UNIQUE (clave),
  CONSTRAINT fk_adicion_pedido FOREIGN KEY (pedido_id) REFERENCES pedidos(id)
) ENGINE=InnoDB;

CREATE TABLE cierres_caja (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  fecha DATE NOT NULL,
  efectivo_esperado DECIMAL(12,2) NOT NULL,
  efectivo_contado DECIMAL(12,2) NOT NULL,
  diferencia DECIMAL(12,2) NOT NULL,
  registrado_por VARCHAR(150) NOT NULL,
  fecha_registro DATETIME(6) NOT NULL,
  CONSTRAINT uq_cierre_dia UNIQUE (fecha)
) ENGINE=InnoDB;

-- Los pagos EFECTIVO históricos conservan el monto y el vuelto.
-- YAPE y POS se registran manualmente. NO conecta con bancos.
-- El cuadre se basa en ingresos por EFECTIVO registrados; fondo inicial,
-- gastos, retiros y depósitos no están modelados en este prototipo.
