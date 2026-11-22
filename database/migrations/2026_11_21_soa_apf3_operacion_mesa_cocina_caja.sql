-- APF3 SOA · Ampliación de operación gastronómica
-- NO SE HA EJECUTADO EN AIVEN; archivo para revisión y respaldo previo.
-- Requiere haber aplicado primero 2026_11_21_soa_apf3_pedidos_inventario.sql.
-- MySQL 8.4 · ejecutar SOLO UNA VEZ; DDL puede autoconfirmar cambios.

ALTER TABLE pedidos
  ADD COLUMN tipo_atencion VARCHAR(12) NOT NULL DEFAULT 'LLEVAR',
  ADD COLUMN mesa_id BIGINT NULL,
  ADD CONSTRAINT fk_pedidos_mesa FOREIGN KEY (mesa_id) REFERENCES mesas(id);

ALTER TABLE detalle_pedidos
  ADD COLUMN estado_preparacion VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE';

CREATE TABLE ventas (
  id BIGINT NOT NULL AUTO_INCREMENT,
  pedido_id BIGINT NOT NULL,
  numero_comprobante VARCHAR(60) NOT NULL,
  total DECIMAL(12,2) NOT NULL,
  monto_recibido DECIMAL(12,2) NOT NULL,
  cambio DECIMAL(12,2) NOT NULL,
  fecha_pago DATETIME(6) NOT NULL,
  medio_pago VARCHAR(20) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uq_venta_pedido UNIQUE (pedido_id),
  CONSTRAINT uq_numero_comprobante UNIQUE (numero_comprobante),
  CONSTRAINT fk_ventas_pedido FOREIGN KEY (pedido_id) REFERENCES pedidos(id),
  CONSTRAINT chk_venta_total CHECK (total >= 0),
  CONSTRAINT chk_venta_cambio CHECK (cambio >= 0)
) ENGINE=InnoDB;

CREATE INDEX idx_ventas_fecha ON ventas(fecha_pago);

CREATE TABLE configuracion_local (
  id BIGINT NOT NULL,
  nombre VARCHAR(100) NOT NULL,
  telefono VARCHAR(25) NOT NULL,
  logo_ruta VARCHAR(255),
  PRIMARY KEY (id)
) ENGINE=InnoDB;

INSERT INTO configuracion_local (id,nombre,telefono,logo_ruta)
VALUES (1,'Delicias Peruanas','966 100 348',NULL);

-- Los usuarios ya disponen de un campo rol de tipo VARCHAR(30).
-- Nuevos valores sin alterar estructura: CAJA y COCINERO.
-- No crear cuentas/contraseñas en esta migración; usar gestión ADMIN.
