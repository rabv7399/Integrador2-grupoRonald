-- DATOS SINTETICOS DE PRUEBA. No corresponden a clientes reales.
INSERT INTO categorias (id, nombre, tipo, descripcion) VALUES (99101,'Categoria laboratorio APF3','COMIDA','Datos ficticios');
INSERT INTO productos (id, codigo, nombre, descripcion, precio, disponible, categoria_id) VALUES (99101,'LAB-APF3-01','Producto laboratorio','Solo para prueba de respaldo',10.50,1,99101);
INSERT INTO mesas (id, numero, capacidad, estado) VALUES (99101,99101,4,'DISPONIBLE');
INSERT INTO reservas (id,cliente,fecha,hora_inicio,hora_fin,cantidad_personas,estado,mesa_id) VALUES (99101,'CLIENTE FICTICIO', '2026-11-07','18:00:00','19:00:00',2,'PENDIENTE',99101);
INSERT INTO usuarios (id,nombre,correo,telefono,password,rol,activo) VALUES (99101,'USUARIO DE PRUEBA','laboratorio@example.invalid',NULL,'NO-ES-UN-HASH-VALIDO','CLIENTE',1);
