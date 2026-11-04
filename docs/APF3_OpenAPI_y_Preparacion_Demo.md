# Observaciones APF2 y preparación de la demostración

Retroalimentación original del docente compartida por el responsable el 04/11/2026. Son recomendaciones de mejora para siguientes iteraciones; su seguimiento se incorpora al punto 1 del APF3.

| Observación | Tratamiento | Criterio de cierre |
|---|---|---|
| Gestionar cold start de Render Free | Verificar disponibilidad antes del ensayo; realizar acceso de preparación y separar el primer acceso de mediciones posteriores. No se garantiza actividad permanente ni se elimina el reposo. | Procedimiento y mediciones diferenciadas, incidencia registrada si la primera petición demora |
| Integrar SpringDoc OpenAPI y Swagger | Dependencia 3.2.0 compatible con Spring Boot 4.1 según documentación oficial; rutas de lectura de documentación, esquema bearer JWT y pruebas de integración | CI satisfactoria, despliegue Live y apertura de Swagger y JSON en Render |
| Mantener commits granulares, ramas sincronizadas y secretos fuera del código | PR y CI existentes; revisión de divergencia main/develop pendiente; no se presume una auditoría integral de secretos | Revisión de ramas y configuración de credenciales documentada |

## Documentación interactiva

- Interfaz: /swagger-ui/index.html
- Especificación: /v3/api-docs
- Configuración de la interfaz: /v3/api-docs/swagger-config
- Login y registro se describen como públicos; el resto de operaciones exige JWT en el esquema.
- La política de roles del backend sigue siendo la fuente de autorización. La interfaz no otorga privilegios.
- Si se usa Authorize, introducir el JWT autorizado solo en el navegador. No adjuntar tokens, contraseñas ni respuestas de login a las capturas del informe.
- La comprobación visual se realiza después de confirmar el nuevo commit Live. La integración en GitHub no demuestra publicación.

## Preparación de pruebas en vivo

1. Antes del ensayo, comprobar Aiven Running y el estado de Render.
2. Abrir la página y esperar la respuesta del primer acceso; registrar una demora si aparece.
3. Iniciar sesión con una cuenta de prueba y medir las consultas posteriores por separado.
4. Abrir Swagger, revisar rutas y ejecutar únicamente operaciones controladas.
5. Si hay un error, consultar logs antes de volver a desplegar.

La recomendación de ping periódico es condicional, no un mecanismo ya implantado. No se configura un ping permanente ni se afirma ausencia de cold start.

## Estado de implementación

Cambio preparado en feature/apf3-openapi. CI y despliegue pendientes al redactar este registro. El estado posterior deberá contrastarse con el PR y la captura de Render.

Fuentes: retroalimentación original del docente aportada por el responsable; https://springdoc.org/faq.html y https://springdoc.org/index.html.
