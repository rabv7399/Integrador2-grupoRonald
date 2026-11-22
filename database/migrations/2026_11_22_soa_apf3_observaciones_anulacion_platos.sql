-- APF3: observaciones y anulaciones lógicas de líneas de comanda.
-- NO APLICADO EN AIVEN. Probar después de respaldo en MySQL 8.4 y migraciones previas.
ALTER TABLE detalle_pedidos
  ADD COLUMN observacion VARCHAR(240) NULL,
  ADD COLUMN motivo_anulacion VARCHAR(300) NULL,
  ADD COLUMN anulado_por VARCHAR(150) NULL,
  ADD COLUMN fecha_anulacion DATETIME(6) NULL;
-- estado_preparacion admite el valor adicional ANULADO en VARCHAR(15).
-- El pedido conserva el detalle anulado como traza y resta su subtotal del total.
-- No eliminar físicamente líneas para preservar evidencia de anulación.
