# verificar.ps1 - Envoltorio de Maven que devuelve SOLO lo que un agente necesita leer.
#
# El mayor sumidero de contexto en un bucle agentico sobre Java no es el codigo fuente: es la salida
# de Maven. Este script corre el build, guarda la salida completa en disco y emite un resumen corto:
# estado, errores de compilacion, pruebas fallidas y cobertura.
#
#   pwsh .claude/tools/verificar.ps1                 ciclo completo (mvnw verify)
#   pwsh .claude/tools/verificar.ps1 -Rapido         solo compila y prueba (mvnw test)
#   pwsh .claude/tools/verificar.ps1 -Compilar       solo compila (mvnw test-compile)
#   pwsh .claude/tools/verificar.ps1 -Prueba TenantNameTests
#   pwsh .claude/tools/verificar.ps1 -Lineas 30      mas contexto por fallo (por defecto 12)
#
# Opera sobre pdp/pom.xml -f (el harness es especifico del PDP). Salida completa siempre en
# pdp/target/verificar-ultimo.log

param(
    [switch]$Rapido,
    [switch]$Compilar,
    [string]$Prueba = '',
    [int]$Lineas = 12
)

$ErrorActionPreference = 'Stop'

$repo = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$mvnw = Join-Path $repo 'mvnw.cmd'

if (-not (Test-Path $mvnw)) {
    Write-Output 'ERROR DE ENTORNO: no se encontro mvnw.cmd en la raiz del repositorio.'
    exit 2
}

# --- Seleccion del JDK ------------------------------------------------------
# El POM exige una version concreta de Java. Si el JAVA_HOME del sistema apunta a otra, el build
# falla con "release version N not supported" y el agente pierde un ciclo diagnosticando el entorno.
# Aqui se resuelve una vez: se lee la version del POM y se busca un JDK que la cumpla.
function Get-JdkVersion([string]$jdkPath) {
    $release = Join-Path $jdkPath 'release'
    if (-not (Test-Path $release)) { return '' }
    $line = Get-Content -Path $release | Where-Object { $_ -like 'JAVA_VERSION=*' } | Select-Object -First 1
    if (-not $line) { return '' }
    $value = $line -replace 'JAVA_VERSION=', '' -replace '"', ''
    return ($value -split '\.')[0]
}

$pom = Get-Content -Path (Join-Path $repo 'pdp/pom.xml') -Raw
$requerida = ''
$m = [regex]::Match($pom, '<java\.version>(\d+)</java\.version>')
if ($m.Success) { $requerida = $m.Groups[1].Value }

$jdkUsado = ''
if ($requerida -ne '') {
    if ($env:JAVA_HOME -and (Get-JdkVersion $env:JAVA_HOME) -eq $requerida) {
        $jdkUsado = $env:JAVA_HOME
    } else {
        $raices = @(
            (Join-Path $env:USERPROFILE '.jdks'),
            'C:\Program Files\Eclipse Adoptium',
            'C:\Program Files\Java',
            'C:\Program Files\Microsoft'
        )
        foreach ($raiz in $raices) {
            if ($jdkUsado -ne '') { break }
            if (-not (Test-Path $raiz)) { continue }
            foreach ($dir in (Get-ChildItem -Path $raiz -Directory -ErrorAction SilentlyContinue)) {
                if ((Get-JdkVersion $dir.FullName) -eq $requerida) {
                    $jdkUsado = $dir.FullName
                    break
                }
            }
        }
        if ($jdkUsado -eq '') {
            Write-Output ('ERROR DE ENTORNO: el POM exige Java {0} y no se encontro ningun JDK {0} instalado.' -f $requerida)
            Write-Output '  Instala un JDK de esa version o apunta JAVA_HOME a uno. No es un fallo del codigo.'
            exit 2
        }
        $env:JAVA_HOME = $jdkUsado
    }
}

if ($Compilar)   { $goal = 'test-compile' }
elseif ($Rapido) { $goal = 'test' }
else             { $goal = 'verify' }

# `verify` es el gate que se corre antes de subir, y jacoco-check depende de target/jacoco.exec.
# El agente jacoco por defecto ANEXA a ese archivo en vez de sobrescribirlo, asi que repetir `verify`
# sin `clean` acumula datos de ejecucion de corridas anteriores (incluso de sesiones de dias
# distintos) y puede reportar VERDE con una cobertura que un checkout limpio no sostiene. Pasó
# exactamente eso: un paquete con 0% de cobertura real paso en local y fallo en CI. `-Rapido` y
# `-Compilar` no corren jacoco-check, asi que no necesitan pagar el costo de una recompilacion total.
$mvnGoal = if ($goal -eq 'verify') { 'clean verify' } else { $goal }

$args = "-B -f pdp/pom.xml $mvnGoal"
if ($Prueba -ne '') { $args = "$args -Dtest=$Prueba -DfailIfNoTests=false" }

$logDir = Join-Path $repo 'pdp/target'
$logFile = Join-Path $logDir 'verificar-ultimo.log'

# `clean` borra target/ completo, y en Windows eso falla si el log vive ahi dentro mientras el
# redirect lo tiene abierto (el propio proceso se bloquea el archivo a si mismo). Se escribe a un
# temporal fuera de target/ y se copia a su sitio de siempre una vez que el build termino de borrar
# y reconstruir esa carpeta.
$logTemp = Join-Path ([System.IO.Path]::GetTempPath()) ('verificar-' + [guid]::NewGuid().ToString('N') + '.log')

$started = Get-Date
Push-Location $repo
try {
    & cmd /c "`"$mvnw`" $args > `"$logTemp`" 2>&1"
    $exit = $LASTEXITCODE
} finally {
    Pop-Location
}

if (-not (Test-Path $logDir)) { New-Item -ItemType Directory -Path $logDir -Force | Out-Null }
Move-Item -Path $logTemp -Destination $logFile -Force
$elapsed = [math]::Round(((Get-Date) - $started).TotalSeconds, 1)

if (-not (Test-Path $logFile)) {
    Write-Output 'ERROR DE ENTORNO: el build no produjo salida.'
    exit 2
}
$log = Get-Content -Path $logFile

# --- Estado -----------------------------------------------------------------
if ($exit -eq 0) { $estado = 'VERDE' } else { $estado = 'ROJO' }
Write-Output ('ESTADO: {0}  (mvnw {1}, {2}s, exit {3})' -f $estado, $mvnGoal, $elapsed, $exit)
if ($jdkUsado -ne '') { Write-Output ('JDK: Java {0} en {1}' -f $requerida, $jdkUsado) }

# --- Errores de compilacion -------------------------------------------------
# Forma: [ERROR] C:\...\Clase.java:[12,34] mensaje
$compilacion = $log | Select-String -Pattern '^\[ERROR\].*\.java:\[\d+,\d+\]' | ForEach-Object {
    ($_.Line -replace '^\[ERROR\]\s*', '') -replace '^.*[\\/]pdp[\\/]src[\\/]', 'pdp/src/'
}
if ($compilacion.Count -gt 0) {
    Write-Output ''
    Write-Output ('COMPILACION: {0} error(es)' -f $compilacion.Count)
    $compilacion | Select-Object -First 20 | ForEach-Object { Write-Output ('  ' + $_) }
    if ($compilacion.Count -gt 20) { Write-Output ('  ... y {0} mas (ver el log)' -f ($compilacion.Count - 20)) }
}

# --- Resumen de pruebas -----------------------------------------------------
$resumen = $log | Select-String -Pattern '^\[INFO\] Tests run: .*Time elapsed' | Select-Object -Last 1
$totales = $log | Select-String -Pattern '^\[(INFO|ERROR)\] Tests run: \d+, Failures: \d+, Errors: \d+, Skipped: \d+\s*$' | Select-Object -Last 1
if ($totales) {
    Write-Output ''
    Write-Output ('PRUEBAS: ' + ($totales.Line -replace '^\[(INFO|ERROR)\]\s*', ''))
}

# --- Pruebas fallidas -------------------------------------------------------
# Maven lista los fallos bajo la seccion "Failures:" / "Errors:" del reporte de Surefire.
$fallidas = $log | Select-String -Pattern '^\[ERROR\]\s{2}\S+\.\S+' | ForEach-Object {
    $_.Line -replace '^\[ERROR\]\s+', ''
} | Where-Object { $_ -notmatch '^(Please|To see|Re-run|help \d)' } | Select-Object -Unique

if ($fallidas.Count -gt 0) {
    Write-Output ''
    Write-Output ('FALLOS: {0}' -f $fallidas.Count)
    foreach ($f in ($fallidas | Select-Object -First 10)) {
        Write-Output ('  ' + $f)
    }
    if ($fallidas.Count -gt 10) { Write-Output ('  ... y {0} mas (ver el log)' -f ($fallidas.Count - 10)) }
}

# --- Cobertura (jacoco-check) -----------------------------------------------
$jacoco = $log | Select-String -Pattern 'Rule violated for (package|bundle)' | ForEach-Object {
    $_.Line -replace '^\[(WARNING|ERROR)\]\s*', ''
}
if ($jacoco.Count -gt 0) {
    Write-Output ''
    Write-Output ('COBERTURA: {0} paquete(s) bajo el umbral (50% de lineas)' -f $jacoco.Count)
    $jacoco | Select-Object -First 10 | ForEach-Object { Write-Output ('  ' + $_) }
}

# --- Violaciones de arquitectura -------------------------------------------
$arch = $log | Select-String -Pattern 'Architecture Violation|Modules? .* violat|was violated' | Select-Object -First 5
if ($arch.Count -gt 0) {
    Write-Output ''
    Write-Output 'ARQUITECTURA: se violo una regla verificada por el build'
    $arch | ForEach-Object { Write-Output ('  ' + ($_.Line -replace '^\[(WARNING|ERROR)\]\s*', '')) }
}

# --- Primer bloque de detalle ----------------------------------------------
# Cuando algo fallo, unas pocas lineas alrededor del primer error valen mas que el log entero.
if ($exit -ne 0) {
    $firstError = $log | Select-String -Pattern '^\[ERROR\]' | Select-Object -First 1
    if ($firstError) {
        $from = [math]::Max(0, $firstError.LineNumber - 2)
        $to   = [math]::Min($log.Count - 1, $from + $Lineas)
        Write-Output ''
        Write-Output ('DETALLE (lineas {0}-{1} del log):' -f ($from + 1), ($to + 1))
        $log[$from..$to] | ForEach-Object { Write-Output ('  ' + $_) }
    }
}

Write-Output ''
Write-Output ('Log completo: pdp/target/verificar-ultimo.log ({0} lineas)' -f $log.Count)

exit $exit
