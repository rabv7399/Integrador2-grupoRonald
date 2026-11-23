# Semana 14: capturas reales para sesiones 27 y 28

## Preparación (Windows)
1. Abre Docker Desktop y verifica que funciona.
2. Abre PowerShell en la carpeta `backend` de la rama `feature/semana14-incidentes`.
3. Ejecuta `powershell -ExecutionPolicy Bypass -File .\scripts\semana14-iniciar.ps1`.
4. Si las pruebas pasan, deja la ventana del backend abierta y entra a `http://localhost:8080/incidentes.html`.
5. Usa un usuario de **pruebas** existente con rol ADMIN u OPERADOR. El módulo requiere inicio de sesión; no captura contraseñas.
6. Si no tienes usuario de pruebas en la BD aislada, será necesario crear uno mediante el flujo de registro de la aplicación, sin usar credenciales reales de producción.

## Momento exacto de cada foto
- **FIGURA 1:** Tras ver `Started BackendApplication`, ejecuta `powershell -ExecutionPolicy Bypass -File .\scripts\semana14-verificar-tablas.ps1` en **otra** consola. Toma captura del bloque `SHOW TABLES` y de las claves foráneas. Puede ser más legible mediante MySQL Workbench si lo tienes.
- **FIGURA 2:** Con sesión autorizada iniciada y antes de pulsar Registrar ticket, rellena el formulario con datos de prueba. Toma captura cuando se vean los cuatro campos, prioridad y botón, mostrando `localhost:8080/incidentes.html`.
- **FIGURA 3:** Pulsa Registrar ticket y comprueba el mensaje `Ticket INC-... registrado` y la nueva fila del listado con fecha, prioridad y estado. Toma captura. Luego ejecuta otra vez `semana14-verificar-tablas.ps1` y toma captura SQL complementaria.

**No captures claves JWT, contraseñas ni URLs de conexión de Aiven.** Usa solo datos de prueba.
Las figuras se deben insertar en la ficha Word después de que estén verificadas.
No se ha desplegado esta rama en Render.
