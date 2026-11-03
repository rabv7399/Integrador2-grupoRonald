# APF3 | Despliegue, verificación y notas de versión

**Proyecto:** Delicias Peruanas — Curso Integrador II  
**Estado del documento:** registro de la versión publicada y preparación de la integración APF3  
**Fecha de elaboración:** 2 de noviembre de 2026 (Perú; registros de plataformas en UTC)  
**Repositorio:** https://github.com/rabv7399/Integrador2-grupoRonald

> **Alcance y precisión:** este documento distingue entre el sistema APF2 que está publicado y las mejoras APF3 que siguen en revisión. No constituye constancia de que se haya desplegado una nueva versión APF3.

## Actualizacion de estado — 03/11/2026 (prevalece sobre las referencias historicas a PR #39)

- **Integracion de GitHub:** PR [#39](https://github.com/rabv7399/Integrador2-grupoRonald/pull/39) y [#41](https://github.com/rabv7399/Integrador2-grupoRonald/pull/41) ya estan fusionados en `develop`, cuyo commit confirmado es `f56af91aa7d3b0bd99048e42e336a371cf9cebcd`. Los textos del 02/11 que describen PR #39 como abierto representan el estado **historico** de preparacion, no el estado actual.
- **Produccion Render:** la captura de **Events** del 03/11 muestra el servicio `delicias-peruanas-api` en estado `Live`, rama `develop`, **pero commit `be949ef`**. Por tanto, **no se ha verificado el despliegue de APF3**. Se registro el desfase en [DEP-APF3-01, Issue #44](https://github.com/rabv7399/Integrador2-grupoRonald/issues/44).
- **Mejoras adicionales:** PR [#42](https://github.com/rabv7399/Integrador2-grupoRonald/pull/42) sigue en borrador, sin fusionar; su configuracion HikariCP y test automatizado **no** se atribuyen a produccion.
- **Base de datos:** se ejecutaron tres EXPLAIN reales en MySQL Workbench sobre Aiven; los indices utilizados fueron `idx_reservas_mesa_fecha`, `idx_reservas_fecha_estado` y `uq_usuarios_correo`. Evidencias Figuras 28–31, detalladas en `docs/APF3_Plan_Capacidad_Rendimiento.md` de la rama del PR #42.
- **Historial de despliegues verificado (Figura 33):** Render > Deploys muestra 4 registros: `be949ef` exitoso (Manual, hace 23 dias, `Live`), `be949ef` fallido (Auto-Deploy, hace 23 dias), `79d1d87` exitoso (Auto-Deploy, hace 26 dias) y `640dcff` exitoso (First Deploy, hace 26 dias). **No aparece un despliegue de `f56af91`** en el historial visible. No puede inferirse la causa sin revisar la configuracion.
- **Proximo control:** revisar **Settings > Build & Deploy > Auto-Deploy**, rama y filtros en Render para determinar por que la revision publicada esta desfasada. No se ha realizado un despliegue manual ni un rollback en esta etapa.

**Figura 32:** `APF3_E32_Render_Version_Anterior_Detectada.png` — Render `Live` con `be949ef`, frente al commit APF3 integrado en GitHub `f56af91`. Esta imagen documenta una brecha de despliegue, **no** un despliegue final exitoso.

**Figura 33:** `APF3_E33_Historial_Despliegues_Render.png` — Cuatro despliegues historicos, sin intento visible de publicar `f56af91`.

## 1. Arquitectura del entorno desplegado

```text
Navegador web (HTTPS)
    |
    v
Render — delicias-peruanas-api (plan Free, contenedor Docker)
    |  Interfaz estática: index.html, styles.css, app.js
    |  Backend: Java 21, Spring Boot 4.1.1
    |  Seguridad: Spring Security, JWT y BCrypt
    |  Persistencia: Spring Data JPA / Hibernate
    |
    v
Aiven — delicias-peruanas-db (MySQL 8.4, plan Free)
```

- **URL de acceso:** https://delicias-peruanas-api.onrender.com/
- **Repositorio de origen:** `rabv7399/Integrador2-grupoRonald`.
- **Rama de despliegue observada en Render:** `develop`.
- **Commit observado en Render:** `be949ef587f75a62c83bd72d80417ec736522c96` (`be949ef`).
- **Estado observado:** servicio `Live` y página principal accesible mediante HTTPS, después de recuperar MySQL.
- **Docker:** compilación Maven con JDK 21 y ejecución con JRE 21; puerto configurable mediante `PORT`, predeterminado 8080.
- **Variables requeridas por el backend:** `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`. Variables opcionales: `DDL_AUTO`, `JWT_EXPIRATION_MS`, `PORT`, `CORS_ALLOWED_ORIGINS`. Los valores y credenciales no se registran aquí.

El frontend está servido por la misma aplicación Spring Boot desde `backend/src/main/resources/static/`. No se ha identificado un segundo servicio frontend independiente en el repositorio revisado.

## 2. Evidencia y validación de la versión actualmente publicada

| Verificación | Resultado observado | Alcance |
|---|---|---|
| Estado de Render | `Live` | Estado mostrado por la plataforma, no garantiza disponibilidad continua |
| Integración continua del candidato APF3 | `mvnw clean verify`: 8 pruebas ejecutadas, 0 fallos, 0 errores, 0 omitidas; `BUILD SUCCESS` | Verificado en GitHub Actions [41580869865](https://github.com/rabv7399/Integrador2-grupoRonald/actions/runs/41580869865), no sustituye pruebas funcionales en producción |
| HTTPS y página principal | La página Delicias Peruanas se abrió en el navegador | Accesibilidad del frontend |
| Documento `GET /` | HTTP 200; 173 ms | Una medición con Chrome DevTools |
| Carga de la página | 589 ms | Una medición de carga, no un promedio |
| `POST /api/auth/login` | HTTP 401; 678 ms | Un intento con credenciales no aceptadas; confirma respuesta del endpoint, no autenticación exitosa |
| Aiven MySQL | `Running`, un nodo activo | Estado observado tras recuperación |
| CPU de MySQL | Utilización visualmente baja y estable | Gráfica de una hora; sin porcentaje exacto |
| Disco de MySQL | Aproximadamente 30 % | Estimación visual, no valor exportado |
| Memoria de MySQL | Aproximadamente 78–83 % | Estimación visual, requiere seguimiento |
| RAM del backend en Render | Límite de 512 MB | Capacidad asignada, **no** consumo real |
| CPU/RAM de Render | No visibles en el plan Free | Limitación explícita de la plataforma |
| Sondeo HTTP GitHub Actions — `GET /` secuencial | 3/3 HTTP 200; promedio 78,9 ms; máximo 97,5 ms | Sondeo exploratorio, no carga sostenida |
| Sondeo HTTP GitHub Actions — `GET /api/categorias` secuencial | 3/3 HTTP 401; promedio 96,6 ms; máximo 111,6 ms | Endpoint protegido sin JWT; respuesta de seguridad, no éxito funcional |
| Sondeo HTTP GitHub Actions — `GET /api/categorias` con concurrencia 2 | 4/4 HTTP 401; promedio 89,3 ms; máximo 111,9 ms | Solo dos solicitudes simultáneas, no prueba de estrés |

**No se verificaron** un login exitoso, transacciones autenticadas de reservas, pruebas de carga sostenida, SLA real de producción ni una nueva publicación APF3. Estas validaciones permanecen pendientes.

## 3. Incidente de disponibilidad y recuperación

1. Los primeros sondeos HTTP de GitHub Actions agotaron los tiempos de espera; no se obtuvieron respuestas HTTP desde esos ejecutores. Esos resultados se conservaron como evidencia de una prueba no concluyente. En una ejecución posterior, después de la recuperación de MySQL, el sondeo sí obtuvo 11/11 respuestas HTTP y registró tiempos de latencia exploratorios.
2. En Render se observaron eventos `Instance failed` y registros de Hibernate indicando imposibilidad de obtener metadatos JDBC.
3. En Aiven se comprobó que `delicias-peruanas-db` estaba `Powered off`.
4. Se inició la recuperación de la instancia; tras pasar por `Rebuilding`, MySQL apareció `Running`.
5. Posteriormente se verificó que el navegador podía cargar la interfaz web y que `/api/auth/login` devolvía una respuesta HTTP.

**Interpretación:** la indisponibilidad de MySQL explica razonablemente el fallo de inicialización JDBC observado. El arranque en frío de Render Free es otra condición de riesgo de disponibilidad. La posterior recuperación de respuestas HTTP es coherente con la restauración del servicio, pero no demuestra por sí sola la causa de todos los tiempos de espera anteriores.

**Ejecución exitosa del sondeo usada para la Figura 24:** https://github.com/rabv7399/Integrador2-grupoRonald/actions/runs/41576386565 — 11 respuestas HTTP, con un calentamiento inicial `GET /` HTTP 200 de 133,9 ms; promedios detallados en la tabla del apartado 2. Artefacto CSV/JSON/Markdown disponible en la ejecución. No se midieron transacciones autenticadas ni carga sostenida. Existen otras ejecuciones satisfactorias con latencias distintas; las cifras de este documento se vinculan a la Figura 24 para mantener trazabilidad.

**Acciones preventivas propuestas:** verificar el estado de Aiven antes de una demostración; calentar el servicio de Render con antelación; documentar incidentes y recuperaciones; monitorizar memoria de MySQL; mantener un procedimiento de respaldo y restauración comprobado; evitar depender de una única prueba externa para evaluar disponibilidad.

## 4. Cambios preparados para APF3 (todavía no desplegados)

**Pull Request:** https://github.com/rabv7399/Integrador2-grupoRonald/pull/39  
**Rama de trabajo:** `apf3-ci`  
**Rama destino:** `develop`  
**Estado verificado:** PR abierto en borrador, sin fusión.

### Notas de versión — candidato APF3

- **Integración continua:** workflow `.github/workflows/ci-backend.yml` para compilar y ejecutar pruebas de Java 21/Maven con `clean verify`. Las ejecuciones de CI revisadas concluyeron correctamente.
- **Aislamiento de pruebas:** uso de H2 en memoria y clave JWT exclusivamente de prueba; no se usan credenciales reales de Aiven/Render en el workflow.
- **Sondeo HTTP exploratorio:** workflow `.github/workflows/apf3-sondeo-http.yml` con solicitudes limitadas, tiempos de espera y exportación de CSV/JSON/Markdown. Las ejecuciones iniciales fallaron por timeout; una ejecución posterior fue satisfactoria con 11 respuestas HTTP. Se presentan como sondeos exploratorios, no como validación de SLA o estrés.
- **Gestión del servicio:** solicitudes REQ-INFO-01, REQ-ACC-01 y REQ-ACC-02 documentadas en GitHub Issues y tablero de seguimiento.
- **Documentación de despliegue:** registro de arquitectura, incidente, verificaciones, restricciones y procedimiento de publicación en este archivo.

### Evaluación técnica previa a la integración

- **Alcance del cambio:** dos archivos de GitHub Actions y un documento; no se modifican controladores, servicios, entidades ni scripts de base de datos en este PR.
- **Validación de CI:** 8 pruebas automatizadas correctas en el último commit revisado, sin fallos ni errores. Esto no equivale a cobertura completa de las funcionalidades.
- **Riesgo operativo:** Render sigue `develop`; integrar el PR puede provocar una reconstrucción/despliegue aun cuando los cambios sean de CI y documentación. En el plan Free existe riesgo de arranque en frío y dependencia de Aiven.
- **Condiciones de aprobación:** revisión y autorización humana del PR, verificación de checks del commit final, disponibilidad de MySQL y comprobación de la configuración de despliegue de Render.
- **Verificación posterior:** constatar el nuevo commit `Live` y probar HTTPS, frontend y endpoints autorizados. Si falla, consultar logs y aplicar el procedimiento de reversión del apartado 6.
- **Estado:** revisión técnica documentada; no existe aprobación humana registrada ni integración a `develop`.

**Sin cambios funcionales confirmados en producción:** las funcionalidades APF2 de autenticación, productos, categorías, mesas y reservas siguen siendo la base funcional publicada. No se anuncia una API Swagger/OpenAPI como implementada ni una corrección funcional no verificada.

## 5. Lista de control para la publicación APF3

- [x] Identificar rama y commit actualmente publicados en Render.
- [x] Comprobar acceso HTTPS a la interfaz y respuesta del endpoint de login.
- [x] Confirmar recuperación de MySQL en Aiven.
- [x] Registrar evidencias de integración continua y gestión de cambios.
- [x] Realizar revisión técnica del contenido del PR #39 y comprobar CI (8 pruebas satisfactorias).
- [ ] Registrar aprobación humana antes de integrar el PR #39.
- [x] Obtener un sondeo HTTP exitoso posterior a la recuperación (11/11 respuestas); conservar los intentos fallidos como antecedente del incidente.
- [ ] Ejecutar pruebas de carga representativas con cuentas autorizadas y condiciones controladas, si se exige una validación de capacidad sostenida.
- [ ] Decidir si se requiere incorporar SpringDoc/OpenAPI antes de la entrega; actualmente no está integrado.
- [ ] Integrar los cambios aprobados en `develop` mediante PR, sin copiar secretos.
- [ ] Confirmar si Render despliega automáticamente la nueva revisión o requiere despliegue manual.
- [ ] Verificar que Render indique `Live` con el **nuevo commit** (no solo `be949ef`).
- [ ] Ejecutar pruebas posteriores al despliegue: `GET /`, autenticación con cuenta de prueba autorizada, consultas protegidas y una operación de reserva de prueba controlada.
- [ ] Registrar fecha, commit, responsable, resultado y capturas del despliegue.
- [ ] Publicar notas de versión definitivas solo después de completar las comprobaciones anteriores.

## 6. Procedimiento de despliegue y reversión

**Antes:** verificar `Running` en Aiven; confirmar variables de entorno por nombre, sin exponer valores; comprobar la rama de despliegue; revisar resultados CI y el riesgo de interrupción por arranque en frío.

**Durante:** pasar el PR por revisión; fusionar únicamente después de aprobación; comprobar el mecanismo de despliegue configurado en Render; observar registros y estado del nuevo despliegue.

**Después:** confirmar commit nuevo y estado `Live`; comprobar HTTPS, recursos estáticos, autenticación y operaciones con datos mediante una cuenta de prueba autorizada; guardar evidencias y registrar incidencias si ocurren.

**Reversión:** si el despliegue nuevo falla, detener cambios adicionales, consultar logs, recuperar la última revisión estable de Render cuando esa opción esté disponible y revertir el cambio problemático en GitHub mediante un nuevo commit/PR. Verificar de nuevo la conexión a Aiven. No revertir ni modificar datos de producción sin evaluar el impacto y disponer de respaldo apropiado.

## 7. Índice de evidencias relacionadas

- **Figura 09:** integración continua con GitHub Actions.
- **Figura 10:** PR #39 de control de cambios.
- **Figura 11:** checks de GitHub Actions asociados al PR.
- **Figura 12:** medición de carga de la página con DevTools.
- **Figura 13:** medición de la respuesta de `/api/auth/login`.
- **Figura 14:** estado `Live` del servicio en Render.
- **Figura 15:** diagnóstico del fallo JDBC/Hibernate.
- **Figura 16:** MySQL de Aiven `Powered off`.
- **Figura 17:** MySQL recuperado en `Running`.
- **Figura 18:** interfaz accesible tras recuperación.
- **Figura 19:** métricas de red y eventos de Render.
- **Figura 20:** restricción de CPU/RAM en Render Free.
- **Figura 21:** CPU de MySQL en Aiven.
- **Figura 22:** almacenamiento de MySQL en Aiven.
- **Figura 23:** memoria de MySQL en Aiven.
- **Figura 24:** sondeo HTTP ligero exitoso de GitHub Actions, con once respuestas y latencias desglosadas.

Las figuras se integrarán al informe final con sus capturas originales. La numeración no significa que una validación pendiente haya sido completada.
