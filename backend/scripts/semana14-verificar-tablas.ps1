# Ejecutar DESPUES de iniciar el backend y registrar un ticket, desde backend:
# powershell -ExecutionPolicy Bypass -File .\scripts\semana14-verificar-tablas.ps1
$ErrorActionPreference='Stop'
$container='delicias-s14-mysql'
$password='LocalSoloS14_2026!'
$db='delicias_s14'
Write-Host 'FIGURA 1: comprobar tablas y claves foraneas' -ForegroundColor Cyan
docker exec $container mysql -uroot "-p$password" -D $db -e "SHOW TABLES; SHOW CREATE TABLE ticket\G"
Write-Host 'FIGURA 3: codigo, fecha, prioridad y estado del ticket guardado' -ForegroundColor Cyan
docker exec $container mysql -uroot "-p$password" -D $db -e "SELECT t.codigo, t.titulo, t.modulo_afectado, t.fecha_registro, p.nivel_nombre AS prioridad, e.nombre_estado AS estado FROM ticket t JOIN prioridad p ON p.id=t.prioridad_id JOIN estado_ticket e ON e.id=t.estado_id ORDER BY t.id DESC LIMIT 10;"
Write-Host 'Si ambos comandos muestran datos reales, toma las capturas de MySQL y del navegador.' -ForegroundColor Green
