# APF3 | Acta de publicación y verificación de versión

## Estado consolidado — 04/11/2026

**Responsable:** Ronald Albert Bustamante Villanueva. Proyecto individual.

| Control | Resultado comprobado | Evidencia |
|---|---|---|
| Publicación en Render | Servicio delicias-peruanas-api, rama develop, commit a9897e4 Live; evento visible del 04/11/2026 a las 00:59 | Figura 90 |
| Interfaz pública | Página de Delicias Peruanas cargada con encabezado SISTEMA WEB · APF3 | Figura 91; esta captura no demuestra autenticación |
| Integración continua | 17 pruebas, 0 fallos, 0 errores y 0 omitidas; BUILD SUCCESS | Figura 76; [ejecución 41782116127](https://github.com/rabv7399/Integrador2-grupoRonald/actions/runs/41782116127) |
| Documentación interactiva | Swagger UI publicada; productos devuelve 401 sin JWT y 200 con JWT | Figuras 70–73, comprobadas en la revisión 7b08b3a |
| Sincronización | PR #64 integrado; comparación de main/develop sin diferencias al verificar | Figuras 74–75 |
| Observaciones APF2 | Contraste de los tres comentarios del docente con acciones y alcance | Figura 79 |
| Retrospectiva Sprint 6 | Revisión individual con resultados y compromisos | Figura 81 |
| Solicitudes de servicio | Catálogo, flujo, herramientas y tablero; accesos reales pendientes | Figuras 82–85; caso #48 identificado como simulación |
| Control de cambios | Calendario y matriz por infraestructura, aplicaciones, documentación y calendario | Figuras 78 y 86 |
| Capacidad | Umbrales propuestos y plan por fases con responsables y criterios | Figuras 87–89; las Figuras 87–88 preceden a la actualización del PR #67 |

**Trazabilidad de revisiones:** el backend de producción verificado es a9897e4, integrado mediante PR #64. Los PR #65, #66 y #67 actualizaron calendario, retrospectiva y capacidad; no modificaron código del backend. La igualdad de ramas se volvió a comprobar tras cada integración. Estos cambios documentales no acreditan que Render haya publicado otro SHA. Esta consolidación también es exclusivamente documental.

**Validación funcional previa:** consultas protegidas 200 y reserva productiva de prueba 201, repetición 409 y cancelación del registro ID 5 (Figuras 66–68). No se repitieron reservas para obtener las capturas 90–91 ni se atribuyen estas operaciones a una nueva prueba posterior al despliegue a9897e4.

**Límites y pendientes:** no hay validación de 20 usuarios ni 6 RPS sostenidos, p95 autenticado reproducible, métricas efectivas del pool o CPU/RAM del backend. Memoria Aiven observada: 86 %, sin demostrar tendencia sostenida. No se declara aprobación independiente de accesos, disponibilidad contractual, restauración ensayada ni aceptación funcional final. La consulta de mesas en producción sigue como mejora pendiente, con permisos comprobados en H2. El informe y la presentación permanecen pendientes de consolidación.

Las secciones históricas conservan las cifras y estados de cada revisión; este resumen prevalece para el cierre técnico actual. Las figuras son capturas aportadas por el responsable; su numeración no implica que los archivos de imagen estén incluidos en el repositorio.


**Proyecto:** Delicias Peruanas — Curso Integrador II  
**Fecha de control:** 03/11/2026 (hora local de Perú, UTC-05:00)  
**Repositorio:** https://github.com/rabv7399/Integrador2-grupoRonald  
**Servicio:** https://delicias-peruanas-api.onrender.com/  
**Base de datos:** MySQL Aiven, esquema `delicias_peruanas`  
**Responsable del cambio:** responsable del proyecto; aprobación explícita recibida antes de fusionar PR #42.  
**RFC:** https://github.com/rabv7399/Integrador2-grupoRonald/issues/43

> Este documento distingue entre despliegue técnico exitoso y aceptación funcional completa. Las verificaciones que no se realizaron figuran como pendientes; no se inventan mediciones ni credenciales.

## Registro histórico del despliegue de presentación — 03/11/2026

- **Producción Render:** `develop` / **`8b07733e2962a3aad2f180c2d0c465b3eb53298a`** / **`Live`**, visible en **Figura 41**; segundo despliegue Auto-Deploy duró 3 min 2 s.
- **Presentación pública:** `SISTEMA WEB · APF3` por HTTPS, **Figura 42**.
- **CI de la revisión final:** [GitHub Actions #41720071675](https://github.com/rabv7399/Integrador2-grupoRonald/actions/runs/41720071675), **success**.
- **Validación de regresión posterior al último despliegue:** login JWT exitoso (Figura 43) y lectura protegida de tres categorías y tres productos, HTTP 200 con JWT válido (Figura 44).
- **Sincronización Git:** `main` incorpora la versión APF3 por PR #46, y su Ruleset está **Active** (Figura 45). Los cambios posteriores de documentación en `main` no equivalen a una nueva versión desplegada en Render.
- **Restricciones vigentes:** sin prueba real de creación/conflicto de reservas ni métricas CPU/RAM del backend, ni certificación de disponibilidad SLA. RFC #43 contiene seguimiento histórico y no debe declararse aceptación integral de todo flujo.

**Nota de interpretación:** las referencias a la revisión `d5818f4` y el PR #42 en la tabla siguiente corresponden a **un despliegue anterior exitoso** de APF3, que fue reemplazado posteriormente por la revisión final `8b07733` del PR #45. **No** son el estado actual de Render.

## 1. Registro histórico de la publicación inicial

Las verificaciones posteriores se detallan en las secciones 7 y 8. Las restricciones de este apartado describen el estado de la revisión 8b07733, no el estado vigente.

| Control | Evidencia verificable | Resultado |
|---|---|---|
| Control de versiones | [PR #42](https://github.com/rabv7399/Integrador2-grupoRonald/pull/42) fusionado a `develop` | **Integrado** |
| Commit del merge | [`d5818f491a55eb45cd1ba9e9f4241cf351ff049e`](https://github.com/rabv7399/Integrador2-grupoRonald/commit/d5818f491a55eb45cd1ba9e9f4241cf351ff049e) | **Verificado** |
| CI Java 21 / Maven | [GitHub Actions #41686936753](https://github.com/rabv7399/Integrador2-grupoRonald/actions/runs/41686936753) | **BUILD SUCCESS: 9 pruebas, 0 fallos, 0 errores, 0 omitidas** |
| Despliegue Render | Captura del historial Render > Deploys, Figura 36 | **Exitoso**, `Auto-Deploy`, revisión `d5818f4`, duración 3 min 26 s |
| Revisión anterior | Figura 33, `be949ef` | Conservada como referencia histórica de reversión |
| HTTPS y frontend | Captura del navegador, Figura 37 | Interfaz y formulario de autenticación visibles; **no se identificó el SHA desde el navegador** |
| Inicio de sesión válido | Captura del frontend autenticado por HTTPS, Figura 39 | **Verificado:** estado `Autenticado`, mensaje `JWT recibido desde el backend` y sección `Sesión activa` |
| Consulta autenticada de categorías y productos | Figura 40: panel con rol CLIENTE, JWT activo, HTTP 200 y 3 categorías + 3 productos | **Verificado en producción**; consulta de mesas no evidenciada |
| Reserva de prueba y validación de conflicto | Requiere entorno/datos controlados | **Pendiente** |
| Estado `Live` y SHA en cabecera de Render | Captura del encabezado de Render > Deploys, Figura 38 | **Verificado:** `Live` en `develop`, SHA `d5818f4` |
| Configuración efectiva HikariCP en ejecución | Propiedades en repositorio y prueba H2; métricas reales del pool no medidas | **Configuración integrada, observabilidad runtime pendiente** |

## 2. Alcance del cambio publicado

- Configuración HikariCP mediante variables `DB_POOL_MAX_SIZE` (valor por defecto 5), `DB_POOL_MIN_IDLE` (1) y `DB_POOL_CONNECTION_TIMEOUT_MS` (30000); sin publicar valores de credenciales.
- Prueba automatizada de configuración HikariCP con H2.
- Plantilla de solicitud de cambio RFC y trazabilidad de solicitudes de servicio.
- Plan de capacidad, resultados de tres `EXPLAIN` reales en MySQL Aiven y evidencias de índices existentes. No se ejecutó DDL como parte de este cambio.
- Documentación del diagnóstico del desfase de Render por `Root Directory=backend/` y `Auto-Deploy=On Commit`.

## 3. Evidencia de datos y rendimiento (alcance limitado)

- **Figuras 28–31:** inspección de índices y tres `EXPLAIN` en MySQL Aiven: `idx_reservas_mesa_fecha` (acceso `ref`), `idx_reservas_fecha_estado` (`ref`) y `uq_usuarios_correo` (`const`). Son planes estimados, **no tiempos de ejecución**.
- **GitHub Actions k6:** [ejecución #41581904824](https://github.com/rabv7399/Integrador2-grupoRonald/actions/runs/41581904824), 2 usuarios virtuales durante 12 s, 24/24 respuestas HTTP 200 en `GET /`, promedio 69,04 ms y p95 127,26 ms. **Sondeo ligero del frontend, no estrés de API autenticada ni SLA de producción.**
- **Render Free:** límite de 512 MB de RAM; el consumo real de CPU y RAM no se verificó. El plan puede suspender la instancia por inactividad.
- **Aiven Free:** instancia MySQL de un nodo; se comprobó conectividad durante las consultas de Workbench. Disponibilidad futura y copias de seguridad no se han validado.

## 4. Validación posterior al despliegue — lista de aceptación

- [x] Integración aprobada y commit registrado.
- [x] CI del commit fusionado satisfactoria (9 pruebas).
- [x] Despliegue automático exitoso de `d5818f4` en Render (Figura 36).
- [x] Página principal HTTPS visible en navegador (Figura 37, comprobación visual).
- [x] Obtener captura de Render con estado `Live` y SHA `d5818f4` en cabecera (Figura 38), y de la versión final `8b07733` (Figura 41).
- [x] Autenticación satisfactoria con cuenta autorizada antes y después del despliegue final (Figuras 39 y 43, credenciales ocultas).
- [x] Consulta protegida de categorías/productos antes y después del despliegue final: HTTP 200, JWT válido, 3 categorías y 3 productos (Figuras 40 y 44).
- [ ] Comprobar consulta autenticada de mesas, si el alcance funcional y el rol lo permiten.
- [x] Crear reserva de prueba ID 5 (201), comprobar superposición (409) y cancelar el registro en Aiven (Figuras 66–68).
- [x] Inspeccionar logs de Render y comprobar el JOIN de productos y categorías (Figuras 58 y 63).
- [ ] Validar conexión estable y límites efectivos del pool en entorno productivo si la plataforma lo permite.
- [x] PR #45 de presentación final APF3 fusionado, nueva publicación `8b07733` en Render `Live` y comprobación visual del encabezado APF3 (Figuras 41–42). También se conservan las Figuras 59, 66 y 67 de comprobaciones posteriores.
- [ ] Registrar aprobación funcional y cerrar RFC #43.

## 5. Incidencias, seguimiento y reversión

**Incidencia resuelta:** [Issue #44](https://github.com/rabv7399/Integrador2-grupoRonald/issues/44). Render estaba en `be949ef` porque los cambios previos en `develop` no tocaban `backend/`. La integración de PR #42 sí modificó archivos de backend y activó el despliegue automático; la captura de Figura 36 confirmó éxito.

**Hallazgo de presentación resuelto:** la revisión anterior `d5818f4` contenía dos etiquetas `APF2` en `index.html`. El [PR #45](https://github.com/rabv7399/Integrador2-grupoRonald/pull/45) las actualizó a `APF3`, se fusionó en `develop` y Render confirmó la revisión `8b07733` en estado `Live` (Figuras 41–42); regresión JWT/consulta HTTP 200 comprobada (Figuras 43–44). La Figura 59 volvió a confirmar 8b07733; las Figuras 66 y 67 muestran el formulario APF3.

**Plan de reversión:** ante fallo persistente de inicio, HTTP 5xx o regresión de seguridad, detener cambios adicionales; recopilar logs, verificar estado de Aiven y, si Render lo permite, restaurar la revisión anterior estable `be949ef`; en paralelo revertir el commit problemático mediante PR. No modificar ni restaurar datos de MySQL sin un respaldo y plan específicos. Tras cualquier reversión, repetir pruebas HTTPS y funcionales.

## 6. Relación de figuras de publicación

- **Figura 32:** `APF3_E32_Render_Version_Anterior_Detectada.png` — versión anterior `be949ef` marcada `Live`.
- **Figura 33:** `APF3_E33_Historial_Despliegues_Render.png` — historial previo sin revisión APF3.
- **Figura 34:** `APF3_E34_Configuracion_Root_Directory_Render.png` — rama `develop`, directorio raíz `backend/`.
- **Figura 35:** `APF3_E35_Auto_Deploy_On_Commit_Render.png` — despliegue automático habilitado.
- **Figura 36:** `APF3_E36_Despliegue_Version_APF3_Render.png` — revisión `d5818f4` desplegada exitosamente.
- **Figura 37:** `APF3_E37_Frontend_HTTPS_Delicias_Peruanas.png` — interfaz web accesible por HTTPS.
- **Figura 38:** `APF3_E38_Render_Live_Commit_APF3.png` — servicio `delicias-peruanas-api` en estado `Live` con revisión `d5818f4` en `develop`.
- **Figura 39:** `APF3_E39_Login_Exitoso_Produccion.png` — autenticación exitosa de una cuenta autorizada, recepción de JWT y sesión activa; campos de correo y contraseña ocultos en la copia para informe.
- **Figura 40:** `APF3_E40_Consulta_Protegida_JWT_Produccion.png` — consulta autenticada de categorías y productos con JWT válido y HTTP 200; identificador de usuario oculto en la copia para informe.
- **Figura 41:** `APF3_E41_Render_Live_Version_Final_APF3.png` — revisión final `8b07733`, rama `develop`, estado `Live`.
- **Figura 42:** `APF3_E42_Frontend_Final_APF3_HTTPS.png` — cabecera `SISTEMA WEB · APF3` en HTTPS.
- **Figura 43:** `APF3_E43_Login_JWT_Postdespliegue_Final.png` — login JWT satisfactorio tras el último despliegue.
- **Figura 44:** `APF3_E44_Consulta_Protegida_Postdespliegue_Final.png` — categorías y productos con JWT válido, HTTP 200.
- **Figura 45:** `APF3_E45_Ruleset_Main_Activo.png` — Ruleset de rama `main` en estado `Active`.

## 7. Verificación posterior y revisión vigente — 03/11/2026

### 7.1 Cambios y validación

| Control | Resultado | Evidencia |
|---|---|---|
| Optimización del listado | EntityGraph para cargar productos y categorías juntos | [PR #58](https://github.com/rabv7399/Integrador2-grupoRonald/pull/58), commit bdf43df |
| Prueba de consulta | Tres productos y dos categorías recuperados con una sola sentencia SQL en H2 | Figura 64; [CI de 10 pruebas](https://github.com/rabv7399/Integrador2-grupoRonald/actions/runs/41773523611) |
| SQL en Render | Consulta de productos con JOIN a categorías | Figura 63 |
| Permisos de mesas | ADMIN/OPERADOR: 200 con datos; CLIENTE: 403; sin token: 401, en H2 con JWT | [PR #59](https://github.com/rabv7399/Integrador2-grupoRonald/pull/59), Figura 65 |
| CI de permisos | 14 pruebas, 0 fallos, 0 errores, 0 omitidas; BUILD SUCCESS | [GitHub Actions #41774836685](https://github.com/rabv7399/Integrador2-grupoRonald/actions/runs/41774836685) |
| Último backend desplegado verificado | 16b5cfe7e01dde20f0c8d817a9a9b8d13fc46572, Live en Render; duración 2 min 43 s | Figura 69 |
| Reserva en producción | ID 5, cliente PRUEBA APF3 - VALIDACION, mesa ID 1 (número 90), 05/11/2026, 19:00–20:00, 2 personas; HTTP 201 | Figura 66 |
| Superposición | Reenvío con la misma mesa, fecha y horario: HTTP 409 | Figura 67 |
| Limpieza | Reserva ID 5 actualizada a CANCELADA mediante SQL restringido al registro de prueba; SELECT posterior confirma el estado | Figura 68 |

La consulta de mesas con ADMIN/OPERADOR queda validada en H2, no en producción. La respuesta de creación de reserva incluye la mesa, pero no sustituye una prueba de GET /api/mesas.

### 7.2 Mediciones desde Chrome

| Ejecución | Categorías | Productos | Estado |
|---|---:|---:|---|
| Anterior al cambio (captura aportada antes de Figura 56) | 1,35 s | 1,54 s | 200 / 200 |
| Anterior al cambio, segunda medición (Figura 57) | 1,16 s | 1,80 s | 200 / 200 |
| Primera medición posterior al despliegue bdf43df (Figura 61) | 9,18 s | 9,18 s | 200 / 200 |
| Segunda medición posterior, servicio activo (Figura 62) | 774 ms | 773 ms | 200 / 200 |

La Figura 56 desglosa productos: 344,26 ms en Stalled, 1,19 s esperando respuesta y 0,86 ms de descarga; total 1,54 s. La última ejecución muestra tiempos inferiores a un segundo, pero la muestra no demuestra mejora sostenida ni permite atribuir toda la reducción al código. Se conserva la ejecución de 9,18 s; su causa no se determinó. Estas mediciones no constituyen una validación de SLA ni son comparables directamente con el sondeo k6 de GET /.

### 7.3 Evidencias adicionales

| Figura | Contenido |
|---|---|
| 56 | Desglose Timing de la consulta de productos |
| 57 | Segunda medición anterior al ajuste |
| 58 | Logs previos con consultas adicionales de categorías |
| 59 | Render Live en develop, revisión 8b07733 |
| 60 | Despliegue exitoso de bdf43df |
| 61 | Primera medición posterior: 9,18 s en ambas consultas |
| 62 | Segunda medición posterior: 774 ms y 773 ms |
| 63 | JOIN de productos y categorías en los logs de Render |
| 64 | CI de 10 pruebas satisfactorias |
| 65 | CI de 14 pruebas satisfactorias, incluidos permisos de mesas |
| 66 | Creación de reserva ID 5: HTTP 201 |
| 67 | Rechazo de superposición: HTTP 409 |
| 68 | Reserva de prueba ID 5 en estado CANCELADA |
| 69 | Render Live con revisión 16b5cfe |

Las figuras corresponden a capturas aportadas durante la validación; esta tabla no implica que los PNG estén almacenados en el repositorio.

### 7.4 Pendientes de cierre

- Consulta de mesas en producción con una cuenta ADMIN u OPERADOR autorizada.
- Comprobación de límites efectivos y métricas de HikariCP en producción; las conexiones de MySQL no equivalen a métricas del pool de la aplicación.
- Consolidar el informe y sus anexos con las figuras numeradas.
- Registrar aceptación funcional final y resolver RFC #43 según su alcance.

**Estado global:** despliegue técnico, autenticación, consultas de categorías/productos, creación de reserva, rechazo por superposición y cancelación del dato de prueba verificados. Permisos de mesas y consulta optimizada validados mediante pruebas automatizadas. La aceptación final y los controles pendientes no se declaran completados.

## 8. Verificación de OpenAPI y Swagger — 04/11/2026

- PR #62 integrado en develop, commit 7b08b3a; 17 pruebas aprobadas sin fallos ni errores.
- Figura 70: Render Live con la revisión 7b08b3a.
- Figura 71: Swagger UI muestra Delicias Peruanas API, APF3, rutas y esquema JWT.
- Figura 72: GET /api/productos sin JWT devuelve 401.
- Figura 73: consulta autenticada de productos devuelve 200 y datos; se conserva la captura sin bloque Curl ni credenciales.
- Las observaciones originales del docente se registran en APF3_OpenAPI_y_Preparacion_Demo.md. La recomendación de documentación interactiva está implementada y comprobada en producción.

La sincronización quedó comprobada mediante PR #64 y comparación de contenido (Figuras 74–75); las integraciones documentales posteriores se verificaron igualmente. El informe se actualizará al final del cierre técnico, según la indicación del responsable.
