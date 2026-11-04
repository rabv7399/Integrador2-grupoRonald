# APF3 | Acta de publicación y verificación de versión

**Proyecto:** Delicias Peruanas — Curso Integrador II  
**Fecha de control:** 03/11/2026 (hora local de Perú, UTC-05:00)  
**Repositorio:** https://github.com/rabv7399/Integrador2-grupoRonald  
**Servicio:** https://delicias-peruanas-api.onrender.com/  
**Base de datos:** MySQL Aiven, esquema `delicias_peruanas`  
**Responsable del cambio:** responsable del proyecto; aprobación explícita recibida antes de fusionar PR #42.  
**RFC:** https://github.com/rabv7399/Integrador2-grupoRonald/issues/43

> Este documento distingue entre despliegue técnico exitoso y aceptación funcional completa. Las verificaciones que no se realizaron figuran como pendientes; no se inventan mediciones ni credenciales.

## 1. Registro histórico de la publicación inicial

La revisión vigente y las verificaciones posteriores se detallan en la sección 7.

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
- [x] Obtener captura de Render con estado `Live` y SHA `d5818f4` en cabecera (Figura 38).
- [x] Autenticación satisfactoria con cuenta de prueba autorizada, sin exponer contraseña ni token (Figura 39, campos ocultos).
- [x] Consultar categorías y productos autenticados; resultado HTTP 200, JWT válido, 3 categorías y 3 productos (Figura 40).
- [ ] Comprobar consulta autenticada de mesas, si el alcance funcional y el rol lo permiten.
- [x] Crear reserva de prueba ID 5 (201), comprobar superposición (409) y cancelar el registro en Aiven (Figuras 66–68).
- [x] Inspeccionar logs de Render y comprobar el JOIN de productos y categorías (Figuras 58 y 63).
- [ ] Validar conexión estable y límites efectivos del pool en entorno productivo si la plataforma lo permite.
- [x] Presentación APF3 publicada mediante PR #45, revisión 8b07733; formulario APF3 y pruebas funcionales evidenciados posteriormente (Figuras 59, 66 y 67).
- [ ] Registrar aprobación funcional y cerrar RFC #43.

## 5. Incidencias, seguimiento y reversión

**Incidencia resuelta:** [Issue #44](https://github.com/rabv7399/Integrador2-grupoRonald/issues/44). Render estaba en `be949ef` porque los cambios previos en `develop` no tocaban `backend/`. La integración de PR #42 sí modificó archivos de backend y activó el despliegue automático; la captura de Figura 36 confirmó éxito.

**Hallazgo histórico de presentación (resuelto por PR #45):** la revisión `d5818f4` contiene en `backend/src/main/resources/static/index.html` el título y encabezado heredados `APF2`. La rama `apf3-release-final` propone actualizar únicamente esas etiquetas a `APF3`, sin alterar la autenticación ni la lógica de negocio. El PR #45 se integró y la Figura 59 confirmó la revisión 8b07733 activa; las Figuras 66 y 67 muestran el formulario APF3.

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
