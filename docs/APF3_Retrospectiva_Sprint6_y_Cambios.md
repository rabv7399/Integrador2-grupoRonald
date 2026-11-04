# APF3 | Levantamiento de observaciones APF2, Sprint 6 y control de cambios

## Estado vigente al 04 de noviembre de 2026

Esta actualización prevalece sobre las referencias históricas que aparecen después. El último backend verificado visualmente en Render es **16b5cfe**, Live (Figura 69). Los PR #58 y #59 están integrados; se aprobaron **14 pruebas**, sin fallos ni errores. La reserva productiva de prueba devolvió **201**, su repetición **409** y el registro ID 5 quedó **CANCELADA** (Figuras 66–68). El PR #42 ya está integrado; no debe interpretarse el borrador histórico como estado actual.

La guía de Semana 13 exige seis apartados. El plan por infraestructura, aplicaciones, documentación y calendario se desarrolla en [Guía de cierre](APF3_Guia_Cierre_Semana13.md). La retrospectiva cuenta ahora con acciones de cierre propuestas para el 04/11/2026 y dependencias explícitas; no se atribuyen reuniones ni aprobaciones externas inexistentes.

Capacidad verificada: **memoria Aiven 86 %, disco 30,2 %, conexiones puntuales 2, pico histórico 13 y límite 76** (Figuras 49–52). Son valores globales de MySQL, distintos del pool HikariCP, cuyo valor predeterminado configurado y probado es 5 / 1 / 30000 ms. La observabilidad runtime del pool sigue pendiente.

Permanecen abiertos el contraste con las observaciones originales APF2, OpenAPI si fue exigido, la revisión de sincronización de ramas y la presentación. Las métricas runtime y mesas en producción se conservan como mejoras; no se atribuyen como requisitos independientes de esta guía general. La aceptación final no está registrada.

El registro detallado de pruebas y mediciones se encuentra en el [acta de verificación](APF3_Acta_Publicacion_y_Verificacion.md). Las cifras antiguas de ocho o nueve pruebas y las menciones a despliegue pendiente describen antecedentes, no el estado vigente.

## Registro histórico de preparación y diagnóstico


**Proyecto:** Delicias Peruanas · **Fecha de consolidación:** 03/11/2026  
**Criterio de evidencia:** hechos verificados en GitHub, capturas de Render/Aiven y registros de prueba. No se atribuyen entrevistas ni acuerdos de equipo que no consten en registros.

## 1. Levantamiento de observaciones del APF2

| Observación del docente | Acción APF3 / evidencia | Estado |
|---|---|---|
| Posibles demoras por arranque en frío del plan gratuito Render | Se registró advertencia del proveedor sobre retrasos de 50 s o más; se añadió calentamiento separado y timeout controlado al workflow `apf3-sondeo-http.yml`. Se documentó el incidente de disponibilidad y la dependencia de Aiven. | **Atendida documentalmente y con sondeo**; no elimina el arranque en frío |
| Recomendar SpringDoc OpenAPI / Swagger | Se evaluó como mejora para documentar los endpoints de Spring Boot y simplificar pruebas. | **Pendiente de implementación y verificación**; no afirmar que Swagger esté disponible |
| Mantener commits granulares y ramas sincronizadas | Se usó rama `apf3-ci`, PR #39, ejecución CI Java 21/Maven, historial de commits y fusión a `develop` (merge commit `5968ed10df4e3cb165bfec1cda843aa8b8439e28`). | **Atendida en la rama de desarrollo**; sincronización con `main` por verificar |
| Evitar secretos en texto plano | El backend usa variables `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`. CI utiliza H2 y clave ficticia de prueba. | **Verificado en los archivos revisados**; no sustituye auditoría de secretos del repositorio completo |

**Nota:** el APF2 recibió calificación 20/20 según el registro académico comunicado; las observaciones anteriores se consideran oportunidades de mejora, no fallos ya corregidos automáticamente.

## 2. Retrospectiva Sprint 6

**Período:** actividades verificadas del ciclo de preparación APF3 hasta el 03/11/2026. No se han aportado actas con fechas formales de inicio y cierre del Sprint 6.

| Dimensión | Evidencia o reflexión |
|---|---|
| **Qué funcionó** | GitHub Issues permitió clasificar tres solicitudes (#36–#38); Projects hizo visible el estado `Todo`; el PR #39 concentró los cambios de CI, medición y documentación; `mvnw clean verify` terminó con 8 pruebas, 0 fallos. |
| **Impedimentos** | Primeros sondeos HTTP agotaron el tiempo de espera; Render registró errores de arranque/JDBC; la instancia MySQL en Aiven fue observada `Powered off`; no se pudo validar un inicio de sesión exitoso; Render Free no expone CPU/RAM de la aplicación. |
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
- `docs/APF3_Despliegue_Notas_Version.md`, que distingue producción APF2 y candidato APF3.
