# APF3 | Levantamiento de observaciones APF2, Sprint 6 y control de cambios

## Estado vigente al 04 de noviembre de 2026

La revisión vigente incorpora los PR #58, #59, #62 y #64. Swagger se verificó en producción con el backend **7b08b3a Live** (Figuras 70–73). La versión consolidada aprobó **17 pruebas**, sin fallos, errores ni omisiones (Figura 76). La comparación de ramas confirmó igualdad de contenido (Figura 75); el calendario se actualizó mediante PR #65.

La reserva de prueba devolvió **201**, su repetición **409** y el registro ID 5 quedó **CANCELADA** (Figuras 66–68). El contraste de los comentarios originales del docente está documentado en la [guía de cierre](APF3_Guia_Cierre_Semana13.md), Figuras 78–79.

Capacidad verificada: memoria Aiven 86 %, disco 30,2 %, conexiones puntuales 2, pico histórico 13 y límite 76 (Figuras 49–52). Las conexiones son valores globales de MySQL; no representan métricas exclusivas de HikariCP. La observabilidad runtime del pool sigue pendiente. El sondeo autenticado básico se completó con 20/20 lecturas válidas, dos trabajadores, 20,56 s y 0,973 RPS; los datos se conservaron y recalcularon en PR #70 (Figura 93). La carga sostenida y la repetición de la muestra siguen pendientes.

El proyecto es individual. Ronald Albert Bustamante Villanueva asume las funciones de desarrollo, pruebas, despliegue y seguimiento. Esta retrospectiva es una revisión individual de registros técnicos; no acredita una reunión de equipo ni aprobaciones independientes. La presentación, el ensayo y la aceptación final permanecen pendientes. El informe se completará al terminar las verificaciones técnicas.

Las secciones 1–4 conservan el diagnóstico histórico; sus cifras y pendientes no sustituyen este estado vigente. La sección 5 presenta la retrospectiva actualizada.

### Período documentado de la revisión

**Ventana de actividades examinadas: 02–04 de noviembre de 2026 (hora de Perú, UTC−05:00).** Es una reconstrucción retrospectiva del período respaldado por los registros seleccionados, no una declaración de las fechas originales de planificación del Sprint 6 ni de su duración total.

| Hito trazable | Fecha local de Perú | Fuente |
|---|---|---|
| Registro de solicitud de información de reservas | 02/11/2026, 18:48 | [Issue #36](https://github.com/rabv7399/Integrador2-grupoRonald/issues/36), creada 02/11 a las 23:48 UTC |
| Apertura del cambio de CI y mediciones APF3 | 02/11/2026, 19:45 | [PR #39](https://github.com/rabv7399/Integrador2-grupoRonald/pull/39), creado 03/11 a las 00:45 UTC |
| Integración de CI y documentación inicial | 03/11/2026, 00:02 | PR #39, integrado 03/11 a las 05:02 UTC |
| Sondeo autenticado con 20 lecturas válidas | 04/11/2026, inicio registrado 19:05 | [JSON de mediciones](../performance/results/APF3_sondeo_autenticado_20261104_191041.json), Figura 93 |
| Incorporación de la evidencia y análisis de capacidad | 04/11/2026, 21:53 | [PR #70](https://github.com/rabv7399/Integrador2-grupoRonald/pull/70), integrado 05/11 a las 02:53 UTC |

**Fecha de corte de esta retrospectiva:** 04/11/2026. **Objetivo del ciclo revisado:** consolidar la gestión del servicio APF3 mediante solicitudes, control de cambios, mediciones y verificación de publicación. **Participante y responsable:** Ronald Albert Bustamante Villanueva, trabajo individual.

Las fechas formales de inicio y fin del Sprint 6 no constan en la retrospectiva ni en los registros revisados. No se inventa un sprint de dos semanas ni una ceremonia celebrada. La guía solicita resultados, impedimentos y mejoras; esta revisión los sustenta con una ventana documental delimitada. Como mejora del proceso, registrar objetivo, fechas previstas y criterios de cierre antes del próximo sprint, y contrastarlos con las fechas reales al terminar.

## Registro histórico de preparación y diagnóstico


**Proyecto:** Delicias Peruanas · **Fecha de consolidación:** 03/11/2026  
**Criterio de evidencia:** hechos verificados en GitHub, capturas de Render/Aiven y registros de prueba. No se atribuyen entrevistas ni acuerdos de equipo que no consten en registros.

## 1. Levantamiento de observaciones del APF2

| Observación del docente | Acción APF3 / evidencia | Estado |
|---|---|---|
| Posibles demoras por arranque en frío del plan gratuito Render | Se registró advertencia del proveedor sobre retrasos de 50 s o más; se añadió calentamiento separado y timeout controlado al workflow `apf3-sondeo-http.yml`. Se documentó el incidente de disponibilidad y la dependencia de Aiven. | **Atendida documentalmente y con sondeo**; no elimina el arranque en frío |
| Recomendar SpringDoc OpenAPI / Swagger | Se evaluó como mejora para documentar los endpoints de Spring Boot y simplificar pruebas. | **Pendiente de implementación y verificación**; no afirmar que Swagger esté disponible |
| Mantener commits granulares y ramas sincronizadas | Se usó rama `apf3-ci`, PR #39, ejecución CI Java 21/Maven, historial de commits y fusión a `develop` (merge commit `5968ed10df4e3cb165bfec1cda843aa8b8439e28`). | **Verificada al cierre:** PR #46 integró APF3 a `main`, CI exitosa y Ruleset activo (Figura 45). |
| Evitar secretos en texto plano | El backend usa variables `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`. CI utiliza H2 y clave ficticia de prueba. | **Verificado en los archivos revisados**; no sustituye auditoría de secretos del repositorio completo |

**Nota:** el APF2 recibió calificación 20/20 según el registro académico comunicado; las observaciones anteriores se consideran oportunidades de mejora, no fallos ya corregidos automáticamente.

## 2. Retrospectiva Sprint 6

**Período:** actividades verificadas del ciclo de preparación APF3 hasta el 03/11/2026. No se han aportado actas con fechas formales de inicio y cierre del Sprint 6; esta retrospectiva es documental y no una ceremonia firmada.

| Dimensión | Evidencia o reflexión |
|---|---|
| **Qué funcionó** | GitHub Issues permitió clasificar tres solicitudes (#36–#38); Projects hizo visible el estado `Todo`; el PR #39 concentró los cambios de CI, medición y documentación; `mvnw clean verify` terminó con 8 pruebas, 0 fallos. |
| **Impedimentos** | Primeros sondeos HTTP agotaron el tiempo de espera; Render registró errores de arranque/JDBC; la instancia MySQL en Aiven fue observada `Powered off`; el login no estaba validado en la fase inicial; posteriormente se verificó en producción con JWT (Figuras 39 y 43); Render Free no expone CPU/RAM de la aplicación. |
| **Lecciones** | Una respuesta 401 demuestra alcance HTTP y aplicación de seguridad, no éxito de negocio. Un `BUILD SUCCESS` no demuestra despliegue. Las capturas deben distinguir el commit publicado y el commit solo integrado en GitHub. |
| **Qué mejorar** | Mantener una lista de comprobación previa a demostraciones, registrar incidentes, añadir pruebas de API autenticada con cuentas autorizadas, formalizar aprobaciones de accesos y completar medición de capacidad con k6 y `EXPLAIN`. |

### Acciones de mejora con responsable y criterio de cierre

| Acción | Responsable propuesto | Plazo propuesto | Criterio de cierre verificable |
|---|---|---|---|
| Verificar Render `Live` con commit nuevo y HTTPS | Responsable de despliegue | Antes de entrega APF3 | Captura de Render con commit y acceso comprobado |
| Implementar documentación OpenAPI si se prioriza | Responsable backend | Próximo ciclo | Ruta Swagger disponible y CI satisfactoria |
| Ejecutar k6 de lectura limitada y registrar p95/errores | Responsable QA | Antes de cerrar sección 5 | Log y artefacto de ejecución, sin falsos SLA |
| Revisar tres consultas con `EXPLAIN` y sus índices | Responsable BD | Próximo ciclo | Salidas reales `EXPLAIN`, plan e índices revisados |
| Formalizar aprobación y cierre de accesos | Responsable del servicio | Próximo ciclo | Tickets con aprobador, ejecutor, fecha y conformidad |

**Carácter de la retrospectiva:** síntesis analítica de los registros técnicos disponibles, no acta firmada de una reunión Scrum ni testimonio de participantes no documentados.

## 3. Gestión del cambio — ITIL 4 y CI

### Política de clasificación

- **Estándar:** cambio repetitivo, de bajo riesgo y preautorizado por procedimiento; por ejemplo, corrección editorial que no cambia comportamiento.
- **Normal:** requiere evaluación, pruebas y aprobación por posible impacto operativo; ejemplo: integrar nuevos workflows y documentación en `develop` cuando Render puede autodesplegar.
- **Emergencia:** corrección urgente ante caída severa, con autorización acelerada, evidencias y revisión posterior; no se afirma que se haya ejecutado un cambio de emergencia APF3.

### Trazabilidad

- **PR #39:** `apf3-ci → develop`, integró CI Java 21, sondeo HTTP y documentación; ejecución previa CI exitosa con 8 pruebas. La fusión fue autorizada por el propietario en la conversación de trabajo, pero **GitHub no registra una aprobación formal de revisor**; no confundir ambas cosas.
- **RFC-APF3-02:** [Issue #40](https://github.com/rabv7399/Integrador2-grupoRonald/issues/40), propuesta de documentación y validación ligera de capacidad antes de modificar `develop`.
- **Procedimiento objetivo:** registrar RFC → evaluar impacto y SoD → crear rama → PR → checks → revisión/aprobación → merge → verificar despliegue y reversión.

### Plan de reversión (backout)

Si una integración genera errores: registrar hora, commit, síntoma y logs; comprobar MySQL en Aiven; detener cambios concurrentes; restaurar la última revisión estable de Render si está disponible y revertir el commit mediante un PR. Comprobar HTTPS y endpoints autorizados. **No** modificar registros productivos ni difundir secretos durante el diagnóstico.

### Riesgos residuales

Render Free puede suspender instancias por inactividad y su panel no muestra CPU/RAM de la aplicación; MySQL Aiven ha sufrido indisponibilidad; el plan gratuito o de prueba puede variar en vigencia. El riesgo debe registrarse y no maquillarse como SLA de disponibilidad cumplido.

## 4. Referencias de evidencia

- [PR #39](https://github.com/rabv7399/Integrador2-grupoRonald/pull/39) y merge `5968ed1`.
- [CI exitosa del último commit antes de integrar](https://github.com/rabv7399/Integrador2-grupoRonald/actions/runs/41581205003).
- [Sondeo HTTP documentado, 11 respuestas](https://github.com/rabv7399/Integrador2-grupoRonald/actions/runs/41576386565).
- `docs/APF3_Despliegue_Notas_Version.md` y `docs/APF3_Acta_Publicacion_y_Verificacion.md`; las capturas 41–44 acreditan el despliegue final y su regresión funcional.


## 5. Retrospectiva individual de cierre — Sprint 6 (actualizada 04/11/2026)

**Naturaleza:** revisión individual de las actividades del Sprint 6 sustentada en GitHub, Render, Aiven y pruebas. Responsable: **Ronald Albert Bustamante Villanueva**. No se cuenta con fechas formales de inicio y fin del sprint ni con acta firmada de ceremonia Scrum. Los roles siguientes son funciones desempeñadas por la misma persona; no constituyen revisores independientes.

### 5.1 Balance verificable del ciclo

| Aspecto | Evidencia de APF3 | Resultado |
|---|---|---|
| Despliegue de versión final | Figura 41: Render `develop` / `8b07733` / `Live`; Figura 42: interfaz HTTPS muestra APF3 | **Verificado** |
| Pruebas funcionales postdespliegue | Figura 43: login JWT satisfactorio; Figura 44: categorías/productos protegidos HTTP 200 | **Verificado**; además se comprobó reserva de prueba 201, repetición 409 y cancelación del dato ID 5 (Figuras 66–68) |
| Integración y protección del código | PR #64 integrado, ramas sin diferencias (Figuras 74–75), Ruleset observado Active (Figura 45), CI con 17 pruebas (Figura 76) | **Verificado**. No se exigen aprobaciones humanas: mínimo 0 |
| Gestión de solicitudes | Issues #36–#38, plantilla Figura 46, caso SIM-REQ-INFO-01 #48 con etapas y cierre simulado Figura 47 | **Proceso documentado**; sin alta de usuarios ni SLA real medido |
| Capacidad de sistema y BD | k6 2 VU × 12 s, 24/24 HTTP 200, p95 127,26 ms en GET /; tres EXPLAIN Aiven y HikariCP parametrizado | **Medición ligera**; no prueba estrés ni CPU/RAM productivos |

### 5.2 ¿Qué funcionó? ¿Qué impedimentos hubo? ¿Qué aprendimos?

- **Funcionó:** cambios mediante PR, pruebas Maven/H2, control RFC, publicación en Render y verificación de JWT. Se integró Swagger (PR #62), se comprobaron respuestas 401 sin JWT y 200 con JWT (Figuras 70–73), se sincronizaron las ramas (PR #64) y se aprobaron 17 pruebas (Figura 76).
- **Impedimentos observados:** arranque en frío y una incidencia previa en Render, Aiven temporalmente `Powered off`, desfase del commit publicado debido a `Root Directory=backend/` mientras los cambios anteriores solo tocaban documentación/workflows, falta de métricas de CPU/RAM del plan gratuito.
- **Aprendizaje clave:** `BUILD SUCCESS`, despliegue exitoso, revisión `Live` y prueba funcional autenticada son evidencias **distintas y complementarias**. Una respuesta HTTP 401 no representa un login exitoso; un caso cerrado como simulación no demuestra SLA real.
- **No demostrado:** aprobación independiente de altas ADMIN/OPERADOR, auditoría inmutable, operación de reservas de clientes reales, disponibilidad contractual y ceremonia Scrum firmada. Sí se verificó una reserva productiva de prueba, su rechazo por superposición y la cancelación posterior del dato (Figuras 66–68).

### 5.3 Acciones de mejora con responsable y condición de cierre

Responsable de todas las acciones: **Ronald Albert Bustamante Villanueva**, según la función indicada.

| Acción | Función | Plazo objetivo | Criterio verificable | Estado |
|---|---|---|---|---|
| Publicar APF3 con HTTPS y revisión correcta | Despliegue | Verificado al 04/11/2026 | Render Live 7b08b3a y Swagger en producción, Figuras 70–73 | Completada para la revisión comprobada |
| Validar consultas y regla de reservas | Pruebas | Verificado al 04/11/2026 | Consultas protegidas 200; reserva de prueba 201, repetición 409 y cancelación, Figuras 66–68 y 73 | Completada para los casos ejecutados |
| Sincronizar main/develop y verificar CI | Control de versiones | Verificado al 04/11/2026 | PR #64, comparación idéntica y 17 pruebas satisfactorias, Figuras 74–76 | Completada; mantener tras cada cambio |
| Registrar solicitudes y recorrido de atención | Gestión del servicio | Cierre APF3 | Plantilla y caso simulado #48, Figuras 46–47 | Demostración documental completada; solicitudes reales pendientes |
| Integrar SpringDoc/Swagger | Desarrollo backend | Completado al 04/11/2026 | PR #62, interfaz pública y seguridad JWT comprobadas, Figuras 70–73; CI, Figura 76 | Completada |
| Medir endpoints autenticados y ampliar gradualmente | Pruebas | Sondeo básico completado 04/11/2026; ampliación en próximo ciclo | JSON original, carga, duración, p95, errores y alcance con cuenta autorizada | Sondeo Python completado, Figura 93 y PR #70; ampliación con k6/carga sostenida pendiente |
| Completar métricas CPU/RAM y del pool | Infraestructura | Próximo ciclo, sujeto a observabilidad disponible | Lecturas con fecha, intervalo y fuente; distinguir MySQL de HikariCP | Pendiente |
| Formalizar aprobación de accesos y segregación | Gestión del servicio | Antes de un alta real privilegiada | Aprobador autorizado distinto del ejecutor, mínimo privilegio y registro de cierre | Pendiente; el trabajo individual no acredita segregación independiente |

Los plazos pendientes son objetivos académicos, no compromisos de SLA. No se atribuyen acuerdos de reunión ni aprobaciones externas.

### 5.4 Indicadores y cierre de la retrospectiva

- **CI:** [ejecución 41782116127](https://github.com/rabv7399/Integrador2-grupoRonald/actions/runs/41782116127), 17 pruebas, cero fallos, errores u omisiones y BUILD SUCCESS (Figura 76).
- **Regresión productiva:** Render Live, HTTPS, login JWT y consulta de categorías/productos verificados con Figuras 41–44. No inferir disponibilidad anual a partir de estas pruebas.
- **SLA de solicitudes reales:** no evaluable; los tickets #36–#38 continúan abiertos y el #48 es una simulación, excluida de tasas de satisfacción/atención real.
- **Rendimiento:** el p95 de k6 corresponde a `GET /` con carga ligera, no a transacciones autenticadas ni a saturación. Los `EXPLAIN` de MySQL expresan planes estimados y uso de índices, no tiempos reales.

**Figura 48 (captura aportada y verificada):** `APF3_E48_Retrospectiva_Sprint6_Acta_Documental_GitHub.png`. La captura muestra el encabezado del acta documental, la advertencia de que no representa una ceremonia Scrum firmada, y la matriz de cinco dimensiones del balance verificable. **No** demuestra una reunión efectivamente celebrada.

**Estado:** retrospectiva técnica de Sprint 6, con ventana documental 02–04/11/2026, actualizada con logros, problemas, aprendizajes, responsables y mejoras; **validación formal del equipo/docente no acreditada**.
