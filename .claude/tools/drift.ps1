# drift.ps1 - Detector de deriva entre la documentacion y el codigo.
#
# El fallo historico de este proyecto es que docs/ afirma cosas que el codigo no sostiene: enlaces de
# "ubicacion verificable" que no resuelven y clases citadas que no existen. Este script lo convierte
# en una comprobacion ejecutable en vez de una revision manual.
#
#   pwsh .claude/tools/drift.ps1              reporta y sale con 0
#   pwsh .claude/tools/drift.ps1 -Estricto    sale con 1 si hay hallazgos (para CI o para un gate)
#
# Excepciones declaradas: docs/ai-harness/drift-ignore.txt (una por linea, '#' para comentarios).
# Solo se declara una excepcion para algo que esta pendiente A PROPOSITO y documentado como tal.

param(
    [switch]$Estricto
)

$ErrorActionPreference = 'Stop'

$repo    = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$docsDir      = Join-Path $repo 'pdp/docs'
$plataformaDir = Join-Path $repo 'docs'
$srcDirs      = @(
    Join-Path $repo 'pdp/src'
    Join-Path $repo 'pep/src'
    Join-Path $repo 'pep/starter/src'
)

if (-not (Test-Path $docsDir)) { Write-Error 'No se encontro pdp/docs/.' }

# --- Excepciones declaradas -------------------------------------------------
$ignorePath = Join-Path $repo 'pdp/docs/ai-harness/drift-ignore.txt'
$ignore = @()
if (Test-Path $ignorePath) {
    $ignore = Get-Content -Path $ignorePath |
              ForEach-Object { $_.Trim() } |
              Where-Object { $_ -ne '' -and -not $_.StartsWith('#') }
}
$ignoreDocs  = $ignore | Where-Object { $_.StartsWith('doc:') } | ForEach-Object { $_.Substring(4).Trim() }
$ignoreValor = $ignore | Where-Object { -not $_.StartsWith('doc:') }

function Test-Ignorado([string]$valor) {
    foreach ($i in $ignoreValor) { if ($valor -like $i) { return $true } }
    return $false
}

# Un documento historico (un reporte fechado, un plan cerrado) describe el estado de otro momento:
# sus referencias a clases que ya no existen son correctas, no deriva.
function Test-DocIgnorado([string]$doc) {
    foreach ($i in $ignoreDocs) { if ($doc -like $i) { return $true } }
    return $false
}

# --- Indice de nombres de archivo en src ------------------------------------
# El repo aloja PDP (pdp/src), PEP (pep/src, pep/starter/src) y OPA: una doc puede citar una
# clase de cualquiera de los tres componentes, asi que el indice cubre los tres.
$srcNames = @{}
foreach ($srcDir in $srcDirs) {
    if (Test-Path $srcDir) {
        foreach ($f in (Get-ChildItem -Path $srcDir -Recurse -Filter *.java)) {
            $srcNames[$f.BaseName] = $true
        }
    }
}

# Las skills y los agentes de .claude/ afirman cosas sobre el codigo igual que docs/, y envejecen
# igual de mal: una skill que describe una convencion retirada convierte a cada agente en un
# multiplicador del error. Se verifican con la misma vara.
# docs/ es la evidencia especifica del PDP (23 criterios); la raiz docs/ es el resumen de
# plataforma (PLATAFORMA.md) que enlaza a los tres componentes -- ambas se vigilan igual.
$docs = @(Get-ChildItem -Path $docsDir -Recurse -Filter *.md)
if (Test-Path $plataformaDir) { $docs += @(Get-ChildItem -Path $plataformaDir -Filter *.md) }
$claudeDir = Join-Path $repo '.claude'
if (Test-Path $claudeDir) {
    $docs += @(Get-ChildItem -Path $claudeDir -Recurse -Filter *.md)
}

# CLAUDE.md y AGENTS.md son los archivos que MAS lee un agente y los unicos que ninguna carpeta
# vigilada contenia: quedaban fuera del detector por estar en la raiz. Un dato caduco ahi se
# multiplica por cada sesion. CLAUDE.md llego a afirmar 18/23 criterios cuando ya eran 22/23.
foreach ($contrato in @('CLAUDE.md', 'AGENTS.md')) {
    $ruta = Join-Path $repo $contrato
    if (Test-Path $ruta) { $docs += @(Get-Item -Path $ruta) }
}

# --- 1. Enlaces relativos rotos ---------------------------------------------
$enlacesRotos = @()
foreach ($doc in $docs) {
    $dir  = $doc.DirectoryName
    $relDoc = ($doc.FullName.Substring($repo.Length + 1) -replace '\\', '/')
    if (Test-DocIgnorado $relDoc) { continue }
    $text = Get-Content -Path $doc.FullName -Raw
    foreach ($m in [regex]::Matches($text, '\]\(([^)\s]+)\)')) {
        $link = $m.Groups[1].Value
        if ($link.StartsWith('http') -or $link.StartsWith('#') -or $link.StartsWith('mailto:')) { continue }
        $target = ($link -split '#')[0]
        if ($target -eq '') { continue }
        $resolved = Join-Path $dir $target
        if (Test-Path $resolved) { continue }
        $rel = ($doc.FullName.Substring($repo.Length + 1) -replace '\\', '/')
        if (Test-Ignorado $target) { continue }
        $enlacesRotos += [pscustomobject]@{ Doc = $rel; Destino = $target }
    }
}

# --- 2. Clases del vocabulario del proyecto citadas y ausentes ---------------
# Solo se miran nombres con un sufijo propio del proyecto: acota el ruido y evita marcar
# tipos de Spring o del JDK que nunca van a estar en src/.
$sufijos = 'UseCase|UseCaseImpl|Rule|RuleImpl|RulesValidator|RulesValidatorImpl|Repository|Interactor|InteractorImpl|Mapper|Tests|Criteria|Schema|Initializer|Messages|Properties|WebResponse|RawRequest|PersistenceMapper|Adapter'
$clasesAusentes = @()
foreach ($doc in $docs) {
    $rel  = ($doc.FullName.Substring($repo.Length + 1) -replace '\\', '/')
    if (Test-DocIgnorado $rel) { continue }
    $text = Get-Content -Path $doc.FullName -Raw
    foreach ($m in [regex]::Matches($text, ('`([A-Z][A-Za-z0-9]*(?:' + $sufijos + '))`'))) {
        $nombre = $m.Groups[1].Value
        if ($srcNames.ContainsKey($nombre)) { continue }
        if (Test-Ignorado $nombre) { continue }
        $clasesAusentes += [pscustomobject]@{ Doc = $rel; Clase = $nombre }
    }
}
# El @() es obligatorio: con UN solo hallazgo el pipeline devuelve un escalar, no un array, y
# .Count sobre un PSCustomObject suelto es $null en PowerShell 5.1 — el total daba 0 y el detector
# reportaba "SIN DERIVA" teniendo una clase ausente. Fallaba solo en el caso de exactamente uno.
$clasesAusentes = @($clasesAusentes | Sort-Object Clase, Doc -Unique)

# --- Reporte ----------------------------------------------------------------
$total = $enlacesRotos.Count + $clasesAusentes.Count

if ($enlacesRotos.Count -gt 0) {
    Write-Output ('ENLACES ROTOS: {0}' -f $enlacesRotos.Count)
    foreach ($g in ($enlacesRotos | Group-Object Doc | Sort-Object Name)) {
        Write-Output ('  {0}' -f $g.Name)
        foreach ($e in ($g.Group | Sort-Object Destino -Unique)) {
            Write-Output ('    -> {0}' -f $e.Destino)
        }
    }
    Write-Output ''
}

if ($clasesAusentes.Count -gt 0) {
    Write-Output ('CLASES CITADAS QUE NO EXISTEN: {0}' -f $clasesAusentes.Count)
    foreach ($c in $clasesAusentes) {
        Write-Output ('  {0}  ({1})' -f $c.Clase, $c.Doc)
    }
    Write-Output ''
}

if ($total -eq 0) {
    Write-Output 'SIN DERIVA: todos los enlaces resuelven y toda clase citada existe.'
} else {
    Write-Output ('TOTAL: {0} hallazgo(s).' -f $total)
    Write-Output 'Arregla la documentacion, o declara la excepcion en docs/ai-harness/drift-ignore.txt'
    Write-Output 'si lo citado esta pendiente a proposito y documentado como tal.'
}

if ($ignore.Count -gt 0) {
    Write-Output ''
    Write-Output ('Excepciones declaradas activas: {0} (ver drift-ignore.txt)' -f $ignore.Count)
}

if ($Estricto -and $total -gt 0) { exit 1 }
exit 0
