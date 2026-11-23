# Semana 14 - Sesiones 27 y 28 - Delicias Peruanas

## Alcance
Módulo de reporte y listado de incidentes en Java/Spring Boot, integrado al frontend estático existente.
La rama de implementación es `feature/semana14-incidentes`. No se aplica a Aiven ni a Render hasta validación.

## Tablas
- `estado_ticket`: Abierto, En Diagnóstico, Resuelto, Cerrado.
- `prioridad`: P1/P2/P3/P4, con tiempos de respuesta expresados en minutos.
- `asignado_a`: catálogo de niveles de soporte.
- `ticket`: código único, fecha automática, usuario autenticado, descripción, módulo y FKs.

La clase `IncidentesInicializador` registra catálogos de forma idempotente tras crear las tablas. `DDL_AUTO=update` las genera automáticamente en una base de pruebas. El SQL de referencia se encuentra en `incidentes_semana14.sql`. **No ejecutar ambas rutas de creación en producción sin revisar el esquema actual y contar con respaldo.**

P1=15 min y P2=60 min se basan en el material docente. P3=240 min y P4=480 min son valores de ejemplo por validar con el SLA del proyecto; no son tiempos obligatorios del material.

## Prueba controlada (no producción)
1. Conectar el backend a una base de datos MySQL **de pruebas**, mediante `DB_URL`, `DB_USER`, `DB_PASSWORD` y `JWT_SECRET` seguros. No copiar credenciales en capturas.
2. Ejecutar `cd backend && ./mvnw test` si existe el wrapper, o `mvn test`; corregir cualquier fallo antes de presentar el trabajo.
3. Levantar el backend local con `mvn spring-boot:run` y confirmar el inicio correcto y las cuatro tablas.
4. Abrir `http://localhost:8080/incidentes.html` e ingresar como ADMIN u OPERADOR.
5. Registrar: título `Fallo de consulta de productos`, módulo `Productos`, descripción `La consulta devuelve HTTP 500 durante la prueba controlada`, prioridad `P2`.
6. Comprobar respuesta HTTP 201 en DevTools, código tipo `INC-000001`, estado `Abierto` y fecha automática. El formulario y listado deben coincidir.
7. Ejecutar en la base **de pruebas**:
```sql
SHOW TABLES;
SHOW CREATE TABLE ticket;
SELECT t.codigo, t.titulo, t.modulo_afectado, t.fecha_registro,
       p.nivel_nombre, e.nombre_estado, t.usuario_reporta
FROM ticket t
JOIN prioridad p ON p.id=t.prioridad_id
JOIN estado_ticket e ON e.id=t.estado_id
ORDER BY t.id DESC LIMIT 10;
```
8. Comprobar accesos sin JWT (HTTP 401), rol CLIENTE (HTTP 403) y datos inválidos (HTTP 400).

## Evidencias para la ficha (capturas originales)
- **Figura 1:** diagrama o inspector de las cuatro tablas y las claves foráneas de `ticket` en MySQL.
- **Figura 2:** página `/incidentes.html` con el formulario funcional en el entorno de pruebas.
- **Figura 3:** listado de tickets y respuesta de inserción correlacionados con el SELECT en MySQL.
- Para sesión 28: mostrar ejecución y explicar roles, flujo POST, ID único, timestamp, prioridad y estado.

No es evidencia académica válida una captura de un diseño no conectado a la base real; tampoco se debe atribuir un ticket de demostración a una incidencia ocurrida realmente.

## Seguridad y despliegue
Se habilitan estáticos `/incidentes.html`, `/incidentes.css`, `/incidentes.js` y la API `/api/incidentes` requiere ADMIN u OPERADOR. El usuario reportante se toma del JWT, no de un campo manipulable.

**No desplegado** por esta rama: merge y despliegue se efectúan solo tras pruebas y revisión.
