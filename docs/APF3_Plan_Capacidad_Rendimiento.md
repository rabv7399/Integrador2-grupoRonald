# APF3 | Plan de capacidad y rendimiento del servicio

**Servicio:** Delicias Peruanas · **Marco:** ITIL 4 / ISO/IEC 20000-1 · **Corte de evidencias:** 03/11/2026.  
**Enfoque:** diferenciar capacidad asignada, métricas observadas, supuestos de negocio y pruebas pendientes. Este plan no certifica un SLA de producción.

## Estado actualizado del criterio 5 — cierre técnico 03/11/2026

**Prevalece sobre las referencias históricas a ramas en preparación que permanecen más abajo.**

| Control | Estado final verificable | Evidencia |
|---|---|---|
| HikariCP | **Integrado y desplegado**: valores por defecto de máximo 5 conexiones, mínimo 1 inactiva, timeout 30 000 ms, sobreescribibles por variables de entorno | [PR #42](https://github.com/rabv7399/Integrador2-grupoRonald/pull/42), Render `develop` / `8b07733` / `Live` (Figura 41), test H2 de integración con CI verde |
| Validación funcional del backend después del despliegue | **Verificada para login y lectura protegida**; no equivale a medición de carga | Figuras 43 (JWT) y 44 (categorías/productos HTTP 200) |
| Inspección de índices y tres `EXPLAIN` en MySQL Aiven | **Completados** con planes `ref`, `ref` y `const`, usando índices existentes; no se ejecutó DDL | Figuras 28–31 y sección 4 del presente plan |
| k6 de concurrencia | **Sondeo ligero completado**: 2 VUs × 12 s, 24/24 HTTP 200, 0 % errores, media 69,04 ms, p95 127,26 ms, 1,857 RPS en `GET /` | [GitHub Actions #41581904824](https://github.com/rabv7399/Integrador2-grupoRonald/actions/runs/41581904824) |
| CPU/RAM real del backend Render Free | **No medido ni disponible mediante el panel Free en la evidencia reunida** | No inventar porcentajes ni métricas de aplicación |
| Capacidad proyectada 20 usuarios / 6 RPS | **Hipótesis de dimensionamiento sin validar** por carga representativa | Sección 1; prueba de 2 VUs no permite extrapolar |
| Conexiones Hikari activas en producción | **No medidas**; se verificaron propiedades en código/CI y que la aplicación operó con autenticación y lectura | No equiparar configuración con observabilidad del pool |
| Conexiones MySQL globales al consultar Workbench | **2 conexiones** (`Threads_connected`) en un instante, no atribuidas al pool Hikari | Figura 50, consulta read-only de MySQL Aiven |
| Máximo histórico de conexiones simultáneas MySQL | **13 conexiones** (`Max_used_connections`), pico desde el último arranque de MySQL; dato global, no exclusivo de HikariCP | Figura 51 (resultado y comando de Workbench) |
| Límite de conexiones del servidor MySQL | **76 conexiones** (`max_connections`), variable de configuración leída desde MySQL Aiven | Figura 52; `SHOW VARIABLES LIKE 'max_connections';` |
| Memoria, almacenamiento y CPU de Aiven | **Memoria 86 %** y **disco 30,2 %** de 1 GB; CPU con tendencia baja no cuantificada; conexión del pool sin medición | Figura 49 (Overview > Service plan usage), captura del responsable |

**Hallazgo crítico para la rúbrica:** se dispone de medición de p95 real de **frontend público**, tres planes MySQL reales y configuración de pool desplegada, pero **aún falta demostrar una carga controlada sobre endpoints autenticados y obtener métricas operativas reales de conexiones/CPU/RAM**. No se representa el sondeo ligero como prueba de estrés ni se atribuyen recursos no medidos. La muestra de k6 es exploratoria y no certifica SLA.

## 1. Escenario de demanda objetivo (hipótesis de planificación)

Para el restaurante se propone un escenario académico de **20 usuarios concurrentes** en hora pico; **no** es un dato de afluencia medido. Si cada usuario realiza una petición cada 10 s, la demanda media estimada es `20/10 = 2 RPS`. Con un factor de seguridad de 3, el escenario de dimensionamiento es **6 RPS**. Si cada respuesta pesa **20 KB (supuesto)**, el tráfico de salida estimado a 6 RPS es `6 × 20 × 8 = 960 kbit/s ≈ 0,96 Mbit/s`, sin sobrecarga de red/TLS. El sondeo de 2 VUs no valida ese escenario.

**Objetivos propuestos, no SLA acordados:** p95 de GET público menor a 500 ms con aplicación caliente y tasa de errores 5xx menor a 1 % durante una prueba controlada. El arranque en frío de Render Free se mide por separado y puede superar ampliamente esos umbrales.

## 2. Matriz de capacidad de servidores, base de datos y red

| Recurso | Capacidad/configuración conocida | Observación real | Acción y limitación |
|---|---|---|---|
| Aplicación Render `delicias-peruanas-api` | Servicio Docker Free, límite RAM **512 MB**. **CPU y uso real RAM no disponibles** en panel Free | Se verificó después `Live` con commit final `8b07733` (Figura 41); Render advierte suspensión por inactividad | Commit final `Live` verificado; no atribuir vCPU ni % de RAM no medidos |
| Base de datos Aiven `delicias-peruanas-db` | MySQL 8.4, **1 nodo** visible | Recuperada a `Running`; en Figura 49 se observan **memoria usada 86 %**, **almacenamiento 30,2 % de 1 GB** y gráfica CPU de 60 min sin valor puntual etiquetado; capturas previas indicaban 78–83 % de memoria | Seguimiento de memoria, plan de respaldo y vigencia del plan; obtener métricas exactas exportadas |
| Pool JDBC | Configuración integrada desde PR #42 y desplegada en `develop`/Render: `spring.datasource.hikari.maximum-pool-size=${DB_POOL_MAX_SIZE:5}`, `minimum-idle=${DB_POOL_MIN_IDLE:1}`, `connection-timeout=${DB_POOL_CONNECTION_TIMEOUT_MS:30000}`; tamaño efectivo del pool en runtime **no medido** | La prueba de integración H2 verificó propiedades en CI; tamaño efectivo y uso de conexiones Render/MySQL **aún no medidos** | Configuración **5 / 1 / 30 s** integrada y publicada; sigue pendiente medir conexiones reales y calibrar bajo carga |
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
| Aiven Overview / Service plan usage, Figura 49 | Memoria RAM, CPU y disco (vista 60 min) | **RAM usada 86 %; disco 30,2 % (1 GB)**; gráfica de CPU aparentemente baja sin valor puntual mostrado. Datos de la consola en captura, no de Render; conexiones no visibles. |
| [k6 GitHub Actions 41581904824](https://github.com/rabv7399/Integrador2-grupoRonald/actions/runs/41581904824) | 2 VUs × 12 s, solo `GET /` | **24/24 HTTP 200**, 0 % errores; media **69,04 ms**, p95 **127,26 ms**, máximo **132,94 ms**, tasa observada **1,857 RPS** |
| Render Metrics Free | CPU/RAM | No disponibles; se observó límite RAM 512 MB, no consumo |

**Interpretación:** 401 es una respuesta válida de la capa HTTP, pero no confirma operación de negocio. Los resultados son exploratorios y no equivalen a prueba de estrés, throughput sostenido ni disponibilidad porcentual.

## 4. Optimización de base de datos: tres consultas críticas

Consultas identificadas en `ReservaRepository` y `UsuarioRepository`. **Tres `EXPLAIN` reales ejecutados en MySQL Aiven el 03/11/2026** (Figuras 29–31); no se inventan tiempos SQL. Las consultas siguientes muestran el procedimiento inicialmente diseñado.

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

**Pendientes reales:** medición de conexiones HikariCP y uso CPU/RAM real del backend; validación de 6 RPS sostenidos y carga de API autenticada. **Completado:** HikariCP desplegado, commit `8b07733` `Live`, tres `EXPLAIN` reales y sondeo k6 de 2 VUs con 24/24 respuestas HTTP 200. No extrapolar estos resultados a capacidad de producción.

## 7. Criterios de validación del pool HikariCP (integrado; medición runtime pendiente)

La configuracion propuesta limita inicialmente a **5 conexiones maximas**, **1 conexion inactiva minima** y **30 000 ms** de espera para obtener conexion. Se eligen limites conservadores por tratarse de una instancia Aiven de recursos reducidos; **no se ha demostrado aun que cinco conexiones sean suficientes para 20 usuarios concurrentes o 6 RPS**.

- **Validacion automatizada:** el test de integracion `SecurityIntegrationTest.poolHikariDebeAplicarLimitesConfigurados` usa H2 y comprueba las propiedades. Una ejecucion verde solo acredita la configuracion en CI.
- **Validacion operativa pendiente:** tras aprobacion y despliegue, observar conexiones MySQL, latencia p95 y errores 5xx con carga moderada; ajustar `DB_POOL_MAX_SIZE` y `DB_POOL_MIN_IDLE` mediante variables de entorno si procede, sin publicar secretos.
- **Criterio de seguridad:** no cambiar limites ni ejecutar estres sobre Aiven/Render Free sin ventana acordada y plan de reversion.
- **Versionado:** la propuesta original se integró mediante PR #42 y se publicó en Render; el commit final `8b07733` está `Live` (Figura 41). **Aun así, no se han medido los valores efectivos de conexiones en operación**.


## 8. Evidencia de capacidad Aiven actualizada — Figura 49

**Fecha de observación:** 03/11/2026. **Fuente:** captura del responsable en la consola Aiven, sección `Overview > Service plan usage`, de la base de datos `delicias-peruanas-db`. Esta sección ya contiene métricas sin necesidad de abrir una pestaña independiente `Metrics`.

| Indicador | Valor realmente visible | Limitación |
|---|---|---|
| Capacidad asignada | 1 CPU; 1 GB RAM; 1 GB de almacenamiento | Capacidad del servicio MySQL, **no del backend Render** |
| Memoria utilizada (60 min) | **86 %** | No se separa el consumo por proceso ni se observa una serie detallada de presión del pool JDBC |
| Almacenamiento utilizado | **30,2 %** de 1 GB | No implica conocer el ritmo futuro de crecimiento |
| CPU across all nodes (60 min) | Gráfica con tendencia baja | **No se dispone de porcentaje exacto** en la imagen |
| Conexiones de MySQL / HikariCP | **No visibles** | No atribuir cantidad o saturación de conexiones |
| Backups and forking | Último respaldo indicado **13 horas** antes, respaldo más antiguo **15 días**, total mostrado **824 MB** | No se probó restauración ni se confirmó cumplimiento de RTO/RPO |

**Interpretación frente a umbrales propuestos:** la RAM de Aiven al **86 %** supera el umbral preventivo de diseño del **75 %**, pero no el crítico propuesto de **90 %**. Corresponde **revisar tendencias y recursos**, no declarar caída, incumplimiento de SLA ni saturación. Frente a la observación histórica aproximada de 78–83 %, la lectura actual es mayor; no se demuestra que el incremento sea sostenido solo con dos capturas.

**Acciones de seguimiento sin riesgo para producción:** revisar más adelante las métricas de memoria y conexiones por el panel Aiven, documentar la política y prueba de restauración en un entorno de ensayo, y medir conexiones activas en una ventana autorizada; no ejecutar cargas agresivas sobre Aiven/Render Free por el solo hecho de ver 86 % de RAM. Render conserva su propia limitación de observabilidad de CPU/RAM.

**Figura N.º 49:** `APF3_E49_Metricas_CPU_RAM_Conexiones_Aiven.png` — *Panel de capacidad de MySQL Aiven de Delicias Peruanas: memoria utilizada 86 %, almacenamiento 30,2 % y tendencia de CPU durante 60 minutos; las conexiones no se muestran en esta captura.* La copia omite la URL del navegador con identificadores internos.

## 9. Conexiones globales del servidor MySQL — Figura 50

**Evidencia observada:** el responsable ejecutó mediante MySQL Workbench, conectado a la base Aiven, la consulta de solo lectura:

```sql
SHOW GLOBAL STATUS LIKE 'Threads_connected';
```

La salida del servidor devuelve exactamente **`Threads_connected = 2`** (una fila); la captura muestra hora local de ejecución **22:12:20** del 03/11/2026. Esta medición constituye un **conteo puntual de conexiones MySQL abiertas**.

**Alcance:** `Threads_connected` es una variable de estado **global del servidor**. Puede incluir la conexión de Workbench y otras conexiones, por lo que **no identifica dos conexiones HikariCP**, no revela conexiones activas/inactivas por aplicación y no valida por sí sola el objetivo de 20 usuarios o 6 RPS. La configuración HikariCP de máximo 5 conexiones por defecto en Spring Boot sigue siendo un dato de código/CI/despliegue, distinto del conteo global.

**Figura N.º 50:** `APF3_E50_Conexiones_Activas_MySQL_Aiven.png` — *Verificación en MySQL Workbench de dos conexiones abiertas en la instancia MySQL Aiven mediante `SHOW GLOBAL STATUS LIKE 'Threads_connected'`, sin atribuirlas exclusivamente al pool HikariCP.* Se ocultaron direcciones de correo visibles en otras líneas SQL, sin tapar la consulta ni su salida.

**Seguimiento recomendado:** revisar `Max_used_connections` (pico desde el arranque del servidor) y `max_connections` (límite del servidor) mediante consultas de solo lectura autorizadas. Comparar solo con contexto temporal y sin ejecutar carga agresiva sobre la instancia Free.

## 10. Pico histórico de conexiones globales — Figura 51

**Consulta de diagnóstico read-only:**

```sql
SHOW GLOBAL STATUS LIKE 'Max_used_connections';
```

**Resultado observado:** `Max_used_connections = 13`. El historial de MySQL Workbench confirma que la consulta se ejecutó correctamente a las **22:18:41**, hora visible del equipo, mientras la rejilla enviada por el responsable muestra `Variable_name=Max_used_connections` y `Value=13`.

**Semántica oficial MySQL 8.4:** [Max_used_connections](https://dev.mysql.com/doc/refman/8.4/en/server-status-variables.html) es el **máximo de conexiones que han estado abiertas simultáneamente desde que se inició el servidor**, no la suma histórica de conexiones. Corresponde a **toda** la instancia, no al pool exclusivo del backend. 

| Indicador | Valor observado | Momento/interpretación |
|---|---:|---|
| `Threads_connected` | **2** | Conteo puntual de conexiones abiertas al ejecutar la consulta anterior; Figura 50, historial a las 22:12:20 |
| `Max_used_connections` | **13** | Pico global registrado desde el último arranque; Figura 51, consulta registrada a las 22:18:41 |
| `max_connections` | **76** | Límite de conexiones configurado en MySQL Aiven; lectura read-only Figura 52 |

**Criterio de interpretación:** las dos lecturas se realizaron en instantes distintos; no se supone que las 13 conexiones estuviesen activas al momento de las dos conexiones observadas. El valor 13 no contradice el límite por defecto **5** de HikariCP, pues dicho límite aplica al pool de **una instancia de la aplicación**, mientras que la cifra histórica MySQL incluye todas las sesiones de cliente y no identifica aplicaciones. Con el límite real `max_connections=76`, el pico histórico 13 equivale a aproximadamente **17,1 % del límite**; la diferencia aritmética es **63**. Esto es **margen teórico de conexiones**, no capacidad garantizada para 63 clientes nuevos ni para 20 usuarios concurrentes. Sin datos de errores por límite, consumo RAM/CPU bajo carga y reparto por aplicaciones, no se deduce saturación ni capacidad final.

**Figura N.º 51:** `APF3_E51_Maximo_Conexiones_MySQL_Aiven.png` — *Verificación del máximo histórico de 13 conexiones simultáneas del servidor MySQL Aiven mediante `Max_used_connections`, utilizando capturas de la consulta y su resultado.* Es un montaje identificado de dos recortes de las capturas originales, sin modificar comando ni valor y sin exponer otras consultas con correos.

**Control realizado:** se ejecutó `SHOW VARIABLES LIKE 'max_connections';` sin modificar datos ni roles; resultado 76 (Figura 52). Para validar rendimiento real siguen pendientes pruebas de carga controladas de la API autenticada y observabilidad de conexiones HikariCP, CPU y RAM.

## 11. Límite configurado de conexiones — Figura 52

**Consulta ejecutada en MySQL Workbench sobre la conexión Aiven** (solo lectura):

```sql
SHOW VARIABLES LIKE 'max_connections';
```

**Resultado real:** `max_connections = 76`, observado en la rejilla de resultados de MySQL Workbench y en el comando SQL seleccionado. No se ejecutó ningún cambio de configuración.

| Indicador MySQL | Valor | Naturaleza |
|---|---:|---|
| `Threads_connected` | **2** | Conexiones abiertas en medición puntual anterior (Figura 50) |
| `Max_used_connections` | **13** | Máximo histórico global desde el arranque (Figura 51) |
| `max_connections` | **76** | Límite configurado de conexiones concurrentes del servidor (Figura 52) |
| Pico histórico / límite | **17,1 %** | Proporción aproximada calculada: 13 / 76 × 100 |
| Diferencia aritmética entre límite y pico | **63** | **Margen teórico**, no garantía de capacidad operativa |

**Diagnóstico técnico:** la cifra 13 es inferior al máximo configurado 76 y no evidencia por sí sola agotamiento del límite. Las conexiones actuales y el pico no identifican cuáles pertenecen a HikariCP, ni el número de usuarios concurrentes, ni el rendimiento de consultas. La lectura de **86 % RAM** en Aiven (Figura 49) merece seguimiento preventivo; la suficiencia de recursos no se demuestra con conexiones solamente.

**Figura N.º 52:** `APF3_E52_Limite_Conexiones_MySQL_Aiven.png` — *Comprobación del límite de 76 conexiones simultáneas configurado en MySQL Aiven mediante `SHOW VARIABLES LIKE 'max_connections'` en MySQL Workbench.* La imagen fue recortada para conservar comando y resultado y excluir un historial inferior que contenía correos de cuentas de prueba; los datos SQL permanecen íntegros.

**Acción recomendada para cerrar el criterio 5 con rigor:** medir carga reproducible de endpoints representativos y revisar métricas del pool/CPU/RAM mediante instrumentación autorizada o una plataforma que las exponga, evitando estresar Render/Aiven Free en producción.

## 12. Evidencia del detalle de k6 — Figuras 53 y 54

**Fuente verificable:** [GitHub Actions, ejecución #41581904824](https://github.com/rabv7399/Integrador2-grupoRonald/actions/runs/41581904824), job `Dos usuarios virtuales - GET publico`, paso `Ejecutar sondeo k6 y guardar resumen`. El responsable aportó dos capturas: resumen del workflow **Success** (Figura 53, duración total 23 s, un artefacto) y resultados k6 del paso (Figura 54). Las cifras concuerdan con logs originales.

| Métrica de prueba | Resultado real observado |
|---|---:|
| Usuarios virtuales (VUs) | **2** |
| Solicitudes HTTP al endpoint público `GET /` | **24** |
| `checks_succeeded` | **24 / 24 (100 %)** |
| `http_req_failed` | **0 / 24 (0 %)** |
| HTTP esperado | **200** en las 24 comprobaciones |
| Latencia media `http_req_duration` | **69,04 ms** |
| Latencia mediana | **61,36 ms** |
| p90 de respuesta | **108,96 ms** |
| p95 de respuesta | **127,26 ms** |
| Máximo de respuesta | **132,94 ms** |
| Throughput del sondeo | **1,857298 solicitudes/s** |
| Iteraciones | **24** |
| Duración media de iteración | **1,07 s** |
| Red total recibida | **137 kB** |
| Red total enviada | **3,1 kB** |

**Figura N.º 53:** `APF3_E53_K6_Resumen_GitHub_Actions.png` — *Resultado satisfactorio del workflow de concurrencia k6 de Delicias Peruanas en GitHub Actions, con dos usuarios virtuales y un artefacto de evidencias.*

**Figura N.º 54:** `APF3_E54_K6_Metricas_24_Requests_P95.png` — *Resultados de k6 del sondeo público de Delicias Peruanas: 24 solicitudes, 100 % de comprobaciones satisfactorias, cero errores HTTP y tiempo de respuesta p95 de 127,26 ms.*

**Límites y riesgos:** las medidas corresponden **únicamente** a la página `GET /` con carga ligera en una ejecución anterior. La pantalla principal se sirvió mediante Render, pero los resultados **no prueban carga de servicios autenticados, eficiencia de consultas MySQL, 20 usuarios concurrentes o disponibilidad bajo saturación**. El workflow de GitHub Actions recibió una advertencia no bloqueante por deprecación de Node.js 20 en acciones utilizadas; conviene revisar compatibilidad durante el siguiente ciclo.

**Próxima validación de capacidad prioritaria:** una prueba moderada con cuentas de ensayo autorizadas y tokens guardados de forma segura (nunca en comentarios de Issues), previa autorización para pruebas contra entorno productivo, más observabilidad del pool de conexiones y recursos de aplicación. No ejecutar automáticamente una prueba de estrés sobre Render/Aiven Free.

## 13. Latencia de peticiones autenticadas mediante navegador — Figura 55

**Fuente:** captura de Chrome DevTools, pestaña Network > Fetch/XHR, durante una sesión autorizada en el frontend de Render. El usuario pulsó una vez el botón **Consultar datos protegidos**, con rol `CLIENTE`, JWT activo y respuesta HTTP válida.

| Petición observada | HTTP Status | Tiempo de red mostrado por Chrome | Tamaño mostrado |
|---|---:|---:|---:|
| `categorias` | **200 OK** | **2,96 s** | 0,3 kB |
| `productos` | **200 OK** | **3,16 s** | 0,4 kB |

**Resultado técnico:** los dos endpoints protegidos respondieron correctamente, pero ambas solicitudes tardaron cerca de tres segundos en **esta observación puntual de cliente**. No se ha calculado media ni p95 de una muestra de peticiones autenticadas; los valores son tiempos individuales de Chrome. No se ha identificado la causa de la latencia. Entre las hipótesis por comprobar figuran arranque en frío del servicio, tiempos de backend/DB, establecimiento de conexión y red. Una prueba puntual no permite atribuir la causa a ninguna de ellas.

**No comparar como iguales:** el k6 de Figuras 53–54 midió **`GET /` público**, con 2 VUs y p95 **127,26 ms**. Figura 55 mide solicitudes protegidas con JWT mediante un **navegador**, sin VUs. Son cargas, rutas y contextos distintos.

**Siguiente experimento seguro y reproducible:** con la misma sesión autorizada y sin refrescar la página, mantener la pestaña Network abierta y pulsar una sola vez `Consultar datos protegidos` para obtener una segunda lectura de `categorias` y `productos`. Registrar `Status`, `Time`, si el servicio estaba despierto y el intervalo desde la primera lectura. Esto generaría únicamente dos solicitudes adicionales, no una prueba de estrés. Dejar de repetir si aparecen errores. **No abrir ni mostrar Request Headers, Authorization o tokens.**

**Figura N.º 55:** `APF3_E55_Tiempos_API_Protegida_Chrome_Network.png` — *Medición en Chrome DevTools de las peticiones protegidas de categorías (HTTP 200, 2,96 s) y productos (HTTP 200, 3,16 s) con sesión JWT activa en Render.* La imagen para el informe conserva las filas y tiempos pero recorta el correo y cabecera del navegador.

**Limitación del criterio 5:** esta evidencia agrega medición funcional auténtica de latencia de API protegida, pero sigue pendiente una muestra suficiente para percentiles/p95 de la API, aislamiento de arranque en frío y observabilidad de CPU/RAM de Render y del pool HikariCP.
