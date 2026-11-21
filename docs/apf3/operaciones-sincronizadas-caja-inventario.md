# Delicias Peruanas | SOA APF3 — Operación sincronizada

## Resumen verificable

Rama: \`feature/soa-apf3-operacion-restaurante\` · PR #80.

Sistema monolítico modular Spring Boot con servicios REST/JSON S01 Identidad, S02 Productos, S03 Pedidos/Cocina, S04 Reservas, S05 Inventario y S06 Ventas/Caja. **No son microservicios físicos independientes**, ni se instaló un ESB. Perfil de demostración H2 temporal y puerto 8081. No se modificó producción de Aiven ni Render.

## Flujos por rol

**MOZO** registra un pedido \`MESA\` (ocupa la mesa) o \`LLEVAR\` (sin mesa), confirma y consulta su estado. El panel de **Órdenes en atención** muestra el pedido, subtotal por línea, monto total, estado, tiempo de espera y botones permitidos. Puede añadir platos a un pedido PENDIENTE o CONFIRMADO mediante un POST con \`Idempotency-Key\`. Puede cancelar solo PENDIENTE. Un plato marcado LISTO por Cocina se puede marcar ENTREGADO. **Mozo no cobra**.

**COCINERO** consulta las comandas PENDIENTES y CONFIRMADAS. Cuando la cocina prepara un plato, lo marca LISTO y se guarda \`fechaListo\`. No puede entregar, modificar stock ni cobrar.

**CAJA y SUPERVISOR** ven pedidos CONFIRMADOS y PENDIENTES, registran pagos de EFECTIVO, YAPE o POS cuando todos los platos están LISTOS o ENTREGADOS. EFECTIVO exige monto recibido suficiente y calcula vuelto. YAPE/POS exigen referencia de operación manual de 4–80 caracteres y registran vuelto cero; no hay integración con bancos ni pasarela. El pago deja la orden PAGADA, libera la mesa y crea ticket **interno académico**, no boleta SUNAT.

**ADMIN** ve todas las cantidades por producto en una tabla: preparado, reservado, vendido y disponible. Puede reponer, modificar el total y registrar salida justificada (motivo de al menos cinco caracteres), sin retirar existencias comprometidas. Las entradas, salidas y ajustes quedan en \`movimientos_inventario\` con usuario/fecha/motivo. CAJA y SUPERVISOR reponen y consultan auditoría; MOZO solo consulta stock, no lo modifica.

**CLIENTE** ve carta pública y solicita reservas PENDIENTES. Las reservas son solicitudes, no asignaciones automáticas confirmadas. Solo el personal autorizado consulta el listado global. Superposición de horarios devuelve 409. No se expone ese listado al cliente.

## Sincronización

El nuevo \`operaciones-extra.js\` ejecuta sondeos REST cada **5 segundos** mientras se visualiza el panel, y actualiza la vista activa sin recargar el navegador (polling, **no WebSocket**, eventual consistencia hasta el siguiente sondeo). No guarda ni expone JWT. Se detiene al cerrar sesión. Los contadores de espera se actualizan por separado cada segundo, basados en marcas temporales del backend.

La comanda **PAGADA desaparece de la lista ACTIVA de Cocina y Mozo** en el siguiente sondeo; no se borra su registro. El historial permanece en Pedidos/Ventas con ticket, fecha, monto y método de pago.

## Cuadre de caja y reportes

\`GET /api/v1/ventas/resumen?fecha=AAAA-MM-DD\` devuelve ingresos de la fecha, importes EFECTIVO/YAPE/POS, ventas del mes de esa fecha, total histórico, ventas por día/mes, operaciones de la fecha, y cuadre registrado si existe. Para arqueo, efectivo esperado = suma de importes de las ventas cobradas en EFECTIVO, no monto recibido bruto incluyendo vuelto. Se introduce el efectivo físicamente contado y se guarda un cuadre único por fecha mediante \`POST /api/v1/ventas/cuadres\`. El cuadre es una foto del estado al registrarlo, no una conciliación bancaria. **No modela fondo inicial, retiros, gastos ni operaciones bancarias reales**, por lo que no debe presentarse como una caja comercial completa.

La venta registrada es persistente si se ejecuta con MySQL y se aplican migraciones controladas; en el perfil \`soa-demo\` H2 in-memory **toda la historia temporal se pierde al reiniciar**. Tomar evidencias antes del reinicio.

## REST incorporado

| Método | Endpoint | Rol / finalidad |
|---|---|---|
| GET | \`/api/v1/pedidos\` | Personal, lista de órdenes; CLIENTE solo las propias |
| POST | \`/api/v1/pedidos/{id}/items\` | Personal autorizado; agregar platos en orden abierta con clave idempotente |
| POST | \`/api/v1/pedidos/{id}/detalles/{detalleId}/entregado\` | MOZO, SUPERVISOR, ADMIN |
| POST | \`/api/v1/pedidos/{id}/detalles/{detalleId}/listo\` | COCINERO, SUPERVISOR, ADMIN |
| GET | \`/api/v1/inventario\` | Personal autorizado; tabla por producto |
| GET | \`/api/v1/inventario/movimientos\` | ADMIN, CAJA, SUPERVISOR |
| PATCH | \`/api/v1/inventario/{productoId}/salida\` | Solo ADMIN, \`{cantidad, comentario}\` |
| PATCH | \`/api/v1/inventario/{productoId}/reponer\` | CAJA, SUPERVISOR, ADMIN |
| POST | \`/api/v1/ventas/pedidos/{id}/cobrar\` | CAJA, SUPERVISOR, ADMIN. \`{medioPago,montoRecibido,referenciaPago}\` |
| GET | \`/api/v1/ventas/resumen?fecha=...\` | Caja/Supervisor/Admin, venta diaria y mensual |
| POST | \`/api/v1/ventas/cuadres\` | Caja/Supervisor/Admin, registro de arqueo \`{fecha, efectivoContado}\` |
| GET | \`/api/v1/ventas/cuadres\` | Caja/Supervisor/Admin, historial de arqueos |

## Ejemplo de secuencia para sustentar

1. MOZO registra Pedido #1 en Mesa 1 con Lomo Saltado + Ají de Gallina. La mesa queda OCUPADA. Pedidos muestra ambos platos y total.
2. Abre otra pestaña en sesión CAJA (con JWT distinto) y observa el pedido tras la siguiente sincronización.
3. MOZO agrega un tercer plato al pedido aún abierto. El importe aumenta. Otra adición con la misma \`Idempotency-Key\` devuelve 200, sin duplicar ítems.
4. COCINERO abre Cocina y marca uno de los tres como LISTO. Los demás siguen PENDIENTES. El tiempo transcurrido muestra espera real.
5. MOZO marca ENTREGADO el plato LISTO. No puede cobrar. CAJA tampoco puede cobrar hasta que todos los platos estén LISTOS o ENTREGADOS.
6. CAJA paga por YAPE con referencia manual o EFECTIVO con dinero recibido. El ticket registra método. La mesa se libera. Tras el siguiente sondeo, la comanda ya no aparece en Cocina.
7. SUPERVISOR selecciona fecha y ve ventas de día, mes y el desglose por medios. Guarda el cuadre con efectivo contado.
8. ADMIN revisa stock tabular, agrega 15 porciones y registra salida de 2 porciones con motivo; el movimiento queda auditado. MOZO ve el stock actualizado sin pulsar F5.
9. MOZO abre Reservas y verifica el listado de solicitudes y sus fechas/horarios. Una reserva solapada devuelve 409.

## Capturas nuevas sugeridas

Guardar en \`D:\CICLO AGOSTO 2026\PROYECTO_DELICIAS_PERUANAS\evidencias\SOA\APF3\`:

- \`SOA_APF3_35_Stock_Tabla_Admin.png\` — estado global del inventario, cantidades de cada producto.
- \`SOA_APF3_36_Salida_Stock_Con_Motivo.png\` — salida auditada y reducción del disponible.
- \`SOA_APF3_37_Mozo_Pedido_Mesa_En_Atencion.png\` — Mesa 1 ocupada, líneas y total.
- \`SOA_APF3_38_Mozo_Entrega_Platos_Con_Tiempo.png\` — mezcla LISTO/PENDIENTE/ENTREGADO y horas.
- \`SOA_APF3_39_Cocina_Comandas_Actualizadas.png\` — comanda y tiempos reales.
- \`SOA_APF3_40_Caja_Metodo_Yape_POS.png\` — cobro por otro medio, sin contraseñas ni referencias bancarias reales.
- \`SOA_APF3_41_Cuadre_Caja_Diario.png\` — suma por medio, efectivo esperado y contado.
- \`SOA_APF3_42_Ventas_Mensuales_Historico.png\` — importes mensuales y operaciones por fecha.
- \`SOA_APF3_43_Reservas_Listado_Mozo.png\` — solicitudes y estado con horario.
- \`SOA_APF3_44_Carta_Productos_Stock.png\` — nombre, precio, porciones disponibles y estado.

No tomar capturas del código anterior Botanero como funcionamiento del sistema nuevo. No mostrar JWT, contraseñas, teléfonos privados ni referencias de cobro reales.

## Despliegue seguro

Migraciones nuevas: \`database/migrations/2026_11_21_soa_apf3_auditoria_caja_tiempos.sql\` después de \`2026_11_21_soa_apf3_operacion_mesa_cocina_caja.sql\` y \`2026_11_21_soa_apf3_pedidos_inventario.sql\`. **Ninguna se ejecutó en producción.** Revisar tablas existentes, respaldo y entorno de ensayo. No fusionar la rama PR #80 sin completar pruebas visuales.
