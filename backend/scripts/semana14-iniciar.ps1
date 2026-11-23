# Ejecutar desde PowerShell: powershell -ExecutionPolicy Bypass -File .\scripts\semana14-iniciar.ps1
# Usa EXCLUSIVAMENTE MySQL Docker de laboratorio (NO Aiven)
$ErrorActionPreference = 'Stop'
Set-Location (Resolve-Path (Join-Path $PSScriptRoot '..'))
$container = 'delicias-s14-mysql'
$db = 'delicias_s14'
$localPwd = 'LocalSoloS14_2026!'
Write-Host 'Semana 14 - Delicias Peruanas - pruebas aisladas' -ForegroundColor Cyan
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { throw 'Falta Docker Desktop.' }
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) { throw 'Falta Maven en PATH. Instala Maven o usa mvnw.cmd si el proyecto lo contiene.' }
docker info *> $null
if ($LASTEXITCODE -ne 0) { throw 'Docker Desktop no esta iniciado.' }
$exists = docker ps -a --format '{{.Names}}' | Where-Object { $_ -eq $container }
if (-not $exists) {
  Write-Host 'Creando MySQL 8.4 de laboratorio en 127.0.0.1:3307...'
  docker run -d --name $container -p '127.0.0.1:3307:3306' -e "MYSQL_ROOT_PASSWORD=$localPwd" -e "MYSQL_DATABASE=$db" mysql:8.4
  if ($LASTEXITCODE -ne 0) { throw 'No se pudo crear el contenedor. Verifica si el puerto 3307 esta ocupado.' }
} else {
  docker start $container *> $null
}
Write-Host 'Comprobando inicio de MySQL...'
$ready = $false
for ($i=0; $i -lt 60; $i++) {
  docker exec $container mysqladmin ping -uroot "-p$localPwd" --silent *> $null
  if ($LASTEXITCODE -eq 0) { $ready=$true; break }
  Start-Sleep -Seconds 2
}
if (-not $ready) { throw 'MySQL no esta listo. Revisa docker logs delicias-s14-mysql.' }
$env:DB_URL = "jdbc:mysql://127.0.0.1:3307/$db?useSSL=false&allowPublicKeyRetrieval=true"
$env:DB_USER = 'root'
$env:DB_PASSWORD = $localPwd
$env:DDL_AUTO = 'update'
$env:PORT = '8080'
# Clave aleatoria temporal solo para esta sesion (no guardarla).
$bytes = [byte[]]::new(48)
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
$env:JWT_SECRET = [Convert]::ToBase64String($bytes)
$env:SPRING_PROFILES_ACTIVE = 's14-local'
# Cuenta OPERADOR solo para esta base MySQL de prueba.
# Al repetir el script con el mismo contenedor, usa la clave mostrada en la primera ejecucion
# o elimina el contenedor de laboratorio y vuelve a crearlo.
$pwbytes = [byte[]]::new(24)
[System.Security.Cryptography.RandomNumberGenerator]::Fill($pwbytes)
$env:S14_OPERATOR_PASSWORD = [Convert]::ToBase64String($pwbytes)
Write-Host 'Ejecutando pruebas Maven...' -ForegroundColor Yellow
mvn -B -ntp test
if ($LASTEXITCODE -ne 0) { throw 'Fallaron las pruebas. Copia las ultimas lineas de Maven antes de seguir.' }
Write-Host ''
Write-Host 'MySQL de laboratorio OK. Iniciando backend local.' -ForegroundColor Green
Write-Host 'Cuando aparezca Started BackendApplication abre: http://localhost:8080/incidentes.html' -ForegroundColor Cyan
Write-Host 'Usuario de laboratorio: operador.s14@prueba.local' -ForegroundColor Cyan
Write-Host "Clave de laboratorio: $env:S14_OPERATOR_PASSWORD" -ForegroundColor Cyan
Write-Host 'No fotografies la clave ni la uses fuera del contenedor de laboratorio.' -ForegroundColor Yellow
Write-Host 'Conserva esta ventana abierta mientras haces las capturas.'
mvn spring-boot:run
