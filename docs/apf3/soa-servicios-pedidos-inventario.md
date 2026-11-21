# APF3 SOA · Prototipo transaccional de Pedidos S03 e Inventario S05

**Estado:** código en rama `feature/soa-apf3-pedidos-inventario`, pendiente de ejecutar contra una base de desarrollo. No se ha aplicado ninguna migración a Aiven.

## Servicios representativos
- S01 Clientes/Autenticación: `POST /api/auth/registro`, `POST /api/auth/login`; JWT en solicitudes protegidas.
- S02 Productos y Menú: `GET /api/productos`; precio y disponibilidad comercial.
- S03 Pedidos: `POST /api/v1/pedidos`, `GET /api/v1/pedidos/{id}`, confirmación y cancelación.
- S04 Reservas: `GET /api/reservas`, `POST /api/reservas`, `PATCH /api/reservas/{id}/cancelar`.
- S05 Inventario de porciones: `GET /api/v1/inventario/{productoId}`, `PUT /api/v1/inventario/{productoId}` (ADMIN).

El prototipo utiliza **un backend modular Spring Boot**, con endpoints REST y coordinación transaccional mediante servicios Java y una base MySQL común. No son cinco microservicios físicamente independientes y no se debe presentar como una red de servicios distribuidos.

## Ejemplo de flujo reproducible (solo en entorno de prueba)
1. Un ADMIN configura el total de porciones del plato con `PUT /api/v1/inventario/1` y JSON `{"totalPreparado":30}`.
2. CLIENTE autenticado solicita `POST /api/v1/pedidos` con cabecera `Idempotency-Key: demostracion-soa-001` y JSON `{"items":[{"productoId":1,"cantidad":2}]}`.
3. Respuesta `201 Created`: estado `PENDIENTE`. El inventario tiene 2 reservadas y 28 disponibles.
4. Reenviar el mismo pedido con la misma referencia produce `200 OK` y el mismo `pedidoId`, sin reservar por segunda vez.
5. Un ADMIN/OPERADOR confirma mediante `POST /api/v1/pedidos/{id}/confirmacion`: inventario pasa a 0 reservadas, 2 vendidas y 28 disponibles.
6. La cancelación de otro pedido aún `PENDIENTE` libera sus porciones. Un pedido confirmado no se cancela automáticamente con este prototipo.

`productoId=1` es solo ejemplo: usar los IDs reales del entorno de demostración.

## Errores de negocio
- `400`: entrada mal formada o cabecera Idempotency-Key inválida.
- `401`: token ausente o inválido.
- `403`: sin permisos para inventario o pedido ajeno.
- `404`: producto, pedido o registro de inventario inexistente.
- `409`: referencia reutilizada con distinto contenido, conflicto de estado o inconsistencia.
- `422`: porciones insuficientes, inventario no configurado o producto inactivo.

## Integridad
- `@Transactional` atomicidad del registro y cambios de stock, con rollback en error.
- `PESSIMISTIC_WRITE` serializa cambios por `productoId` para evitar sobreventa.
- Confirmación convierte reservadas en vendidas; cancelación de pendientes libera.
- `referencia_operacion` única y firma SHA-256 para reintentos idempotentes.
- Los detalles guardan el precio unitario vigente en el momento de registrar el pedido.
- No se modela inventario por ingredientes ni recetas.

## Seguridad de la base de datos
El código nuevo introduce tres tablas. El archivo de migración está en
`database/migrations/2026_11_21_soa_apf3_pedidos_inventario.sql`.
**No ejecutar directamente sobre la base de Aiven** sin copia de seguridad, inspección de datos, permisos y ventana de migración. Si se usa `DDL_AUTO=validate` antes de aplicar las tablas a una base de desarrollo, el arranque fallará por diseño.

## Evidencias futuras, tras comprobar ejecución
- `SOA_APF3_01_Postman_Inventario_Configuracion.png`
- `SOA_APF3_02_Postman_Pedido_201.png`
- `SOA_APF3_03_Stock_Reservado.png`
- `SOA_APF3_04_Pedido_Idempotente_200.png`
- `SOA_APF3_05_Pedido_Confirmado.png`
- `SOA_APF3_06_Error_Stock_422.png`
- `SOA_APF3_07_Roles_403.png`

Estas capturas deben provenir del programa real; no inventarlas ni mostrar credenciales.
