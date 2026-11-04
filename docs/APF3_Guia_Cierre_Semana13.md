# Guía de cierre del APF3 de Delicias Peruanas

Revisión documental del 04/11/2026. Base: Guía de Presentación del APF3, Semana 13, Curso Integrador II Sistemas, Mgtr. César Augusto Cárdenas Latorre. Responsable: Ronald Albert Bustamante Villanueva.

## Ruta de trabajo

| Etapa | Trabajo | Criterio de cierre | Situación |
|---|---|---|---|
| 1 | Alinear el informe con los seis puntos, actualizar métricas y completar cambio por ámbito y calendario | Word revisado visualmente, datos coincidentes con figuras | Revisión en curso |
| 2 | Ordenar documentos de GitHub | Estado vigente separado del historial; PR y CI satisfactorios | Este cambio |
| 3 | Contrastar observaciones originales del APF2 | Cada comentario del docente asociado a corrección y evidencia | Falta el registro original |
| 4 | Preparar presentación y ensayo | Seis apartados, evidencias legibles y demo preparada | Pendiente |

No se repiten las reservas, los tres EXPLAIN ni las capturas ya verificadas. No se declaran requisitos independientes de la guía general la consulta de mesas en producción ni las métricas runtime de HikariCP. Se conservan como mejoras técnicas; pueden ser obligatorias si la retroalimentación original lo exige.

## Matriz de cambios por ámbito

| Ámbito | Control previo y responsable | Validación | Reversión |
|---|---|---|---|
| Infraestructura | RFC de plan, variables, conexiones o despliegue; propietario aprueba, responsable técnico ejecuta y registra | Disponibilidad, conexiones, latencia y errores antes/después | Recuperar configuración estable; rollback de código no restaura MySQL |
| Aplicaciones | Rama, PR, CI y autorización antes de integrar | Compilación, pruebas y commit Live si cambia backend | Revertir mediante PR y restaurar versión estable cuando proceda |
| Documentación | Revisar contenido, fechas, evidencias y versiones; Ronald mantiene el documento | Coherencia entre informe, acta y notas; revisión visual de Word | Nueva versión o corrección trazable, conservando historia |
| Calendario | Fecha prevista, ventana, dependencias y responsable en RFC | Registrar inicio/fin y diferencias entre plan y ejecución | Reprogramar con causa e impacto; no omitir pruebas |

## Calendario de cambios

| Hito | Fecha o ventana | Dependencia | Estado |
|---|---|---|---|
| Optimización del listado, PR 58 | 03/11/2026, Live 23:33 | Diez pruebas aprobadas | Ejecutado, Figuras 60–64 |
| Permisos de mesas, PR 59 | 03/11/2026, Live 23:46 | Catorce pruebas aprobadas | Ejecutado, Figuras 65 y 69 |
| Revisión documental | 04/11/2026 | Guía Semana 13, métricas y capturas | En curso |
| Contraste de observaciones APF2 | 04/11/2026, propuesto | Comentario original del docente | Pendiente |
| Correcciones técnicas exigidas | Después del contraste y antes de la entrega | RFC, CI y ventana acordada si modifican backend | Condicionado |
| Presentación y ensayo | Tras el informe corregido, antes de Semana 13 | Selección de evidencias y fecha oficial de entrega | Planificado |

Si falta autorización, CI, disponibilidad o evidencia, se registra causa y nueva ventana en la RFC. La fecha oficial se confirma en el aula virtual; no se inventa una fecha de exposición.

## Datos de capacidad que debe incluir el informe

- Figura 49: memoria Aiven 86 %, disco 30,2 % de 1 GB; gráfica CPU sin valor puntual etiquetado.
- Figura 50: Threads_connected = 2, lectura puntual global.
- Figura 51: Max_used_connections = 13, pico global desde el arranque.
- Figura 52: max_connections = 76, límite configurado.
- El pico equivale a aproximadamente 17,1 % del límite; no representa capacidad garantizada de usuarios ni métricas exclusivas de HikariCP.
- k6: 2 VUs, 24/24 respuestas 200, p95 127,26 ms, solo GET público.
- API protegida: conservar también la medición de 9,18 s; la última captura registra 774 ms y 773 ms.

## Observaciones que requieren comprobación

OpenAPI/Swagger y sincronización de main/develop aparecen como mejoras pendientes. Solo el registro original del docente permite distinguir recomendación de corrección obligatoria del punto 1. No se cambian sus estados a completados sin pruebas.

## Presentación prevista

Portada, observaciones APF2, retrospectiva Sprint 6, solicitudes, gestión del cambio, capacidad, despliegue y conclusiones. Puede separarse capacidad o solicitudes en dos diapositivas si la legibilidad lo requiere. Usar blanco y negro con rojo oscuro o verde. Las 66 capturas son respaldo del informe; seleccionar pocas para la exposición.

## Evidencia de cierre técnico disponible

PR 58 y 59 integrados, 14 pruebas aprobadas, JOIN en Render, reserva 201, rechazo 409 y cancelación del dato ID 5; último backend verificado Live 16b5cfe. Acta canónica: [APF3_Acta_Publicacion_y_Verificacion.md](APF3_Acta_Publicacion_y_Verificacion.md).
