<#
.SYNOPSIS
  Prueba de punta a punta de HU-034: bloqueos/detecciones del WAF consultables en Loki y Grafana
  (regla, puntuacion, ruta y correlationId) y audit log acotado.

.DESCRIPTION
  Levanta una pila EFIMERA (observability/compose.test.yml, proyecto hu034-obs-test) con el edge
  real (security-edge:local, reglas y scripts del repo), un stub de PDP, el colector de produccion
  (observability/collector.yml, sin copias), Loki y Grafana; genera trafico; y asierta contra la
  API de Loki y la de Grafana. Al terminar derriba SOLO su proyecto (down -v) y borra su directorio
  temporal. No toca los stacks edge/edge-pep/observability ni volumenes existentes.

  No descarga nada: si falta una imagen (security-edge:local, alpine:3.20, collector, loki,
  grafana) termina con codigo 2 y un mensaje.

  Contrato: PLAN-HU-034.md seccion 6 (asercion por id: S-0, ST-*, D-*, G-*, B-*, R-*) mas las
  siete propuestas aprobadas en Gate 1 (sin edge.status; ruta de request.uri antes de '?';
  edge.rule_main = primera regla de 'messages' que no sea de evaluacion: excluye 949110, 959100,
  980xxx y 100900; allValue '.+'; timestamp epoch; pipeline fail-closed; docs actualizadas).

.PARAMETER KeepStack
  No derriba la pila ni borra el directorio temporal (para depurar). Imprime como consultarla.

.PARAMETER Fases
  Subconjunto de: estatico, deteccion, bloqueo, rotacion. Por defecto todas.
  (Con -File se admite 'deteccion,bloqueo' separado por comas.)

.PARAMETER SkipBlocking
  Equivale a quitar 'bloqueo' de -Fases.

.PARAMETER SkipRotation
  Equivale a quitar 'rotacion' de -Fases.

.EXAMPLE
  powershell -NoProfile -ExecutionPolicy Bypass -File observability\tests\Test-WafObservability.ps1
  powershell -NoProfile -ExecutionPolicy Bypass -File observability\tests\Test-WafObservability.ps1 -Fases estatico,deteccion -KeepStack

  Salida: una linea PASS|FAIL por asercion. Codigo de salida: 0 todo verde, 1 alguna falla,
  2 precondicion (imagen local ausente, docker/curl.exe no disponibles).
#>
[CmdletBinding()]
param(
    [switch]$KeepStack,
    [switch]$SkipBlocking,
    [switch]$SkipRotation,
    [string[]]$Fases = @('estatico', 'deteccion', 'bloqueo', 'rotacion')
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

# --------------------------------------------------------------------------------------------
# Configuracion
# --------------------------------------------------------------------------------------------
$ObsDir      = Split-Path -Parent $PSScriptRoot            # .../observability
$RepoRoot    = Split-Path -Parent $ObsDir
$ComposeFile = Join-Path $ObsDir 'compose.test.yml'
$Project     = 'hu034-obs-test'
$Images      = @('security-edge:local', 'alpine:3.20',
                 'otel/opentelemetry-collector-contrib:0.160.0', 'grafana/loki:3.7.7', 'grafana/grafana:13.2.1')
$CollectorImage = 'otel/opentelemetry-collector-contrib:0.160.0'
$GrafanaUser = 'admin'
$GrafanaPass = 'test-only-not-a-secret'                    # valor de prueba del compose efimero
$EdgeHost    = 'edge.test'                                 # Host fijo: puntuaciones deterministas
$Run         = [guid]::NewGuid().ToString('N').Substring(0, 8)

$PhaseSet = @($Fases | ForEach-Object { $_ -split ',' } | ForEach-Object { $_.Trim().ToLowerInvariant() } | Where-Object { $_ })
if ($SkipBlocking) { $PhaseSet = @($PhaseSet | Where-Object { $_ -ne 'bloqueo' }) }
if ($SkipRotation) { $PhaseSet = @($PhaseSet | Where-Object { $_ -ne 'rotacion' }) }
foreach ($ph in $PhaseSet) {
    if (@('estatico', 'deteccion', 'bloqueo', 'rotacion') -notcontains $ph) { Write-Host "Fase desconocida: $ph"; exit 2 }
}
$NeedStack = ($PhaseSet -contains 'deteccion') -or ($PhaseSet -contains 'bloqueo') -or ($PhaseSet -contains 'rotacion')

$script:Results = New-Object System.Collections.ArrayList
$script:Ports   = @{}
$script:StackUp = $false
$Stopwatch      = [Diagnostics.Stopwatch]::StartNew()

# Directorio temporal propio (logs montados + trabajo). Ruta "larga" para el bind-mount de Docker.
$TempBase = if ($env:LOCALAPPDATA) { Join-Path $env:LOCALAPPDATA 'Temp' } else { [IO.Path]::GetTempPath() }
$RunDir   = Join-Path $TempBase ("hu034-obs-" + $Run)
$LogDir   = Join-Path $RunDir 'logs'
$WorkDir  = Join-Path $RunDir 'work'

# --------------------------------------------------------------------------------------------
# Utilidades de marco de pruebas
# --------------------------------------------------------------------------------------------
function Expect([bool]$Condition, [string]$Message) {
    if (-not $Condition) { throw (New-Object System.ApplicationException $Message) }
}

function Check([string]$Id, [string]$Description, [scriptblock]$Body) {
    $t = [Diagnostics.Stopwatch]::StartNew()
    $why = ''
    $ok = $false
    try {
        & $Body | Out-Null
        $ok = $true
    } catch {
        if ($_.Exception -is [System.ApplicationException]) { $why = $_.Exception.Message }
        else { $why = 'ERROR DEL SCRIPT: ' + $_.Exception.Message + ' @ ' + $_.InvocationInfo.PositionMessage.Replace("`r", ' ').Replace("`n", ' ') }
    }
    $tag = if ($ok) { 'PASS' } else { 'FAIL' }
    Write-Host ("{0} {1} {2}  ({3:N1}s)" -f $tag, $Id, $Description, $t.Elapsed.TotalSeconds)
    if (-not $ok) { Write-Host ("       -> " + $why) }
    [void]$script:Results.Add([pscustomobject]@{ Id = $Id; Ok = $ok; Why = $why })
}

function Skip-Checks([string[]]$Ids, [string]$Reason) {
    foreach ($id in $Ids) {
        Write-Host ("FAIL {0} no ejecutada: {1}" -f $id, $Reason)
        [void]$script:Results.Add([pscustomobject]@{ Id = $id; Ok = $false; Why = $Reason })
    }
}

function Section([string]$Title) { Write-Host ''; Write-Host ("== " + $Title + " ==") }

function Get-Prop($Obj, [string]$Name) {
    if ($null -ne $Obj -and $Obj.PSObject.Properties[$Name]) { return $Obj.PSObject.Properties[$Name].Value }
    return $null
}

function LV($Labels, [string]$Key) {   # valor de una etiqueta/metadata, '' si no existe
    if ($Labels.ContainsKey($Key)) { return [string]$Labels[$Key] }
    return ''
}

function Wait-Until([scriptblock]$Condition, [int]$TimeoutSec = 60, [int]$EverySec = 2) {
    $end = (Get-Date).AddSeconds($TimeoutSec)
    while ($true) {
        $r = $false
        try { $r = & $Condition } catch { $r = $false }
        if ($r) { return $true }
        if ((Get-Date) -ge $end) { return $false }
        Start-Sleep -Seconds $EverySec
    }
}

# --------------------------------------------------------------------------------------------
# Docker / HTTP
# --------------------------------------------------------------------------------------------
function Invoke-Native([string]$Exe, [string[]]$CmdArgs, [switch]$AllowFail) {
    $prev = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try { $out = & $Exe @CmdArgs } finally { $ErrorActionPreference = $prev }
    $code = $LASTEXITCODE
    if ($code -ne 0 -and -not $AllowFail) { throw ("{0} {1} -> exit {2}" -f $Exe, ($CmdArgs -join ' '), $code) }
    return [pscustomobject]@{ Code = $code; Out = @($out) }
}

function Invoke-Compose([string[]]$CmdArgs, [switch]$AllowFail) {
    $all = @('compose', '-p', $Project, '-f', $ComposeFile) + $CmdArgs
    return Invoke-Native 'docker' $all -AllowFail:$AllowFail
}

function Test-ImagePresent([string]$Image) {
    $r = Invoke-Native 'docker' @('image', 'inspect', $Image, '--format', '{{.Id}}') -AllowFail
    return ($r.Code -eq 0)
}

function Invoke-Http([string]$Url, [string]$Method = 'GET', [string]$BodyFile = '', [string]$User = '', [string[]]$Headers = @()) {
    $outFile = Join-Path $WorkDir ('http-' + [guid]::NewGuid().ToString('N') + '.out')
    $a = @('-s', '-g', '-m', '30', '-o', $outFile, '-w', '%{http_code}', '-X', $Method)
    if ($User) { $a += @('-u', $User) }
    foreach ($h in $Headers) { $a += @('-H', $h) }
    if ($BodyFile) { $a += @('--data-binary', ('@' + $BodyFile)) }
    $a += $Url
    $prev = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try { $codeText = & curl.exe @a } finally { $ErrorActionPreference = $prev }
    $curlExit = $LASTEXITCODE
    $body = ''
    if (Test-Path -LiteralPath $outFile) { $body = [IO.File]::ReadAllText($outFile); Remove-Item -LiteralPath $outFile -Force }
    $code = 0
    if ($curlExit -eq 0 -and "$codeText" -match '^\d+$') { $code = [int]"$codeText" }
    return [pscustomobject]@{ Code = $code; Body = $body }
}

function Get-HostPort([string]$Service, [int]$ContainerPort) {
    $r = Invoke-Compose @('port', $Service, "$ContainerPort")
    $line = (@($r.Out) | Where-Object { "$_" -match ':(\d+)\s*$' } | Select-Object -First 1)
    Expect ($null -ne $line) "no pude descubrir el puerto de $Service/$ContainerPort (salida: $($r.Out -join ' '))"
    [void]("$line" -match ':(\d+)\s*$')
    return [int]$Matches[1]
}

function Refresh-Ports {
    $script:Ports.edge      = Get-HostPort 'edge' 8088
    $script:Ports.loki      = Get-HostPort 'loki' 3100
    $script:Ports.grafana   = Get-HostPort 'grafana' 3000
    $script:Ports.collector = Get-HostPort 'collector' 13133
}

# Peticion al edge. Las cabeceras van en un archivo (-H @archivo): evita que PowerShell 5.1 mutile
# las comillas del correlationId hostil al pasar argumentos a curl.exe.
function Send-Edge([string]$PathAndQuery, [string]$CorrelationId) {
    $hdr = Join-Path $WorkDir 'edge-headers.txt'
    [IO.File]::WriteAllText($hdr, "Host: $EdgeHost`nX-Correlation-Id: $CorrelationId`n", [Text.Encoding]::ASCII)
    return Invoke-Http -Url ("http://127.0.0.1:{0}{1}" -f $script:Ports.edge, $PathAndQuery) -Headers @('@' + $hdr)
}

# --------------------------------------------------------------------------------------------
# Loki
# --------------------------------------------------------------------------------------------
function Get-LokiEntries([string]$Query, [int]$Limit = 1000) {
    $url = "http://127.0.0.1:$($script:Ports.loki)/loki/api/v1/query_range?query=" + [uri]::EscapeDataString($Query) + "&since=1h&limit=$Limit&direction=forward"
    $r = Invoke-Http -Url $url
    if ($r.Code -ne 200) { throw (New-Object System.ApplicationException ("Loki respondio HTTP {0} a la consulta [{1}]: {2}" -f $r.Code, $Query, $r.Body)) }
    $j = $r.Body | ConvertFrom-Json
    $list = New-Object System.Collections.ArrayList
    foreach ($s in @($j.data.result)) {
        $labels = @{}
        foreach ($p in $s.stream.PSObject.Properties) { $labels[$p.Name] = [string]$p.Value }
        foreach ($v in @($s.values)) {
            [void]$list.Add([pscustomobject]@{ Ts = [decimal]$v[0]; Line = [string]$v[1]; Labels = $labels })
        }
    }
    return $list.ToArray()
}

function Get-LokiCount([string]$Query) { return @(Get-LokiEntries $Query 5000).Count }

function Get-LokiLabelNames {
    $r = Invoke-Http -Url "http://127.0.0.1:$($script:Ports.loki)/loki/api/v1/labels?since=1h"
    Expect ($r.Code -eq 200) "Loki /labels respondio HTTP $($r.Code)"
    return @(($r.Body | ConvertFrom-Json).data)
}

function Q([string]$Template, [string]$Cid) { return $Template.Replace('@C@', $Cid).Replace('@R@', $Run) }

$QEdgeByCid = '{service_name="security-edge"} | correlationId="@C@"'
$QPdpByCid  = '{service_name="security-pdp"} | correlationId="@C@"'
# Lineas del audit proyectado = todo lo de security-edge que no es el log de acceso de Caddy.
$QAuditAll  = '{service_name="security-edge"} != "http.log.access"'

# --------------------------------------------------------------------------------------------
# Lecturas del sistema de archivos (el directorio de logs es un bind-mount: se lee desde el host)
# --------------------------------------------------------------------------------------------
function Read-SharedText([string]$Path) {
    $fs = [IO.File]::Open($Path, 'Open', 'Read', 'ReadWrite')
    try { $sr = New-Object IO.StreamReader($fs); return $sr.ReadToEnd() } finally { $fs.Dispose() }
}

function Get-AuditPath { return (Join-Path $LogDir 'security-edge-audit.json') }

function Get-AuditSize {
    $p = Get-AuditPath
    if (Test-Path -LiteralPath $p) { return (Get-Item -LiteralPath $p).Length }
    return 0
}

function Get-EdgeContainerId {
    $r = Invoke-Compose @('ps', '-q', 'edge')
    return ("$(@($r.Out)[0])").Trim()
}

function Set-EdgeConfig([hashtable]$Settings) {
    foreach ($n in 'EDGE_TEST_MODE', 'EDGE_TEST_AUDIT_MAX_BYTES', 'EDGE_TEST_AUDIT_KEEP', 'EDGE_TEST_ROTATE_INTERVAL', 'EDGE_TEST_PROBE_INTERVAL') {
        Remove-Item -Path ("Env:" + $n) -ErrorAction SilentlyContinue
    }
    foreach ($k in $Settings.Keys) { Set-Item -Path ("Env:" + $k) -Value ([string]$Settings[$k]) }
}

function Restart-Edge {
    Invoke-Compose @('up', '-d', '--pull', 'never', '--force-recreate', '--no-deps', '--wait', '--wait-timeout', '120', 'edge') | Out-Null
    $script:Ports.edge = Get-HostPort 'edge' 8088
}

# --------------------------------------------------------------------------------------------
# Aserciones compartidas entre fases (D-* y B-*)
# --------------------------------------------------------------------------------------------
function Test-EvalRuleId([string]$Id) { return (@('949110', '959100', '100900') -contains $Id) -or $Id.StartsWith('980') }

function Assert-ExactlyOneEntry([string]$Cid) {
    $e = @(Get-LokiEntries (Q $QEdgeByCid $Cid))
    Expect ($e.Count -eq 1) "esperaba exactamente 1 entrada de security-edge con correlationId=$Cid y hay $($e.Count)"
    return $e[0]
}

function Assert-Contract($Entry, [string]$Cid, [string]$ExpectInterrupted) {
    $L = $Entry.Labels
    $scoreText = LV $L 'edge_score'
    Expect ($scoreText -match '^[0-9]+$') "edge_score no es un entero positivo: '$scoreText'"
    $score = [int]$scoreText
    Expect ($score -ge 5) "edge_score=$score (esperaba >= 5, el umbral)"
    $msgs = LV $L 'edge_rule_msgs'
    Expect ($msgs.Contains('Inbound Anomaly Score Exceeded (Total Score: ')) "edge_rule_msgs no contiene el texto completo 'Inbound Anomaly Score Exceeded (Total Score: ' (truncado o ausente): '$msgs'"
    Expect ($msgs -match '\(Total Score: (\d+)\)') "edge_rule_msgs no trae '(Total Score: N)' completo: '$msgs'"
    $totalScore = [int]$Matches[1]
    Expect ($score -eq $totalScore) "edge_score=$score no coincide con (Total Score: $totalScore) de edge_rule_msgs"
    $ids = @((LV $L 'edge_rule_ids') -split ',')
    Expect ($ids -contains '941100') "edge_rule_ids no contiene 941100: '$($ids -join ',')'"
    Expect ($ids -contains '949110') "edge_rule_ids no contiene 949110: '$($ids -join ',')'"
    Expect ($ids -notcontains '100900') "edge_rule_ids contiene 100900 (regla de auditoria, no una deteccion): '$($ids -join ',')'"
    Expect ((LV $L 'edge_method') -eq 'GET') "edge_method='$(LV $L 'edge_method')' (esperaba GET)"
    Expect ((LV $L 'edge_interrupted') -eq $ExpectInterrupted) "edge_interrupted='$(LV $L 'edge_interrupted')' (esperaba $ExpectInterrupted)"
    Expect ((LV $L 'edge_transaction_id') -ne '') 'edge_transaction_id vacio'
    Expect ((LV $L 'correlationId') -eq $Cid) "correlationId='$(LV $L 'correlationId')' (esperaba $Cid)"
}

$ExpectedBodyKeys = @('correlationId', 'edge.rule_ids', 'edge.rule_msgs', 'edge.score', 'edge.path', 'edge.method',
                      'edge.interrupted', 'edge.transaction_id', 'edge.rule_main')

function Assert-LineSchema($Entry) {
    $obj = $null
    try { $obj = $Entry.Line | ConvertFrom-Json } catch { }
    Expect ($null -ne $obj -and $obj -isnot [string]) "la linea no es un objeto JSON: [$($Entry.Line)]"
    $keys = @($obj.PSObject.Properties.Name)
    $missing = @($ExpectedBodyKeys | Where-Object { $keys -notcontains $_ })
    $extra = @($keys | Where-Object { $ExpectedBodyKeys -notcontains $_ })
    Expect (($missing.Count -eq 0) -and ($extra.Count -eq 0)) "claves de la linea distintas de las 9 esperadas. Faltan: [$($missing -join ', ')] Sobran: [$($extra -join ', ')]"
    foreach ($k in $ExpectedBodyKeys) {
        $lineVal = ("" + $obj.PSObject.Properties[$k].Value).ToLowerInvariant()
        $metaVal = (LV $Entry.Labels ($k.Replace('.', '_'))).ToLowerInvariant()
        Expect ($lineVal -eq $metaVal) "linea[$k]='$lineVal' no coincide con structured metadata '$metaVal'"
    }
}

function Assert-RuleMain($Entry, [string]$Expected) {
    $L = $Entry.Labels
    $main = LV $L 'edge_rule_main'
    Expect ($main -ne '') 'edge_rule_main ausente o vacio'
    $ids = @((LV $L 'edge_rule_ids') -split ',')
    Expect ($ids -contains $main) "edge_rule_main=$main no esta en edge_rule_ids [$($ids -join ',')]"
    Expect (-not (Test-EvalRuleId $main)) "edge_rule_main=$main es una regla de evaluacion (949110/959100/980xxx/100900)"
    $first = @($ids | Where-Object { -not (Test-EvalRuleId $_) } | Select-Object -First 1)
    Expect (($first.Count -eq 1) -and ($main -eq $first[0])) "edge_rule_main=$main no es la primera regla no-evaluacion de edge_rule_ids [$($ids -join ',')]"
    Expect ($main -eq $Expected) "edge_rule_main=$main (esperaba $Expected)"
}

$PrivacyRegex = '(?i)hunter2-@R@|secret|%3C|<script|Matched Data|client_ip|q=|unix_timestamp|[0-9]{1,3}[.][0-9]{1,3}[.][0-9]{1,3}[.][0-9]{1,3}'

function Assert-PrivacyForCid([string]$Cid) {
    $n = @(Get-LokiEntries (Q $QEdgeByCid $Cid)).Count
    Expect ($n -ge 1) "sin entrada del edge para ${Cid}: la privacidad no es verificable (la tuberia no entrega)"
    $bad = Get-LokiCount ((Q $QEdgeByCid $Cid) + ' |~ `' + (Q $PrivacyRegex $Cid) + '`')
    Expect ($bad -eq 0) "$bad linea(s) de $Cid contienen query/IP/Matched Data/secreto"
    $rawEntries = @(Get-LokiEntries (Q $QEdgeByCid $Cid))
    foreach ($e in $rawEntries) {
        foreach ($k in $e.Labels.Keys) {
            Expect (-not $e.Labels[$k].Contains("hunter2-$Run")) "la metadata '$k' de $Cid contiene el secreto del query"
        }
    }
}

# Clave de metadata permitida en Loki para las entradas proyectadas del audit.
$AllowedMetaKeys  = @('correlationId', 'deployment_environment_name', 'detected_level', 'observed_timestamp', 'service_name',
                      'edge_rule_ids', 'edge_rule_msgs', 'edge_score', 'edge_path', 'edge_method', 'edge_interrupted',
                      'edge_transaction_id', 'edge_rule_main')
$RequiredMetaKeys = @('correlationId', 'edge_rule_ids', 'edge_score', 'edge_path')

# --------------------------------------------------------------------------------------------
# Grafana
# --------------------------------------------------------------------------------------------
function Invoke-Grafana([string]$PathAndQuery, [string]$Method = 'GET', [string]$BodyFile = '') {
    $h = @()
    if ($BodyFile) { $h += 'Content-Type: application/json' }
    return Invoke-Http -Url ("http://127.0.0.1:{0}{1}" -f $script:Ports.grafana, $PathAndQuery) -Method $Method -BodyFile $BodyFile -User "${GrafanaUser}:${GrafanaPass}" -Headers $h
}

function Get-AllPanels($Dashboard) {
    $out = New-Object System.Collections.ArrayList
    foreach ($p in @(Get-Prop $Dashboard 'panels')) {
        if ($null -eq $p) { continue }
        [void]$out.Add($p)
        foreach ($c in @(Get-Prop $p 'panels')) { if ($null -ne $c) { [void]$out.Add($c) } }
    }
    return $out.ToArray()
}

function Invoke-DsQuery([string]$Expr) {
    $body = @{
        queries = @(@{ refId = 'A'; datasource = @{ type = 'loki'; uid = 'loki' }; expr = $Expr; queryType = 'range'; maxLines = 100; intervalMs = 15000; maxDataPoints = 500 })
        from = 'now-1h'; to = 'now'
    } | ConvertTo-Json -Depth 8 -Compress
    $bf = Join-Path $WorkDir 'dsquery.json'
    [IO.File]::WriteAllText($bf, $body, (New-Object Text.UTF8Encoding($false)))
    $r = Invoke-Grafana '/api/ds/query' 'POST' $bf
    $json = $null
    try { $json = $r.Body | ConvertFrom-Json } catch { }
    $res = $null
    if ($null -ne $json) { $res = Get-Prop (Get-Prop $json 'results') 'A' }
    $err = $null
    if ($null -ne $res) { $err = Get-Prop $res 'error' }
    if (-not $err -and $r.Code -ge 400) { $err = "HTTP $($r.Code): $($r.Body)" }
    $frames = @()
    if ($null -ne $res) { $frames = @(Get-Prop $res 'frames') | Where-Object { $null -ne $_ } }
    return [pscustomobject]@{ Error = $err; Frames = @($frames) }
}

function Get-FrameRows($Frame) {
    $fields = @($Frame.schema.fields)
    $cols = @($Frame.data.values)
    $n = 0
    if ($cols.Count -gt 0) { $n = @($cols[0]).Count }
    $rows = New-Object System.Collections.ArrayList
    for ($i = 0; $i -lt $n; $i++) {
        $row = @{}
        for ($c = 0; $c -lt $fields.Count; $c++) { $row[[string]$fields[$c].name] = @($cols[$c])[$i] }
        [void]$rows.Add($row)
    }
    return $rows.ToArray()
}

function Get-DsRowCount($Result) {
    $n = 0
    foreach ($f in $Result.Frames) { $n += @(Get-FrameRows $f).Count }
    return $n
}

function Get-DsSeriesLabels($Result) {   # una tabla hash de etiquetas por serie
    $out = New-Object System.Collections.ArrayList
    foreach ($f in $Result.Frames) {
        foreach ($fld in @($f.schema.fields)) {
            $lab = Get-Prop $fld 'labels'
            if ($null -eq $lab) { continue }
            $h = @{}
            foreach ($p in $lab.PSObject.Properties) { $h[$p.Name] = [string]$p.Value }
            [void]$out.Add($h)
        }
    }
    return $out.ToArray()
}

function Get-PanelExpr($Dashboard, [int]$PanelId) {
    $p = @(Get-AllPanels $Dashboard | Where-Object { [int](Get-Prop $_ 'id') -eq $PanelId } | Select-Object -First 1)
    Expect ($p.Count -eq 1) "el dashboard no tiene el panel $PanelId"
    $t = @(Get-Prop $p[0] 'targets')
    Expect (($t.Count -ge 1) -and ($null -ne $t[0])) "el panel $PanelId no tiene targets"
    $expr = [string](Get-Prop $t[0] 'expr')
    Expect ($expr -ne '') "el panel $PanelId no tiene expr"
    return $expr
}

function Resolve-PanelExpr([string]$Expr, [string]$AllValue, [string]$Correlation) {
    return $Expr.Replace('${correlation:raw}', $Correlation).Replace('$service', $AllValue)
}

# --------------------------------------------------------------------------------------------
# Precondiciones (sin descargar nada)
# --------------------------------------------------------------------------------------------
Write-Host ("HU-034 prueba WAF-observabilidad  run={0}  fases={1}" -f $Run, ($PhaseSet -join ','))

$curlOk = $false
try { $null = Get-Command curl.exe -ErrorAction Stop; $curlOk = $true } catch { }
if (-not $curlOk) { Write-Host 'Falta curl.exe en el PATH.'; exit 2 }
$dockerOk = $false
try { $dv = Invoke-Native 'docker' @('compose', 'version') -AllowFail; $dockerOk = ($dv.Code -eq 0) } catch { }
if (-not $dockerOk) { Write-Host 'Docker o docker compose no disponibles.'; exit 2 }

$needImages = $Images
if (-not $NeedStack) { $needImages = @($CollectorImage) }
$missing = @($needImages | Where-Object { -not (Test-ImagePresent $_) })
if ($missing.Count -gt 0) {
    Write-Host ('Faltan imagenes locales: ' + ($missing -join ', '))
    if ($missing -contains 'security-edge:local') {
        Write-Host 'Construya el edge (docker build edge/ requiere red y descarga capas) o pida autorizacion: esta prueba NO descarga ni construye nada.'
    } else {
        Write-Host 'Esta prueba no descarga imagenes (pull_policy: never). Pida autorizacion para descargarlas.'
    }
    exit 2
}

New-Item -ItemType Directory -Force -Path $LogDir | Out-Null
New-Item -ItemType Directory -Force -Path $WorkDir | Out-Null

# Variables que exige compose.test.yml (a proposito sin default: ver el archivo).
$rotateRepo = Join-Path $RepoRoot 'edge\audit-rotate.sh'
if (Test-Path -LiteralPath $rotateRepo -PathType Leaf) { $RotateScript = $rotateRepo }
else {
    $RotateScript = Join-Path $RunDir 'audit-rotate.placeholder.sh'
    [IO.File]::WriteAllText($RotateScript, "#!/bin/sh`n# placeholder de la prueba: edge/audit-rotate.sh aun no existe`nexit 0`n", (New-Object Text.UTF8Encoding($false)))
}
$env:OBS_TEST_LOG_DIR       = $LogDir.Replace('\', '/')
$env:OBS_TEST_ROTATE_SCRIPT = $RotateScript.Replace('\', '/')
Set-EdgeConfig @{}

$exitCode = 1
try {
    # ----------------------------------------------------------------------------------------
    # Fase estatica (sin pila)
    # ----------------------------------------------------------------------------------------
    if ($PhaseSet -contains 'estatico') {
        Section 'Fase estatica'

        Check 'ST-1' 'collector.yml de produccion es valido para otelcol-contrib 0.160.0 (validate)' {
            $dataDir = Join-Path $WorkDir 'otelcol-data'
            New-Item -ItemType Directory -Force -Path $dataDir | Out-Null
            $cfg = (Join-Path $ObsDir 'collector.yml').Replace('\', '/')
            $r = Invoke-Native 'docker' @('run', '--rm', '--pull', 'never', '--user', '0:0',
                '-e', 'DEPLOYMENT_ENVIRONMENT=test', '-e', 'PDP_METRICS_TARGET=x:1', '-e', 'PEP_METRICS_TARGET=x:1',
                '-v', "${cfg}:/etc/otelcol/config.yml:ro", '-v', ($dataDir.Replace('\', '/') + ':/var/lib/otelcol'),
                $CollectorImage, 'validate', '--config=/etc/otelcol/config.yml') -AllowFail
            Expect ($r.Code -eq 0) ("otelcol validate fallo (exit {0}): {1}" -f $r.Code, (($r.Out | Select-Object -Last 5) -join ' | '))
        }

        Check 'ST-2' 'el dashboard es JSON valido con uid security-platform' {
            $raw = [IO.File]::ReadAllText((Join-Path $ObsDir 'grafana\dashboards\security-platform.json'))
            $d = $raw | ConvertFrom-Json
            Expect ((Get-Prop $d 'uid') -eq 'security-platform') "uid del dashboard = '$(Get-Prop $d 'uid')'"
        }

        Check 'ST-3' 'compose.test.yml es valido (docker compose config)' {
            $r = Invoke-Compose @('config', '-q') -AllowFail
            Expect ($r.Code -eq 0) ("docker compose config fallo: " + (($r.Out | Select-Object -Last 5) -join ' | '))
        }

        Check 'ST-4' 'compose.test.yml monta el collector.yml de produccion tal cual (sin copias)' {
            $y = [IO.File]::ReadAllText($ComposeFile)
            Expect ($y.Contains('./collector.yml:/etc/otelcol/config.yml:ro')) 'el servicio collector no monta ./collector.yml'
        }
    }

    if (-not $NeedStack) { throw (New-Object System.OperationCanceledException 'sin fases con pila') }

    # ----------------------------------------------------------------------------------------
    # Arranque de la pila y Fase 0
    # ----------------------------------------------------------------------------------------
    Section 'Fase 0 - pila lista'
    Invoke-Compose @('down', '-v', '--remove-orphans') -AllowFail | Out-Null    # restos de una corrida abortada (solo este proyecto)
    $stackError = ''
    try {
        Invoke-Compose @('up', '-d', '--pull', 'never', '--wait', '--wait-timeout', '180') | Out-Null
        $script:StackUp = $true
        Refresh-Ports
    } catch {
        $stackError = $_.Exception.Message
        $script:StackUp = $true      # hay que derribar lo que haya quedado
    }

    Check 'S-0' 'imagenes presentes; Loki, colector y Grafana listos; edge healthy en detection' {
        Expect ($stackError -eq '') "la pila no arranco: $stackError"
        foreach ($img in $Images) { Expect (Test-ImagePresent $img) "falta la imagen $img" }
        $ready = Wait-Until { (Invoke-Http "http://127.0.0.1:$($script:Ports.loki)/ready").Code -eq 200 } 90 3
        Expect $ready 'Loki /ready no llego a 200 en 90 s'
        $ready = Wait-Until { (Invoke-Http "http://127.0.0.1:$($script:Ports.collector)/").Code -eq 200 } 90 3
        Expect $ready 'el health_check del colector (13133) no llego a 200 en 90 s'
        $ready = Wait-Until {
            $r = Invoke-Http "http://127.0.0.1:$($script:Ports.grafana)/api/health"
            ($r.Code -eq 200) -and ((Get-Prop ($r.Body | ConvertFrom-Json) 'database') -eq 'ok')
        } 90 3
        Expect $ready 'Grafana /api/health no reporta database ok en 90 s'
        $cid = Get-EdgeContainerId
        $health = ((Invoke-Native 'docker' @('inspect', '-f', '{{.State.Health.Status}}', $cid)).Out | Select-Object -First 1)
        Expect ("$health" -eq 'healthy') "el contenedor edge esta '$health' (esperaba healthy)"
        $mode = ((Invoke-Native 'docker' @('inspect', '-f', '{{range .Config.Env}}{{println .}}{{end}}', $cid)).Out) -join "`n"
        Expect ($mode -match '(?m)^EDGE_MODE=detection') 'el edge no esta en EDGE_MODE=detection'
    }
    if (-not $script:Results[$script:Results.Count - 1].Ok) {
        Skip-Checks @('(resto)') 'S-0 fallo: no hay pila sobre la que probar'
        if ($stackError -ne '') {
            Write-Host '--- registros del edge (ultimas lineas) ---'
            (Invoke-Compose @('logs', '--no-color', '--tail', '30', 'edge') -AllowFail).Out | ForEach-Object { Write-Host "  $_" }
        }
        throw (New-Object System.OperationCanceledException 'S-0 fallo')
    }

    # ----------------------------------------------------------------------------------------
    # Fase 1 - detection
    # ----------------------------------------------------------------------------------------
    if ($PhaseSet -contains 'deteccion') {
        Section 'Fase 1 - detection (criterios 1, 2, 3)'
        $cidXss = "waf-$Run-xss"; $cidSqli = "waf-$Run-sqli"; $cidOk = "waf-$Run-ok"
        $cidHostile = "bad id`"}{$Run"
        $hostilePath = "/api/v1/hostile-$Run"
        $xssPath = "/api/v1/applications?q=%3Cscript%3Ealert%281%29%3C%2Fscript%3E&secret=hunter2-$Run"
        $rXss = Send-Edge $xssPath $cidXss
        $rSqli = Send-Edge '/api/v1/applications?id=1%27%20OR%20%271%27%3D%271' $cidSqli
        $rOk = Send-Edge '/api/v1/applications?page=1' $cidOk
        $rHostile = Send-Edge ($hostilePath + '?q=%3Cscript%3E') $cidHostile

        # Una sola espera comun (hasta 60 s) para que todo lo esperado llegue a Loki.
        $qHostile = '{service_name="security-edge"} | edge_path="' + $hostilePath + '"'
        $null = Wait-Until {
            (Get-LokiCount (Q $QEdgeByCid $cidXss)) -ge 1 -and
            (Get-LokiCount (Q $QEdgeByCid $cidSqli)) -ge 1 -and
            (Get-LokiCount $qHostile) -ge 1 -and
            (Get-LokiCount (Q $QPdpByCid $cidXss)) -ge 1 -and
            (Get-LokiCount (Q $QPdpByCid $cidOk)) -ge 1
        } 60 2
        Start-Sleep -Seconds 3      # margen para detectar duplicados

        Check 'D-1' 'detection no bloquea: 200 en XSS, SQLi, benigna y hostil' {
            $codes = @($rXss.Code, $rSqli.Code, $rOk.Code, $rHostile.Code)
            Expect (@($codes | Where-Object { $_ -ne 200 }).Count -eq 0) "codigos HTTP (xss,sqli,ok,hostil) = $($codes -join ',') (esperaba 200 en todos)"
        }

        Check 'D-2' 'XSS: exactamente 1 entrada en Loki por correlationId (criterio 1)' {
            [void](Assert-ExactlyOneEntry $cidXss)
        }

        Check 'D-3' 'consulta literal del criterio 1 (regla 941100, ruta sin query, puntuacion) devuelve 1 entrada' {
            $q = (Q $QEdgeByCid $cidXss) + ' | edge_rule_ids=~".*941100.*" | edge_path="/api/v1/applications" | edge_score != ""'
            $n = Get-LokiCount $q
            Expect ($n -eq 1) "la consulta [$q] devolvio $n entradas (esperaba 1)"
        }

        Check 'D-4' 'XSS: edge_score entero >= 5 e igual a (Total Score: N); ids/msgs/metodo/interrupted/transaction_id' {
            Assert-Contract (Assert-ExactlyOneEntry $cidXss) $cidXss 'false'
        }

        Check 'D-5' 'la linea es JSON con exactamente las 9 claves y coincide con la structured metadata' {
            Assert-LineSchema (Assert-ExactlyOneEntry $cidXss)
        }

        Check 'D-6' 'timestamp de la entrada = unix_timestamp del audit (parseo epoch) y cercano a ahora' {
            $e = Assert-ExactlyOneEntry $cidXss
            $now = [decimal][DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds() * 1000000
            Expect (([math]::Abs($e.Ts - $now)) -le 120000000000) "el timestamp de la entrada esta a $([math]::Round(([math]::Abs($e.Ts - $now)) / 1000000000)) s de ahora (>120 s)"
            $tid = LV $e.Labels 'edge_transaction_id'
            Expect ($tid -ne '') 'sin edge_transaction_id para localizar la transaccion en el audit'
            $audit = Read-SharedText (Get-AuditPath)
            Expect ($audit -match ('"unix_timestamp":(\d+),"id":"' + [regex]::Escape($tid) + '"')) "no encontre la transaccion $tid en el audit del host"
            $src = [decimal]$Matches[1]
            $diffNs = [math]::Abs($e.Ts - $src)
            Expect ($diffNs -lt 1000000) "el timestamp de Loki difiere $diffNs ns de transaction.unix_timestamp (el layout no se esta parseando; se usa la hora de lectura)"
        }

        Check 'D-7' 'SQLi: regla 942100 con edge_score=5 (deteccion en el umbral)' {
            $e = Assert-ExactlyOneEntry $cidSqli
            Expect ((LV $e.Labels 'edge_rule_ids') -match '(^|,)942100(,|$)') "edge_rule_ids='$(LV $e.Labels 'edge_rule_ids')' no contiene 942100"
            Expect ((LV $e.Labels 'edge_score') -eq '5') "edge_score='$(LV $e.Labels 'edge_score')' (esperaba 5)"
        }

        Check 'D-8' 'privacidad de la linea: ninguna linea proyectada con query, secreto, Matched Data, client_ip, IPs' {
            Expect ((Get-LokiCount (Q $QEdgeByCid $cidXss)) -ge 1) 'sin entrada proyectada del XSS: la privacidad no es verificable (la tuberia no entrega)'
            $q = $QAuditAll + ' |~ `' + (Q $PrivacyRegex $cidXss) + '`'
            $n = Get-LokiCount $q
            Expect ($n -eq 0) "$n linea(s) del audit en Loki contienen datos sensibles (consulta: $q)"
        }

        Check 'D-9' 'privacidad de metadata: solo claves permitidas; las 4 requeridas presentes; sin el secreto' {
            $e = Assert-ExactlyOneEntry $cidXss
            foreach ($k in $RequiredMetaKeys) { Expect ($e.Labels.ContainsKey($k)) "falta la clave de metadata requerida '$k'" }
            $all = @(Get-LokiEntries $QAuditAll 5000)
            foreach ($x in $all) {
                foreach ($k in $x.Labels.Keys) {
                    Expect ($AllowedMetaKeys -contains $k) "clave de metadata no permitida '$k' en una entrada del audit (valor: '$($x.Labels[$k])')"
                    Expect (-not $x.Labels[$k].Contains("hunter2-$Run")) "la metadata '$k' contiene el secreto del query"
                }
            }
        }

        Check 'D-10' 'cardinalidad: las unicas etiquetas indexadas son service_name y deployment_environment_name' {
            $names = @(Get-LokiLabelNames | Sort-Object)
            $want = @('deployment_environment_name', 'service_name')
            Expect (($names -join ',') -eq ($want -join ',')) "etiquetas indexadas: [$($names -join ', ')] (esperaba exactamente [$($want -join ', ')])"
        }

        Check 'D-11' 'ninguna linea cruda del audit en Loki (unix_timestamp, "messages", client_ip)' {
            Expect ((Get-LokiCount (Q $QEdgeByCid $cidXss)) -ge 1) 'precondicion: el audit no llega a Loki en absoluto (ver D-2); sin ella "0 lineas crudas" seria vacuo'
            $q = $QAuditAll + ' |~ `unix_timestamp|"messages"|client_ip`'
            $n = Get-LokiCount $q
            Expect ($n -eq 0) "$n linea(s) crudas del audit llegaron a Loki"
        }

        Check 'D-12' 'criterio 2: el mismo correlationId esta en los logs del PDP (peticion que el edge dejo pasar)' {
            $n = Get-LokiCount (Q $QPdpByCid $cidXss)
            Expect ($n -ge 1) "el PDP no tiene lineas con correlationId=$cidXss en Loki"
        }

        Check 'D-12b' 'criterio 2: una consulta por correlationId devuelve edge Y pdp' {
            $e = @(Get-LokiEntries ('{service_name=~"security-(edge|pdp)"} | correlationId="' + $cidXss + '"'))
            $svc = @($e | ForEach-Object { LV $_.Labels 'service_name' } | Sort-Object -Unique)
            Expect (($svc -contains 'security-edge') -and ($svc -contains 'security-pdp')) "servicios devueltos: [$($svc -join ', ')] (esperaba security-edge y security-pdp)"
        }

        Check 'D-13' 'benigna: el PDP la recibe y el edge NO la audita (con la tuberia viva)' {
            Expect ((Get-LokiCount (Q $QEdgeByCid $cidXss)) -ge 1) 'precondicion: la tuberia del audit no entrega (ver D-2); sin ella "0 en el edge" seria vacuo'
            Expect ((Get-LokiCount (Q $QPdpByCid $cidOk)) -ge 1) 'la benigna no llego a los logs del PDP'
            $n = Get-LokiCount (Q $QEdgeByCid $cidOk)
            Expect ($n -eq 0) "el edge audito la benigna ($n entradas): no deberia (puntuacion 0)"
        }

        Check 'D-14' 'hostil: el correlationId se sanea a [A-Za-z0-9._:-]{1,128}; ninguna entrada con espacios/comillas/llaves' {
            $e = @(Get-LokiEntries $qHostile)
            Expect ($e.Count -ge 1) "no hay entrada del edge para la ruta $hostilePath (la tuberia no entrega)"
            $cid = LV $e[0].Labels 'correlationId'
            Expect ($cid -match '^[A-Za-z0-9._:-]{1,128}$') "correlationId proyectado='$cid' no casa ^[A-Za-z0-9._:-]{1,128}$"
            Expect ($cid.Contains($Run)) "correlationId proyectado='$cid' perdio los caracteres validos del original"
            $n = Get-LokiCount ('{service_name="security-edge"} | correlationId=~".*[ \"{}].*"')
            Expect ($n -eq 0) "$n entrada(s) con correlationId que contiene espacio, comilla o llave"
        }

        Check 'D-16' 'el canario de deteccion queda en Loki como edge-healthcheck-detect y el filtro del panel 15 lo excluye' {
            $canary = Get-LokiCount '{service_name="security-edge"} | correlationId="edge-healthcheck-detect"'
            Expect ($canary -ge 1) 'no hay entradas del canario edge-healthcheck-detect en Loki (el canario no se audita/proyecta)'
            $n = Get-LokiCount '{service_name="security-edge"} | edge_rule_ids != "" | correlationId !~ "edge-healthcheck.*" | correlationId =~ "edge-healthcheck.*"'
            Expect ($n -eq 0) "el filtro del panel deja pasar $n entradas del canario"
        }

        Check 'D-17' 'edge.rule_main: primera regla no-evaluacion (XSS=941100, SQLi=942100)' {
            Assert-RuleMain (Assert-ExactlyOneEntry $cidXss) '941100'
            Assert-RuleMain (Assert-ExactlyOneEntry $cidSqli) '942100'
        }

        Check 'D-18' 'edge.status no existe (ni en la linea ni en la metadata)' {
            $e = Assert-ExactlyOneEntry $cidXss
            Expect (-not $e.Labels.ContainsKey('edge_status')) 'existe edge_status en la metadata'
            Expect (-not $e.Line.Contains('edge.status')) 'la linea contiene edge.status'
        }

        # ---------------- Fase 1b - Grafana (criterio 3) ----------------
        Section 'Fase 1b - Grafana (criterio 3)'
        $dash = $null
        $dashResp = $null
        $null = Wait-Until { $script:dashResp = Invoke-Grafana '/api/dashboards/uid/security-platform'; $script:dashResp.Code -eq 200 } 60 3
        if ($null -ne $script:dashResp -and $script:dashResp.Code -eq 200) { $dash = ($script:dashResp.Body | ConvertFrom-Json).dashboard }
        $allValue = ''
        if ($null -ne $dash) {
            $svcVar = @(Get-Prop (Get-Prop $dash 'templating') 'list' | Where-Object { (Get-Prop $_ 'name') -eq 'service' } | Select-Object -First 1)
            if ($svcVar.Count -eq 1) { $allValue = [string](Get-Prop $svcVar[0] 'allValue') }
        }

        Check 'G-1' 'dashboard security-platform provisionado con paneles 12, 15, 16, 17; datasource loki explicito en panel y targets' {
            Expect ($null -ne $dash) "GET /api/dashboards/uid/security-platform -> HTTP $(if ($null -ne $script:dashResp) { $script:dashResp.Code } else { 'sin respuesta' })"
            $panels = Get-AllPanels $dash
            foreach ($id in 12, 15, 16, 17) {
                $p = @($panels | Where-Object { [int](Get-Prop $_ 'id') -eq $id } | Select-Object -First 1)
                Expect ($p.Count -eq 1) "falta el panel $id"
                Expect ((Get-Prop (Get-Prop $p[0] 'datasource') 'uid') -eq 'loki') "el panel $id no declara datasource.uid=loki"
                $t = @(Get-Prop $p[0] 'targets')
                Expect ($t.Count -ge 1) "el panel $id no tiene targets"
                foreach ($x in $t) { Expect ((Get-Prop (Get-Prop $x 'datasource') 'uid') -eq 'loki') "un target del panel $id no declara datasource.uid=loki" }
            }
        }

        Check 'G-2' 'datasource loki responde OK en Grafana' {
            $r = Invoke-Grafana '/api/datasources/uid/loki/health'
            Expect ($r.Code -eq 200) "health del datasource -> HTTP $($r.Code): $($r.Body)"
            Expect ((Get-Prop ($r.Body | ConvertFrom-Json) 'status') -eq 'OK') "health del datasource: $($r.Body)"
        }

        Check 'G-3' 'paneles 12, 15, 16, 17: la expr guardada (con allValue real) ejecuta sin error y devuelve datos' {
            Expect ($null -ne $dash) 'sin dashboard (ver G-1)'
            $problems = New-Object System.Collections.ArrayList
            foreach ($id in 12, 15, 16, 17) {
                try {
                    $expr = Resolve-PanelExpr (Get-PanelExpr $dash $id) $allValue '.*'
                    $res = Invoke-DsQuery $expr
                    if ($res.Error) { [void]$problems.Add("panel ${id}: error de Loki/Grafana: $($res.Error)  [allValue='$allValue' expr=$expr]") }
                    elseif ((Get-DsRowCount $res) -lt 1) { [void]$problems.Add("panel ${id}: 0 filas/series  [expr=$expr]") }
                } catch { [void]$problems.Add("panel ${id}: $($_.Exception.Message)") }
            }
            Expect ($problems.Count -eq 0) ($problems -join ' || ')
        }

        Check 'G-4' 'panel 15: incluye la deteccion XSS de la prueba y ninguna fila del canario' {
            $res = Invoke-DsQuery (Resolve-PanelExpr (Get-PanelExpr $dash 15) $allValue '.*')
            Expect (-not $res.Error) "panel 15 con error: $($res.Error)"
            $lines = New-Object System.Collections.ArrayList
            foreach ($f in $res.Frames) { foreach ($row in (Get-FrameRows $f)) { $l = $row['Line']; if ($null -eq $l) { $l = $row['body'] }; [void]$lines.Add([string]$l) } }
            Expect (@($lines | Where-Object { $_.Contains($cidXss) }).Count -ge 1) "ninguna fila del panel 15 contiene $cidXss (filas: $($lines.Count))"
            Expect (@($lines | Where-Object { $_ -match 'edge-healthcheck' }).Count -eq 0) 'el panel 15 muestra filas del canario edge-healthcheck*'
        }

        Check 'G-5' 'panel 16: series por regla principal 941100 y 942100' {
            $res = Invoke-DsQuery (Resolve-PanelExpr (Get-PanelExpr $dash 16) $allValue '.*')
            Expect (-not $res.Error) "panel 16 con error: $($res.Error)"
            $vals = @(Get-DsSeriesLabels $res | ForEach-Object { $h = $_; foreach ($k in 'edge_rule_main', 'rule_main') { if ($h.ContainsKey($k)) { $h[$k] } } })
            Expect ($vals -contains '941100') "no hay serie con regla principal 941100 (series: [$($vals -join ', ')])"
            Expect ($vals -contains '942100') "no hay serie con regla principal 942100 (series: [$($vals -join ', ')])"
        }

        Check 'G-6' 'panel 17: serie con edge_path=/api/v1/applications' {
            $res = Invoke-DsQuery (Resolve-PanelExpr (Get-PanelExpr $dash 17) $allValue '.*')
            Expect (-not $res.Error) "panel 17 con error: $($res.Error)"
            $vals = @(Get-DsSeriesLabels $res | ForEach-Object { if ($_.ContainsKey('edge_path')) { $_['edge_path'] } })
            Expect ($vals -contains '/api/v1/applications') "no hay serie con edge_path=/api/v1/applications (series: [$($vals -join ', ')])"
        }

        Check 'G-7' 'panel 12 con correlation=<id>: filas de security-edge Y security-pdp' {
            $res = Invoke-DsQuery (Resolve-PanelExpr (Get-PanelExpr $dash 12) $allValue $cidXss)
            Expect (-not $res.Error) "panel 12 con error: $($res.Error)"
            $svc = New-Object System.Collections.ArrayList
            foreach ($f in $res.Frames) {
                foreach ($row in (Get-FrameRows $f)) {
                    $lab = $row['labels']
                    if ($null -ne $lab) { $s = Get-Prop $lab 'service_name'; if ($s) { [void]$svc.Add([string]$s) } }
                }
            }
            $u = @($svc | Sort-Object -Unique)
            Expect (($u -contains 'security-edge') -and ($u -contains 'security-pdp')) "servicios en el panel 12: [$($u -join ', ')] (esperaba security-edge y security-pdp)"
        }

        # ---------------- D-15 al final de la fase: ensucia el audit a proposito ----------------
        Check 'D-15' 'fail-closed: lineas basura en el audit no llegan a Loki (SECRET123) y no envenenan el lote' {
            $neg = Join-Path $ObsDir 'test-fixtures\edge-audit.negative.jsonl'
            Expect (Test-Path -LiteralPath $neg) 'falta test-fixtures/edge-audit.negative.jsonl'
            Invoke-Compose @('cp', $neg, 'edge:/tmp/edge-audit.negative.jsonl') | Out-Null
            Invoke-Compose @('exec', '-T', 'edge', 'sh', '-c', 'cat /tmp/edge-audit.negative.jsonl >> /var/log/security/security-edge-audit.json') | Out-Null
            $cidAfter = "waf-$Run-after"
            $r = Send-Edge $xssPath $cidAfter
            Expect ($r.Code -eq 200) "la peticion posterior dio HTTP $($r.Code)"
            Start-Sleep -Seconds 20
            $leak = Get-LokiCount '{service_name="security-edge"} |= "SECRET123"'
            Expect ($leak -eq 0) "$leak linea(s) con SECRET123 llegaron a Loki (el pipeline deja pasar lineas sin sanear)"
            $arrived = Wait-Until { (Get-LokiCount (Q $QEdgeByCid $cidAfter)) -ge 1 } 60 2
            Expect $arrived "la peticion posterior $cidAfter no aparece en Loki: el lote quedo envenenado por las lineas basura (o la tuberia no entrega)"
        }
    }

    # ----------------------------------------------------------------------------------------
    # Fase 2 - blocking
    # ----------------------------------------------------------------------------------------
    if ($PhaseSet -contains 'bloqueo') {
        Section 'Fase 2 - blocking (criterio 1 en bloqueo, criterio 2 negativo)'
        Set-EdgeConfig @{ EDGE_TEST_MODE = 'blocking' }
        Restart-Edge
        $cidBlk = "waf-$Run-blk"; $cidEnv = "waf-$Run-env"
        $rBlk = Send-Edge "/api/v1/applications?q=%3Cscript%3Ealert%281%29%3C%2Fscript%3E&secret=hunter2-$Run" $cidBlk
        $rEnv = Send-Edge '/.env' $cidEnv
        $null = Wait-Until { (Get-LokiCount (Q $QEdgeByCid $cidBlk)) -ge 1 -and (Get-LokiCount (Q $QEdgeByCid $cidEnv)) -ge 1 } 60 2
        Start-Sleep -Seconds 3

        Check 'B-1' 'blocking: XSS -> 403 con REQUEST_REJECTED y el correlationId en el cuerpo' {
            Expect ($rBlk.Code -eq 403) "HTTP $($rBlk.Code) (esperaba 403)"
            Expect ($rBlk.Body.Contains('REQUEST_REJECTED')) 'el cuerpo no contiene REQUEST_REJECTED'
            Expect ($rBlk.Body.Contains($cidBlk)) 'el cuerpo no contiene el correlationId'
        }

        Check 'B-2' 'bloqueo en Loki: 1 entrada, contrato (D-4), linea (D-5), regla principal, privacidad, edge_interrupted=true' {
            $e = Assert-ExactlyOneEntry $cidBlk
            Assert-Contract $e $cidBlk 'true'
            Assert-LineSchema $e
            Assert-RuleMain $e '941100'
            Assert-PrivacyForCid $cidBlk
            foreach ($k in $e.Labels.Keys) { Expect ($AllowedMetaKeys -contains $k) "clave de metadata no permitida '$k'" }
        }

        Check 'B-3' 'el bloqueo no llega al PDP (con la tuberia viva)' {
            Expect ((Get-LokiCount (Q $QEdgeByCid $cidBlk)) -ge 1) 'precondicion: no hay entrada del bloqueo en Loki (ver B-2); sin ella "0 en el PDP" seria vacuo'
            $n = Get-LokiCount (Q $QPdpByCid $cidBlk)
            Expect ($n -eq 0) "el PDP recibio $n linea(s) de una peticion bloqueada"
        }

        Check 'B-4' 'GET /.env: 403, regla propia 100010, ruta /.env, regla principal 100010' {
            Expect ($rEnv.Code -eq 403) "HTTP $($rEnv.Code) (esperaba 403)"
            $e = Assert-ExactlyOneEntry $cidEnv
            Expect ((LV $e.Labels 'edge_rule_ids') -match '(^|,)100010(,|$)') "edge_rule_ids='$(LV $e.Labels 'edge_rule_ids')' no contiene 100010"
            Expect ((LV $e.Labels 'edge_path') -eq '/.env') "edge_path='$(LV $e.Labels 'edge_path')' (esperaba /.env)"
            Assert-RuleMain $e '100010'
        }
    }

    # ----------------------------------------------------------------------------------------
    # Fase 3 - rotacion y canario
    # ----------------------------------------------------------------------------------------
    if ($PhaseSet -contains 'rotacion') {
        Section 'Fase 3 - rotacion y canario (criterio 4)'
        $maxBytes = 262144; $keep = 3
        # Estado limpio: parar el edge, vaciar su audit (solo en el directorio temporal de esta corrida) y recrearlo.
        Invoke-Compose @('stop', 'edge') -AllowFail | Out-Null
        Invoke-Compose @('run', '--rm', '--no-deps', '-T', '--entrypoint', 'sh', 'edge', '-c', 'rm -f /var/log/security/security-edge-audit.json*') -AllowFail | Out-Null
        Set-EdgeConfig @{ EDGE_TEST_MODE = 'detection'; EDGE_TEST_AUDIT_MAX_BYTES = $maxBytes; EDGE_TEST_AUDIT_KEEP = $keep; EDGE_TEST_ROTATE_INTERVAL = 2; EDGE_TEST_PROBE_INTERVAL = 600 }
        Restart-Edge

        $rotCodes = New-Object System.Collections.ArrayList
        Check 'R-1' '60 ataques XSS en 6 rafagas (~12 KB de audit cada uno, ~2,7x el tope) aceptados en detection' {
            for ($i = 1; $i -le 60; $i++) {
                $r = Send-Edge "/api/v1/applications?q=%3Cscript%3Ealert%281%29%3C%2Fscript%3E&n=$i" "waf-$Run-rot-$i"
                [void]$rotCodes.Add($r.Code)
                Start-Sleep -Milliseconds 150
                # Rafagas de 10 con 3 s de calma: la rotacion copytruncate pierde, por diseno, lo que el
                # colector aun no ha leido; la calma permite rotar sin perder (ver PLAN-HU-034 R4 y R-6).
                if (($i % 10) -eq 0) { Start-Sleep -Seconds 3 }
            }
            $bad = @($rotCodes | Where-Object { $_ -ne 200 })
            Expect ($bad.Count -eq 0) "$($bad.Count) de 60 peticiones no dieron 200 (codigos: $(($bad | Select-Object -Unique) -join ','))"
        }

        $gz1 = Join-Path $LogDir 'security-edge-audit.json.1.gz'
        Check 'R-2' 'aparece security-edge-audit.json.1.gz en <= 30 s (hay rotacion)' {
            $found = Wait-Until { Test-Path -LiteralPath $gz1 } 30 2
            Expect $found 'no existe security-edge-audit.json.1.gz tras 30 s: el audit no se rota'
        }

        Start-Sleep -Seconds 10
        Check 'R-3' 'tras 10 s sin trafico el archivo vivo pesa < tope + 64 KiB' {
            $size = Get-AuditSize
            Expect ($size -lt ($maxBytes + 65536)) "el audit vivo pesa $size bytes (limite $($maxBytes + 65536)): sin tope"
        }

        Check 'R-4' 'hay entre 1 y KEEP archivos .N.gz (N <= KEEP) y el total <= (KEEP+1)*tope + 64 KiB' {
            $files = @(Get-ChildItem -LiteralPath $LogDir -Filter 'security-edge-audit.json*' -File)
            $gz = @($files | Where-Object { $_.Name -match '^security-edge-audit\.json\.(\d+)\.gz$' })
            Expect ($gz.Count -ge 1) 'no hay ningun security-edge-audit.json.N.gz: la rotacion no ocurrio'
            Expect ($gz.Count -le $keep) "hay $($gz.Count) archivos .gz (KEEP=$keep)"
            $maxN = ($gz | ForEach-Object { [void]($_.Name -match '\.(\d+)\.gz$'); [int]$Matches[1] } | Measure-Object -Maximum).Maximum
            Expect ($maxN -le $keep) "existe un .json.$maxN.gz con N > KEEP ($keep)"
            $total = ($files | Measure-Object -Property Length -Sum).Sum
            $limit = ($keep + 1) * $maxBytes + 65536
            Expect ($total -le $limit) "los archivos del audit suman $total bytes (limite $limit)"
        }

        Check 'R-5' 'el .1.gz es gzip valido y su primera linea es JSON con transaction.id' {
            Expect (Test-Path -LiteralPath $gz1) 'no existe el .1.gz (ver R-2)'
            $fs = [IO.File]::Open($gz1, 'Open', 'Read', 'ReadWrite')
            try {
                $gz = New-Object IO.Compression.GZipStream($fs, [IO.Compression.CompressionMode]::Decompress)
                $sr = New-Object IO.StreamReader($gz)
                $first = $sr.ReadLine()
            } finally { $fs.Dispose() }
            Expect (-not [string]::IsNullOrEmpty($first)) 'el .1.gz esta vacio'
            $o = $first | ConvertFrom-Json
            Expect ($null -ne (Get-Prop (Get-Prop $o 'transaction') 'id')) 'la primera linea del .1.gz no tiene transaction.id'
        }

        Check 'R-6' 'sin perdida ni duplicados: los 60 ataques aparecen exactamente una vez en Loki' {
            $q = '{service_name="security-edge"} | correlationId=~"waf-' + $Run + '-rot-.*"'
            $null = Wait-Until { @(Get-LokiEntries $q 5000 | ForEach-Object { LV $_.Labels 'correlationId' } | Sort-Object -Unique).Count -ge 60 } 60 3
            Start-Sleep -Seconds 5
            $e = @(Get-LokiEntries $q 5000)
            $groups = @($e | Group-Object { LV $_.Labels 'correlationId' })
            $missingIds = @(1..60 | Where-Object { $id = "waf-$Run-rot-$_"; -not ($groups | Where-Object { $_.Name -eq $id }) })
            if ($missingIds.Count -gt 0) { throw (New-Object System.ApplicationException ("faltan {0} de 60 ataques en Loki (p. ej. rot-{1})" -f $missingIds.Count, $missingIds[0])) }
            $dups = @($groups | Where-Object { $_.Count -ne 1 })
            if ($dups.Count -gt 0) { throw (New-Object System.ApplicationException ("{0} correlationId con mas de una entrada (p. ej. {1} x{2})" -f $dups.Count, $dups[0].Name, $dups[0].Count)) }
        }

        Check 'R-7' 'canario espaciado: en 30 s sin trafico <= 1 canario nuevo en el audit y el edge sigue healthy' {
            function Count-Canary { return [regex]::Matches((Read-SharedText (Get-AuditPath)), 'correlationId=edge-healthcheck-detect').Count }
            $size0 = Get-AuditSize
            $c0 = Count-Canary
            Start-Sleep -Seconds 30
            $size1 = Get-AuditSize
            $c1 = Count-Canary
            Expect ($size1 -ge $size0) 'el audit se roto durante la ventana de reposo (medicion indeterminada)'
            $delta = $c1 - $c0
            Expect ($delta -le 1) "$delta canarios nuevos en el audit en 30 s (esperaba <= 1 con EDGE_DETECT_PROBE_INTERVAL=600)"
            $cid = Get-EdgeContainerId
            $health = ((Invoke-Native 'docker' @('inspect', '-f', '{{.State.Health.Status}}', $cid)).Out | Select-Object -First 1)
            Expect ("$health" -eq 'healthy') "el edge esta '$health' (esperaba healthy)"
        }

        Check 'R-8' 'contrato de healthcheck: con intervalo 0 la sonda corre y el audit cambia; con el default y de inmediato no' {
            function Get-ContainerAuditSize {
                $r = Invoke-Compose @('exec', '-T', 'edge', 'sh', '-c', 'wc -c < /var/log/security/security-edge-audit.json')
                return [long](("$(@($r.Out)[0])").Trim())
            }
            $b = Get-ContainerAuditSize
            $r1 = Invoke-Compose @('exec', '-T', '-e', 'EDGE_DETECT_PROBE_INTERVAL=0', 'edge', 'sh', '/usr/local/bin/edge-healthcheck.sh') -AllowFail
            Expect ($r1.Code -eq 0) "healthcheck con EDGE_DETECT_PROBE_INTERVAL=0 salio $($r1.Code): $($r1.Out -join ' ')"
            Start-Sleep -Seconds 2
            $a = Get-ContainerAuditSize
            Expect ($a -ne $b) 'con intervalo 0 el tamano del audit no cambio: la sonda no corrio/detecto'
            $r2 = Invoke-Compose @('exec', '-T', 'edge', 'sh', '/usr/local/bin/edge-healthcheck.sh') -AllowFail
            Expect ($r2.Code -eq 0) "healthcheck con el intervalo por defecto salio $($r2.Code): $($r2.Out -join ' ')"
            Start-Sleep -Seconds 2
            $a2 = Get-ContainerAuditSize
            Expect ($a2 -eq $a) "el healthcheck con el intervalo por defecto, inmediatamente despues, escribio en el audit ($a -> $a2): la sonda no esta espaciada"
        }

        Check 'R-9' 'documentacion: edge/README.md (EDGE_AUDIT_MAX_BYTES, EDGE_AUDIT_KEEP, EDGE_DETECT_PROBE_INTERVAL) y TELEMETRY-CONVENTION.md al dia' {
            $readme = [IO.File]::ReadAllText((Join-Path $RepoRoot 'edge\README.md'))
            foreach ($v in 'EDGE_AUDIT_MAX_BYTES', 'EDGE_AUDIT_KEEP', 'EDGE_DETECT_PROBE_INTERVAL') {
                Expect ($readme.Contains($v)) "edge/README.md no menciona $v"
            }
            $conv = [IO.File]::ReadAllText((Join-Path $ObsDir 'TELEMETRY-CONVENTION.md'))
            Expect ($conv.Contains('edge.rule_ids')) 'TELEMETRY-CONVENTION.md no describe edge.rule_ids'
            Expect (-not $conv.Contains('una marca de detecci')) 'TELEMETRY-CONVENTION.md conserva la frase obsoleta "una marca de deteccion"'
        }
    }
}
catch [System.OperationCanceledException] {
    # fin anticipado ya reportado (sin fases con pila, o S-0 fallo)
}
catch {
    Write-Host ('ERROR NO CONTROLADO DEL SCRIPT: ' + $_.Exception.Message)
    Write-Host $_.InvocationInfo.PositionMessage
    [void]$script:Results.Add([pscustomobject]@{ Id = 'SCRIPT'; Ok = $false; Why = $_.Exception.Message })
}
finally {
    if ($KeepStack -and $script:StackUp) {
        Write-Host ''
        Write-Host '-KeepStack: pila conservada. Para consultarla (PowerShell):'
        Write-Host ('  $env:OBS_TEST_LOG_DIR="{0}"; $env:OBS_TEST_ROTATE_SCRIPT="{1}"' -f $env:OBS_TEST_LOG_DIR, $env:OBS_TEST_ROTATE_SCRIPT)
        Write-Host ('  docker compose -p {0} -f "{1}" logs collector' -f $Project, $ComposeFile)
        Write-Host ('  puertos: edge={0} loki={1} grafana={2} collector={3}' -f $script:Ports.edge, $script:Ports.loki, $script:Ports.grafana, $script:Ports.collector)
        Write-Host ('  derribar: docker compose -p {0} -f "{1}" down -v --remove-orphans ; luego borrar {2}' -f $Project, $ComposeFile, $RunDir)
    } else {
        if ($script:StackUp) {
            try { Invoke-Compose @('down', '-v', '--remove-orphans') -AllowFail | Out-Null } catch { Write-Host "teardown: $($_.Exception.Message)" }
        }
        # Borrado del directorio temporal propio, solo si es inequivocamente el de esta corrida.
        try {
            $full = [IO.Path]::GetFullPath($RunDir)
            $base = [IO.Path]::GetFullPath($TempBase)
            if ($full.StartsWith($base, [StringComparison]::OrdinalIgnoreCase) -and $full.Contains('hu034-obs-') -and (Test-Path -LiteralPath $full)) {
                Remove-Item -LiteralPath $full -Recurse -Force -ErrorAction SilentlyContinue
            }
        } catch { Write-Host "limpieza: $($_.Exception.Message)" }
    }
}

# --------------------------------------------------------------------------------------------
# Resumen
# --------------------------------------------------------------------------------------------
$total = $script:Results.Count
$failed = @($script:Results | Where-Object { -not $_.Ok })
Write-Host ''
Write-Host ("RESUMEN: {0} aserciones, {1} PASS, {2} FAIL, {3:N0} s" -f $total, ($total - $failed.Count), $failed.Count, $Stopwatch.Elapsed.TotalSeconds)
if ($failed.Count -gt 0) { Write-Host ('FALLAN: ' + (($failed | ForEach-Object { $_.Id }) -join ', ')) }
if ($failed.Count -eq 0 -and $total -gt 0) { $exitCode = 0 }
exit $exitCode
