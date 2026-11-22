# Delicias Peruanas — SOA APF3: cocina, caja, inventario y carta pública

## Entorno y alcance

Rama de trabajo `feature/soa-apf3-operacion-restaurante`, PR #80. La aplicación es un **monolito modular Spring Boot** con APIs REST/JSON, roles JWT y base compartida. **NO se han desplegado estos cambios a Render o Aiven.** Perfil `soa-demo`: H2 en memoria; las fotos, pedidos y ventas de la demostración desaparecen al reiniciar.

## Comandas y autorización

- **Cocina**: ve solo pedidos CONFIRMADOS que todavía tengan al menos una línea **PENDIENTE**. Únicamente la línea PENDIENTE muestra **Marcar LISTO**. Una línea LISTO/ENTREGADO nunca vuelve a presentar ese botón.
- Si Cocina terminó todas las líneas, la comanda desaparece de **la cola de preparación**, aunque aún no esté pagada. **No se elimina de Pedidos**; Caja la ve pendiente de pago y Mozo puede consultar/entregar platos.
- **ENTREGADO es exclusivo de MOZO**, tanto en la interfaz como en el backend (Spring Security + validación de rol). Caja, Supervisor y Administrador no pueden registrar entregas. Pueden consultar/registrar pedidos si su rol lo permite.
- **Pedidos** es un único módulo reutilizado para MOZO, CAJA, SUPERVISOR y ADMIN: modalidad MESA/LLEVAR, mesa ocupada/comanda, productos, stock, adiciones, total y estados. Los permisos de ENTREGADO son distintos.

## Caja: medio de pago y recargo POS

Medios: **EFECTIVO**, **YAPE**, **POS** y **TRANSFERENCIA**. Los métodos digitales requieren una referencia de operación manual; el prototipo no valida pagos reales ante bancos.

La API `GET /api/v1/ventas/pedidos/{pedidoId}/cotizacion?medioPago=POS` muestra **subtotalBase**, **recargoPos** y **totalCobro** antes de confirmar el pago.

La regla de demostración POS es:

`totalPOS = techo((subtotal * 1.04) / 0.50) * 0.50`.

**Ejemplo:** subtotal S/ 26.00 → con 4% S/ 27.04 → total POS **S/ 27.50**. Recargo total **S/ 1.50**. Si el subtotal es S/ 21.26, con la comisión el bruto es S/ 22.1104 y se cobra S/ 22.50. La regla es **redondeo hacia arriba**, nunca redondeo al más cercano.

- Antes de cobrar POS se muestra la cotización, la comisión y una **casilla de aceptación**. El backend rechaza POS sin aceptación (`RECARGO_POS_NO_ACEPTADO`, HTTP 400).
- Los precios unitarios y la orden original **NO cambian**; el ticket y la venta guardan subtotalBase, recargoPos, total cobrado y medio de pago.
- Efectivo calcula cambio; Yape, POS y Transferencia tienen cambio cero.
- El cuadre diario muestra por separado Efectivo, Yape, POS y Transferencia. La suma de sus importes coincide con **ventasFecha** (ingresos brutos realmente cobrados, recargo POS incluido). Además muestra **recargosPosDia** para separar ese concepto.
- El arqueo físico de efectivo usa únicamente ventas EFECTIVO, no movimientos digitales.
- Para informar al cliente, la carta pública y la pantalla de tomar pedidos muestran una nota visible sobre el eventual recargo POS. Según el artículo 7 del Código peruano de Protección y Defensa del Consumidor, una diferencia por método debe comunicarse previamente de modo visible y accesible. No convertir el prototipo en cobro comercial real sin revisar la política y obligaciones aplicables.

## Inventario

Todos los roles autorizados (MOZO, CAJA, SUPERVISOR, ADMIN) acceden a **la misma tabla** del stock actual (preparadas, reservadas, porciones en pedidos confirmados, salidas manuales acumuladas y disponibles). Solo ADMIN configura el total; **ADMIN, CAJA y SUPERVISOR** pueden reponer y dar salida a porciones con un comentario obligatorio. MOZO solo consulta y no modifica stock. El backend impide descontar porciones ya comprometidas.

### Aclaración: porciones confirmadas vs. ventas cobradas

- **En pedidos confirmados**: stock ya comprometido con comandas, que puede incluir pedidos NO cobrados; su valor no debe sumarse a los ingresos del día.
- **Salidas manuales acumuladas**: total de porciones retiradas o mermadas, auditadas por causa/usuario en movimientos de inventario. NO equivalen a ventas. La columna se ve para los roles autorizados a consultar Inventario; el detalle con comentarios/usuario queda restringido a CAJA, SUPERVISOR y ADMIN.
- Para controlar el dinero, usar únicamente **Ventas del día**, que suma los pedidos PAGADOS; el sistema conserva separados los ajustes de stock.

## Filtro de fecha y tipo en el historial de Inventario

En el módulo **Inventario → Historial de movimientos** (acceso a detalle de auditoría solo **ADMIN, CAJA y SUPERVISOR**) se agregaron:
- Selector de **fecha del movimiento**; vacío = movimientos recientes.
- Selector de tipo: **Todos**, **Solo salidas**, **Solo entradas**, **Solo ajustes**.
- Botón **Consultar movimientos**, búsqueda automática al cambiar filtros y **Limpiar filtros**.
- Resumen visible: número de movimientos, número de SALIDAS y suma de porciones retiradas manualmente en el resultado.
- Cada movimiento muestra fecha/hora, producto, tipo, cantidad, comentario y usuario. Los movimientos se ordenan del más reciente al más antiguo.
- Filtro **en el servidor** para recuperar también fechas antiguas más allá de los 200 movimientos recientes; sin filtro se conserva la vista previa de últimos 200.

**Contrato REST:** `GET /api/v1/inventario/movimientos?fecha=AAAA-MM-DD&tipo=SALIDA`; ambos parámetros opcionales, tipo `TODOS|SALIDA|ENTRADA|AJUSTE`. Tipos desconocidos devuelven HTTP 400. La seguridad REST restringe este recurso; MOZO recibe HTTP 403 incluso cuando indica una fecha, y un visitante anónimo HTTP 401.

Importante: este historial registra **entradas, salidas manuales y ajustes**, no es el historial monetario de las ventas. Los platos facturados se consultan en `Ventas del día`. Durante la demo `soa-demo`, H2 se reinicia con el servidor; para consultar fechas reales de meses anteriores se requiere base persistente.

Evidencia real propuesta:
- `SOA_APF3_62_Historial_Salidas_Por_Fecha.png`: rol CAJA/ADMIN/SUPERVISOR, tipo SALIDA, fecha seleccionada, resultado con usuario y motivo.
- `SOA_APF3_63_Historial_Todas_Entradas_Salidas.png`: tipo TODOS y comparación con entradas/ajustes.
- Opcional: mostrar otra fecha sin resultados para evidenciar el filtro.

Prueba automatizada: salida y reposición reales, tipos SALIDA/ENTRADA, fecha con movimientos, fecha sin movimientos, 400 por tipo inválido, 403 Mozo y 401 anónimo.

## Error corregido: importe obsoleto tras cobrar

La lista de Caja puede quedar sin pedidos activos luego de un cobro, pero antes conservaba el ID del último pedido y su monto. Ahora:
1. La lista distingue órdenes **LISTAS PARA COBRAR** de órdenes que esperan Cocina. Solo permite seleccionar las primeras.
2. Al cobrar, limpia el ID, el monto recibido, el código de operación, la aceptación POS y la cotización. Deshabilita el botón hasta seleccionar otra orden lista.
3. El ticket histórico tiene un **campo independiente de consulta por ID** para no confundirlo con un cobro pendiente.
4. `GET /api/v1/ventas/pedidos/{id}/cotizacion` rechaza órdenes PAGADAS con HTTP 409 `PEDIDO_YA_PAGADO`, evitando que la interfaz siga mostrando como deuda un importe liquidado.

## Nueva pestaña: Ventas del día

Menú visible para **CAJA, SUPERVISOR y ADMIN**. Filtro por fecha, lista de cada producto vendido, cantidad de unidades, precio unitario guardado al registrar cada pedido y subtotal del producto. Las ventas de un mismo producto con precios históricos diferentes aparecen en líneas distintas, evitando reescribir ventas anteriores tras un cambio de precio. Se suman únicamente tickets cobrados.

Totales: subtotal de platos, recargos POS, **TOTAL FINAL COBRADO**, cuatro medios de pago, unidades y tickets. La API `GET /api/v1/ventas/detalle-diario?fecha=AAAA-MM-DD` devuelve `lineas`, `subtotalPlatos`, `recargosPos`, `totalCobrado`, `porMedioPago` y `cuadreDetalle`.

La fecha comercial se interpreta usando la zona horaria **America/Lima** incluso si la aplicación se ejecuta en servidores de otra zona. En la demo local H2, solo persiste hasta detener/reiniciar el servidor.

**Ecuación de verificación:** suma subtotales por producto + recargos POS = total de ingresos del día; Efectivo + Yape + POS + Transferencia = mismo total. Mermas y salidas NO se contabilizan como ingreso.

## CRUD por plato: observaciones, cantidades y anulación individual (Mozo)

Cada línea de una orden tiene ID propio, cantidad, precio fijado cuando se creó, observación opcional de Cocina y estado `PENDIENTE`, `LISTO`, `ENTREGADO` o `ANULADO`. Ejemplo real de demostración: **Mesa #1 · 2 × Lomo Saltado**, con dos líneas independientes: «Bajo en sal» y «Sin cebolla». El backend separa líneas que tengan el mismo producto pero **distinta observación**. La observación viaja junto con `POST /api/v1/pedidos/envio`, por lo que Cocina la recibe desde el momento del envío.

- **Crear:** seleccionar mesa o PARA LLEVAR, producto, cantidad y campo «Observación para Cocina». Al agregar al carrito la nota queda ligada a la línea. También aplica a adiciones del mismo pedido.
- **Consultar:** Mozo ve estado, nota, precio y total por línea; Cocina ve la nota resaltada en las comandas.
- **Editar:** el Mozo puede guardar una observación nueva o cambiar cantidad de **una sola línea PENDIENTE**. El backend calcula la diferencia de porciones y recalcula subtotal/total dentro de la misma transacción; mantiene el precio unitario original, sin usar precios actualizados de la carta.
- **Anular un plato:** Mozo indica motivo (5–300 caracteres) y confirma. La línea pasa a `ANULADO`, conserva ID, motivo, autor y fecha; baja el total de la orden y devuelve las porciones comprometidas al stock. **No se borra físicamente**, para mantener auditoría. Caja no considera esa línea en tickets ni ventas del día y Cocina no vuelve a prepararla.
- **Última línea de la mesa:** si Mozo anula el único plato activo, la orden entera pasa a `CANCELADO`, se libera la mesa y el detalle conserva motivo, autor y fecha. No se crea una orden abierta de total S/ 0.
- **Seguridad y restricciones:** solo rol **MOZO** puede editar/anular líneas, verificadas en Spring Security y en la capa de negocio. `LISTO`, `ENTREGADO` y `ANULADO` no admiten edición/anulación (HTTP 409); una orden `PAGADO` o `CANCELADO` tampoco. El cobro y los recargos POS se calculan sobre el total vigente, descontando las anulaciones.
- **Idempotencia:** observaciones diferentes producen firma distinta de operación; repetir una misma clave con notas cambiadas responde 409. Dos preparaciones diferentes de un mismo producto no se fusionan.

Contratos REST nuevos:
| Método | Recurso | Cuerpo de ejemplo | Permisos |
|---|---|---|---|
| PATCH | `/api/v1/pedidos/{id}/detalles/{detalleId}/observacion` | `{"observacion":"Bajo en sal"}` | MOZO |
| PATCH | `/api/v1/pedidos/{id}/detalles/{detalleId}/cantidad` | `{"cantidad":2}` | MOZO |
| DELETE | `/api/v1/pedidos/{id}/detalles/{detalleId}` | `{"motivo":"Cliente cambió de opinión"}` | MOZO |

Migración para MySQL pendiente de aprobación y respaldo: `database/migrations/2026_11_22_soa_apf3_observaciones_anulacion_platos.sql`. NO aplicada a Aiven/Render; H2 `soa-demo` crea el esquema temporalmente.

### Guion de evidencia por plato

1. Entrar con MOZO, escoger Mesa #1 disponible y añadir **Lomo Saltado** con observación «Bajo en sal» y otro con «Sin cebolla». Enviar a Cocina.
2. Abrir la comanda de Mesa #1 y comprobar dos líneas distintas y sus observaciones; en COCINERO comprobar que también ve las notas.
3. Como MOZO cambiar la cantidad de uno de los lomos de 1 a 2, verificar total y stock; volver a 1.
4. Anular **solo un** Lomo con motivo, observar que ese plato queda `ANULADO` y los otros siguen en la orden. Verificar la porción devuelta en Inventario y el nuevo total.
5. Marcar los otros platos LISTO desde Cocina; comprobar que el plato anulado **no** ofrece el botón. Cobrar desde Caja y verificar que el ticket y la venta del día **no incluyen** la línea anulada.
6. Intentar cambiar un plato LISTO o realizar la anulación desde CAJA: debe fallar (409 / 403). No editar estados desde navegador ni falsificar evidencia.

Evidencias nuevas: `SOA_APF3_58_Observaciones_Por_Plato_Mozo.png`, `SOA_APF3_59_Nota_Visible_Cocina.png`, `SOA_APF3_60_Anulacion_Parcial_Stock_Total.png`, `SOA_APF3_61_Cobro_Sin_Plato_Anulado.png`.

## Producto CRUD e imagen pública

- **Carta y productos** como módulo del panel interno solo está en ADMIN. El cliente accede a la carta virtual pública a través de `/portal/index.html#carta`.
- **Productos · CRUD** es exclusivo de ADMIN para crear, modificar nombre, descripción, código, precio, categoría, disponibilidad y dar de baja lógica.
- ADMIN puede **subir una fotografía JPG o PNG (máximo 1.5 MB y 64–5000 px por lado)** después de guardar el producto. Se valida formato/dimensiones en el servidor y se guarda en la base de datos activa en `producto_imagenes`. El producto publica `imagenUrl`; `GET /api/productos/{id}/imagen` es público; POST de imagen es exclusivo de ADMIN.
- La web `/portal/index.html` utiliza el catálogo del backend como carta ACTUAL por defecto, muestra fotografías publicadas, precios y nombres editados, oculta productos dados de baja y actualiza cada 15 segundos con la página abierta. También actualiza los tres destacados de su sección gastronómica.
- El catálogo heredado Botanero queda accesible mediante filtro histórico separado y está etiquetado como **referencial, no vigente**.
- La foto **no requiere guardarse previamente en `static/portal/images`**; el formulario Admin la carga al backend.

## Contratos REST principales

| Método | Ruta | Rol |
|---|---|---|
| POST | `/api/v1/pedidos/envio` | Personal autorizado; crea y confirma de forma atómica |
| POST | `/api/v1/pedidos/{id}/detalles/{detalleId}/listo` | Cocina/roles autorizados de preparación |
| POST | `/api/v1/pedidos/{id}/detalles/{detalleId}/entregado` | **Solo MOZO** |
| GET | `/api/v1/inventario` | Personal que consulta stock, incluida cifra de salidas manuales |
| PATCH | `/api/v1/inventario/{id}/salida` | CAJA/SUPERVISOR/ADMIN; `{cantidad,comentario}` |
| GET | `/api/v1/ventas/pedidos/{id}/cotizacion?medioPago=POS` | CAJA/SUPERVISOR/ADMIN; cotización con recargo |
| POST | `/api/v1/ventas/pedidos/{id}/cobrar` | CAJA/SUPERVISOR/ADMIN; `{medioPago,referenciaPago,montoRecibido,recargoInformadoAceptado}` |
| GET | `/api/v1/ventas/resumen?fecha=AAAA-MM-DD` | Caja; resumen por medio y día |
| GET | `/api/v1/ventas/detalle-diario?fecha=AAAA-MM-DD` | Caja/Supervisor/Admin; listado de productos cobrados y total final |
| POST | `/api/productos/{id}/imagen` | ADMIN multipart `archivo` |
| GET | `/api/productos/{id}/imagen` | Público |

## Secuencia recomendada para probar y evidenciar

1. Abrir un pedido de Mesa 1 como MOZO; enviar a Cocina. Ver confirmación, total y reserva de stock.
2. En COCINERO marcar un plato LISTO. Ver que LISTO/ENTREGADO no ofrece otra vez botón. Cuando todos queden LISTOS, la comanda sale de la cola de preparación; permanece visible para Caja.
3. Como MOZO registrar ENTREGADO; como CAJA comprobar que no aparece dicho botón.
4. Como CAJA seleccionar pedido y cambiar EFECTIVO → YAPE → POS → TRANSFERENCIA: observar nueva cotización. Para POS aceptar explícitamente el recargo, cobrar y verificar subtotal, recargo y total del ticket.
5. En SUPERVISOR seleccionar la fecha, verificar que Efectivo + Yape + POS + Transferencia = venta total; guardar arqueo de efectivo.
6. Como CAJA entrar a Inventario y registrar salida de una porción con comentario; como MOZO ver el nuevo stock sin modificarlo.
7. Después de cobrar, comprobar que Caja **no muestre el ID y monto antiguo**: «No hay pedidos por cobrar», campos limpios y botón deshabilitado. Consultar el ticket mediante campo independiente.
8. Abrir **Ventas del día**, filtrar fecha y fotografiar unidades, precios, subtotal y total. Comparar con Resumen de Caja y su distribución por cuatro medios.
9. Como ADMIN entrar a Productos · CRUD, actualizar nombre/precio de un producto, elegir imagen y Guardar fotografía. En otra pestaña del portal actualizar la carta y verificar foto/nombre/precio. Probar baja lógica y comprobar que desaparezca de la carta ACTUAL.

## Evidencias nuevas

Guardar capturas reales en `evidencias/SOA/APF3/`:
- `SOA_APF3_49_Cocina_Solo_Pendientes.png`
- `SOA_APF3_50_Caja_POS_Desglose_Recargo.png`
- `SOA_APF3_51_Caja_Transferencia_Cuadre.png`
- `SOA_APF3_52_Admin_Carga_Foto_CRUD.png`
- `SOA_APF3_53_Portal_Carta_Foto_Actualizada.png`
- `SOA_APF3_54_Caja_Salida_Stock_Motivo.png`
- `SOA_APF3_55_Inventario_Salidas_Separadas_De_Pedidos.png`
- `SOA_APF3_56_Caja_Limpia_Tras_Cobro.png`
- `SOA_APF3_57_Ventas_Dia_Productos_Precios_Total.png`

Sin contraseñas, JWT ni referencias bancarias personales. El ticket es académico, no factura ni boleta SUNAT.

## Migración y cautela operativa

`database/migrations/2026_11_22_soa_apf3_pos_transferencia_imagenes.sql` añade columnas de ventas, `imagen_url` y `producto_imagenes`. NO está aplicado en producción. Requiere revisión, prueba en un esquema de ensayo MySQL, respaldo y compatibilidad con migraciones previas. El perfil demo H2 crea sus tablas automáticamente pero no conserva datos tras reiniciar.
