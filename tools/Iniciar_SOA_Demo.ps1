# Delicias Peruanas - APF3 SOA - Arranque local seguro con H2.
# No conecta a la base de Aiven. No guarda contraseñas en archivos.
$ErrorActionPreference = "Stop"
$raiz = Split-Path -Parent $PSScriptRoot
$backend = Join-Path $raiz "backend"
if (-not (Test-Path -LiteralPath (Join-Path $backend "mvnw.cmd"))) {
    throw "No encuentro backend\mvnw.cmd. Ejecuta desde el repositorio correcto."
}
$env:Path = "$env:WINDIR\System32;$env:WINDIR\System32\WindowsPowerShell\v1.0;$env:Path"
$env:SPRING_PROFILES_ACTIVE = "soa-demo"
$env:PORT = "8081"

# JWT efímero distinto para cada ejecución; nunca se escribe en Git.
$bytes = New-Object byte[] 32
$rng = New-Object System.Security.Cryptography.RNGCryptoServiceProvider
try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
$env:SOA_DEMO_JWT_SECRET = [Convert]::ToBase64String($bytes)

# Un solo secreto temporal para seis perfiles en H2 local; no se muestra ni persiste.
$claveDemo = Read-Host "Define una CONTRASEÑA TEMPORAL para los seis usuarios demo (8+ caracteres)" -AsSecureString
$claveTexto = [System.Net.NetworkCredential]::new("", $claveDemo).Password
if ($claveTexto.Length -lt 8) { throw "La contraseña debe tener mínimo ocho caracteres." }
$env:SOA_DEMO_ADMIN_PASSWORD = $claveTexto
$env:SOA_DEMO_CLIENT_PASSWORD = $claveTexto
$env:SOA_DEMO_MOZO_PASSWORD = $claveTexto
$env:SOA_DEMO_SUPERVISOR_PASSWORD = $claveTexto
$env:SOA_DEMO_CAJA_PASSWORD = $claveTexto
$env:SOA_DEMO_COCINERO_PASSWORD = $claveTexto
$claveTexto = $null

Write-Host ""
Write-Host "MODO SOA-DEMO: base H2 en memoria, sin cambios en Aiven." -ForegroundColor Green
Write-Host "ADMIN:   admin.demo@delicias.local"
Write-Host "CLIENTE:    cliente.demo@delicias.local"
Write-Host "MOZO:       mozo.demo@delicias.local"
Write-Host "SUPERVISOR: supervisor.demo@delicias.local"
Write-Host "CAJA: caja.demo@delicias.local"
Write-Host "COCINERO: cocinero.demo@delicias.local"
Write-Host "PAGINA:  http://localhost:8081/soa-demo/index.html"
Write-Host "Para detener el servidor: Ctrl + C"
Write-Host ""
Push-Location $backend
try {
    & .\mvnw.cmd spring-boot:run
} finally {
    Pop-Location
    Remove-Item Env:SOA_DEMO_ADMIN_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:SOA_DEMO_CLIENT_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:SOA_DEMO_MOZO_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:SOA_DEMO_SUPERVISOR_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:SOA_DEMO_CAJA_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:SOA_DEMO_COCINERO_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:SOA_DEMO_JWT_SECRET -ErrorAction SilentlyContinue
}
