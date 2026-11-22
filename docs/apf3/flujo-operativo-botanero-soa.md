# Delicias Peruanas · SOA APF3 · Flujo operativo inspirado en Botanero Ventas

## Alcance real del prototipo

Se ha conservado el **único backend modular Spring Boot** con endpoints REST/JSON y una base de datos compartida. Las capas y módulos representan servicios de negocio; no afirmar que hay microservicios independientes ni un ESB.

Se integraron: S01 identidad, S02 productos/carta, S03 pedidos, S04 reservas, S05 inventario de porciones y S06 ventas/caja. La notificación S07 continúa siendo una propuesta, no un servicio de envío real.

Los cambios están en `feature/soa-apf3-operacion-restaurante` y **no se desplegaron** sobre Aiven ni Render. El modo `soa-demo` usa una H2 temporal.

## Roles

| Rol | Acceso |
|---|---|
| CLIENTE | Consultar carta y solicitar reservas PENDIENTES para que las atienda un mozo; sin acceso al panel de cobros |
| MOZO | Ver mesas, registrar pedidos de MESA/LLEVAR, confirmar y consultar disponibilidad de platos |
| COCINERO | Consultar comandas y marcar cada línea como LISTO; no puede cobrar |
| CAJA | Registrar pedidos, confirmar, cobrar, generar ticket, consultar y reponer porciones |
| SUPERVISOR | Funciones de seguimiento y caja, inventario y cocina |
| ADMIN | Todas las operaciones, CRUD de platos/categorías, altas y bajas lógicas de usuarios, ajustes del local |
| OPERADOR | Compatibilidad de funcionalidades heredadas; no crear nuevas cuentas con este rol |

La autorización se verifica en **Spring Security**; los menús ocultos son solo presentación.

## Prueba guiada — Orden a mesa con dos platos

1. Inicia Spring Boot en modo demo desde `tools/Iniciar_SOA_Demo.ps1`.
2. Accede a `http://localhost:8081/soa-demo/index.html`.
3. Inicia sesión como MOZO: `mozo.demo@delicias.local` con la contraseña temporal.
4. Ve a **Pedidos**, selecciona `MESA`, Mesa #1. Elige Lomo Saltado (1 porción) y pulsa **+ Añadir plato a esta orden**; después añade Ají de Gallina (1 porción).
5. Comprueba los dos platos en el carrito y pulsa **Enviar pedido a Cocina** (POST `/api/v1/pedidos/envio`, HTTP 201). Esta operación atómica **registra y confirma** la orden; no hace falta pulsar otro botón. La Mesa #1 queda OCUPADA. El mismo flujo puede probarse vía Postman con `{"tipoAtencion":"MESA","mesaId":1,"items":[{"productoId":1,"cantidad":1},{"productoId":2,"cantidad":1}]}` y cabecera `Idempotency-Key`.
6. En **Pedidos**, haz clic en la tarjeta **Mesa #1 · OCUPADA · Ver comanda**. Comprueba el visor: platos, preparación, tiempos, subtotales, total y botones **Agregar platos / bebidas**, **Marcar ENTREGADO** (solo cuando Cocina los marque LISTO) y **Cancelar orden** (solo antes de que Cocina comience). No aparece **Cobrar** para el Mozo. El ID y las operaciones HTTP avanzadas se encuentran en **Opciones técnicas APF3**.
7. Inicia sesión COCINERO: `cocinero.demo@delicias.local`. En **Cocina**, marca cada uno de los platos por separado como LISTO.
8. Inicia sesión CAJA: `caja.demo@delicias.local`. En **Caja y cobros**, selecciona el pedido, escribe el monto recibido y cobra.
9. Se abrirá un **ticket de venta interno**, con platos, total, monto recibido y cambio. En el ticket, elige `Imprimir o guardar como PDF`, y selecciona impresora `Microsoft Print to PDF`.
10. Regresa a Pedidos: la Mesa #1 vuelve a DISPONIBLE, y el resumen de ventas incluye el pago.

**No es una boleta electrónica SUNAT.** El número INT es solo un identificador de comprobante interno académico.

## Prueba guiada — Pedido libre para llevar

Inicia sesión como MOZO o CAJA. En **Pedidos** selecciona `LLEVAR` y agrega uno o varios productos con stock. Pulsa **Enviar pedido a Cocina**; el pedido se confirma sin `mesaId`, no ocupa ninguna mesa y aparece en **Órdenes en atención**. Selecciona **Ver comanda / total** para abrir su detalle, ver su estado y añadir platos. Sigue el mismo flujo Cocina → Mozo (entrega) → Caja.


## Prueba guiada — Mesa separada con ID y horario

1. Entra como MOZO en **Reservas de mesas**. En el formulario introduce el nombre del cliente, por ejemplo "Cliente de práctica", fecha futura, Mesa ID 2, horas 19:00–20:00 y dos personas; registra la solicitud.
2. La sección **Mesas separadas · agenda de reservas** muestra una tarjeta **RESERVA #ID**, **Mesa #2**, cliente, fecha, horario, personas y estado real **PENDIENTE**. Debajo queda el historial en tabla con ID y acción autorizada de cancelar.
3. Si la reserva corresponde al día actual, la cuadrícula de **Pedidos** indica debajo de la mesa **RESERVA #ID · 19:00–20:00**, sin marcarla ocupada automáticamente.
4. Prueba un horario solapado sobre la misma mesa y fecha: debe responder **HTTP 409**, y no duplicar la reserva.
5. PENDIENTE significa **solicitud registrada, no reserva confirmada**. El estado OCUPADA corresponde a un pedido de atención activo y es independiente del horario de la reserva. No afirmaremos que existe un proceso de confirmación de reservas si no se implementó.

## Cambio en la interfaz del Mozo

- Selecciona cualquier mesa: si está **DISPONIBLE** aparece el formulario **Tomar orden**, con buscador de platos, carrito y botón único **Enviar pedido a Cocina**.
- Si está **OCUPADA** sigue siendo seleccionable, pero abre su comanda existente (evita crear un segundo pedido). Se ven platos, estado PENDIENTE/LISTO/ENTREGADO, tiempo y total.
- Para **LLEVAR**, se crea una orden sin mesa y se abre con **Ver comanda / total**.
- Se sincronizan pedidos, estados de mesa y agenda de reservas cada cinco segundos mediante consultas REST. No es WebSocket ni tiempo real instantáneo.
- Para mostrar la idempotencia HTTP 200 o confirmar una orden antigua PENDIENTE, abre la sección plegable **Opciones técnicas APF3**.
- La cancelación de CONFIRMADO se admite únicamente para personal autorizado si **todos** sus platos están todavía PENDIENTES de preparación. Si alguno está LISTO o ENTREGADO, el servidor responde 409 y se necesita una devolución supervisada que no está implementada en este prototipo.

**Evidencias sugeridas:** `SOA_APF3_45_Mesa_Ocupada_Comanda.png`, `SOA_APF3_46_Tomar_Orden_Un_Paso.png`, `SOA_APF3_47_Orden_Para_Llevar.png`, `SOA_APF3_48_Reservas_Tarjetas_ID_Horario.png` (capturas reales desde el navegador y sin JWT visible).

## Disponibilidad y porciones

Al elegir un plato en Pedidos, el frontend consulta `GET /api/v1/inventario/{productoId}`. Con disponibilidad cero se deshabilita el registro; el backend también rechaza pedidos sobrevendidos con HTTP 422 bajo bloqueo transaccional.

En **Inventario**, CAJA, SUPERVISOR o ADMIN pueden usar `PATCH /api/v1/inventario/{productoId}/reponer` con `{"cantidad":15}`. Se suma a las porciones **terminadas**, no a ingredientes. Solo ADMIN puede reconfigurar el total absoluto mediante PUT.

## Productos y usuarios

- **Productos:** crear, editar, consulta y baja lógica mediante DELETE (establece `disponible=false`), sin destruir pedidos históricos.
- **Categorías:** creación, consulta, edición y eliminación solo si no contienen productos; con productos devuelve 409.
- **Usuarios:** altas con contraseña BCrypt, edición de nombre/rol/teléfono, reactivación y baja lógica; jamás se devuelve la contraseña al frontend. Administrador no puede quitarse a sí mismo su rol ni desactivar su cuenta activa.
- **Configuración:** nombre, teléfono y ruta del logotipo dentro de `/portal/images/`. Primero hay que copiar el archivo real. No hay subida remota automática de fotos.

## Datos de demostración

- Seis usuarios: admin.demo, cliente.demo, mozo.demo, caja.demo, cocinero.demo y supervisor.demo, todos con dominio `@delicias.local`.
- Para simplificar la sustentación, **los seis utilizan una única contraseña temporal elegida al arrancar**. No compartas ni registres esa contraseña en las capturas.
- 15 mesas.
- 22 productos basados en la carta antigua, conservando Lomo S/25, Ají S/22 y Chicha Morada S/8 como datos del prototipo previo. Los demás precios provenientes de Botanero son **históricos y referenciales**, no tarifas comerciales actuales.
- Los artículos cuya carta antigua no indicaba precio se guardan no disponibles y no se pueden pedir.

## Fotografías faltantes

Los fondos «nublosos» de Inicio y Platos Destacados son las alternativas CSS cuando faltan `portada.jpg`, `rocoto.jpg`, `ceviche.jpg` y otros. **Las fotos no están subidas a GitHub**.

El script `tools/Importar_Fotos_APF3.ps1` puede extraer imágenes desde el ZIP optimizado que descargaste a `D:\DESCARGAS`. Antes de ejecutarlo, valida el nombre real del ZIP. El script conserva archivos existentes, no borra originales ni sobrescribe salvo solicitud explícita.

Ejemplo:

```powershell
cd "D:\CICLO AGOSTO 2026\PROYECTO_DELICIAS_PERUANAS"
& ".\tools\Importar_Fotos_APF3.ps1" -ArchivoZip "D:\DESCARGAS\APF3_Fotografias_Carta_Botanero_Original.zip"
Test-Path ".\backend\src\main\resources\static\portal\images\portada.jpg"
```

La fotografía principal se verá en `http://localhost:8081/portal/index.html` cuando el script devuelva True. **Si no encuentra el ZIP, no inventar fotografías ni guardar capturas de la web como si estuviesen integradas.**

## Migración a MySQL productivo

Necesita primero `2026_11_21_soa_apf3_pedidos_inventario.sql` y luego `2026_11_21_soa_apf3_operacion_mesa_cocina_caja.sql`. Es necesaria una copia de seguridad, inspección manual y entorno de ensayo. **No ejecutar por ahora en Aiven**: la demostración H2 permite completar la sustentación sin arriesgar la base en producción.

## Evidencias recomendadas (obtener únicamente del sistema nuevo)

| Archivo | Qué mostrar |
|---|---|
| `SOA_APF3_27_Mesas_Disponibles.png` | Listado de las 15 mesas antes de ocupar |
| `SOA_APF3_28_Pedido_Mesa_201.png` | Pedido MESA registrado y mesa OCUPADA |
| `SOA_APF3_29_Pedido_Llevar_201.png` | Pedido para llevar con `mesaId:null` |
| `SOA_APF3_30_Cocina_Platos_Listos.png` | Una línea LISTO y otra PENDIENTE |
| `SOA_APF3_31_Caja_Cobro_Cambio.png` | Cobro efectivo con vuelto calculado |
| `SOA_APF3_32_Ticket_Interno_PDF.png` | Ticket impreso/guardado en PDF |
| `SOA_APF3_33_Reposicion_Porciones.png` | CAJA suma +15 porciones preparadas |
| `SOA_APF3_34_Dashboard_Ventas.png` | Gráfico real tras el cobro |

Guarda en `evidencias/SOA/APF3`, sin fotos de credenciales, sin reutilizar capturas del sistema histórico como si demostraran la API nueva.
