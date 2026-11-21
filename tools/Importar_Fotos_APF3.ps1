param(
    [string]$ArchivoZip = "D:\DESCARGAS\APF3_Fotografias_Carta_Botanero_Original.zip",
    [switch]$Reemplazar
)

$ErrorActionPreference = "Stop"
$raiz = Split-Path -Parent $PSScriptRoot
$destino = Join-Path $raiz "backend\src\main\resources\static\portal\images"
$esperadas = @(
    "portada.jpg","rocoto.jpg","ceviche.jpg","anticucho.jpg","lomo.jpg",
    "papa.jpg","tacacho.jpg","juane.jpg","parrillada.jpg","chaufa.jpg",
    "caldo.jpg","chicha.jpg","maracuya.jpg","jora.jpg","infusion.jpg",
    "gaseosas.jpg","escocesa.jpg","pisco.jpg","chilcano.jpg","sangria.jpg",
    "cusquena.jpg","vino.jpg","nosotros.jpg"
)

Write-Host "IMAGENES DEL PORTAL SOA APF3" -ForegroundColor Cyan
if(-not (Test-Path -LiteralPath $ArchivoZip -PathType Leaf)) {
    Write-Warning "No se encontro el ZIP en: $ArchivoZip"
    Write-Host "Indica el ZIP que descargaste de las fotografias optimizadas. No se modifico ningun archivo."
    return
}
if(-not (Test-Path -LiteralPath $destino)) {
    New-Item -ItemType Directory -Path $destino -Force | Out-Null
}
$temp=Join-Path $env:TEMP ("FotosDelicias_"+[Guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $temp | Out-Null
try {
    Expand-Archive -LiteralPath $ArchivoZip -DestinationPath $temp -Force
    $archivos=Get-ChildItem -LiteralPath $temp -Recurse -File
    $copiadas=0
    foreach($nombre in $esperadas) {
        $origen=$archivos | Where-Object { $_.Name -ieq $nombre } | Select-Object -First 1
        $dest=Join-Path $destino $nombre
        if(-not $origen) { Write-Host "PENDIENTE (no esta en el ZIP): $nombre" -ForegroundColor Yellow; continue }
        if((Test-Path -LiteralPath $dest) -and -not $Reemplazar) {
            Write-Host "CONSERVADA (ya existe): $nombre"; continue
        }
        Copy-Item -LiteralPath $origen.FullName -Destination $dest -Force:$Reemplazar
        if((Get-Item -LiteralPath $dest).Length -lt 100) {
            Write-Warning "Revisar imagen demasiado pequena: $nombre"
        } else {
            $copiadas++
            Write-Host "COPIADA: $nombre" -ForegroundColor Green
        }
    }
    Write-Host "Archivos nuevos: $copiadas. No se borro ningun archivo original."
    Write-Host "Foto principal instalada: $(Test-Path -LiteralPath (Join-Path $destino 'portada.jpg'))"
    Write-Host "Prueba: http://localhost:8081/portal/index.html"
} finally {
    Remove-Item -LiteralPath $temp -Recurse -Force -ErrorAction SilentlyContinue
}
