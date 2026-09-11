# consistencia.ps1 - Verifica que todos los slices se vean iguales.
#
# ArchUnit comprueba la DIRECCION de las dependencias y Modulith el mapa entre modulos. Ninguno de
# los dos comprueba que un slice tenga la misma FORMA que los demas: se puede resolver el mismo
# problema de tres maneras distintas sin romper ninguna regla de capas.
#
# Eso es justo lo que hace que alguien se pierda al entrar en un modulo y no reconocerlo. Este
# script lo convierte en una comprobacion ejecutable.
#
#   pwsh .claude/tools/consistencia.ps1              reporta
#   pwsh .claude/tools/consistencia.ps1 -Estricto    sale con 1 si hay hallazgos
#
# Excepciones declaradas: docs/ai-harness/consistencia-ignore.txt

param(
    [switch]$Estricto
)

$ErrorActionPreference = 'Stop'

$repo     = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$pdpRoot  = Join-Path $repo 'pdp/src/main/java/co/edu/uco/seguridad/pdp'

if (-not (Test-Path $pdpRoot)) { Write-Error 'No se encontro pdp/src/main/java/co/edu/uco/seguridad/pdp.' }

# --- Excepciones declaradas -------------------------------------------------
$ignorePath = Join-Path $repo 'pdp/docs/ai-harness/consistencia-ignore.txt'
$ignore = @()
if (Test-Path $ignorePath) {
    $ignore = Get-Content -Path $ignorePath |
              ForEach-Object { $_.Trim() } |
              Where-Object { $_ -ne '' -and -not $_.StartsWith('#') }
}
function Test-Ignorado([string]$clave) {
    foreach ($i in $ignore) { if ($clave -like $i) { return $true } }
    return $false
}

$hallazgos = @()
function Add-Hallazgo([string]$slice, [string]$regla, [string]$detalle) {
    $clave = "$slice::$regla"
    if (Test-Ignorado $clave) { return }
    if (Test-Ignorado $regla) { return }
    $script:hallazgos += [pscustomobject]@{ Slice = $slice; Regla = $regla; Detalle = $detalle }
}

# `commons` es vocabulario compartido, no un slice: no tiene capas ni adaptadores.
$slices = Get-ChildItem -Path $pdpRoot -Directory |
          Where-Object { $_.Name -ne 'commons' } |
          Sort-Object Name

foreach ($slice in $slices) {
    $n = $slice.Name
    $root = $slice.FullName
    function Has([string]$rel) { Test-Path (Join-Path $root $rel) }
    function Files([string]$rel) {
        $p = Join-Path $root $rel
        if (-not (Test-Path $p)) { return @() }
        return @(Get-ChildItem -Path $p -Filter *.java -ErrorAction SilentlyContinue |
                 Where-Object { $_.Name -ne 'package-info.java' })
    }
    function Names([string]$rel) { return @(Files $rel | ForEach-Object { $_.BaseName }) }

    # --- 1. Las tres capas ---------------------------------------------------
    foreach ($capa in @('domain', 'application', 'infrastructure')) {
        if (-not (Has $capa)) { Add-Hallazgo $n 'capa-faltante' "no existe el paquete '$capa'" }
    }

    # --- 2. Todo contrato tiene su implementacion ----------------------------
    foreach ($par in @(
        @{ C = 'application/usecase';                                R = 'application/usecase/impl';                                Que = 'caso de uso' },
        @{ C = 'domain/rule';                                        R = 'domain/rule/impl';                                        Que = 'regla' },
        @{ C = 'application/rule/validator';                        R = 'application/rule/validator/impl';                        Que = 'validador de reglas' },
        @{ C = 'infrastructure/adapter/primary/web/interactor';      R = 'infrastructure/adapter/primary/web/interactor/impl';      Que = 'interactor' }
    )) {
        $contratos = Names $par.C
        if ($contratos.Count -eq 0) { continue }
        $impls = Names $par.R
        foreach ($c in $contratos) {
            if ($impls -notcontains ($c + 'Impl')) {
                Add-Hallazgo $n 'contrato-sin-impl' ("$($par.Que) '$c' no tiene '$($c)Impl'")
            }
        }
    }

    # --- 3. Persistencia: entity + mapper + repository + schema, o ninguno ----
    # El proyecto de referencia nunca mapea la fila al dominio sin pasar por una Entity, y en este
    # repo tenants y resources lo hacen asi. Un adaptador que construye el dominio directamente
    # desde el JSON mezcla dos responsabilidades y hace que el slice no se parezca al de al lado.
    $repos = Names 'infrastructure/adapter/secondary/persistence/repository'
    if ($repos.Count -gt 0) {
        foreach ($obligatorio in @('entity', 'mapper', 'schema')) {
            if ((Names "infrastructure/adapter/secondary/persistence/$obligatorio").Count -eq 0) {
                Add-Hallazgo $n 'persistencia-incompleta' `
                    "hay adaptador de repositorio pero falta 'persistence/$obligatorio'"
            }
        }
    }

    # --- 4. Web: si hay controller, la cadena completa -----------------------
    $controllers = Names 'infrastructure/adapter/primary/web/controller'
    if ($controllers.Count -gt 0) {
        foreach ($obligatorio in @(
            'infrastructure/adapter/primary/web/dto/request/raw',
            'infrastructure/adapter/primary/web/dto/response',
            'infrastructure/adapter/primary/web/interactor',
            'infrastructure/adapter/primary/web/mapper'
        )) {
            if ((Names $obligatorio).Count -eq 0) {
                Add-Hallazgo $n 'web-incompleta' "hay controller pero falta '$obligatorio'"
            }
        }
    }

    # --- 5. Catalogo de mensajes por slice -----------------------------------
    # Un slice con excepciones propias necesita donde poner su texto: sin catalogo, los mensajes
    # acaban como literales dentro de las excepciones.
    $excepciones = (Names 'application/exception').Count + (Names 'domain/exception').Count
    if ($excepciones -gt 0) {
        $mensajes = Names 'domain/message'
        $esperado = (Get-Culture).TextInfo.ToTitleCase($n) + 'Messages'
        if ($mensajes -notcontains $esperado) {
            Add-Hallazgo $n 'catalogo-faltante' "tiene $excepciones excepcion(es) pero no '$esperado'"
        }
    }

    # --- 6. Cableado ---------------------------------------------------------
    $configs = Names 'infrastructure/config'
    $esperadoConfig = (Get-Culture).TextInfo.ToTitleCase($n) + 'Configuration'
    if ($configs -notcontains $esperadoConfig) {
        Add-Hallazgo $n 'cableado-faltante' "no existe '$esperadoConfig'"
    }

    # --- 7. Decision de negocio dentro del caso de uso -----------------------
    # Un `Mono.error` de negocio en el use case es una regla que no se extrajo: la misma decision
    # vive como Rule en otro slice, y ahi empieza la divergencia.
    foreach ($f in (Files 'application/usecase/impl')) {
        $texto = Get-Content -Path $f.FullName -Raw
        foreach ($m in [regex]::Matches($texto, 'Mono\.error\(\(\)\s*->\s*new (\w+Exception)')) {
            Add-Hallazgo $n 'regla-en-usecase' `
                ("$($f.BaseName) lanza $($m.Groups[1].Value) directamente; en otros slices eso es una Rule")
        }
    }

    # --- 8. La regla decide, no consulta -------------------------------------
    # Una regla que inyecta un puerto o devuelve un Mono es dos cosas a la vez, y por eso deja de
    # caber en el dominio. Quien resuelve el dato es el validador; aqui solo llega la respuesta.
    foreach ($f in (Files 'domain/rule/impl')) {
        $texto = Get-Content -Path $f.FullName -Raw
        if ($texto -match 'reactor\.core\.publisher') {
            Add-Hallazgo $n 'regla-reactiva' "$($f.BaseName) usa Reactor; una regla de dominio es sincrona"
        }
        if ($texto -match 'secondaryport') {
            Add-Hallazgo $n 'regla-con-puerto' "$($f.BaseName) conoce un puerto; la consulta va en el validador"
        }
    }
}

# --- 8. Un mismo rol, un mismo nombre de metodo -----------------------------
# Los adaptadores de persistencia convierten fila -> dominio. Si cada uno llama a ese metodo de otra
# forma, leer dos slices seguidos cuesta el doble.
foreach ($slice in $slices) {
    $dir = Join-Path $slice.FullName 'infrastructure/adapter/secondary/persistence/repository'
    if (-not (Test-Path $dir)) { continue }
    foreach ($f in (Get-ChildItem -Path $dir -Filter *.java)) {
        $texto = Get-Content -Path $f.FullName -Raw
        # Se acepta 'toDomain' (un solo agregado) o 'to{TipoDeRetorno}' (un repositorio que maneja
        # varios). Lo que no se acepta es un nombre que no diga a que convierte.
        $convierte = [regex]::Matches($texto, 'private static (\w+) (to[A-Z]\w*)\(JsonNode')
        foreach ($m in $convierte) {
            $tipo   = $m.Groups[1].Value
            $metodo = $m.Groups[2].Value
            if ($metodo -ne 'toDomain' -and $metodo -ne ('to' + $tipo)) {
                Add-Hallazgo $slice.Name 'conversion-mal-nombrada' `
                    ("$($f.BaseName).$metodo(JsonNode) devuelve $tipo; deberia ser 'toDomain' o 'to$tipo'")
            }
        }
    }
}

# --- Reporte ----------------------------------------------------------------
if ($hallazgos.Count -eq 0) {
    Write-Output 'CONSISTENTE: todos los slices siguen la misma forma.'
} else {
    Write-Output ('INCONSISTENCIAS: {0}' -f $hallazgos.Count)
    Write-Output ''
    foreach ($g in ($hallazgos | Group-Object Slice | Sort-Object Name)) {
        Write-Output ('  {0}' -f $g.Name)
        foreach ($h in $g.Group) {
            Write-Output ('    [{0}] {1}' -f $h.Regla, $h.Detalle)
        }
    }
    Write-Output ''
    Write-Output 'Una divergencia legitima (una capacidad que ese slice no necesita) se declara en'
    Write-Output 'pdp/docs/ai-harness/consistencia-ignore.txt con su razon. Todo lo demas se corrige.'
}

Write-Output ''
Write-Output ('Slices verificados: {0}' -f (($slices | ForEach-Object { $_.Name }) -join ', '))
if ($ignore.Count -gt 0) {
    Write-Output ('Excepciones declaradas: {0}' -f $ignore.Count)
}

if ($Estricto -and $hallazgos.Count -gt 0) { exit 1 }
exit 0
