# Cierre de validación APF3 — criterios de revisión

Responsable: Ronald Albert Bustamante Villanueva. Revisión 04/11/2026. Base: guía del docente para Semana 13; no existe una escala de puntajes adicional aportada.

| Apartado | Criterio verificable de cierre | Estado |
|---|---|---|
| Observaciones APF2 | Tres comentarios originales, acción, evidencia y límites | Documentado; Figuras 70–79 |
| Retrospectiva Sprint 6 | Logros, impedimentos, aprendizaje y compromisos con responsable y criterio | Revisión individual y ventana documental reconstruida 02–04/11/2026 con enlaces a Issues/PR; fechas originales del sprint no acreditadas |
| Solicitudes | Catálogo, procedimiento, herramientas y caso trazable | Documentado; Figuras 82–85; simulación separada de atención real |
| Cambio | Infraestructura, aplicaciones, documentación, calendario, CI y reversión | Documentado y CI comprobada; Figuras 74–78 y 86 |
| Capacidad | Demanda hipotética, mediciones, limitaciones, decisiones y seguimiento | Plan documentado; muestra autenticada exploratoria ejecutada y recalculada, Figura 93; repetición/carga sostenida y observabilidad pendientes |
| Despliegue | SHA Live y comprobación funcional posterior con fecha y alcance | a9897e4 Live observado; 20/20 lecturas autenticadas posteriores, SHA declarado por operador en JSON; no es verificación automática de versión |
| Entregables | Informe consistente, figuras legibles, presentación y ensayo | Pendientes |

## Sondeo autenticado limitado

Archivo: [performance/sondeo_autenticado_apf3.py](../performance/sondeo_autenticado_apf3.py). Requiere Python 3, sin paquetes adicionales.

1. Confirmar en Render el SHA Live y que Aiven esté disponible. Preparar el primer acceso antes de medir; registrar cualquier incidencia.
2. Ejecutar desde la raíz del repositorio: `python performance/sondeo_autenticado_apf3.py`.
3. Introducir localmente el correo y la contraseña de una cuenta de ensayo autorizada. La contraseña se solicita sin eco; no compartir credenciales ni JWT.
4. El script hace un login y dos lecturas de preparación excluidas de la muestra. Ejecuta como máximo 20 lecturas repartidas entre categorías y productos con dos trabajadores y pausa de un segundo por trabajador. Son como máximo 23 solicitudes totales.
5. Cada respuesta debe ser 200 y contener una lista JSON. Ante error, formato inesperado o duración superior a cinco segundos, interrumpe el resto; una petición ya iniciada puede terminar.
6. Introducir el SHA observado en Render cuando se solicite. Es una referencia declarada por el operador, no un SHA obtenido automáticamente desde la aplicación.
7. Conservar el JSON y una captura del resumen; comprobar cantidad prevista/obtenida, errores, preparación excluida, media, mediana, p95, máximo y RPS. El archivo no guarda cuerpo de respuestas, correo, contraseña ni token.

**Estado:** ejecución real completada, JSON conservado y 20 mediciones recalculadas; Figura 93. Véase el análisis en el plan de capacidad. Los ensayos locales previos con respuestas simuladas no se usan como evidencia de producción. La duración incluye pausas; los tiempos por solicitud incluyen red y descarga, no solo servidor. El p95 usa rango más próximo y una muestra pequeña (10 observaciones por ruta si se completa): es exploratorio, no certifica SLA, estrés, capacidad máxima, 20 usuarios o 6 RPS.

## Criterios de análisis posterior

- Comparar únicamente muestras de igual ruta y método; no equiparar GET público con consultas autenticadas.
- Conservar fallos y ejecuciones interrumpidas; no seleccionar solo resultados rápidos.
- Revisar memoria/conexiones Aiven alrededor de la ejecución si hay datos disponibles, con fecha e intervalo; no atribuir sesiones globales a HikariCP.
- Si la muestra es insatisfactoria, diagnosticar antes de ampliar carga. Una ampliación requiere entorno o ventana adecuados; no se ejecuta por defecto.
- La revisión documental no sustituye el informe y la presentación. Incluir seis secciones, enlaces funcionales, leyendas legibles y conclusiones limitadas a los resultados obtenidos. La fecha oficial y las fechas del Sprint 6 deben confirmarse con registros, sin inventarlas.


## Revisión final contra la guía de Semana 13 — 04/11/2026

La guía suministrada exige seis contenidos en informe y presentación; no aporta ponderaciones ni una rúbrica numérica. La valoración siguiente verifica cobertura y evidencia disponible, no garantiza una nota ni declara terminados los entregables.

| Requisito de la guía | Evidencia disponible | Juicio y condición de entrega |
|---|---|---|
| 1. Levantamiento APF2 | Tres comentarios originales contrastados; preparación ante inactividad; Swagger 401/200; ramas y CI; Figuras 70–79 | Contenido cubierto. Incluir acción, evidencia y alcance de cada observación; no afirmar que se eliminó el cold start ni que hubo auditoría histórica completa de secretos. |
| 2. Retrospectiva Sprint 6 | Logros, impedimentos, aprendizajes, responsable y compromisos; Figura 81 y actualización PR #71 | Contenido cubierto como revisión individual. Ventana documental reconstruida 02–04/11; no presentar estas fechas como planificación original ni inventar una reunión de equipo. |
| 3. Gestión de solicitudes | Catálogo de información/acceso, procedimiento de cinco etapas, plantillas, Issues/Projects y simulación #48; Figuras 46–47 y 82–85 | Definiciones, procedimientos y herramientas cubiertos. Distinguir demostración de atención real. Preparar diagrama legible para el informe; Figura 83 es demasiado estrecha para su reproducción sin adaptación. |
| 4. Gestión del cambio | Matriz de infraestructura/aplicaciones/documentación/calendario, RFC, PR, CI y reversión; Figuras 74–78 y 86 | Contenido cubierto. Conservar cronología y separar fechas previstas de ejecuciones; documentar funciones asumidas por el responsable individual. |
| 5. Gestión de capacidad | Plan por fases, demanda hipotética, EXPLAIN, k6 público, JSON autenticado 20/20 y seguimiento Aiven 82 %; Figuras 28–31, 49–54, 87–89 y 93–94 | Análisis y plan disponibles con alcance exploratorio. Explicar resultados, límites, decisión de mantener recursos y seguimiento; no declarar validados 20 usuarios, 6 RPS o SLA. |
| 6. Despliegue final | Docker/Render, SHA a9897e4 Live, HTTPS, Swagger y consultas protegidas posteriores; Figuras 70–73 y 90–93 | Publicación y verificación técnica evidenciadas. Vincular cada prueba con su revisión y no atribuir reservas previas a la última prueba. El SHA del sondeo fue declarado por operador. |

### Pendientes necesarios antes de entregar

1. Consolidar el informe con los seis apartados en el orden de la guía, carátula/datos, conclusiones, enlaces y anexos; conservar el formato APF2 solicitado.
2. Revisar coherencia: fechas y zonas horarias, SHA por evidencia, métricas, estados vigentes e históricos. No copiar como actuales pendientes antiguos ya resueltos.
3. Seleccionar figuras legibles; redibujar el procedimiento de solicitudes desde su fuente, conservar numeración y ocultar credenciales/tokens en cualquier captura.
4. Preparar presentación con los seis contenidos, diseño sobrio y evidencias seleccionadas; realizar ensayo y preparar la demo considerando inactividad de Render.
5. Confirmar fecha oficial de entrega/exposición y cualquier plantilla o indicación adicional del aula virtual. No inventarlas.

### Mejoras técnicas que permanecen planificadas

Repetir y ampliar la muestra autenticada, medir CPU/RAM del backend y HikariCP, validar carga sostenida, ensayar restauración y consultar mesas en producción con rol autorizado. Formalizar aprobaciones independientes antes de altas privilegiadas reales. La guía suministrada no las enumera como pruebas específicas obligatorias; no se declaran realizadas ni se convierten en requisitos adicionales por inferencia. Su necesidad operativa se mantiene en los planes y dependerá de retroalimentación adicional.

**Conclusión de revisión:** los seis contenidos cuentan con material técnico/documental suficiente para comenzar la consolidación de los entregables con sus límites explícitos. La entrega completa sigue pendiente del informe, presentación y revisión de calidad; no equivale a aceptación del docente ni a garantía de 20/20.
