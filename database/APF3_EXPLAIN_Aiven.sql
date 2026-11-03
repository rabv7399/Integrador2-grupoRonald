-- APF3 | Delicias Peruanas | Diagnostico MySQL Aiven
-- SOLO LECTURA: no modifica tablas, indices, registros ni permisos.
-- Ejecutar en la base de datos de la aplicacion, con autorizacion.
-- Guardar captura de SHOW INDEX y de cada EXPLAIN, ocultando datos sensibles.
-- Los valores de ejemplo no representan reservas o usuarios reales.

SELECT DATABASE() AS esquema_actual;
SHOW INDEX FROM reservas;
SHOW INDEX FROM usuarios;

-- Consulta critica 1: deteccion de solapamiento de reservas por mesa y fecha.
EXPLAIN
SELECT id
FROM reservas
WHERE mesa_id = 1
  AND fecha = CURRENT_DATE()
  AND estado <> 'CANCELADA'
  AND hora_inicio < '21:00:00'
  AND hora_fin > '19:00:00';

-- Consulta critica 2: busqueda de reservas por fecha.
EXPLAIN
SELECT id, fecha, hora_inicio, mesa_id, estado
FROM reservas
WHERE fecha = CURRENT_DATE();

-- Consulta critica 3: localizacion de usuario por correo.
-- Usar correo ficticio; no se leen passwords.
EXPLAIN
SELECT id, correo, rol, activo
FROM usuarios
WHERE correo = 'prueba@example.invalid';

-- Interpretacion:
-- type: ALL = escaneo completo; ref/range/const suelen indicar acceso mas selectivo.
-- key: indice elegido por el optimizador (NULL = ninguno).
-- rows: estimacion del optimizador, no filas realmente procesadas.
-- Extra: detalles como Using where o Using index.
-- No crear indices sin revisar cardinalidad, duplicados, respaldo y RFC.
