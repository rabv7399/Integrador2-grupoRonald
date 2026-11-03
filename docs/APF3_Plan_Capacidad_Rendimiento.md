# APF3 | Plan de capacidad y rendimiento del servicio

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
| Pool JDBC | `application.properties` **no fija** `spring.datasource.hikari.maximum-pool-size` | Tamaño real no medido en runtime; el valor de referencia por defecto de HikariCP suele ser 10 si se utiliza su configuración predeterminada | Propuesta inicial: máximo **5** conexiones y mínimo inactivo **1**, **sujeto a ensayo**; no afirmar que esté configurado |
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

**Procedimiento pendiente:** en Aiven abrir consola SQL autorizada, ejecutar `SHOW INDEX FROM reservas;`, `SHOW INDEX FROM usuarios;` y los tres `EXPLAIN`; conservar salidas (ocultando datos sensibles), identificar recorridos `ALL` y decidir índices en una RFC separada. No ejecutar DDL en producción sin respaldo/aprobación.

## 5. Prueba de concurrencia y umbrales

Se incorpora `performance/k6_sondeo_apf3.js`, una prueba **no destructiva** de `GET /` con **2 VUs durante 12 segundos**, que registra latencia, tasa de errores y checks. El workflow asociado guarda resumen JSON y consola. Esta simulación de concurrencia cumple el objetivo de **ejercicio básico**; no acredita soporte de 20 usuarios ni 6 RPS. Los umbrales del script son de control de prueba, no alertas de producción permanentes.

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

**Pendientes reales:** resultado ejecutado de k6, salidas `EXPLAIN`, uso CPU/RAM real del backend, validación de 6 RPS y confirmación del nuevo commit en Render. No se deben marcar como realizados antes de obtener evidencia.
