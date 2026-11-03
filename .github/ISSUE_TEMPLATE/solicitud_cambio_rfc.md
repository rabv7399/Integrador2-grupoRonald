---
name: "RFC | Solicitud de cambio"
about: "Registrar, evaluar y aprobar cambios de Delicias Peruanas (ITIL 4)"
title: "RFC-APF3-XX: "
assignees: []
---

## 1. Identificacion
- **Solicitante / fecha:**
- **Servicio afectado:** Backend Spring Boot / frontend / MySQL Aiven / Render / CI
- **Rama, commit o PR asociado:**
- **Tipo:** [ ] Estandar (preautorizado) [ ] Normal [ ] Emergencia
- **Prioridad e impacto en usuarios:**
- **Ventana propuesta de ejecucion (zona horaria Peru):**

## 2. Motivo y alcance
**Problema u oportunidad:**

**Cambios concretos (archivos, endpoints, BD):**

**Fuera de alcance:**

## 3. Evaluacion de riesgo y capacidad
- **Riesgo de indisponibilidad y mitigacion:**
- **Riesgo para datos, permisos y seguridad:**
- **Dependencias:** MySQL Aiven, Render, secretos por variables de entorno.
- **Recursos y rendimiento:** impacto en CPU/RAM, pool JDBC, consultas y latencia.
- **¿Requiere migracion SQL o backup?** [ ] Si [ ] No

## 4. Autorizacion y segregacion de funciones
- **Aprobador de negocio:** pendiente / nombre y fecha
- **Responsable tecnico:** pendiente / nombre y fecha
- **Ejecutor del cambio:** pendiente / nombre y fecha
- **Evidencia de autorizacion:** comentario, acta o enlace
- **Excepcion SoD (si aplica):** justificacion y aprobacion independiente

> No marcar como aprobado sin evidencia; el autor no debe autoaprobar cambios de alto riesgo.

## 5. Pruebas previas y criterios de aceptacion
- [ ] CI Java 21 / Maven `clean verify` satisfactoria
- [ ] Revisar PR, diff y dependencias
- [ ] Comprobar Aiven `Running` y respaldo cuando corresponda
- [ ] Verificar que no se publican secretos
- [ ] Registrar mediciones base de API y logs

**Resultados reales / enlaces a Actions:**

## 6. Plan de implementacion
1. Crear rama y PR sin desplegar directamente en produccion.
2. Verificar CI y autorizaciones.
3. Ejecutar merge en la ventana aprobada.
4. Verificar commit `Live` en Render, HTTPS y operaciones autorizadas.
5. Registrar hora, responsable y evidencias.

## 7. Plan de reversion (Backout Plan)
- **Condicion de activacion:** error 5xx persistente, fallo de arranque, regresion de seguridad o datos.
- **Pasos:** suspender cambios adicionales, conservar logs, validar Aiven, restaurar revision estable en Render si esta disponible y/o crear PR de `git revert`.
- **Base de datos:** no revertir esquemas ni datos sin backup y plan especifico.
- **Validacion tras rollback:** HTTPS, inicio de sesion autorizado, endpoints y logs.
- **Responsable / ventana maxima:**

## 8. Resultado, auditoria y cierre
- **Decision:** [ ] Aprobado [ ] Rechazado [ ] Pospuesto
- **Ejecucion:** [ ] Pendiente [ ] En progreso [ ] Terminada [ ] Revertida
- **Commit publicado y URL Render:**
- **Resultados de pruebas posdespliegue:**
- **Incidentes y acciones posteriores:**
- **Confirmacion del responsable y fecha de cierre:**

> Nunca incluir contrasenas, tokens, cadenas de conexion ni datos personales en Issues.
