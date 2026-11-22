-- APF3 SOA: pagos por Transferencia, recargo POS y fotos administradas (MySQL 8.4)
-- SIN EJECUTAR EN AIVEN NI RENDER. Requiere respaldo y ensayo previo.
-- Debe aplicarse DESPUÉS de 2026_11_21_soa_apf3_auditoria_caja_tiempos.sql.
-- Ejecutar una sola vez; DDL MySQL puede crear COMMIT implícito.

ALTER TABLE ventas
 ADD COLUMN subtotal_base DECIMAL(12,2) NOT NULL DEFAULT 0.00,
 ADD COLUMN recargo_pos DECIMAL(12,2) NOT NULL DEFAULT 0.00,
 ADD COLUMN recargo_informado_aceptado BOOLEAN NOT NULL DEFAULT FALSE;

-- Reconstruir subtotal histórico. Anteriormente NO se cobraba recargo POS.
UPDATE ventas SET subtotal_base=total,recargo_pos=0.00
 WHERE subtotal_base=0.00;

ALTER TABLE productos
 ADD COLUMN imagen_url VARCHAR(255) NULL;

-- Almacenamiento de fotos dentro de la MISMA base de datos.
-- Sólo PNG/JPEG verificados en servidor, 1.5 MB máximo por foto.
CREATE TABLE producto_imagenes (
 producto_id BIGINT NOT NULL,
 contenido LONGBLOB NOT NULL,
 tipo_mime VARCHAR(30) NOT NULL,
 PRIMARY KEY(producto_id),
 CONSTRAINT fk_producto_foto FOREIGN KEY(producto_id) REFERENCES productos(id)
) ENGINE=InnoDB;

-- Nueva modalidad TRANSFERENCIA comparte el varchar medio_pago existente.
-- Regla de prototipo POS = techo((subtotal*1.04)*2)/2 PEN.
-- La columna "total" es el IMPORTE EFECTIVAMENTE COBRADO (incluye recargo POS).
-- El subtotal_base guarda la venta de platos original, recargo_pos el incremento.
-- El consentimiento es obligatorio para registrar POS.
-- Aviso: precios diferenciados por medio de pago deben estar informados ANTES
-- de la compra, conforme al Artículo 7 del Código de Protección y Defensa
-- del Consumidor de Perú. No desplegar recargos sin aviso visible previo.
