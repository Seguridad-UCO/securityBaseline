# mapa.ps1 - Nivel 0 del grafo de conocimiento.
#
# Regenera docs/ai-harness/PROJECT-MAP.md a partir del codigo real: inventario por slice y por rol,
# puertos con sus implementaciones, endpoints y cobertura de pruebas por slice.
#
# Determinista y sin dependencias: la salida solo cambia si cambia el codigo.
#
#   pwsh .claude/tools/mapa.ps1              genera el mapa
#   pwsh .claude/tools/mapa.ps1 -Check       falla (exit 1) si el mapa esta desactualizado

param(
    [switch]$Check
)

$ErrorActionPreference = 'Stop'

$repo    = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$base    = 'src/main/java/co/edu/uco/seguridad'
$srcRoot = Join-Path $repo $base
$outFile = Join-Path $repo 'docs/ai-harness/PROJECT-MAP.md'

if (-not (Test-Path $srcRoot)) {
    Write-Error "No se encontro $base. Ejecuta el script desde el repositorio securityBaseline."
}

# --- Clasificacion por ruta -------------------------------------------------
# Cada fila es: patron de ruta (relativa al slice) -> rol legible. Primera coincidencia gana,
# asi que las rutas mas especificas van antes que las generales.
$roles = @(
    @{ P = 'commons/exception/';                       R = 'Excepcion de dominio' },
    @{ P = 'commons/message/';                         R = 'Catalogo de mensajes' },
    @{ P = 'commons/model/';                            R = 'Value object' },
    @{ P = 'domain/rule/impl/';                        R = 'Regla de dominio (impl)' },
    @{ P = 'domain/rule/model/';                       R = 'Dato de regla (hecho resuelto)' },
    @{ P = 'domain/rule/';                             R = 'Regla de dominio (contrato)' },
    @{ P = 'domain/exception/';                        R = 'Excepcion de dominio' },
    @{ P = 'domain/event/';                            R = 'Evento de dominio' },
    @{ P = 'domain/message/';                          R = 'Catalogo de mensajes' },
    @{ P = 'domain/model/';                            R = 'Value object' },
    @{ P = 'domain/';                                  R = 'Dominio (agregado / criteria)' },
    @{ P = 'application/usecase/impl/';                R = 'Caso de uso (impl)' },
    @{ P = 'application/usecase/';                     R = 'Caso de uso (contrato)' },
    @{ P = 'application/rule/validator/impl/';         R = 'Validador de reglas (impl)' },
    @{ P = 'application/rule/validator/';              R = 'Validador de reglas (contrato)' },
    @{ P = 'application/secondaryport/';               R = 'Puerto de salida' },
    @{ P = 'application/primaryport/request/';         R = 'DTO de entrada al nucleo' },
    @{ P = 'application/primaryport/response/';        R = 'DTO de salida del nucleo' },
    @{ P = 'web/controller/';                          R = 'Controller' },
    @{ P = 'web/dto/request/raw/';                     R = 'DTO crudo HTTP' },
    @{ P = 'web/dto/response/';                        R = 'DTO de respuesta HTTP' },
    @{ P = 'web/interactor/impl/';                     R = 'Interactor (impl)' },
    @{ P = 'web/interactor/';                          R = 'Interactor (contrato)' },
    @{ P = 'web/mapper/';                              R = 'Mapper web' },
    @{ P = 'persistence/entity/';                      R = 'Entidad de persistencia' },
    @{ P = 'persistence/mapper/';                      R = 'Mapper de persistencia' },
    @{ P = 'persistence/repository/';                  R = 'Adaptador de repositorio' },
    @{ P = 'persistence/schema/';                      R = 'Esquema de tabla' },
    @{ P = 'secondary/audit/';                         R = 'Adaptador de auditoria' },
    @{ P = 'infrastructure/config/';                   R = 'Cableado (@Bean)' },
    @{ P = 'infrastructure/properties/';               R = 'Propiedades' }
)

function Get-Role([string]$relative) {
    foreach ($entry in $roles) {
        if ($relative -like ('*' + $entry.P + '*')) { return $entry.R }
    }
    return 'Otro'
}

function Get-Relative([System.IO.FileInfo]$file) {
    return ($file.FullName.Substring($repo.Length + 1) -replace '\\', '/')
}

$allFiles = Get-ChildItem -Path $srcRoot -Recurse -Filter *.java |
            Where-Object { $_.Name -ne 'package-info.java' }

$pdpRoot    = Join-Path $srcRoot 'pdp'
$sharedRoot = Join-Path $srcRoot 'shared'

$slices = @()
if (Test-Path $pdpRoot) {
    $slices = Get-ChildItem -Path $pdpRoot -Directory | Sort-Object Name | Select-Object -ExpandProperty Name
}

# --- Puertos e implementaciones --------------------------------------------
# Un puerto es toda interfaz bajo application/secondaryport, en cualquier subpaquete: repository/
# para los de persistencia, y sueltos los que hablan con un servicio externo (PolicyDecisionPort).
# Su implementacion es la clase que la nombra en su clausula implements.
$ports = @{}
foreach ($f in $allFiles) {
    $rel = Get-Relative $f
    if ($rel -like '*application/secondaryport/*') {
        $ports[$f.BaseName] = @()
    }
}
foreach ($f in $allFiles) {
    $text = Get-Content -Path $f.FullName -Raw
    foreach ($port in @($ports.Keys)) {
        if ($text -match ('implements\s+[^{]*\b' + [regex]::Escape($port) + '\b')) {
            $ports[$port] += (Get-Relative $f)
        }
    }
}

# --- Endpoints --------------------------------------------------------------
$endpoints = @()
foreach ($f in $allFiles) {
    $rel = Get-Relative $f
    if ($rel -notlike '*web/controller/*' -and $rel -notlike '*/web/*Controller.java') { continue }
    $text = Get-Content -Path $f.FullName -Raw
    $baseMatch = [regex]::Match($text, '@RequestMapping\("([^"]+)"\)')
    $basePath = ''
    if ($baseMatch.Success) { $basePath = $baseMatch.Groups[1].Value }
    $verbMatches = [regex]::Matches($text, '@(Get|Post|Put|Patch|Delete)Mapping(?:\("([^"]*)"\))?')
    foreach ($m in $verbMatches) {
        $suffix = $m.Groups[2].Value
        $endpoints += [pscustomobject]@{
            Verb  = $m.Groups[1].Value.ToUpper()
            Path  = ($basePath + $suffix)
            File  = $rel
        }
    }
}

# --- Pruebas por slice ------------------------------------------------------
$testRoot  = Join-Path $repo 'src/test/java/co/edu/uco/seguridad'
$testFiles = @()
if (Test-Path $testRoot) {
    $testFiles = Get-ChildItem -Path $testRoot -Recurse -Filter *.java
}

# --- Construccion del documento --------------------------------------------
$sb = New-Object System.Text.StringBuilder
function Add-Line([string]$text = '') { [void]$sb.AppendLine($text) }

$mainCount = $allFiles.Count
$testCount = $testFiles.Count

Add-Line '# PROJECT-MAP'
Add-Line ''
Add-Line '> Generado por `.claude/tools/mapa.ps1`. **No editar a mano** - se regenera desde el codigo.'
Add-Line '> Es el nivel 0 del grafo de conocimiento: responde "que existe y donde va lo nuevo".'
Add-Line ''
Add-Line ('- Clases de produccion: **{0}**' -f $mainCount)
Add-Line ('- Clases de prueba: **{0}**' -f $testCount)
Add-Line ('- Slices de negocio: **{0}** ({1})' -f $slices.Count, ($slices -join ', '))
Add-Line ''
Add-Line '---'
Add-Line ''
Add-Line '## Slices de negocio (`pdp`)'

foreach ($slice in $slices) {
    $sliceRoot  = Join-Path $pdpRoot $slice
    $sliceFiles = $allFiles | Where-Object { $_.FullName.StartsWith($sliceRoot + [IO.Path]::DirectorySeparatorChar) }
    if ($sliceFiles.Count -eq 0) { continue }

    Add-Line ''
    Add-Line ('### `{0}` - {1} clases' -f $slice, $sliceFiles.Count)
    Add-Line ''
    Add-Line '| Rol | Clases |'
    Add-Line '|---|---|'

    $grouped = $sliceFiles |
        Select-Object @{N='Rel';E={ Get-Relative $_ }}, BaseName |
        ForEach-Object { $_ | Add-Member -NotePropertyName Role -NotePropertyValue (Get-Role $_.Rel) -PassThru }

    $order = @()
    foreach ($entry in $roles) { if ($order -notcontains $entry.R) { $order += $entry.R } }
    $order += 'Otro'

    foreach ($role in $order) {
        $inRole = $grouped | Where-Object { $_.Role -eq $role } | Sort-Object BaseName
        if ($inRole.Count -eq 0) { continue }
        $names = ($inRole | ForEach-Object { '`' + $_.BaseName + '`' }) -join ', '
        Add-Line ('| {0} | {1} |' -f $role, $names)
    }
}

Add-Line ''
Add-Line '---'
Add-Line ''
Add-Line '## Capacidades tecnicas (`shared`)'
Add-Line ''
Add-Line '| Subpaquete | Clases |'
Add-Line '|---|---|'

if (Test-Path $sharedRoot) {
    $sharedFiles = $allFiles | Where-Object { $_.FullName.StartsWith($sharedRoot + [IO.Path]::DirectorySeparatorChar) }
    $sharedGroups = $sharedFiles | ForEach-Object {
        $rel = Get-Relative $_
        $tail = $rel.Substring($rel.IndexOf('/shared/') + 8)
        $dir = Split-Path -Parent $tail
        if ([string]::IsNullOrEmpty($dir)) { $dir = '(raiz)' }
        [pscustomobject]@{ Dir = ($dir -replace '\\', '/'); Name = $_.BaseName }
    } | Group-Object Dir | Sort-Object Name

    foreach ($g in $sharedGroups) {
        $names = ($g.Group | Sort-Object Name | ForEach-Object { '`' + $_.Name + '`' }) -join ', '
        Add-Line ('| `{0}` | {1} |' -f $g.Name, $names)
    }
}

Add-Line ''
Add-Line '---'
Add-Line ''
Add-Line '## Puertos de salida y sus implementaciones'
Add-Line ''
Add-Line '| Puerto | Implementado por |'
Add-Line '|---|---|'

foreach ($port in ($ports.Keys | Sort-Object)) {
    $impls = $ports[$port]
    if ($impls.Count -eq 0) {
        $value = '**sin implementacion**'
    } else {
        $value = ($impls | ForEach-Object { '`' + (Split-Path -Leaf $_).Replace('.java', '') + '`' }) -join ', '
    }
    Add-Line ('| `{0}` | {1} |' -f $port, $value)
}

Add-Line ''
Add-Line '---'
Add-Line ''
Add-Line '## Endpoints'
Add-Line ''
Add-Line '| Verbo | Ruta | Controller |'
Add-Line '|---|---|---|'

foreach ($e in ($endpoints | Sort-Object Path, Verb)) {
    $controller = (Split-Path -Leaf $e.File).Replace('.java', '')
    Add-Line ('| {0} | `{1}` | `{2}` |' -f $e.Verb, $e.Path, $controller)
}

Add-Line ''
Add-Line '---'
Add-Line ''
Add-Line '## Pruebas por area'
Add-Line ''
Add-Line '| Area | Clases de prueba |'
Add-Line '|---|---|'

$testGroups = $testFiles | ForEach-Object {
    $rel = (Get-Relative $_)
    $area = 'raiz'
    if ($rel -match '/seguridad/pdp/([^/]+)/') { $area = 'pdp/' + $Matches[1] }
    elseif ($rel -match '/seguridad/shared/([^/]+)/') { $area = 'shared/' + $Matches[1] }
    [pscustomobject]@{ Area = $area; Name = $_.BaseName }
} | Group-Object Area | Sort-Object Name

foreach ($g in $testGroups) {
    $names = ($g.Group | Sort-Object Name | ForEach-Object { '`' + $_.Name + '`' }) -join ', '
    Add-Line ('| `{0}` | {1} |' -f $g.Name, $names)
}

$content = $sb.ToString()

# --- Escritura o verificacion ----------------------------------------------
$outDir = Split-Path -Parent $outFile
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir -Force | Out-Null }

if ($Check) {
    if (-not (Test-Path $outFile)) {
        Write-Output 'DESACTUALIZADO: PROJECT-MAP.md no existe.'
        exit 1
    }
    $current = Get-Content -Path $outFile -Raw
    if ($current -ne $content) {
        Write-Output 'DESACTUALIZADO: PROJECT-MAP.md no coincide con el codigo. Ejecuta mapa.ps1 sin -Check.'
        exit 1
    }
    Write-Output 'OK: PROJECT-MAP.md esta al dia.'
    exit 0
}

[IO.File]::WriteAllText($outFile, $content, (New-Object System.Text.UTF8Encoding($false)))
Write-Output ('Generado docs/ai-harness/PROJECT-MAP.md - {0} clases de produccion, {1} de prueba, {2} slices.' -f $mainCount, $testCount, $slices.Count)
