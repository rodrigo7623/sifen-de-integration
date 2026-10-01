<#
Levanta el backend de SIFEN Manager, cargando primero las variables de entorno de backend/.env
si existe (ver .env.example para la lista completa). En modo stub (default) no hace falta ningún
archivo .env -- alcanza con correr este script tal cual.
#>

$envFile = Join-Path $PSScriptRoot ".env"

if (Test-Path $envFile) {
    Write-Host "Cargando variables de entorno desde $envFile"
    Get-Content $envFile | ForEach-Object {
        $linea = $_.Trim()
        if ($linea -eq "" -or $linea.StartsWith("#")) { return }
        $partes = $linea -split "=", 2
        if ($partes.Count -eq 2) {
            $nombre = $partes[0].Trim()
            $valor = $partes[1].Trim()
            Set-Item -Path "env:$nombre" -Value $valor
        }
    }
} else {
    Write-Host "No se encontró $envFile -- arrancando en modo stub con los defaults de application.yml"
    Write-Host "(Para modo real: copiá .env.example a .env y completá los valores)"
}

Push-Location $PSScriptRoot
try {
    mvn spring-boot:run
} finally {
    Pop-Location
}
