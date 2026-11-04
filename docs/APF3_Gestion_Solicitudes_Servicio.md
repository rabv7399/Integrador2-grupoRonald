# APF3 | Gestión de solicitudes de servicio y accesos

**Sistema:** Delicias Peruanas · **Marco de referencia:** ITIL 4 e ISO/IEC 20000-1 · **Fecha:** 03/11/2026  
**Canal de registro:** GitHub Issues del repositorio; seguimiento visual en [GitHub Projects APF3](https://github.com/users/rabv7399/projects/5/views/1).  
**Alcance:** procedimiento propuesto y tickets de demostración registrados. **No** se afirma que se hayan concedido accesos, resuelto tickets o medido cumplimiento de SLA.

## 1. Catálogo de solicitudes (entregable 1)

| Código y ticket | Tipo | Solicitante y campos obligatorios | Canal / entregable |
|---|---|---|---|
| [REQ-INFO-01 (#36)](https://github.com/rabv7399/Integrador2-grupoRonald/issues/36) | Información: consulta sobre reservas | Nombre, contacto, asunto, fecha/hora de consulta, descripción | Issue; respuesta documentada sobre procedimiento de reservas y cierre confirmado |
| [REQ-ACC-01 (#38)](https://github.com/rabv7399/Integrador2-grupoRonald/issues/38) | Acceso: alta de cuenta OPERADOR | Identificación del solicitante, correo corporativo, motivo, supervisor, alcance y vigencia | Issue; cuenta habilitada **solo tras aprobación** y confirmación al solicitante |
| [REQ-ACC-02 (#37)](https://github.com/rabv7399/Integrador2-grupoRonald/issues/37) | Provisión de recursos/privilegios: permisos ADMIN | Identificación, rol actual, necesidad justificada, alcance, duración, responsable de negocio | Issue; ajuste de permisos o denegación motivada con evidencia de auditoría |

Las solicitudes de acceso se relacionan con los roles `ADMIN`, `OPERADOR` y `CLIENTE` definidos en `SecurityConfig.java`; por ejemplo, la gestión de mesas exige `ADMIN` u `OPERADOR`, y las altas de categorías exigen `ADMIN`. Ningún ticket concede permisos por sí mismo.

## 2. Matriz de gobernanza y segregación de funciones (entregable 2)

| Servicio | Aprobación formal | Aprobador de negocio propuesto | Ejecutor técnico propuesto | Auditoría |
|---|---|---|---|---|
| REQ-INFO-01 | No, salvo consulta sensible | Responsable de atención/soporte | Soporte | Historial de Issue, comentario de respuesta y confirmación |
| REQ-ACC-01 | **Sí** | Supervisor de operaciones del restaurante | Administrador de usuarios distinto del aprobador | Ticket, autorización, registro de alta, rol mínimo, fecha y revocación |
| REQ-ACC-02 | **Sí, reforzada** | Propietario del servicio / responsable del negocio | Administrador de seguridad distinto del aprobador | Ticket, justificación, aprobación, evidencia del cambio, caducidad y revisión |

**Control SoD:** quien solicita no se autoaprueba; quien aprueba no debería ejecutar el cambio privilegiado. Si el equipo real es unipersonal, se documenta la excepción y se solicita validación del docente o responsable del negocio antes de conceder privilegios. **Estado real:** la segregación está diseñada, pero no se han aportado aprobaciones ni ejecuciones de alta reales. El historial de GitHub es trazable, aunque no debe calificarse como registro técnicamente inmutable.

## 3. SLA propuestos e impacto por incumplimiento (entregable 3)

Horario de cómputo **propuesto:** lunes a viernes, 09:00–18:00, excluyendo feriados. El reloj se inicia cuando la solicitud está completa; se documentan las pausas justificadas por falta de información/aprobación.

| Servicio | Prioridad | Primera respuesta (TTR) | Resolución objetivo | Impacto o penalidad referencial |
|---|---|---:|---:|---|
| REQ-INFO-01 | Baja | 4 h laborables | 8 h laborables | S/ 10 por incumplimiento injustificado (valor de ejercicio) |
| REQ-ACC-01 | Media | 2 h laborables | 16 h laborables | S/ 25 por incumplimiento injustificado (valor de ejercicio) |
| REQ-ACC-02 | Alta | 1 h laborable | 8 h laborables | S/ 50 por incumplimiento injustificado (valor de ejercicio) |

**Precisión:** estos montos son **estimaciones académicas**, no multas contractuales vigentes ni costos reales registrados. La penalidad requeriría un SLA formal firmado. No hay datos suficientes para calcular tasas reales de cumplimiento.

**Indicadores futuros:** `TTR = primera respuesta - registro válido`; `TTRsol = cierre - registro válido - pausas justificadas`; `cumplimiento = tickets dentro del plazo / tickets cerrados con datos completos × 100`. Medir por prioridad y por período; no confundir un comentario de seguimiento con la resolución.

## 4. Ciclo de vida de una solicitud de acceso (entregable 4)

1. **Registro.** Solicitante abre GitHub Issue REQ-ACC-01/02, declara necesidad y plazo; se asigna identificador y responsable de seguimiento.
2. **Validación.** Soporte comprueba identidad, datos mínimos, rol requerido, justificación y ausencia de duplicados. Solicita información faltante sin otorgar permisos.
3. **Aprobación.** Supervisor/propietario del servicio aprueba o rechaza por escrito; para `ADMIN` se requiere justificación reforzada y plazo de vigencia. Sin aprobación, el ticket permanece pendiente.
4. **Ejecución.** Personal autorizado configura la cuenta o el rol mínimo necesario; comprueba acceso y ausencia de privilegios excesivos. No se publican contraseñas ni tokens en GitHub.
5. **Cierre y encuesta.** Se registra resultado, fecha, evidencia sin datos sensibles y conformidad del solicitante; se actualiza a `Done`/cerrado únicamente cuando se haya verificado. Se invita a calificar la atención y se programa revisión/revocación del acceso.

**Flujo operativo:** `Todo → In Progress → Done`, con rechazo o espera de aprobación registrados mediante comentarios/etiquetas. Las Issues #36–#38 se crearon y clasificaron; su estado pendiente no equivale a atención finalizada.

### Diagrama operativo con decisiones de control

El siguiente flujo es un **procedimiento diseñado**, no evidencia de altas de usuarios ya ejecutadas. Cada solicitud debe dejar registro de su responsable, fecha, aprobación y resultado.

```mermaid
flowchart TD
    A["1. Registro en GitHub Issue REQ-ACC"] --> B["2. Validacion: identidad, rol, necesidad y vigencia"]
    B --> C{"Datos completos?"}
    C -- "No" --> D["Solicitar correccion y dejar ticket pendiente"]
    D --> B
    C -- "Si" --> E["3. Aprobacion formal por supervisor / propietario"]
    E --> F{"Aprobado?"}
    F -- "No" --> G["Denegacion motivada y registro de decision"]
    G --> K["5. Cierre, notificacion y encuesta"]
    F -- "Si" --> H["4. Ejecucion por administrador distinto del aprobador"]
    H --> I["Validar rol minimo, acceso y fecha de revocacion"]
    I --> J{"Validacion conforme?"}
    J -- "No" --> L["Corregir o revertir acceso; registrar incidente"]
    L --> H
    J -- "Si" --> K
    K --> M["Actualizar Issue y Projects; conservar evidencias"]
```

### Auditoria y conservacion de evidencias

- **Rastro disponible hoy:** ID de Issue, comentarios fechados, PR, checks y commits. Proporcionan trazabilidad, **pero no inmutabilidad garantizada**.
- **Control propuesto para alcanzar mayor rigor:** exportar periodicamente tickets y decisiones a almacenamiento de solo adicion o retencion protegida, con hash SHA-256 y acceso restringido al responsable de auditoria; guardar comprobante de exportacion y periodo de retencion.
- **SoD:** aprobador y ejecutor deben ser personas distintas; cuando el equipo no pueda cumplirlo, documentar la excepcion y obtener validacion externa antes de ejecutar.
- **Estado de implantacion:** exportacion inmutable y controles de retencion **no implementados ni comprobados**. No se atribuyen evidencias inexistentes.

## 5. Evidencias verificables

- **Figura 05:** clasificación y responsable del ticket REQ-INFO-01.
- **Figura 06:** tablero de GitHub Projects con tres solicitudes en `Todo`.
- **Figura 07:** prioridad alta y trazabilidad de REQ-ACC-02.
- **Figura 08:** comentario de seguimiento inicial en REQ-INFO-01.
- [Issue #36](https://github.com/rabv7399/Integrador2-grupoRonald/issues/36), [Issue #37](https://github.com/rabv7399/Integrador2-grupoRonald/issues/37) e [Issue #38](https://github.com/rabv7399/Integrador2-grupoRonald/issues/38).

**Mejora priorizada:** mantener las plantillas de Issues integradas, registrar aprobaciones diferenciadas, formalizar el horario de soporte y medir SLA a partir de tickets cerrados reales.


## 6. Seguimiento operacional verificado — 03/11/2026 (entregable complementario)

Se verificó el estado de los tickets mediante GitHub Issues. La tabla distingue explícitamente **registro y seguimiento interno** de ejecución real, aprobación o cumplimiento SLA.

| Ticket | Prioridad / asignado | Validación documentada | Aprobación y ejecución reales | Estado verificable |
|---|---|---|---|---|
| [REQ-INFO-01, #36](https://github.com/rabv7399/Integrador2-grupoRonald/issues/36) | Baja / `rabv7399` | Se registró la clasificación y una **respuesta modelo no enviada**; faltan pregunta específica, horario y disponibilidad confirmados | No se requiere aprobación general; **sin respuesta real ni confirmación del solicitante** | **Abierto**, pendiente de validación y respuesta |
| [REQ-ACC-02, #37](https://github.com/rabv7399/Integrador2-grupoRonald/issues/37) | Alta / `rabv7399` | Se registró matriz de control: identidad, necesidad ADMIN, justificación, caducidad, SoD | **Sin aprobación ni permisos ADMIN concedidos** | **Abierto**, a la espera de documentación y aprobación |
| [REQ-ACC-01, #38](https://github.com/rabv7399/Integrador2-grupoRonald/issues/38) | Media / `rabv7399` | Se registró matriz de control: identidad, necesidad OPERADOR, autorización, vigencia, SoD | **Sin aprobación ni cuenta OPERADOR creada** | **Abierto**, a la espera de datos y aprobación |

**Registros nuevos:** se añadieron comentarios de seguimiento fechados a [#36](https://github.com/rabv7399/Integrador2-grupoRonald/issues/36), [#37](https://github.com/rabv7399/Integrador2-grupoRonald/issues/37) y [#38](https://github.com/rabv7399/Integrador2-grupoRonald/issues/38). La fecha del comentario no demuestra atención al solicitante ni cumplimiento del SLA.

**Plantilla reutilizable preparada:** [`.github/ISSUE_TEMPLATE/solicitud_servicio_apf3.md`](../.github/ISSUE_TEMPLATE/solicitud_servicio_apf3.md), que recopila clasificación, datos no sensibles, validación, evidencia de autorización, segregación de funciones, tiempos, pausas, prueba, satisfacción y cierre. La plantilla está integrada en el repositorio principal y su disponibilidad en la creación de Issues se comprobó en la Figura 46.

### 6.1 Evidencia disponible y límites académicos

- **Registrado y verificable:** tres tickets identificados con responsable, etiquetas de prioridad, catálogo, cinco etapas del proceso, reglas de aprobación y comentarios de seguimiento.
- **Diseñado pero no ejecutado:** alta OPERADOR, permiso ADMIN, entrega real de información al solicitante, conformidad, encuesta, aprobación independiente, proceso de revocación y exportación a un almacenamiento inmutable.
- **SLA:** objetivos y horario están propuestos. Con **0 tickets cerrados** y sin horas de atención real, el cumplimiento del SLA resulta **no evaluable**, no 100 %. Para medirse correctamente se requieren solicitud válida, primera respuesta, pausas, cierre y confirmación.

### 6.2 Próxima decisión de control

El responsable del proyecto debe confirmar si los tickets #36–#38 representan **casos de prueba académicos** o solicitudes reales con personas autorizadas. Si son ejemplos, es válido conservarlos abiertos con escenarios y decisiones modeladas sin afirmar altas/aprobaciones reales. Si son reales, deberán obtenerse las autorizaciones y datos mínimos por canal seguro antes de cualquier cambio de permisos. El rol de aprobador debe permanecer separado del ejecutor cuando sea posible; de lo contrario, registrar excepción SoD y validación externa.

**Criterio de cierre:** ningún ticket se cerrará como atendido o autorizado mientras falten verificación, comunicación del resultado y constancia de conformidad o denegación.


## 7. Demostración académica completa de cinco etapas — caso SIM-REQ-INFO-01

**Caso registrado:** [Issue #48 — SIM-REQ-INFO-01](https://github.com/rabv7399/Integrador2-grupoRonald/issues/48), cerrado con razón `completed` **exclusivamente como simulación académica**, no como solicitud real atendida. Se identificó un solicitante ficticio sin datos personales. El escenario fue una pregunta informativa: *¿cómo consultar categorías y productos después de iniciar sesión?*

| Etapa ITIL 4 | Evidencia en el Issue #48 | Resultado del ejercicio |
|---|---|---|
| 1. Registro | Título, descripción, etiqueta `solicitud-informacion`, prioridad baja, asignación a responsable | Escenario registrado como **SIMULACIÓN** |
| 2. Validación | Comentario de validación de alcance, pregunta específica y evidencias técnicas | Sin validación de identidad real porque no existe cliente |
| 3. Aprobación | Comentario de decisión sobre la aprobación | **No aplica** a consulta informativa general; sin fingir firmas |
| 4. Ejecución | Respuesta modelo publicada como comentario y fundamentada en Figuras 41, 43 y 44 | Instrucciones verificadas técnicamente, **no enviadas** a cliente real |
| 5. Cierre y encuesta | Comentario de cierre, checklist, razón de cierre y modelo de encuesta no aplicado | **Cierre administrativo de la simulación**; satisfacción no recogida |

**Distinción rigurosa:** en este apartado se demuestra el **diseño y recorrido documental** del proceso, no ejecución de una solicitud recibida de un usuario real. La evidencia real independiente es que la aplicación desplegada ofreció login JWT y devolvió HTTP 200 a una consulta protegida de categorías y productos (Figuras 43 y 44). No se crearon usuarios ni reservas en este escenario.

**Indicadores SLA:** al existir **0 solicitudes reales cerradas** con datos completos y evidencia de entrega/conformidad, la tasa real de cumplimiento de SLA continúa **no evaluable**. El cierre de #48 no se incorpora al denominador de atención real ni genera un tiempo de respuesta imputable al soporte.

**Figura N.º 46:** `APF3_E46_Plantilla_Solicitud_Servicio_GitHub.png` — pantalla de creación de Issues que muestra las dos plantillas activas: RFC de cambios y solicitud de servicio APF3.

**Figura N.º 47 (captura aportada y verificada):** `APF3_E47_Caso_Simulado_Cinco_Etapas_GitHub.png` — Issue #48 con título de **SIMULACIÓN**, estado **Closed**, responsable `rabv7399`, etiquetas `prioridad-baja` y `solicitud-informacion`, y estado **Done** en el proyecto APF3. La imagen muestra la primera etapa y estado de cierre; **las etapas 2–5 están documentadas en cuatro comentarios del Issue**, no todas dentro de esta captura.

**Pendientes reales para evolución del servicio:** obtener una solicitud informativa auténtica o autorización verificable de altas/roles para medir los SLA e implantar SoD operativa; los tickets #36–#38 permanecen abiertos. La demostración no suple esas pruebas.
