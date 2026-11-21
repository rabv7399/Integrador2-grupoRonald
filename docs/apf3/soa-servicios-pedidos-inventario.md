# APF3 SOA · Prototipo transaccional de Pedidos S03 e Inventario S05

**Estado:** código en rama `feature/soa-apf3-pedidos-inventario`, pendiente de ejecutar contra una base de desarrollo. No se ha aplicado ninguna migración a Aiven.

## Inicio de sesión y panel de roles (estilo flujo de Botanero Ventas)

El módulo `/soa-demo/index.html` abre primero **una pantalla de inicio de sesión**, no el menú operativo. El usuario ingresa correo y contraseña por `POST /api/auth/login`. Si el JWT es válido y el perfil está reconocido, se abre una segunda vista: **el escritorio de Delicias Peruanas**, con navegación lateral. No se guarda el token en localStorage, sessionStorage ni en el código fuente: solo en memoria hasta cerrar la vista o recargar la página.

| Perfil | Visibilidad del menú | Permiso clave |
|---|---|---|
| CLIENTE | Resumen, Carta, Pedidos, Reservas, Laboratorio REST | Registrar pedidos propios y reservas |
| MOZO | Anteriores + Inventario | Consultar stock y confirmar pedidos |
| SUPERVISOR | Anteriores + Inventario | Seguimiento operativo y confirmación |
| ADMIN | Todos los módulos | Configurar porciones y gestionar el resto |
| OPERADOR | Compatibilidad con cuenta de legado | Operaciones de personal, sin configuración de porciones |

**Seguridad real:** además del menú, Spring Security exige roles en los endpoints. `PUT /api/v1/inventario/{id}` está restringido a ADMIN; `GET /api/v1/inventario/{id}` a personal; `POST /api/v1/pedidos/{id}/confirmacion` a personal. En reservas se restringe la lectura global y la cancelación al personal, mientras que el cliente puede registrar una reserva nueva. El registro público sigue asignando exclusivamente CLIENTE. No se ha añadido todavía una función administrativa para crear usuarios de personal en producción.

## Modo local de demostración sin Aiven

El proyecto incorpora un perfil `soa-demo` con base H2 **en memoria**. Este perfil **NO se usa en Render**, no consulta la base de Aiven y descarta los registros de demostración al detener el servidor.

1. En VS Code abre `D:\CICLO AGOSTO 2026\PROYECTO_DELICIAS_PERUANAS` y asegúrate de tener una copia limpia.
2. Actualiza `feature/soa-apf3-pedidos-inventario` desde GitHub.
3. En PowerShell desde la raíz ejecuta:

   ```powershell
   & "$env:WINDIR\System32\WindowsPowerShell\v1.0\powershell.exe" -NoProfile -ExecutionPolicy Bypass -File ".\tools\Iniciar_SOA_Demo.ps1"
   ```

4. El script pide **cuatro contraseñas temporales diferentes** (8 caracteres mínimo): ADMIN, CLIENTE, MOZO y SUPERVISOR; genera un JWT secreto efímero y activa Spring Boot en el puerto 8081.
5. Abre `http://localhost:8081/soa-demo/index.html`, inicia sesión y comprueba que el navegador muestra solo el panel autorizado. Para volver al acceso, pulsa **Salir**.
6. Elige uno de estos correos; utiliza la contraseña temporal que acabas de asignar a su perfil:

   - `cliente.demo@delicias.local` — CLIENTE
   - `mozo.demo@delicias.local` — MOZO
   - `supervisor.demo@delicias.local` — SUPERVISOR
   - `admin.demo@delicias.local` — ADMIN

7. No uses contraseñas reales ni variables de Aiven/Render. Para detener el programa usa `Ctrl+C`. Los datos H2 se reinician al siguiente arranque.

Los datos de prueba incluyen Lomo Saltado (S/ 25, 30 porciones), Ají de Gallina (S/ 22, 20 porciones), Chicha Morada (S/ 8, 25 porciones), Anticuchos (S/ 18, 12 porciones) y Mesa 1 (4 personas). Son **datos didácticos**, no disponibilidad comercial.

### Capturas del APF3 SOA — obtener del programa real

| Nombre de captura | Pantalla o proceso |
|---|---|
| `SOA_APF3_01_Login_Inicial.png` | Pantalla de acceso antes de introducir contraseñas |
| `SOA_APF3_02_Panel_Cliente.png` | CLIENTE con su menú y permisos |
| `SOA_APF3_03_Panel_Mozo.png` | MOZO con lectura de inventario |
| `SOA_APF3_04_Panel_Supervisor.png` | SUPERVISOR con sus módulos |
| `SOA_APF3_05_Panel_Admin.png` | ADMIN con acceso a inventario |
| `SOA_APF3_06_Catalogo_REST.png` | Consulta S02 desde el sistema |
| `SOA_APF3_07_Inventario_Porciones.png` | S05: preparado, reservado, vendido |
| `SOA_APF3_08_Pedido_HTTP_201.png` | Creación S03 |
| `SOA_APF3_09_Idempotencia_HTTP_200.png` | Reintento con Idempotency-Key |
| `SOA_APF3_10_Error_Stock_HTTP_422.png` | Rechazo de pedido sin porciones |
| `SOA_APF3_11_Acceso_Restringido_HTTP_403.png` | Prueba de permisos del backend |
| `SOA_APF3_12_Reserva_Conflicto_HTTP_409.png` | S04 con solapamiento de horarios |
| `SOA_APF3_13_Confirmacion_Stock.png` | Confirmación S03 y venta de porciones S05 |

Guarda las capturas en `evidencias/SOA/APF3/` y nunca incluyas tokens completos, contraseñas ni datos privados.

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

