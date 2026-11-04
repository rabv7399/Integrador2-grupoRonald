# Guía de cierre del APF3 de Delicias Peruanas

Revisión documental del 04/11/2026. Base: Guía de Presentación del APF3, Semana 13, Curso Integrador II Sistemas, Mgtr. César Augusto Cárdenas Latorre. Responsable: Ronald Albert Bustamante Villanueva.

## Ruta de trabajo

| Etapa | Trabajo | Criterio de cierre | Situación |
|---|---|---|---|
| 1 | Alinear el informe con los seis puntos, actualizar métricas y completar cambio por ámbito y calendario | Word revisado visualmente, datos coincidentes con figuras | Pendiente hasta cerrar las verificaciones técnicas |
| 2 | Ordenar documentos de GitHub | Estado vigente separado del historial; PR y CI satisfactorios | Calendario actualizado; cierre documental en curso |
| 3 | Contrastar observaciones originales del APF2 | Cada comentario del docente asociado a corrección y evidencia | Contraste realizado; evidencias 70–76 |
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

Responsable de los hitos: Ronald Albert Bustamante Villanueva. Las horas Live se transcriben de las capturas de Render; no representan fechas previstas.

| Hito | Fecha o ventana | Dependencia | Estado |
|---|---|---|---|
| Optimización del listado, PR 58 | 03/11/2026, Live 23:33 | Diez pruebas aprobadas | Ejecutado, Figuras 60–64 |
| Permisos de mesas, PR 59 | 03/11/2026, Live 23:46 | Catorce pruebas aprobadas | Ejecutado, Figuras 65 y 69 |
| OpenAPI/Swagger, PR 62 | 04/11/2026, Live 00:37 | CI y despliegue 7b08b3a | Validado en producción: interfaz, 401 sin JWT y 200 con JWT; Figuras 70–73 |
| Sincronización de main y develop, PR 64 | 04/11/2026 | Integración y comparación de ramas | Completada al verificar; Figuras 74–75 |
| CI de la versión consolidada | 04/11/2026 | GitHub Actions, ejecución 41782116127 | Diecisiete pruebas, cero fallos, errores u omisiones; BUILD SUCCESS, Figura 76 |
| Contraste de observaciones APF2 | 04/11/2026 | Comentarios originales proporcionados por el docente | Realizado: preparación ante inactividad documentada, Swagger validado y ramas sincronizadas |
| Revisión de documentación de cierre | 04/11/2026, en curso | Guía Semana 13, métricas y evidencias verificadas | Actualización del calendario; informe reservado para el final |
| Correcciones adicionales | Antes de la entrega, si se identifica una brecha | RFC, CI y ventana acordada si modifican backend | Condicionado a una necesidad comprobada |
| Presentación y ensayo | Tras cerrar verificaciones e informe; fecha oficial por confirmar | Selección de evidencias y calendario del aula virtual | Planificado |


Si falta autorización, CI, disponibilidad o evidencia, se registra causa y nueva ventana en la RFC. La fecha oficial se confirma en el aula virtual; no se inventa una fecha de exposición.

## Datos de capacidad que debe incluir el informe

- Figura 49: memoria Aiven 86 %, disco 30,2 % de 1 GB; gráfica CPU sin valor puntual etiquetado.
- Figura 50: Threads_connected = 2, lectura puntual global.
- Figura 51: Max_used_connections = 13, pico global desde el arranque.
- Figura 52: max_connections = 76, límite configurado.
- El pico equivale a aproximadamente 17,1 % del límite; no representa capacidad garantizada de usuarios ni métricas exclusivas de HikariCP.
- k6: 2 VUs, 24/24 respuestas 200, p95 127,26 ms, solo GET público.
- API protegida: conservar también la medición de 9,18 s; la última captura registra 774 ms y 773 ms.

## Contraste de observaciones APF2

| Comentario del docente | Acción y comprobación | Alcance |
|---|---|---|
| Considerar el cold start de Render Free | Preparación manual de la demo y primera consulta documentadas en [OpenAPI y preparación de demo](APF3_OpenAPI_y_Preparacion_Demo.md) | No se configuró un ping periódico; no se atribuye sin diagnóstico la medición de 9,18 s a un cold start |
| Integrar documentación interactiva | SpringDoc OpenAPI/Swagger publicado; interfaz y consultas protegidas comprobadas, Figuras 70–73 | Swagger accesible y JWT conservado en operaciones protegidas |
| Mantener commits granulares, ramas sincronizadas y evitar secretos | PR 64 integrado; comparación sin diferencias, Figuras 74–75; CI con 17 pruebas, Figura 76 | Revisión de patrones limitada a 60 archivos de texto de la revisión 7b08b3a, sin coincidencias de los patrones examinados; no equivale a una auditoría histórica completa |

## Presentación prevista

Portada, observaciones APF2, retrospectiva Sprint 6, solicitudes, gestión del cambio, capacidad, despliegue y conclusiones. Puede separarse capacidad o solicitudes en dos diapositivas si la legibilidad lo requiere. Usar blanco y negro con rojo oscuro o verde. Las evidencias numeradas son respaldo; seleccionar pocas para la exposición.

## Evidencia de cierre técnico disponible

PR 58, 59, 62 y 64 integrados; 17 pruebas aprobadas, JOIN en Render, reserva 201, rechazo 409 y cancelación del dato ID 5. Swagger verificado en producción con el backend Live 7b08b3a. La comparación de main y develop confirmó igualdad de contenido (Figura 75); no constituye evidencia de un despliegue posterior en Render. Acta canónica: [APF3_Acta_Publicacion_y_Verificacion.md](APF3_Acta_Publicacion_y_Verificacion.md).
