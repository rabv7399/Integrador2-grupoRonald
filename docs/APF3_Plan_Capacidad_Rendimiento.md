# APF3 | Plan de capacidad y rendimiento del servicio

## Estado vigente al 04 de noviembre de 2026

Esta actualización prevalece sobre las referencias históricas que aparecen después. El último backend verificado visualmente en Render es **16b5cfe**, Live (Figura 69). Los PR #58 y #59 están integrados; se aprobaron **14 pruebas**, sin fallos ni errores. La reserva productiva de prueba devolvió **201**, su repetición **409** y el registro ID 5 quedó **CANCELADA** (Figuras 66–68). El PR #42 ya está integrado; no debe interpretarse el borrador histórico como estado actual.

La guía de Semana 13 exige seis apartados. El plan por infraestructura, aplicaciones, documentación y calendario se desarrolla en [Guía de cierre](APF3_Guia_Cierre_Semana13.md). La retrospectiva cuenta ahora con acciones de cierre propuestas para el 04/11/2026 y dependencias explícitas; no se atribuyen reuniones ni aprobaciones externas inexistentes.

Capacidad verificada: **memoria Aiven 86 %, disco 30,2 %, conexiones puntuales 2, pico histórico 13 y límite 76** (Figuras 49–52). Son valores globales de MySQL, distintos del pool HikariCP, cuyo valor predeterminado configurado y probado es 5 / 1 / 30000 ms. La observabilidad runtime del pool sigue pendiente.

Permanecen abiertos el contraste con las observaciones originales APF2, OpenAPI si fue exigido, la revisión de sincronización de ramas y la presentación. Las métricas runtime y mesas en producción se conservan como mejoras; no se atribuyen como requisitos independientes de esta guía general. La aceptación final no está registrada.

El registro detallado de pruebas y mediciones se encuentra en el [acta de verificación](APF3_Acta_Publicacion_y_Verificacion.md). Las cifras antiguas de ocho o nueve pruebas y las menciones a despliegue pendiente describen antecedentes, no el estado vigente.

## Registro histórico de preparación y diagnóstico


**Servicio:** Delicias Peruanas · **Marco:** ITIL 4 / ISO/IEC 20000-1 · **Corte de evidencias:** 03/11/2026.  
**Enfoque:** diferenciar capacidad asignada, métricas observadas, supuestos de negocio y pruebas pendientes. Este plan no certifica un SLA de producción.

## 1. Escenario de demanda objetivo (hipótesis de planificación)

Para el restaurante se propone un escenario académico de **20 usuarios concurrentes** en hora pico; **no** es un dato de afluencia medido. Si cada usuario realiza una petición cada 10 s, la demanda media estimada es `20/10 = 2 RPS`. Con un factor de seguridad de 3, el escenario de dimensionamiento es **6 RPS**. Si cada respuesta pesa **20 KB (supuesto)**, el tráfico de salida estimado a 6 RPS es `6 × 20 × 8 = 960 kbit/s ≈ 0,96 Mbit/s`, sin sobrecarga de red/TLS. El sondeo de 2 VUs no valida ese escenario.

**Objetivos propuestos, no SLA acordados:** p95 de GET público menor a 500 ms con aplicación caliente y tasa de errores 5xx menor a 1 % durante una prueba controlada. El arranque en frío de Render Free se mide por separado y puede superar ampliamente esos umbrales.

## 2. Matriz de capacidad de servidores, base de datos y red

| Recurso | Capacidad/configuración conocida | Observación real | Acción y limitación |
|---|---|---|---|
| Aplicación Render `delicias-peruanas-api` | Servicio Docker Free, límite RAM **512 MB**. **CPU y uso real RAM no disponibles** en panel Free | Se observó `Live` con commit `be949ef` antes del merge APF3; Render advierte de suspensión por inactividad | Verificar nuevo commit `Live`; no atribuir vCPU ni % de RAM no medidos |
| Base de datos Aiven `delicias-peruanas-db` | MySQL 8.4, **1 nodo** visible | Recuperada a `Running`; CPU baja en gráfica, disco ~30 %, memoria ~78–83 % (estimación visual) | Seguimiento de memoria, plan de respaldo y vigencia del plan; obtener métricas exactas exportadas |
| Pool JDBC | Rama de mejora `apf3-cierre-calidad`: `spring.datasource.hikari.maximum-pool-size=${DB_POOL_MAX_SIZE:5}`, `minimum-idle=${DB_POOL_MIN_IDLE:1}`, `connection-timeout=${DB_POOL_CONNECTION_TIMEOUT_MS:30000}`; **pendiente de merge/despliegue** | CI de la rama de mejora comprobará los valores con H2; tamaño efectivo de Render/MySQL **aún no medido** | Configuración propuesta **5 / 1 / 30 s** añadida en rama de mejora; requiere CI verde, aprobación del PR, despliegue y verificación de métricas de conexiones |
| Red HTTPS | URL pública HTTPS en Render | Navegador: `GET /` HTTP 200 y 173 ms en una observación; carga de página 589 ms | Observar p95 y errores desde múltiples ejecuciones; separar latencia de cliente, red y backend |
| Respaldo / redundancia | No se verificó instancia de respaldo ni balanceador | MySQL mostrado con 1 nodo; no hay evidencia de alta disponibilidad | Registrar RTO/RPO deseados, validar copias de seguridad antes de cambios de esquema |

**RTO/RPO propuestos:** RTO 4 h y RPO 24 h para el ejercicio, **sin garantía técnica** hasta validar backups y restauración. No se afirma que la infraestructura Free soporte recuperación dentro de esos tiempos.

## 3. Mediciones observadas y límites de interpretación

| Fuente | Escenario | Resultado |
|---|---|---|
| Chrome DevTools | `GET /` | HTTP 200, **173 ms** (medición individual) |
| Chrome DevTools | `POST /api/auth/login` | HTTP 401, **678 ms** (credenciales rechazadas, no login exitoso) |
| GitHub Actions [sondeo 41576386565](https://github.com/rabv7399/Integrador2-grupoRonald/actions/runs/41576386565) | `GET /` secuencial | 3/3 HTTP 200, **78,9 ms** de media, máximo 97,5 ms |
| Mismo sondeo | `GET /api/categorias` secuencial | 3/3 HTTP 401, **96,6 ms** de media, máximo 111,6 ms |
| Mismo sondeo | `GET /api/categorias`, concurrencia 2 | 4/4 HTTP 401, **89,3 ms** de media, máximo 111,9 ms |
| Aiven Metrics | CPU, disco, memoria | CPU baja, disco ~30 %, RAM ~78–83 % por lectura visual de capturas; **no valores exportados** |
| [k6 GitHub Actions 41581904824](https://github.com/rabv7399/Integrador2-grupoRonald/actions/runs/41581904824) | 2 VUs × 12 s, solo `GET /` | **24/24 HTTP 200**, 0 % errores; media **69,04 ms**, p95 **127,26 ms**, máximo **132,94 ms**, tasa observada **1,857 RPS** |
| Render Metrics Free | CPU/RAM | No disponibles; se observó límite RAM 512 MB, no consumo |

**Interpretación:** 401 es una respuesta válida de la capa HTTP, pero no confirma operación de negocio. Los resultados son exploratorios y no equivalen a prueba de estrés, throughput sostenido ni disponibilidad porcentual.

## 4. Optimización de base de datos: tres consultas críticas

Consultas inferidas de `ReservaRepository` y `UsuarioRepository`. **Pendientes de ejecutar en MySQL Aiven**; no se inventan `EXPLAIN` ni tiempos SQL.

**Consulta 1: detección de reservas superpuestas** (`ReservaRepository.buscarConflictos`).

```sql
EXPLAIN SELECT id FROM reservas
WHERE mesa_id = 1
  AND fecha = CURRENT_DATE()
  AND estado <> 'CANCELADA'
  AND hora_inicio < '21:00:00'
  AND hora_fin > '19:00:00';
```

Índice candidato, sujeto a inspección previa con `SHOW INDEX FROM reservas`: `(mesa_id, fecha, hora_inicio)`. Evaluar `type`, `key`, `rows`, `Extra`; no crear índice duplicado.

**Consulta 2: listado de reservas por fecha** (`ReservaRepository.findByFecha`).

```sql
EXPLAIN SELECT id, fecha, hora_inicio, mesa_id, estado
FROM reservas WHERE fecha = CURRENT_DATE();
```

Índice candidato: `(fecha)`, solo si no existe otro índice con `fecha` como primera columna y la cardinalidad lo justifica.

**Consulta 3: autenticación por correo** (`UsuarioRepository.findByCorreo`).

```sql
EXPLAIN SELECT id, correo, rol, activo
FROM usuarios WHERE correo = 'prueba@example.invalid';
```

`Usuario.correo` está declarado `unique = true` en JPA, por lo que **se espera** un índice único si el esquema fue creado conforme al modelo; comprobar con `SHOW INDEX FROM usuarios` y `EXPLAIN`. No consultar contraseñas ni registrar datos personales.

**Procedimiento realizado (03/11/2026):** se inspeccionaron los indices y ejecutaron los tres EXPLAIN reales en MySQL Workbench. El script de solo lectura [`database/APF3_EXPLAIN_Aiven.sql`](../database/APF3_EXPLAIN_Aiven.sql) en cliente SQL autorizado para Aiven; ejecutar `SHOW INDEX FROM reservas;`, `SHOW INDEX FROM usuarios;` y los tres `EXPLAIN`; conservar salidas (ocultando datos sensibles), identificar recorridos `ALL` y decidir índices en una RFC separada. No ejecutar DDL en producción sin respaldo/aprobación.

### Resultados reales de inspeccion de indices y EXPLAIN (03/11/2026)

**Fuente:** capturas de MySQL Workbench compartidas por el responsable del proyecto, conectado a MySQL Aiven, esquema `delicias_peruanas`. Los siguientes son datos observados, no simulados:

- **Figura 28 — `INFORMATION_SCHEMA.STATISTICS`:** `reservas` tiene `idx_reservas_fecha_estado(fecha, estado)`, `idx_reservas_mesa_fecha(mesa_id, fecha)` y `PRIMARY(id)`; `usuarios` tiene `idx_usuarios_rol_activo(rol, activo)`, `uq_usuarios_correo(correo)` (unico) y `PRIMARY(id)`.
- **Figura 29 — EXPLAIN deteccion de conflictos:** para `SELECT id FROM reservas WHERE mesa_id=1 AND fecha=CURRENT_DATE() AND estado <> 'CANCELADA' AND hora_inicio<'21:00:00' AND hora_fin>'19:00:00'`, MySQL devuelve `type=ref`, `key=idx_reservas_mesa_fecha`, `key_len=11`, `ref=const,const`, `rows=1`, `filtered=50.00`. La captura no muestra completo el campo `Extra`, por lo que no se afirma su contenido.
- **Interpretacion:** el indice compuesto se utiliza para los filtros de igualdad de mesa y fecha; no hay evidencia de recorrido completo de tabla en ese plan. `rows` y `filtered` son estimaciones del optimizador y no representan latencia ni volumen de carga.
- **Figura 30 — EXPLAIN reservas por fecha:** para `SELECT id, fecha, hora_inicio, mesa_id, estado FROM reservas WHERE fecha=CURRENT_DATE()`, MySQL devuelve `type=ref`, `possible_keys=idx_reservas_fecha_estado`, `key=idx_reservas_fecha_estado`, `key_len=3`, `ref=const`, `rows=1`, `filtered=100.00`, `Extra=NULL`. Se utiliza el prefijo `fecha` del indice compuesto; no se observa escaneo completo de tabla. La fila estimada no es una medicion de latencia.
- **Figura 31 — EXPLAIN usuarios por correo, validacion definitiva:** el primer intento con correo ficticio `prueba@example.invalid` devolvio `Extra=no matching row in const table` al no existir coincidencia. Se repitio con una variable de sesion `@correo_prueba` asignada mediante una consulta de solo lectura a un correo existente, **sin exponer su valor en la captura**. Resultado real: `table=usuarios`, `type=const`, `possible_keys=uq_usuarios_correo`, `key=uq_usuarios_correo`, `key_len=602`, `ref=const`, `rows=1`, `filtered=100.00`, `Extra=NULL`. El optimizador aprovecha la clave unica y no realiza escaneo completo; no se requiere un indice adicional con esta evidencia.
- No se han creado indices nuevos ni ejecutado DDL.

## 5. Prueba de concurrencia y umbrales

Se ejecutó `performance/k6_sondeo_apf3.js` en [GitHub Actions, ejecución 41581904824](https://github.com/rabv7399/Integrador2-grupoRonald/actions/runs/41581904824): **2 VUs durante 12 segundos**, solo `GET /`, **24 solicitudes y 24 checks correctos**, **0 fallos HTTP**, **p95 127,26 ms** y **1,857 RPS**. El workflow guardó `k6-summary.json` y `k6-console.txt` como [artefacto descargable](https://github.com/rabv7399/Integrador2-grupoRonald/actions/runs/41581904824/artifacts/13206407341). El preflight HTTP fue satisfactorio. Esta simulación controlada cumple el ejercicio básico de concurrencia, **no** demuestra capacidad de 20 usuarios, 6 RPS sostenidos ni un SLA real. Los umbrales de k6 son de control de prueba, no alertas permanentes.

| Métrica | Aviso propuesto | Crítico propuesto | Acción |
|---|---|---|---|
| p95 latencia de GET público, servicio caliente | > 500 ms | > 1 500 ms | Repetir sondeo; revisar arranque en frío, logs y red |
| HTTP 5xx sobre ventana de prueba | > 1 % | > 5 % | Detener nuevas pruebas; revisar Render y Aiven |
| CPU MySQL | > 70 % sostenido 5 min | > 85 % sostenido 5 min | Revisar consultas y conexiones |
| Memoria MySQL | > 75 % sostenido | > 90 % sostenido | Revisar consumo, buffers, plan y capacidad |
| Disco MySQL | > 70 % | > 85 % | Depurar datos/archivos autorizados y ampliar capacidad |
| CPU/RAM de Render | No configurables con evidencia actual | No configurables con evidencia actual | Instrumentar Actuator/APM o plan con métricas; **no afirmar alertas activas** |

Los valores son **umbrales de diseño**. El gráfico de memoria Aiven observado (~78–83 %) supera el umbral preventivo propuesto de 75 %, por lo que amerita revisión; no prueba saturación ni falla.

## 6. Plan de capacidad por fases

1. **Línea base:** repetir k6 de 2 VUs y registrar p50/p95, fallos y RPS; separar el primer acceso de arranque en frío.
2. **Base de datos:** ejecutar los tres `EXPLAIN`, revisar índices, métricas de conexiones y pool real; registrar capturas.
3. **Escalado controlado:** solo con autorización, pasar a 5 y 10 VUs en entorno no productivo o ventana acordada; medir p95 y 5xx; detener si hay degradación.
4. **Decisión de infraestructura:** si p95/errores superan objetivos, evaluar plan de pago, observabilidad y disponibilidad de Aiven antes de afirmar capacidad de 20 usuarios.
5. **Revisión continua:** actualizar matriz tras cambios de plan, consultas, carga o despliegue.

**Pendientes reales:** confirmación de HikariCP en producción, uso CPU/RAM real del backend, validación de 6 RPS y confirmación del nuevo commit en Render. **Completado:** sondeo k6 de 2 VUs, 24/24 respuestas HTTP 200 y métricas registradas. No extrapolar estos resultados a capacidad de producción.

## 7. Criterios de validacion del pool HikariCP (mejora en rama)

La configuracion propuesta limita inicialmente a **5 conexiones maximas**, **1 conexion inactiva minima** y **30 000 ms** de espera para obtener conexion. Se eligen limites conservadores por tratarse de una instancia Aiven de recursos reducidos; **no se ha demostrado aun que cinco conexiones sean suficientes para 20 usuarios concurrentes o 6 RPS**.

- **Validacion automatizada:** el test de integracion `SecurityIntegrationTest.poolHikariDebeAplicarLimitesConfigurados` usa H2 y comprueba las propiedades. Una ejecucion verde solo acredita la configuracion en CI.
- **Validacion operativa pendiente:** tras aprobacion y despliegue, observar conexiones MySQL, latencia p95 y errores 5xx con carga moderada; ajustar `DB_POOL_MAX_SIZE` y `DB_POOL_MIN_IDLE` mediante variables de entorno si procede, sin publicar secretos.
- **Criterio de seguridad:** no cambiar limites ni ejecutar estres sobre Aiven/Render Free sin ventana acordada y plan de reversion.
- **Versionado:** estos cambios estan propuestos en `apf3-cierre-calidad`; no atribuirlos al commit de produccion hasta comprobarlo.
