# PLAN: HU-034 — Observabilidad del WAF: bloqueos en Loki y Grafana con regla, ruta, puntuación y `correlationId`

## Metadata

| Campo | Valor |
|---|---|
| Historia | [HU-034](HU-034.md) (infraestructura / observabilidad; **no hay lógica Java**) |
| Rama / worktree | `fix/HU-034-observabilidad-waf` (base `feature/seguridad-waf-sesiones`), `sb-wt-hu034` |
| Planificador | agente 1-planificador adaptado a infraestructura (2026-10-30) |
| Fuentes consultadas | `HU-034.md` (decisiones 1-6, cerradas), `observability/*` (collector, loki, compose, README, TELEMETRY-CONVENTION, dashboard, provisioning), `edge/rules/00-engine.conf`, `85-audit-on-detection.conf`, `10-platform-before.conf`, `edge/healthcheck.sh`, `entrypoint.sh`, `Caddyfile`, `Dockerfile`, `README.md`, `compose.pdp.yml`, `ci/edge-pipeline.yml`, `EVIDENCIA-HU-027.md`, ADR-032, y los logs reales de `securityBaseline/observability/logs/` (solo lectura) |
| Verificación empírica | Todo lo marcado **[V]** se ejecutó el 2026-10-30 con contenedores efímeros (`security-edge:local`, `otel/opentelemetry-collector-contrib:0.160.0`, `grafana/loki:3.7.7`, `grafana/grafana:13.2.1`, `alpine:3.20`, todas ya locales; ninguna descarga), proyectos `hu034exp` y `hu034full`, ya retirados con sus volúmenes. No se tocó el stack `edge`/`edge-pdp` en marcha. |
| Criterios de la línea base | Ningún criterio Java se toca (`verificar.ps1` sin cambios). Relacionado: **8** (logging sin datos sensibles) — esta historia lo aplica al camino edge → Loki. Criterios de la historia: **1-4** (sección 7). |

**El "contrato" de esta historia** no son firmas Java: es (a) el texto exacto de la regla 85, (b) el esquema de salida del Collector (cuerpo + structured metadata + etiquetas), (c) las consultas LogQL del panel, (d) el comportamiento de rotación/canario, y (e) la prueba de punta a punta (sección 6). El tester escribe (e) primero; el implementador hace (a)-(d).

---

## 1. Resumen funcional

Hoy ningún bloqueo/detección del WAF es consultable en Loki con regla + puntuación + ruta + `correlationId`. La causa no son tres defectos sino **seis** (los tres de la historia más tres hallados al verificar). Esta historia repara la cadena `Coraza audit log → Collector → Loki → Grafana`, acota el crecimiento del audit log y deja una prueba reproducible que la ejerce completa.

## 2. Hechos verificados (lo que el planificador comprobó, no lo que dice la documentación)

### 2.1 Qué trae realmente el audit log actual (partes `AKZ`) [V]

Muestra real y actual: `observability/test-fixtures/edge-audit.sample.jsonl` (4 líneas generadas por el edge con la regla 85 corregida de la sección 4; una de ellas en `blocking`).

| Dónde | Campo | Contenido real |
|---|---|---|
| `transaction.id` | | id de transacción **de Coraza** (16 caracteres). **No** es el `correlationId`. |
| `transaction.timestamp` | | `"2026/10/30 13:03:06"` (UTC, formato `YYYY/MM/DD HH:MM:SS`; **no** ISO-8601). |
| `transaction.unix_timestamp` | | nanosegundos epoch (entero de 19 dígitos). |
| `transaction.client_ip`, `client_port`, `host_ip`, `host_port`, `server_id` | | IP del cliente (en Compose, la del gateway/contenedor), `server_id` = Host. |
| `transaction.request` | `method`, `protocol`, `uri`, `http_version`, `headers`, `body`, `files`, `args`, `length` | `uri` **incluye el query string completo** (`/api/v1/applications?q=%3Cscript%3E…&secret=…`). `headers` es `null`, `body` `""`, `args` `{}`: **no hay cabeceras**. |
| `transaction.is_interrupted` | | `true` solo en `blocking` cuando se interrumpe. `highest_severity` siempre `""`. |
| (no existen) | `transaction.response`, `producer` | Solo estaban en las 715 líneas antiguas (sink de otra configuración, 2026-09-21); las 155 actuales no los traen. **No hay estado HTTP ni `X-Correlation-Id`** en el audit actual. |
| `messages[]` | `message`, `actionset`, `data{file,line,id,rev,msg,data,severity,ver,tags,raw}` | `data.id` es **número**; `data.msg` el mensaje de la regla; **`data.data` es el `logdata` de la regla**: en las reglas CRS es `Matched Data: … <script>alert(1)</script>` (**el valor que disparó la regla**: secreto/PII potencial); `data.raw` es el texto completo de la regla. |

Dónde está cada dato que exige el criterio 5 (HU-027) **hoy**, sin tocar nada:

| Dato | ¿Está en el audit actual? | Dónde |
|---|---|---|
| id(s) de regla | Sí | `messages[].data.id` y `messages[].data.msg` |
| ruta | Sí (con query) | `transaction.request.uri` (antes del `?`) |
| puntuación | **Solo si superó el umbral**, y como texto | `msg` de la regla 949110: `Inbound Anomaly Score Exceeded (Total Score: 20)`. Una detección bajo el umbral (p. ej. 3 puntos) no la lleva en ningún sitio. |
| `correlationId` | **No** | no hay cabeceras; la ruta de entrega prevista era el `logdata` de la regla 100900 |

### 2.2 `85-audit-on-detection.conf` NO entrega nada en `messages[].data.data` [V]

- Contraste con el log real: **0 de 870** líneas de `securityBaseline/observability/logs/security-edge-audit.json` contienen `data.id = 100900`.
- Reproducido con el edge construido (`security-edge:local`, reglas del worktree, modo `detection`, upstream de prueba): ataque con `X-Correlation-Id: att-0001` → el audit tiene los ids 941100/941110/941160/941390/949110 y **ningún 100900**.
- Causa (Coraza): el `logdata` está en la regla *hija* de la cadena; la cabeza lleva `nolog`. Coraza registra la coincidencia de una cadena con los metadatos (`msg`, `logdata`, `log`) de la **cabeza**; la hija no produce mensaje con datos.
- Segundo defecto en el mismo `logdata`: `%{TX.INBOUND_ANOMALY_SCORE}` **no existe** a esa altura (el log del edge avisa `key not found in collection … inbound_anomaly_score`). CRS solo calcula `tx.blocking_inbound_anomaly_score` en 949060, *después* de la regla 85; antes solo hay `tx.inbound_anomaly_score_pl1..pl4`.
- Trampa de Coraza descubierta al corregirlo [V]: **las acciones no disruptivas de la cabeza de una cadena se ejecutan cuando la cabeza casa, aunque la hija no case.** Un `ctl:auditEngine=On` en la cabeza auditó *todas* las peticiones (incluida una benigna sin ningún punto). El `ctl` debe ir en la **hija**; el `msg/logdata/log` en la **cabeza**.
- Con la regla corregida (sección 4), el mensaje 100900 aparece con `data.data = "anomaly_score=20 path=/api/v1/applications correlationId=att-0001"`, también en `blocking` con `is_interrupted: true` [V]. Queda además un segundo mensaje 100900 **vacío** (`msg:""`) emitido por la hija pese a `nolog` [V]; el Collector lo descarta.

⇒ La **decisión 1** (correlationId/ruta/puntuación salen del `logdata` de la regla 85) es viable pero **hoy es falsa en el repo**: hay que reescribir la regla 85. La decisión admite exactamente esto ("si un campo no está en AKZ+logdata se añade al `logdata`") y no reintroduce la parte B.

### 2.3 Qué hace hoy el Collector con el audit [V]

1. El `json_parser` pone todo el JSON en `attributes` y **el cuerpo del registro sigue siendo la línea cruda** (`uri` con query, `client_ip`, `Matched Data`, `raw` de cada regla). Los `delete_key` solo tocan atributos: la línea cruda viaja entera a Loki (defecto de privacidad de la decisión 2).
2. Borra `messages` (ids de regla) y deja `transaction.request.uri` fuera: no hay regla ni ruta.
3. Extrae el `correlationId` de `transaction.request.headers` (que es `null`).
4. El `timestamp` usa `parse_from: attributes.transaction.timestamp` con layout ISO (`2006-01-02T15:04:05…Z07:00`) frente a `2026/10/30 13:03:06`: **no puede casar** (inferido del formato; el fallback es la hora de lectura). Corrección probada: `parse_from: attributes.transaction.unix_timestamp`, `layout_type: epoch`, `layout: ns` → `Timestamp` correcto con 256 ns de error (float64), irrelevante.
5. **Fail-open y envenenamiento** [V]: la transformación está en el pipeline compartido y condicionada a `attributes["transaction"] != nil`. Una línea que no parsea (p. ej. truncada por una rotación) conserva su cuerpo crudo y **sale a Loki sin sanear**. Además `error_mode: propagate` hace que una sola línea inesperada falle el lote entero. Con `error_mode: ignore` y un `set(body, …)` que falla (lo reproduje con `Values()` dentro de un mapa literal) el cuerpo crudo también sobrevive: por eso la sustitución del cuerpo debe ser a prueba de fallos (sección 5.2).

### 2.4 Qué hace hoy Grafana [V]

- Panel 15 `Edge — detecciones por regla y ruta`: `{service_name="security-edge"} |= "anomaly_score=" | json` → **0 filas** (vía `/api/ds/query` con datos reales presentes). Ninguna línea contiene `anomaly_score=`.
- **Cuarto defecto, no listado en la historia:** panel 12 `Logs correlacionados` usa `{service_name=~"$service"}` y la variable `service` tiene `allValue: ".*"`. Con "All" (valor por defecto) Loki 3 responde **HTTP 400** `queries require at least one regexp or equality matcher that does not have an empty-compatible value`. Con `allValue: ".+"` funciona y devuelve las líneas del edge y del PDP del mismo `correlationId` (que es justo lo que pide el criterio 2) [V].
- Quinto: la variable `service` se alimenta de Prometheus (`label_values(jvm_memory_used_bytes, service)`), que no existe en la pila de prueba; no impide el test (se sustituye con `allValue`), pero el edge nunca aparece como opción concreta de esa variable (solo con "All").
- El dashboard provisiona bien (`uid security-platform`, carpeta `Seguridad`) y el datasource Loki (`uid loki`) responde `OK` [V].

### 2.5 Loki [V]

Con el Collector tal como está y `allow_structured_metadata: true`, Loki 3.7.7 solo indexa `service_name` y `deployment_environment_name` (`/loki/api/v1/labels`). Todo atributo de registro (`correlationId`, `edge.*`) llega como **structured metadata** (visible como etiqueta de la línea; filtrable con `| correlationId="…"` sin parsear). Loki añade solos `detected_level` y `observed_timestamp`. Nombres: `edge.rule_ids` → `edge_rule_ids`. El cuerpo de tipo mapa se guarda como línea JSON (`{"correlationId":"att-0001","edge.rule_ids":"…",…}`).

### 2.6 Rotación del audit log [V]

- Coraza abre `SecAuditLog` una vez con `O_APPEND` y **no lo reabre**: tras `mv` del archivo siguió escribiendo en el archivo renombrado (rotación por rename inútil sin recargar Caddy). Tras `cp` + `: > archivo` (copytruncate) siguió escribiendo desde el byte 0 sin huecos ni NUL (0 bytes `\x00`). ⇒ **la única rotación soportada sin reiniciar es copytruncate.**
- El Collector (`file_log`, `storage: file_storage`) tolera el truncamiento: dos ataques, uno antes y otro después del truncamiento, aparecieron **exactamente una vez** cada uno en Loki, sin duplicados ni pérdida.
- `logging:` de Compose no sirve: solo rota stdout/stderr, no un archivo.
- Tamaño: ~11,4 KB por entrada de ataque (9,9 MB / 870 líneas reales; el `raw` de cada regla domina). El canario de `detection` (cada 10 s) = ~95 MB/día, como dice la historia. El Caddy del propio edge ya rota su log de acceso (`roll_size 10MiB`, `roll_keep 7`): el audit es el único sin tope.
- La imagen del edge (Alpine/busybox) trae `stat`, `gzip`, `cp`, `date`, `flock`; **no** trae `logrotate`.

### 2.7 Otros hechos útiles [V]

- Con la regla corregida: petición benigna sin puntos → **no** se audita; `X-Correlation-Id: edge-healthcheck` exacto → no se audita (100001); canario `edge-healthcheck-detect` → sí; ataque pasa en `detection` (200) y el upstream recibe la petición; en `blocking` → 403 y **el upstream no recibe nada**.
- Puntuaciones medidas (CRS 4.15.0, `Host: edge.test`): XSS `<script>alert(1)</script>` = 20 (941100/941110/941160/941390); SQLi `1' OR '1'='1` = 5 (942100); `/.env` = 5 (100010 + 930130…). Con `Host` numérico (`127.0.0.1`) se suma 920350 (+3): usar `Host: edge.test` hace los valores deterministas.
- `ci/edge-pipeline.yml::assert_detection_logs_clean` solo mira `messages` no vacío fuera del canario (`/_edge/canary`): la regla corregida **no** añade mensajes a flujos legítimos (score 0), así que es compatible.
- Los scripts de `edge/` están en git con modo `100644` (sin bit ejecutable) y `.gitattributes` fuerza `edge/** eol=lf` y `*.sh eol=lf`. En Windows el bind-mount los muestra `rwxrwxrwx`, en Linux no: **el compose de prueba debe invocarlos con `sh <ruta>`**, no confiar en el bit de ejecución.
- `alpine:3.20` no trae `httpd` pero sí `nc -l -p 8080 -e script` (probado): sirve de PDP de prueba (`observability/test-fixtures/stub-pdp.sh`, ya creado y probado).
- El stderr de Caddy (docker logs) también recibe de Coraza la línea completa con `uri` y `Matched Data` de cada regla. **No llega al Collector** (no se lee), pero es dato sensible en `docker logs`; fuera de alcance, anotado en riesgos.

## 3. Decisiones cerradas: cómo se respetan y qué cambia (para el responsable, **no resuelto aquí**)

| # | Decisión | Estado en este plan |
|---|---|---|
| 1 | Fuente: AKZ + `logdata` de la regla 85, sin parte B | **Respetada**, pero la regla 85 **debe reescribirse** (hecho 2.2): hoy no entrega nada. Salvedad menor: la **ruta** la propongo tomar de `transaction.request.uri` antes de `?` (sin ambigüedad de parseo), no de `path=` del `logdata`, porque el `logdata` mezcla tres valores controlados por el cliente en una cadena y se parsea con regex; `path=` se mantiene en el `logdata` para quien lea `docker logs`. Si el responsable prefiere `path=` del `logdata`, es un cambio de una línea. |
| 2 | Proyección saneada con `correlationId, edge.rule_ids, edge.rule_msgs, edge.score, edge.path, edge.method, edge.status, edge.interrupted, edge.transaction_id` | **Respetada salvo `edge.status`**: el audit AKZ **no tiene estado HTTP** (hecho 2.1) y en fase 2 aún no existe (`RESPONSE_STATUS` vacío). No se puede producir sin reintroducir la parte F, que `00-engine.conf` prohíbe. **Propuesta: omitirlo**; el estado y la latencia están en el log de acceso del edge (`security-edge.json`: `status`, `resp_headers.X-Correlation-Id`) y `edge.interrupted=true` equivale a 403 en este despliegue. **Requiere que el responsable lo confirme.** |
| 3 | Solo `service_name` y `deployment_environment_name` como etiquetas | **Respetada** y comprobada [V]. |
| 4 | Tope por rotación documentada + canario sin auditarse cada 10 s | **Respetada**: copytruncate por tamaño en el contenedor (único método que Coraza soporta, hecho 2.6) y sonda de detección con intervalo largo. Consecuencia a aceptar: un motor roto *en medio* de la ventana se detecta con retraso ≤ `EDGE_DETECT_PROBE_INTERVAL`. |
| 5 | Compose efímero + script; descarga requiere autorización | **Respetada**: ninguna imagen nueva (solo las tres autorizadas + `security-edge:local` + `alpine:3.20`, ya locales). **Si `security-edge:local` no existe en otra máquina, reconstruirla (`docker build edge/`) descarga capas/módulos Go: no está autorizado; el script falla con mensaje claro en vez de construir.** |
| 6 | Criterio 2 con una petición que el edge deja pasar, buscada en `security-pdp.json`/Loki | **Respetada con una salvedad**: el PDP de la prueba es un **stub** que escribe una línea con la forma exacta de `security-pdp.json` (el PDP real exige Keycloak+SurrealDB+Redis+OPA). Prueba que el id llega al log del upstream, que el Collector lo ingiere como `service_name="security-pdp"` y que Grafana lo cruza con el del edge. La integración con el PDP real ya la cubre `assert_pdp_correlation` de `ci/edge-pipeline.yml`. |

**Hallazgos nuevos que el responsable debe conocer** (no cambian decisiones, amplían alcance): (a) `allValue: ".*"` rompe el panel 12 con "All" (hecho 2.4); (b) el layout de timestamp del audit no puede casar (2.3.4); (c) el pipeline actual es fail-open y envenenable (2.3.5); (d) `TELEMETRY-CONVENTION.md` y `edge/README.md` afirman que el Collector "conserva solo el `correlationId` y una marca de detección": quedan obsoletos y deben reescribirse.

**Límite de "por regla"**: LogQL no puede repartir una lista (`edge_rule_ids="941100,941110,…"`) en filas. Sin añadir campos, el panel "por regla" agrupa por la **primera regla** del conjunto (`line_format` + `regexp`, probado [V]) y una vista por conjunto completo. Si el responsable quiere conteo exacto *por cada* regla de un mismo disparo, habría que emitir un registro por regla (el OTTL actual no puede) o añadir un campo `edge.rule_main` explícito. **Sin cambiar nada de lo cerrado, el criterio 3 queda cubierto con "regla principal + ruta".**

## 4. Diseño 1 — `edge/rules/85-audit-on-detection.conf` (texto exacto, probado [V])

Sustituye el bloque de las dos líneas `SecRule` (se conserva el comentario explicativo, ampliado con lo de arriba). Probado en `detection` y `blocking`:

```
SecRule REQUEST_HEADERS:X-Correlation-Id "!@streq edge-healthcheck" \
    "id:100900,phase:2,pass,log,t:none,msg:'edge-detection',logdata:'anomaly_score=%{TX.EDGE_SCORE} path=%{REQUEST_FILENAME} correlationId=%{REQUEST_HEADERS.x-correlation-id}',chain"
    SecRule TX:INBOUND_ANOMALY_SCORE_PL1|TX:INBOUND_ANOMALY_SCORE_PL2|TX:INBOUND_ANOMALY_SCORE_PL3|TX:INBOUND_ANOMALY_SCORE_PL4 "@gt 0" \
        "t:none,nolog,ctl:auditEngine=On,setvar:'tx.edge_score=+%{tx.inbound_anomaly_score_pl1}',setvar:'tx.edge_score=+%{tx.inbound_anomaly_score_pl2}',setvar:'tx.edge_score=+%{tx.inbound_anomaly_score_pl3}',setvar:'tx.edge_score=+%{tx.inbound_anomaly_score_pl4}'"
```

Reglas del diseño (cada una costó un fallo real):
1. `msg:'edge-detection'` es **constante** y es la clave por la que el Collector encuentra el mensaje (no el id, ver 5.2).
2. `log` + `msg` + `logdata` en la **cabeza**; `ctl:auditEngine=On` y los `setvar` en la **hija** (la cabeza ejecuta sus acciones aunque la hija no case).
3. La puntuación sale de `tx.edge_score`, suma de `inbound_anomaly_score_pl1..4` (igual al `Total Score` de 949110: 20 = 20 [V]); nunca de `TX.INBOUND_ANOMALY_SCORE` (no existe).
4. La exclusión exacta del latido `edge-healthcheck` se conserva; el canario `edge-healthcheck-detect` sigue auditándose (contrato de HU-027).
5. `correlationId` se lee de `REQUEST_HEADERS.x-correlation-id` (el Caddyfile lo genera si falta, antes del WAF).
6. No se tocan `00-engine.conf` ni las partes `AKZ`.

Efecto esperado: cada transacción con puntuación > 0 (excepto el latido) lleva un mensaje 100900 con `data.data = "anomaly_score=<n> path=<ruta> correlationId=<id>"` y un segundo 100900 vacío. Las transacciones con puntuación 0 siguen sin auditarse.

## 5. Diseño 2 — Collector, Loki, Grafana, rotación, canario

### 5.1 `observability/collector.yml` — receptor y pipeline propio

1. `file_log/edge_audit` (probado [V]):
   - `json_parser` con **`on_error: drop`** (línea que no parsea ⇒ se descarta, nunca viaja cruda) y `timestamp: {parse_from: attributes.transaction.unix_timestamp, layout_type: epoch, layout: ns}`.
   - Se conservan los `add` de `resource["service.name"]=security-edge` y `deployment.environment.name`.
2. **Pipeline propio e incondicional** para el audit (fail-closed por construcción; sustituye la rama `if attributes["transaction"] != nil` del pipeline compartido):
   ```yaml
   logs:                # sin file_log/edge_audit
     receivers: [file_log, file_log/edge, otlp]
     processors: [memory_limiter, batch]
     exporters: [otlp_http/loki]
   logs/edge_audit:
     receivers: [file_log/edge_audit]
     processors: [memory_limiter, transform/edge_audit_projection, batch]
     exporters: [otlp_http/loki]
   ```
   Se elimina `transform/edge_audit_privacy` (la extracción de cabeceras ya no tiene sentido). El resto de pipelines no cambia.
3. La rotación por copytruncate hace que el `file_log` vea un archivo "nuevo" (cambia la huella del primer KB) y lo lea desde el inicio: sin duplicados [V].

### 5.2 `transform/edge_audit_projection` (OTTL, probado con `otelcol-contrib 0.160.0` [V])

Principios: **`error_mode: ignore`** (una línea rara no envenena el lote) **pero** el orden de las sentencias garantiza que `set(log.body, …)` y `keep_keys(log.attributes, [])` se ejecutan siempre, así un fallo de extracción deja campos vacíos y nunca la línea cruda. **Lista blanca**: `keep_keys` + cuerpo nuevo; nada de `delete_key` (lo que no se nombra no sale).

Sentencias probadas (cada una es una cadena YAML entre comillas simples; el implementador las ordena igual):

```
# 1. extracción a log.cache (no toca body ni atributos de salida)
set(log.cache["byMsg"],  SliceToMap(log.attributes["messages"], ["data","msg"], ["data","id"]))
set(log.cache["detMsg"], SliceToMap(log.attributes["messages"], ["data","msg"], ["data","data"]))
set(log.cache["logdata"], log.cache["detMsg"]["edge-detection"])
delete_key(log.cache["byMsg"], "edge-detection")  where log.cache["byMsg"] != nil
delete_key(log.cache["byMsg"], "")                where log.cache["byMsg"] != nil
set(log.cache["score"], ExtractPatterns(log.cache["logdata"], "^anomaly_score=(?P<score>[0-9]+) ")["score"])  where log.cache["logdata"] != nil
set(log.cache["cid"],   ExtractPatterns(log.cache["logdata"], " correlationId=(?P<cid>.*)$")["cid"])            where log.cache["logdata"] != nil
replace_pattern(log.cache["cid"], "[^A-Za-z0-9._:-]", "_") where log.cache["cid"] != nil
set(log.cache["ids"],  String(Values(log.cache["byMsg"])))  where log.cache["byMsg"] != nil
replace_pattern(log.cache["ids"], "[][]", "")                where log.cache["ids"] != nil
set(log.cache["msgs"], String(Keys(log.cache["byMsg"])))     where log.cache["byMsg"] != nil
replace_pattern(log.cache["msgs"], "^..|..$", "")            where log.cache["msgs"] != nil
replace_pattern(log.cache["msgs"], "\",\"", " | ")           where log.cache["msgs"] != nil
set(log.cache["path"],   Split(log.attributes["transaction"]["request"]["uri"], "?")[0])
set(log.cache["method"], log.attributes["transaction"]["request"]["method"])
set(log.cache["tid"],    log.attributes["transaction"]["id"])
set(log.cache["intr"],   log.attributes["transaction"]["is_interrupted"])
# 2. cuerpo nuevo (SUSTITUYE la línea cruda) y lista blanca de atributos
set(log.body, {"correlationId": log.cache["cid"], "edge.rule_ids": log.cache["ids"], "edge.rule_msgs": log.cache["msgs"], "edge.score": log.cache["score"], "edge.path": log.cache["path"], "edge.method": log.cache["method"], "edge.interrupted": log.cache["intr"], "edge.transaction_id": log.cache["tid"]})
keep_keys(log.attributes, [])
set(log.attributes["correlationId"],        log.cache["cid"])
set(log.attributes["edge.rule_ids"],        log.cache["ids"])
set(log.attributes["edge.rule_msgs"],       log.cache["msgs"])
set(log.attributes["edge.score"],           log.cache["score"])
set(log.attributes["edge.path"],            log.cache["path"])
set(log.attributes["edge.method"],          log.cache["method"])
set(log.attributes["edge.interrupted"],     log.cache["intr"])
set(log.attributes["edge.transaction_id"],  log.cache["tid"])
```

Notas verificadas que el implementador debe respetar:
- **Prefijo `log.`** en todas las rutas (sin él, 0.160.0 avisa "paths were modified to include their context prefix": hoy solo aviso).
- **`SliceToMap` exige claves de tipo string** (probado: con `["data","id"]` como clave falla). Por eso la clave es `data.msg` y el valor `data.id`: `Keys()` y `Values()` quedan **alineados** (`edge.rule_msgs[i]` ↔ `edge.rule_ids[i]`). Consecuencia aceptada: dos reglas con el mismo `msg` colapsan en una (en CRS 4.15 no se observó ninguna colisión).
- **`Concat` solo admite lista literal**; `String(Values(...))` produce `[941100,941110,…]`, de ahí los dos `replace_pattern`. `Values()` no puede ir dentro de un mapa literal (`Invalid value type pcommon.Slice`).
- Los backslashes en OTTL: `"[][]"` y `"^..|..$"` evitan escapes (los `\\[` fallaron al parsear); `"\",\""` es la única secuencia con escape.
- **Truncado**: `truncate_all(log.cache, 256)` (probado) **cortó `edge.rule_msgs`**. Implementar dos mapas de caché: `log.cache["f"]` truncado a 1024 y `log.cache["c"]` (solo `cid`) truncado a 128, o `Substring`. Hay aserción para esto (A-D4).
- Las guardas `where … != nil` evitan avisos `expected pcommon.Map but got nil` en líneas sin 100900 (formato antiguo o transacciones sin puntuación). `set` con nil emite un aviso de la feature gate `ottl.set.allowNil`; es inocuo.
- Sanitización del `correlationId` (controlado por el cliente): se reemplaza todo fuera de `[A-Za-z0-9._:-]` por `_` y se acota a 128. Probado con `abc def{x}"y` → `abc_def_x__y`.
- `edge.rule_ids` conserva **949110** ("superó el umbral") y descarta **100900** (la regla de auditoría, que no es una detección).

**Esquema de salida (contrato observable)**

| Dónde | Clave | Valor |
|---|---|---|
| Etiquetas Loki (índice) | `service_name` = `security-edge`, `deployment_environment_name` | solo estas dos |
| Structured metadata | `correlationId`, `edge_rule_ids` (CSV `941100,941110,…,949110`), `edge_rule_msgs` (`a \| b \| …`), `edge_score` (entero), `edge_path` (**sin query**), `edge_method`, `edge_interrupted` (`true`/`false`), `edge_transaction_id` | más `detected_level` y `observed_timestamp` que añade Loki |
| Línea (cuerpo) | JSON con **exactamente** las 8 claves `correlationId, edge.rule_ids, edge.rule_msgs, edge.score, edge.path, edge.method, edge.interrupted, edge.transaction_id` | nada más |
| No viajan nunca | query string, `client_ip/port`, `host_*`, `server_id`, `Matched Data` (`data.data` de las reglas CRS), `raw`, `tags`, `uri` completo, `transaction.id` de Coraza fuera de `edge.transaction_id` | |

Casos de borde cubiertos por fixtures (`observability/test-fixtures/edge-audit.negative.jsonl`): línea truncada ⇒ descartada; `messages` que no es lista ⇒ registro con campos vacíos y **sin** `SECRET123` del query (cuerpo sustituido igualmente).

Transacciones sin mensaje 100900 (cuerpo > `SecRequestBodyLimit` con `Reject`, que interrumpe antes de la fase 2): el registro sale con `correlationId` y `edge.score` vacíos y las reglas que haya; se anota como riesgo R5.

### 5.3 `observability/loki.yml`

Sin cambios necesarios (`allow_structured_metadata: true` ya está). Opcional (no se hace): `reject_old_samples` — ver riesgo R7.

### 5.4 `observability/grafana/dashboards/security-platform.json`

| Cambio | Detalle exacto |
|---|---|
| `templating.list[service].allValue` | `".*"` → **`".+"`** (corrige el 400 de Loki en el panel 12; sigue valiendo para las consultas Prometheus) |
| Panel 12 | consulta sin cambios; ahora funciona con "All" y, con `correlation=<id>`, devuelve las líneas del **edge y del PDP** del mismo id |
| **Panel 15** (logs) título `Edge — detecciones (regla, puntuación, ruta, correlationId)` | `expr`: `{service_name="security-edge"} \| edge_rule_ids != "" \| correlationId !~ "edge-healthcheck.*"` (la línea JSON ya trae todo; el canario queda fuera) |
| **Panel 16 [N]** (timeseries, barras) título `Edge — detecciones por regla principal` | `topk(10, sum by (rule_main) (count_over_time({service_name="security-edge"} \| edge_rule_ids != "" \| correlationId !~ "edge-healthcheck.*" \| line_format "{{.edge_rule_ids}}" \| regexp "^(?P<rule_main>[0-9]+)" [$__auto])))`, `legendFormat` `{{rule_main}}`. Descripción: "regla principal = primera regla que disparó en la transacción (949110 = superó el umbral)". Probado en Grafana vía `/api/ds/query` [V]. |
| **Panel 17 [N]** (timeseries, barras) título `Edge — detecciones por ruta` | `topk(10, sum by (edge_path) (count_over_time({service_name="security-edge"} \| edge_rule_ids != "" \| correlationId !~ "edge-healthcheck.*" [$__auto])))`, `legendFormat` `{{edge_path}}` |
| Todos los paneles Loki | `datasource: {"type":"loki","uid":"loki"}` explícito en panel **y** en cada target (hoy los targets no lo declaran) |
| Posiciones | 15 queda donde está (`y:68`); 16 y 17 en `y:80`, `w:12` cada uno |

`topk(10, …)` acota la cardinalidad visual de rutas con ids (`/api/v1/applications/{uuid}`). `$__auto` lo resuelve el backend de Grafana 13 [V]. La interpolación en la prueba sustituye `$service`/`${correlation:raw}` (sección 6.4).

### 5.5 Rotación y canario (criterio 4)

**Rotación (`edge/audit-rotate.sh` [N], lanzado en segundo plano desde `entrypoint.sh` justo antes de `exec caddy`)** — copytruncate por tamaño, probado pieza a pieza [V]:

- Variables (con validación de enteros en `entrypoint.sh`, falla cerrado igual que el resto): `EDGE_AUDIT_MAX_BYTES` (default `10485760`), `EDGE_AUDIT_KEEP` (default `5`), `EDGE_AUDIT_ROTATE_INTERVAL` (segundos, default `60`). Defaults en `Dockerfile` (`ENV`).
- Bucle: cada intervalo, `stat -c %s` del `SecAuditLog`; si `>= MAX`: desplazar `…json.N.gz → …json.(N+1).gz` descartando el que supere `KEEP`; `cp` del archivo a `…json.1`, **truncar inmediatamente** (`: > archivo`) y *después* `gzip -f …json.1` (la ventana de pérdida es solo entre `cp` y el truncado: milisegundos); `chmod 600` a los archivos rotados (contienen `Matched Data`). El archivo vivo se queda como lo crea Coraza.
- Cota total: `(KEEP + 1) × MAX` sin comprimir (≈ `MAX + KEEP×MAX/10` en la práctica). Con defaults: ≤ 60 MB frente a los ~95 MB **por día** de hoy. El Collector solo lee `security-edge-audit.json` (los `.gz` no coinciden con `include`).
- **No** se usa rename ni reload de Caddy (rename no reabre el archivo, hecho 2.6). Documentar en `edge/README.md` la advertencia de pérdida acotada (ventana `cp`→truncado y líneas aún no leídas por el Collector si estuvo caído; siempre están en el `.json.1.gz`).
- `Dockerfile`: `COPY audit-rotate.sh /usr/local/bin/edge-audit-rotate.sh` + `chmod +x` (mismo patrón que los otros dos scripts).

**Canario de detección (`edge/healthcheck.sh` [M])** — conserva el contrato de HU-027 ("en `detection` el audit crece tras el canario"):

- `/_edge/health` se comprueba **siempre** (cada latido, no audita).
- La sonda del motor (canario `edge-healthcheck-detect` + comprobación de crecimiento) solo corre si el archivo de estado `/tmp/edge-detect-probe` no existe o su mtime es más viejo que `EDGE_DETECT_PROBE_INTERVAL` (default `600` s; `0` = siempre). Éxito ⇒ `touch` del estado; **fallo ⇒ borrar el estado y salir 1** (el siguiente latido reintenta de inmediato, así que `unhealthy` llega a los `retries` normales).
- La comprobación de crecimiento pasa de `after -gt before` a **`after -ne before`** (una rotación entre las dos lecturas deja `after < before`).
- Efecto: de ~8 640 entradas/día (~95 MB) a 144 (~1,6 MB/día) con el default. `blocking` no cambia (el canario de `blocking` usa `edge-healthcheck`, que no se audita).
- Latencia de detección de un motor roto: ≤ `EDGE_DETECT_PROBE_INTERVAL` + una pasada de latido (decisión 4; documentarlo).

### 5.6 Documentación a corregir

- `observability/TELEMETRY-CONVENTION.md` §edge y `edge/README.md` §Observabilidad: reemplazar "conserva solo el `correlationId` y una marca de detección … elimina … detalles de reglas" por el esquema de la 5.2 (qué viaja, qué no, etiquetas vs structured metadata), el `logdata` de la regla 100900 y la rotación (`EDGE_AUDIT_*`, `EDGE_DETECT_PROBE_INTERVAL`).
- `observability/README.md`: cómo ejecutar la prueba (sección 6.6) y consultas LogQL de ejemplo.
- `edge/README.md` §"Observabilidad y estado": tabla de variables de rotación y la advertencia de pérdida acotada.

## 6. Prueba reproducible de punta a punta

Se escribe **primero** (rojo hoy, ver 6.5). Dos artefactos: `observability/compose.test.yml` y `observability/tests/Test-WafObservability.ps1`.

### 6.1 `observability/compose.test.yml` [N]

Principios: efímero, proyecto propio (`hu034-obs-test`), **usa los archivos reales del repo** (el `collector.yml` de producción sin copias, `loki.yml`, el provisioning y el dashboard reales, y `edge/rules`, `Caddyfile`, `entrypoint.sh`, `healthcheck.sh`, `audit-rotate.sh` montados `:ro` sobre `security-edge:local`, así la prueba no depende de reconstruir la imagen y no descarga nada), `pull_policy: never` en todo, sin Jaeger/Prometheus.

```yaml
name: hu034-obs-test
services:
  upstream:                                  # PDP de pruebas (escribe security-pdp.json)
    image: alpine:3.20
    pull_policy: never
    command: ["sh","-c","while true; do nc -l -p 8080 -e /stub-pdp.sh; done"]
    volumes:
      - ./test-fixtures/stub-pdp.sh:/stub-pdp.sh:ro
      - ${OBS_TEST_LOG_DIR:?}:/var/log/security
  edge:
    image: security-edge:local
    pull_policy: never
    entrypoint: ["sh","/usr/local/bin/edge-entrypoint.sh"]       # sin depender del bit +x
    environment:
      EDGE_MODE: ${EDGE_TEST_MODE:-detection}
      EDGE_UPSTREAM: http://upstream:8080
      EDGE_LISTEN: ":8088"
      EDGE_RATE_DEFAULT: "100000/1m"         # la prueba dispara decenas de peticiones desde una IP
      EDGE_AUDIT_MAX_BYTES: ${EDGE_TEST_AUDIT_MAX_BYTES:-10485760}
      EDGE_AUDIT_KEEP: ${EDGE_TEST_AUDIT_KEEP:-5}
      EDGE_AUDIT_ROTATE_INTERVAL: ${EDGE_TEST_ROTATE_INTERVAL:-60}
      EDGE_DETECT_PROBE_INTERVAL: ${EDGE_TEST_PROBE_INTERVAL:-600}
      DEPLOYMENT_ENVIRONMENT: test
    ports: ["127.0.0.1::8088"]               # puerto aleatorio; el script lo descubre con `docker compose port`
    volumes:
      - ${OBS_TEST_LOG_DIR:?}:/var/log/security
      - ../edge/rules:/etc/coraza/rules:ro
      - ../edge/Caddyfile:/etc/caddy/Caddyfile:ro
      - ../edge/entrypoint.sh:/usr/local/bin/edge-entrypoint.sh:ro
      - ../edge/healthcheck.sh:/usr/local/bin/edge-healthcheck.sh:ro
      - ../edge/audit-rotate.sh:/usr/local/bin/edge-audit-rotate.sh:ro
    healthcheck:
      test: ["CMD-SHELL","sh /usr/local/bin/edge-healthcheck.sh"]
      interval: 5s
      timeout: 5s
      retries: 3
      start_period: 10s
    depends_on: [upstream]
  collector:
    image: otel/opentelemetry-collector-contrib:0.160.0
    pull_policy: never
    user: "0:0"
    command: ["--config=/etc/otelcol/config.yml"]
    volumes:
      - ./collector.yml:/etc/otelcol/config.yml:ro          # EL MISMO archivo de producción
      - ${OBS_TEST_LOG_DIR:?}:/var/log/security:ro
      - collector-data:/var/lib/otelcol
    environment:
      PDP_METRICS_TARGET: upstream:8080     # solo evita errores de scrape; no hay métricas en la prueba
      PEP_METRICS_TARGET: upstream:8080
      DEPLOYMENT_ENVIRONMENT: test
    ports: ["127.0.0.1::13133"]
    depends_on: [loki]
  loki:
    image: grafana/loki:3.7.7
    pull_policy: never
    command: ["-config.file=/etc/loki/local.yml"]
    volumes: ["./loki.yml:/etc/loki/local.yml:ro"]
    ports: ["127.0.0.1::3100"]
  grafana:
    image: grafana/grafana:13.2.1
    pull_policy: never
    environment:
      GF_SECURITY_ADMIN_USER: admin
      GF_SECURITY_ADMIN_PASSWORD: test-only-not-a-secret     # valor de prueba, solo para este compose efímero
      GF_USERS_ALLOW_SIGN_UP: "false"
      GF_AUTH_ANONYMOUS_ENABLED: "false"
    volumes:
      - ./grafana/provisioning:/etc/grafana/provisioning:ro
      - ./grafana/dashboards:/var/lib/grafana/dashboards:ro
    ports: ["127.0.0.1::3000"]
    depends_on: [loki]
volumes:
  collector-data:
```

Detalles comprobados [V]: con `jaeger`/`prometheus` ausentes, el Collector arranca igual (solo avisa de scrapes fallidos; los exportadores a Jaeger no se usan); el datasource Prometheus/Jaeger aprovisionado queda inalcanzable sin afectar a Loki; Loki tarda ~15 s en estar `ready`; Grafana provisiona el dashboard al arrancar. `edge:2019` resuelve en la red por defecto del proyecto.

### 6.2 `observability/tests/Test-WafObservability.ps1` [N] — ejecución en Windows

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File observability\tests\Test-WafObservability.ps1
# opciones: -KeepStack (no derriba), -SkipBlocking, -SkipRotation, -Fases deteccion,bloqueo,rotacion
```

Requisitos de implementación (Windows PowerShell 5.1 **y** pwsh 7):
- `Set-StrictMode -Version Latest`, `$ErrorActionPreference = 'Stop'`; comprobar `$LASTEXITCODE` tras cada `& docker …`; **no** usar `2>&1` sobre ejecutables nativos (5.1 lo convierte en error).
- **Texto ASCII** en el script (o guardarlo con BOM UTF-8): 5.1 lee UTF-8 sin BOM como ANSI y rompe las tildes.
- Peticiones con `curl.exe` (no el alias `curl`): `curl.exe -s -o NUL -w "%{http_code}" -H "Host: edge.test" -H "X-Correlation-Id: <id>" "<url>"`. `Invoke-WebRequest` en 5.1 no permite fijar `Host` ni leer cuerpos de 403 con comodidad. Para Loki y Grafana: `Invoke-RestMethod` (Basic con `admin:test-only-not-a-secret` construido a mano en `Authorization`).
- Directorio de logs temporal: `Join-Path $env:TEMP "hu034-obs-$run"` pasado a Compose como `OBS_TEST_LOG_DIR` **con barras normales** (`C:/Users/…`).
- Teardown (`finally`, salvo `-KeepStack`): `docker compose -p hu034-obs-test -f observability\compose.test.yml down -v --remove-orphans` (**solo su proyecto**) y borrado del directorio temporal verificando que su ruta empieza por `$env:TEMP` y contiene `hu034-obs-`.
- Salida: una línea `PASS|FAIL <id> <descripción>` por aserción; código de salida 1 si alguna falla; resumen final. Duración esperada 3-5 min (arranque ~40 s, ~20 s por fase de consulta, rotación ~60 s).
- Precondiciones (fallo inmediato y claro, **sin descargar nada**): `docker image inspect` de las 5 imágenes (`security-edge:local`, `alpine:3.20`, collector, loki, grafana); si falta `security-edge:local`, mensaje "construya el edge (requiere red) o pida autorización" y salida 2.
- `docker compose … up -d --pull never --wait` (`--wait` espera `healthy` del edge; el resto no define healthcheck: el script sondea `/ready` de Loki, `13133` del Collector y `/api/health` de Grafana hasta 90 s). Fases 2 y 3 reconfiguran el edge con variables de entorno de proceso (`EDGE_TEST_MODE`, `EDGE_TEST_AUDIT_*`) y `docker compose up -d --force-recreate --no-deps --wait edge`; los volúmenes de logs se conservan.

Ids de la ejecución: `$run = [guid]::NewGuid().ToString('N').Substring(0,8)`; correlationIds `waf-$run-xss`, `-sqli`, `-ok`, `-hostile`, `-after`, `-blk`, `-env`, `-rot-<i>`.

### 6.3 Pasos y aserciones exactas

Notación: `Loki(expr)` = `GET /loki/api/v1/query_range?query=<expr>&since=1h&limit=100` (o `query` para instantáneas); "espera" = reintento cada 2 s, tope 60 s. Todas las expresiones LogQL usan comillas dobles o acentos graves (backtick) para regex con comillas.

**Fase 0 — pila lista**

| ID | Aserción |
|---|---|
| S-0 | Las 5 imágenes existen; Loki `/ready` 200; Collector `13133` 200; Grafana `/api/health` con `database: ok`; contenedor `edge` `healthy` en `detection` (contrato de healthcheck de HU-027 sigue vivo con el canario espaciado). |

**Fase 1 — `detection` (criterios 1, 2, parte de 3, privacidad)**

Peticiones (todas `Host: edge.test`):
- XSS: `GET /api/v1/applications?q=%3Cscript%3Ealert%281%29%3C%2Fscript%3E&secret=hunter2-<run>` con `waf-<run>-xss`
- SQLi: `GET /api/v1/applications?id=1%27%20OR%20%271%27%3D%271` con `waf-<run>-sqli`
- benigna: `GET /api/v1/applications?page=1` con `waf-<run>-ok`
- hostil: `GET …?q=%3Cscript%3E` con `X-Correlation-Id: bad id"}{<run>` (el stub sanea al escribir)

| ID | Aserción (exacta) | Criterio |
|---|---|---|
| D-1 | HTTP 200 en XSS, SQLi, benigna y hostil (detección no bloquea) | |
| D-2 | espera + `Loki({service_name="security-edge"} \| correlationId="waf-<run>-xss")` ⇒ **exactamente 1 entrada** | 1 |
| D-3 | `Loki({service_name="security-edge"} \| correlationId="waf-<run>-xss" \| edge_rule_ids=~".*941100.*" \| edge_path="/api/v1/applications" \| edge_score != "")` ⇒ 1 entrada (la consulta literal del criterio 1) | 1 |
| D-4 | Sobre esa entrada: `edge_score` ∈ ℤ⁺ ≥ 5 **y** igual al `N` de `(Total Score: N)` dentro de `edge_rule_msgs`; `edge_rule_ids` contiene `941100` y `949110` y **no** `100900`; `edge_rule_msgs` contiene el texto completo `Inbound Anomaly Score Exceeded (Total Score: ` (comprueba que no se trunca); `edge_method="GET"`; `edge_interrupted="false"`; `edge_transaction_id` no vacío | 1 |
| D-5 | La línea es JSON con **exactamente** las claves `correlationId, edge.rule_ids, edge.rule_msgs, edge.score, edge.path, edge.method, edge.interrupted, edge.transaction_id` y sus valores coinciden con la structured metadata | 1 |
| D-6 | `abs(timestamp de la entrada − ahora) ≤ 120 s` (valida el parseo epoch) | 1 |
| D-7 | SQLi: entrada con `edge_rule_ids=~".*942100.*"`, `edge_score="5"` (detección *en* el umbral) | 1 |
| D-8 | **Privacidad de la línea**: `Loki({service_name="security-edge"} \|= "edge.transaction_id" \|~ "(?i)hunter2-<run>\|secret\|%3C\|<script\|Matched Data\|client_ip\|q=\|unix_timestamp\|[0-9]{1,3}[.][0-9]{1,3}[.][0-9]{1,3}[.][0-9]{1,3}")` ⇒ **0** | 1 |
| D-9 | **Privacidad de metadata**: el conjunto de claves de las entradas con `edge.transaction_id` ⊆ `{correlationId, deployment_environment_name, detected_level, observed_timestamp, service_name, edge_rule_ids, edge_rule_msgs, edge_score, edge_path, edge_method, edge_interrupted, edge_transaction_id}` **y** ⊇ `{correlationId, edge_rule_ids, edge_score, edge_path}`; ningún *valor* contiene `hunter2-<run>` | 1 |
| D-10 | **Cardinalidad** (`GET /loki/api/v1/labels?since=1h`): el conjunto de etiquetas indexadas es exactamente `{service_name, deployment_environment_name}` (ninguna `correlationId`, ninguna `edge_*`) | 1 |
| D-11 | **Ninguna línea cruda del audit en Loki**: `Loki({service_name="security-edge"} \|~ "unix_timestamp\|\"messages\"\|client_ip")` ⇒ 0 | 1 |
| D-12 | **Criterio 2**: espera + `Loki({service_name="security-pdp"} \| correlationId="waf-<run>-xss")` ⇒ ≥ 1; y `Loki({service_name=~"security-(edge\|pdp)"} \| correlationId="waf-<run>-xss")` devuelve 2 flujos con `service_name` ∈ {`security-edge`,`security-pdp`} | 2 |
| D-13 | Benigna: PDP ≥ 1 línea, **edge 0** (no se audita lo que no puntúa) | 2 |
| D-14 | **Hostil**: la entrada del edge tiene `correlationId` que casa `^[A-Za-z0-9._:-]{1,128}$`, y `Loki({service_name="security-edge"} \| correlationId=~".*[ \"{}].*")` ⇒ 0 | 1 |
| D-15 | **Fail-closed**: añadir al audit (con `docker compose exec -T edge sh -c "cat >> /var/log/security/security-edge-audit.json"`) las 2 líneas de `test-fixtures/edge-audit.negative.jsonl`; esperar 20 s; `Loki({service_name="security-edge"} \|= "SECRET123")` ⇒ 0 **y** una segunda petición `waf-<run>-after` aparece en Loki (el lote no quedó envenenado) | 1 |
| D-16 | Canario fuera de lo "real": `Loki({service_name="security-edge"} \| edge_rule_ids != "" \| correlationId !~ "edge-healthcheck.*" \| correlationId =~ "edge-healthcheck.*")` ⇒ 0 (coherencia del filtro del panel) | 3 |

**Fase 1b — Grafana (criterio 3), mismo estado de datos**

Autenticación Basic `admin:<pw de prueba>`; `base = puerto aleatorio de grafana`.

| ID | Aserción |
|---|---|
| G-1 | `GET /api/dashboards/uid/security-platform` ⇒ 200; contiene paneles con `id` 12, 15, 16, 17; cada panel Loki declara `datasource.uid = "loki"` (panel y targets) |
| G-2 | `GET /api/datasources/uid/loki/health` ⇒ `status: OK` |
| G-3 | Para cada panel 12, 15, 16, 17: tomar `targets[0].expr` **tal como está guardado**, sustituir `$service` por `templating.list[name=service].allValue` (así el test falla si queda `.*`), `${correlation:raw}` por `.*` (panel 12) y `$__auto`/`$__interval` se dejan a Grafana; `POST /api/ds/query` con `{queries:[{refId:A, datasource:{type:loki,uid:loki}, expr, queryType: range, maxLines:100}], from:now-1h, to:now}` ⇒ `results.A.error` ausente **y** ≥ 1 fila/serie |
| G-4 | Panel 15: las filas incluyen `waf-<run>-xss` y ninguna fila con `correlationId` que empiece por `edge-healthcheck` |
| G-5 | Panel 16: existe una serie con etiqueta `rule_main="941100"` y otra con `rule_main="942100"` |
| G-6 | Panel 17: existe una serie con etiqueta `edge_path="/api/v1/applications"` |
| G-7 | Panel 12 con `${correlation:raw}` = `waf-<run>-xss`: devuelve filas de **ambos** `service_name` (`security-edge` y `security-pdp`) |

**Fase 2 — `blocking` (recrear edge con `EDGE_TEST_MODE=blocking`)**

| ID | Aserción | Criterio |
|---|---|---|
| B-1 | XSS con `waf-<run>-blk`: HTTP **403**, cuerpo contiene `REQUEST_REJECTED` y el `correlationId` | 1 |
| B-2 | Mismas aserciones D-2…D-5 y D-8…D-9 para `waf-<run>-blk`, con `edge_interrupted="true"` | 1 |
| B-3 | Tras confirmar B-2 (la tubería fluye), `Loki({service_name="security-pdp"} \| correlationId="waf-<run>-blk")` ⇒ **0** (el bloqueo no llegó al PDP) | 2 (negativo) |
| B-4 | `GET /.env` con `waf-<run>-env`: 403; entrada con `edge_rule_ids=~".*100010.*"`, `edge_path="/.env"` (regla propia de plataforma) | 1 |

**Fase 3 — rotación y canario (criterio 4); edge en `detection` con `EDGE_TEST_AUDIT_MAX_BYTES=262144`, `EDGE_TEST_AUDIT_KEEP=3`, `EDGE_TEST_ROTATE_INTERVAL=2`, `EDGE_TEST_PROBE_INTERVAL=600`**

| ID | Aserción |
|---|---|
| R-1 | Enviar 60 ataques XSS `waf-<run>-rot-<i>` (≈ 12 KB c/u ⇒ ≈ 720 KB ≈ 2,7 × tope), ≈ 5 por segundo |
| R-2 | Espera ≤ 30 s hasta que exista `security-edge-audit.json.1.gz` en el directorio de logs |
| R-3 | Tras 10 s sin tráfico: tamaño del archivo vivo **< tope + 64 KiB** |
| R-4 | Existen como máximo `KEEP` archivos `security-edge-audit.json.N.gz` (N ≤ 3) y la suma de `security-edge-audit.json*` ≤ `(KEEP+1) × tope + 64 KiB` |
| R-5 | El `.1.gz` es gzip válido y su primera línea es JSON con `transaction.id` |
| R-6 | **Sin pérdida**: los 60 `waf-<run>-rot-<i>` aparecen cada uno **exactamente una vez** en Loki (espera ≤ 60 s). Si resultara intermitente, el remedio es espaciar las peticiones, no relajar la aserción |
| R-7 | **Canario espaciado**: sin tráfico durante 30 s (≥ 6 latidos de 5 s) el número de entradas `correlationId="edge-healthcheck-detect"` nuevas es **≤ 1**, y `State.Health.Status` sigue `healthy` |
| R-8 | **Contrato de healthcheck intacto**: `docker compose exec -T -e EDGE_DETECT_PROBE_INTERVAL=0 edge sh /usr/local/bin/edge-healthcheck.sh` sale 0 y el tamaño del audit cambió (la sonda corre y detecta); una segunda llamada con el intervalo por defecto, inmediata, sale 0 **sin** que cambie el tamaño |
| R-9 | Documentación: `edge/README.md` contiene `EDGE_AUDIT_MAX_BYTES`, `EDGE_AUDIT_KEEP`, `EDGE_DETECT_PROBE_INTERVAL`; `observability/TELEMETRY-CONVENTION.md` contiene `edge.rule_ids` y **no** contiene la frase `una marca de detección` |

### 6.4 Notas de implementación del test

- `Loki()` devuelve por entrada `stream` (etiquetas + structured metadata mezcladas) y `values[i][1]` (línea); el timestamp es `values[i][0]` en ns.
- Para "exactamente N" el script cuenta entradas, no flujos.
- El stub escribe `security-pdp.json` con la forma del PDP real (campos `@timestamp, level, message, correlationId, event.name, http.response.status_code, service.name, deployment.environment.name`; muestra en `test-fixtures/pdp-line.sample.json`).
- Las aserciones de privacidad D-8/D-9 deben ejecutarse **también** en la fase 2.

### 6.5 Rojo esperado hoy (el tester debe comprobarlo antes de pasar el testigo)

| Aserciones que fallan con el repo actual | Por qué |
|---|---|
| D-2…D-7, D-14, B-2, B-4 | la regla 85 no entrega `logdata`; el Collector no extrae `correlationId`/regla/ruta de ahí |
| D-8, D-9, D-11 | el cuerpo viaja crudo (query, `client_ip`, `Matched Data`) |
| D-6 | timestamp con layout inválido |
| D-15 | línea sin sanear y pipeline fail-open |
| G-3 (panel 12 y 15), G-4…G-7 | `allValue ".*"` ⇒ 400; panel 15 sin datos; paneles 16/17 inexistentes |
| R-2…R-4, R-7, R-8 (variables), R-9 | no hay rotación, ni intervalo de sonda, ni documentación |
| Pasan hoy (control de que la prueba no es vacua) | S-0, D-1, D-10 (índice ya correcto), D-12/D-13 parcialmente (el PDP de prueba sí escribe) |

### 6.6 Cómo lo usa el implementador

Primero `-Fases deteccion` hasta verde (regla 85 + colector), luego `bloqueo`, luego `rotacion`; ciclo corto con `-KeepStack` y `docker compose -p hu034-obs-test -f observability\compose.test.yml logs collector`. Validación rápida de OTTL sin pila: `docker run --rm -v <dir>:/in otel/opentelemetry-collector-contrib:0.160.0 validate --config=/in/collector.yml` (ya local; requiere `directory must exist` para `file_storage`: montar `/var/lib/otelcol` o usar la pila).

## 7. Criterios de aceptación enumerados y trazabilidad

| # | Criterio de la historia | Se cumple cuando | Aserciones | Artefactos |
|---|---|---|---|---|
| **1** | Tras un ataque de la suite, la consulta LogQL por `correlationId` devuelve la entrada con regla, puntuación y ruta | D-2…D-7, B-2, B-4 verdes; privacidad D-8…D-11, D-14, D-15 verdes (sin ellas el criterio se cumpliría filtrando datos) | D-2, D-3, D-4, D-5, D-6, D-7, D-8, D-9, D-10, D-11, D-14, D-15, B-1, B-2, B-4 | regla 85, `collector.yml` |
| **2** | El mismo `correlationId` es localizable en los logs del PDP cuando la petición sí pasó | D-12 y D-13 verdes; B-3 (el bloqueo no llega) | D-12, D-13, B-3, G-7 | `collector.yml` (receptor PDP sin cambios), stub |
| **3** | El panel muestra bloqueos por regla y por ruta con datos reales | G-1…G-7 verdes con datos generados por la propia prueba | G-1…G-7, D-16 | `security-platform.json` |
| **4** | El audit log tiene tope de tamaño o rotación documentada | R-2…R-9 verdes | R-2, R-3, R-4, R-5, R-6, R-7, R-8, R-9 | `audit-rotate.sh`, `entrypoint.sh`, `healthcheck.sh`, `Dockerfile`, READMEs |

Criterios transversales (no son de la historia): ninguna descarga de imágenes; `ci/edge-pipeline.yml` y `assert_detection_logs_clean` siguen válidos (hecho 2.7); HU-027 no regresa (healthcheck de `detection` vivo, R-8; `blocking` intacto, B-1).

## 8. Árbol de archivos ([N] nuevo, [M] modifica)

```
edge/rules/85-audit-on-detection.conf                 [M] texto exacto de la sección 4
edge/audit-rotate.sh                                  [N] copytruncate por tamaño (5.5)
edge/entrypoint.sh                                    [M] validar EDGE_AUDIT_*; lanzar edge-audit-rotate.sh en segundo plano antes de `exec caddy`
edge/healthcheck.sh                                   [M] sonda espaciada + `-ne` (5.5)
edge/Dockerfile                                       [M] COPY/chmod de audit-rotate.sh; ENV de los defaults
edge/README.md                                        [M] observabilidad, variables y advertencia de pérdida acotada
observability/collector.yml                           [M] receptor, pipeline propio, transform/edge_audit_projection (5.1-5.2)
observability/grafana/dashboards/security-platform.json [M] allValue, paneles 15/16/17, datasource explícito (5.4)
observability/TELEMETRY-CONVENTION.md                 [M] esquema real del audit proyectado (5.6)
observability/README.md                               [M] cómo ejecutar la prueba
observability/compose.test.yml                        [N] sección 6.1
observability/tests/Test-WafObservability.ps1         [N] sección 6.2-6.3  (lo escribe el TESTER primero)
observability/test-fixtures/edge-audit.sample.jsonl   [N] ya creado: 4 líneas reales (3 detection, 1 blocking)
observability/test-fixtures/edge-audit.negative.jsonl [N] ya creado: truncada + `messages:"oops"` con SECRET123
observability/test-fixtures/pdp-line.sample.json      [N] ya creado: forma de security-pdp.json
observability/test-fixtures/stub-pdp.sh               [N] ya creado y probado: PDP de prueba (nc -e)
observability/loki.yml, observability/compose.yml     sin cambios
ci/edge-pipeline.yml                                  sin cambios (conectar la prueba a CI queda fuera de alcance)
```

`edge/compose.pdp.yml` / `compose.pep.yml`: sin cambios (los defaults de rotación y sonda llegan por el `ENV` de la imagen; el overlay ya fija `EDGE_HEALTHCHECK_INTERVAL`).

## 9. Riesgos

| # | Riesgo | Mitigación / decisión |
|---|---|---|
| R1 | **`edge.status` no existe en el audit AKZ** (decisión 2) | Omitir; confirmar con el responsable (sección 3) |
| R2 | "Por regla" es "regla principal" (primera del conjunto), no conteo exacto por cada regla | Documentado en el panel; alternativas en la sección 3 |
| R3 | La regla 85 actual **audita nada**; la corregida audita toda transacción con puntuación > 0: si un cliente legítimo recurrente puntúa (p. ej. `Host` numérico +3 por 920350), el audit crece con tráfico real | La rotación acota el peso; HU-027 criterio 2 ya exige que los flujos legítimos no puntúen |
| R4 | Pérdida acotada en copytruncate (ventana `cp`→truncado y líneas aún no leídas si el Collector estuvo caído) | Siempre quedan en `.json.1.gz`; documentado; R-6 la vigila |
| R5 | Transacciones interrumpidas antes de la fase 2 (cuerpo > `SecRequestBodyLimit` con `Reject`) no tienen mensaje 100900 ⇒ sin `correlationId`/puntuación en Loki | Se proyectan igualmente con las reglas disponibles; si molesta, el `correlationId` está en el log de acceso del edge (`resp_headers.X-Correlation-Id`) |
| R6 | Colisión de `msg` en `SliceToMap` (dos reglas con el mismo mensaje colapsan) | No observada en CRS 4.15.0; aserción D-4 compara ids con mensajes; si aparece, cambiar a clave `msg` + sufijo |
| R7 | `start_at: beginning` con `storage` nuevo reenvía el audit histórico a Loki: entradas > 7 días son rechazadas (`reject_old_samples_max_age`) y pueden generar errores ruidosos | Fuera de alcance; solo ruido en el arranque en un entorno con histórico |
| R8 | Dependencia de versiones fijadas: OTTL/`SliceToMap`/`ExtractPatterns` (collector 0.160.0), CRS 4.15.0 (puntuaciones), Loki 3.7.7 (structured metadata) | La prueba usa las mismas versiones que producción; los números exactos solo se asertan donde son deterministas (20 y 5 con `Host: edge.test`) |
| R9 | La prueba monta scripts/reglas del repo sobre `security-edge:local` (no valida el empaquetado `COPY` del Dockerfile) | Cubrir el `COPY` con una comprobación estática (`docker build` requiere red ⇒ no aquí); el pipeline de HU-027 reconstruye la imagen |
| R10 | PDP de prueba es un stub (hecho/decisión 6) | Documentado; el PDP real lo cubre `assert_pdp_correlation` del pipeline del edge |
| R11 | Windows: `.ps1` en 5.1 + tildes, alias `curl`, bind-mount de rutas con `\`, bit `+x` ausente en Linux | Requisitos de 6.2 (ASCII, `curl.exe`, barras normales, `sh <script>`) |
| R12 | Datos sensibles fuera del camino al Collector: el stderr de Caddy (docker logs) y el archivo local del audit contienen query y `Matched Data` | Ya anotado en TELEMETRY-CONVENTION ("evidencia sensible, no se publica como artefacto"); los `.gz` rotados se crean `0600`; fuera de alcance reducir el stderr de Coraza |
| R13 | La sonda espaciada retrasa la detección de un motor roto hasta `EDGE_DETECT_PROBE_INTERVAL` | Decisión 4 lo prevé ("intervalo largo"); el blocking sigue siendo instantáneo |
| R14 | Cardinalidad de `edge_path` (ids en rutas) y de `edge_transaction_id` | `topk(10, …)` en los paneles; structured metadata no se indexa; no se usan como etiquetas |

## 10. Ambigüedades pendientes (para el responsable)

1. **`edge.status`**: ¿se omite (propuesto) o se acepta derivarlo como `403` cuando `is_interrupted=true`? (3, fila 2)
2. **Ruta**: ¿de `request.uri` antes de `?` (propuesto) o del `path=` del `logdata`? (3, fila 1)
3. **"Por regla"**: ¿basta la regla principal + conjunto (propuesto) o se quiere un campo explícito / un registro por regla? (3, "Límite de por regla")
4. **Intervalos por defecto**: `EDGE_DETECT_PROBE_INTERVAL=600`, `EDGE_AUDIT_MAX_BYTES=10 MiB`, `EDGE_AUDIT_KEEP=5` (≤ 60 MB). ¿Conformes?
5. **Autorización para construir `security-edge:local`** en máquinas donde no exista (requiere descargar capas): hoy el script solo falla con mensaje.
6. **Conexión a CI** de `Test-WafObservability.ps1` (fuera de alcance de esta historia; el agente de CI Linux necesitaría `pwsh`, que ubuntu-latest trae).
