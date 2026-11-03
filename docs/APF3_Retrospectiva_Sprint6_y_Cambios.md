# APF3 | Levantamiento de observaciones APF2, Sprint 6 y control de cambios

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


## 5. Acta documental de retrospectiva de cierre — Sprint 6 (actualizada 03/11/2026)

**Naturaleza:** retrospectiva de análisis basada en registros de GitHub, Render, Aiven y pruebas; **no equivale a acta firmada de ceremonia Scrum**. No constan participantes, votos, fecha oficial de inicio/cierre ni moderador de una reunión. El seguimiento técnico en Issues se asignó a `rabv7399`; los responsables funcionales mencionados abajo son **roles propuestos**, no acuerdos atribuidos a personas.

### 5.1 Balance verificable del ciclo

| Aspecto | Evidencia de APF3 | Resultado |
|---|---|---|
| Despliegue de versión final | Figura 41: Render `develop` / `8b07733` / `Live`; Figura 42: interfaz HTTPS muestra APF3 | **Verificado** |
| Pruebas funcionales postdespliegue | Figura 43: login JWT satisfactorio; Figura 44: categorías/productos protegidos HTTP 200 | **Verificado para estos dos flujos**; no implica probar reservas |
| Integración y protección del código | PR #46 fusionó a `main`; Ruleset `main` Active, Figura 45; CI Java 21/Maven del commit final satisfactoria | **Verificado**. No se exigen aprobaciones humanas: mínimo 0 |
| Gestión de solicitudes | Issues #36–#38, plantilla Figura 46, caso SIM-REQ-INFO-01 #48 con etapas y cierre simulado Figura 47 | **Proceso documentado**; sin alta de usuarios ni SLA real medido |
| Capacidad de sistema y BD | k6 2 VU × 12 s, 24/24 HTTP 200, p95 127,26 ms en GET /; tres EXPLAIN Aiven y HikariCP parametrizado | **Medición ligera**; no prueba estrés ni CPU/RAM productivos |

### 5.2 ¿Qué funcionó? ¿Qué impedimentos hubo? ¿Qué aprendimos?

- **Funcionó:** control de commits mediante PR, pruebas Maven/H2, corrección del encabezado a APF3, control RFC, Render Auto-Deploy y verificación de JWT y API protegida con resultado HTTP 200.
- **Impedimentos observados:** arranque en frío y una incidencia previa en Render, Aiven temporalmente `Powered off`, desfase del commit publicado debido a `Root Directory=backend/` mientras los cambios anteriores solo tocaban documentación/workflows, falta de métricas de CPU/RAM del plan gratuito.
- **Aprendizaje clave:** `BUILD SUCCESS`, despliegue exitoso, revisión `Live` y prueba funcional autenticada son evidencias **distintas y complementarias**. Una respuesta HTTP 401 no representa un login exitoso; un caso cerrado como simulación no demuestra SLA real.
- **No demostrado:** Swagger/OpenAPI implementado, aprobación independiente de altas ADMIN/OPERADOR, auditoría inmutable, pruebas de reservas reales, disponibilidad contractual y acta firmada de reunión retrospectiva.

### 5.3 Acciones de mejora con responsable y condición de cierre

| Acción | Responsable propuesto | Plazo objetivo propuesto | Criterio verificable | Estado |
|---|---|---|---|---|
| Publicar APF3 con HTTPS y revisión correcta | Responsable despliegue | Entrega APF3 | Figuras 41–42 y SHA `8b07733` | **Cerrada técnicamente** |
| Repetir login y consulta protegida tras publicar | Responsable QA | Entrega APF3 | Figuras 43–44, JWT y HTTP 200 | **Cerrada técnicamente** |
| Sincronizar/proteger `main` y verificar CI | Responsable Git | Entrega APF3 | PR #46, Ruleset Active Figura 45, check CI | **Cerrada técnicamente** |
| Registrar solicitudes y recorrido ITIL | Responsable servicio | Entrega APF3 | Figuras 46–47, plantilla, Issues y caso #48 | **Demostración documental cerrada**; aprobación real pendiente |
| Ampliar prueba k6 a endpoint autenticado y métricas CPU/RAM | QA / infraestructura | Próximo ciclo | Resultados de VU, p95, errores y captura confiable de uso | **Pendiente** |
| Evaluar SpringDoc / Swagger compatible con Spring Boot | Backend | Próximo ciclo | Ruta OpenAPI accesible y CI verde | **Pendiente** |
| Aprobar accesos reales y demostrar segregación SoD | Negocio / ejecutor distinto | Próximo ciclo | Aprobación firmada o registro real, mínimo privilegio y evidencia | **Pendiente** |

**Precisión:** estas acciones son propuestas sustentadas en datos técnicos; no afirmar que fueron aprobadas mediante reunión con participantes no identificados. Las fechas son objetivos académicos y no compromisos de SLA.

### 5.4 Indicadores y cierre de la retrospectiva

- **CI:** [run #41720071675](https://github.com/rabv7399/Integrador2-grupoRonald/actions/runs/41720071675) `success` para la versión `8b07733`; runs posteriores de `main` documentan sincronización sin alterar producción.
- **Regresión productiva:** Render Live, HTTPS, login JWT y consulta de categorías/productos verificados con Figuras 41–44. No inferir disponibilidad anual a partir de estas pruebas.
- **SLA de solicitudes reales:** no evaluable; los tickets #36–#38 continúan abiertos y el #48 es una simulación, excluida de tasas de satisfacción/atención real.
- **Rendimiento:** el p95 de k6 corresponde a `GET /` con carga ligera, no a transacciones autenticadas ni a saturación. Los `EXPLAIN` de MySQL expresan planes estimados y uso de índices, no tiempos reales.

**Figura 48 (pendiente):** `APF3_E48_Retrospectiva_Sprint6_Acta_Documental_GitHub.png`. Captura del encabezado y matriz de balance de esta sección; acredita el acta documental, **no** una ceremonia Scrum realizada.

**Estado:** retrospectiva técnica de Sprint 6 actualizada con logros, problemas, aprendizajes, responsables y mejoras; **validación formal del equipo/docente no acreditada**.
