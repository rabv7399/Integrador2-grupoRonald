# APF3 | Acta de publicación y verificación de versión

**Proyecto:** Delicias Peruanas — Curso Integrador II  
**Fecha de control:** 03/11/2026 (hora local de Perú, UTC-05:00)  
**Repositorio:** https://github.com/rabv7399/Integrador2-grupoRonald  
**Servicio:** https://delicias-peruanas-api.onrender.com/  
**Base de datos:** MySQL Aiven, esquema `delicias_peruanas`  
**Responsable del cambio:** responsable del proyecto; aprobación explícita recibida antes de fusionar PR #42.  
**RFC:** https://github.com/rabv7399/Integrador2-grupoRonald/issues/43

> Este documento distingue entre despliegue técnico exitoso y aceptación funcional completa. Las verificaciones que no se realizaron figuran como pendientes; no se inventan mediciones ni credenciales.

## 1. Identificación de la revisión publicada

| Control | Evidencia verificable | Resultado |
|---|---|---|
| Control de versiones | [PR #42](https://github.com/rabv7399/Integrador2-grupoRonald/pull/42) fusionado a `develop` | **Integrado** |
| Commit del merge | [`d5818f491a55eb45cd1ba9e9f4241cf351ff049e`](https://github.com/rabv7399/Integrador2-grupoRonald/commit/d5818f491a55eb45cd1ba9e9f4241cf351ff049e) | **Verificado** |
| CI Java 21 / Maven | [GitHub Actions #41686936753](https://github.com/rabv7399/Integrador2-grupoRonald/actions/runs/41686936753) | **BUILD SUCCESS: 9 pruebas, 0 fallos, 0 errores, 0 omitidas** |
| Despliegue Render | Captura del historial Render > Deploys, Figura 36 | **Exitoso**, `Auto-Deploy`, revisión `d5818f4`, duración 3 min 26 s |
| Revisión anterior | Figura 33, `be949ef` | Conservada como referencia histórica de reversión |
| HTTPS y frontend | Captura del navegador, Figura 37 | Interfaz y formulario de autenticación visibles; **no se identificó el SHA desde el navegador** |
| Inicio de sesión válido | Requiere cuenta autorizada y prueba controlada | **Pendiente** |
| Consulta autenticada de productos, categorías y mesas | Requiere JWT de cuenta autorizada | **Pendiente** |
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
- [ ] Autenticación satisfactoria con cuenta de prueba autorizada, sin exponer contraseña ni token.
- [ ] Consultar categorías, productos y mesas autenticadas; registrar códigos de respuesta y resultado esperado.
- [ ] Ejecutar una reserva de prueba controlada y verificar rechazo de conflicto, sin afectar reservas reales.
- [ ] Inspeccionar logs de Render tras despliegue, sin exponer secretos.
- [ ] Validar conexión estable y límites efectivos del pool en entorno productivo si la plataforma lo permite.
- [ ] Revisar y aprobar PR de presentación final APF3, verificar nueva publicación y repetir prueba visual.
- [ ] Registrar aprobación funcional y cerrar RFC #43.

## 5. Incidencias, seguimiento y reversión

**Incidencia resuelta:** [Issue #44](https://github.com/rabv7399/Integrador2-grupoRonald/issues/44). Render estaba en `be949ef` porque los cambios previos en `develop` no tocaban `backend/`. La integración de PR #42 sí modificó archivos de backend y activó el despliegue automático; la captura de Figura 36 confirmó éxito.

**Hallazgo de presentación pendiente:** la revisión `d5818f4` contiene en `backend/src/main/resources/static/index.html` el título y encabezado heredados `APF2`. La rama `apf3-release-final` propone actualizar únicamente esas etiquetas a `APF3`, sin alterar la autenticación ni la lógica de negocio. **Este ajuste no está publicado mientras su PR no se fusione y Render no confirme nuevo despliegue.**

**Plan de reversión:** ante fallo persistente de inicio, HTTP 5xx o regresión de seguridad, detener cambios adicionales; recopilar logs, verificar estado de Aiven y, si Render lo permite, restaurar la revisión anterior estable `be949ef`; en paralelo revertir el commit problemático mediante PR. No modificar ni restaurar datos de MySQL sin un respaldo y plan específicos. Tras cualquier reversión, repetir pruebas HTTPS y funcionales.

## 6. Relación de figuras de publicación

- **Figura 32:** `APF3_E32_Render_Version_Anterior_Detectada.png` — versión anterior `be949ef` marcada `Live`.
- **Figura 33:** `APF3_E33_Historial_Despliegues_Render.png` — historial previo sin revisión APF3.
- **Figura 34:** `APF3_E34_Configuracion_Root_Directory_Render.png` — rama `develop`, directorio raíz `backend/`.
- **Figura 35:** `APF3_E35_Auto_Deploy_On_Commit_Render.png` — despliegue automático habilitado.
- **Figura 36:** `APF3_E36_Despliegue_Version_APF3_Render.png` — revisión `d5818f4` desplegada exitosamente.
- **Figura 37:** `APF3_E37_Frontend_HTTPS_Delicias_Peruanas.png` — interfaz web accesible por HTTPS.
- **Figura 38:** `APF3_E38_Render_Live_Commit_APF3.png` — servicio `delicias-peruanas-api` en estado `Live` con revisión `d5818f4` en `develop`.

**Estado global:** publicación técnica APF3 verificada; aceptación funcional completa pendiente. No cerrar el cambio RFC hasta terminar las verificaciones de la sección 4.
