#!/bin/sh
# Healthcheck del MOTOR, no solo del proxy (PLAN-HU-027 §7.4).
#  - /_edge/health debe responder 200.
#  - blocking:  el canario con payload debe dar 403 — si da 200, el motor no bloquea.
#  - detection: el canario da 200 Y el audit log debe crecer — si no crece, el motor no detecta.
set -u
PORT="${EDGE_LISTEN#:}"
BASE="http://127.0.0.1:${PORT}"
AUDIT="${EDGE_LOG_DIR:-/var/log/security}/security-edge-audit.json"
PAYLOAD='%3Cscript%3Ealert%281%29%3C%2Fscript%3E'

# Todas las peticiones del healthcheck llevan X-Correlation-Id edge-healthcheck*: la regla 100001
# las saca del audit log (y el Host numérico 127.0.0.1 dispararía 920350 en cada latido).
code=$(curl -s -o /dev/null -w '%{http_code}' -H 'X-Correlation-Id: edge-healthcheck' "${BASE}/_edge/health" || echo 000)
[ "$code" = "200" ] || { echo "edge: /_edge/health -> $code"; exit 1; }

case "${EDGE_MODE:-}" in
  blocking)
    code=$(curl -s -o /dev/null -w '%{http_code}' -H 'X-Correlation-Id: edge-healthcheck' "${BASE}/_edge/canary?p=${PAYLOAD}" || echo 000)
    [ "$code" = "403" ] || { echo "edge: canario en blocking -> $code (esperado 403): el motor no bloquea"; exit 1; }
    ;;
  detection)
    # En detection el canario SÍ debe auditarse (es la evidencia de que el motor detecta): por eso
    # va con edge-healthcheck-detect y no con edge-healthcheck (que la regla 100001 excluye del audit).
    # El pipeline ignora las transacciones cuyo X-Correlation-Id empieza por edge-healthcheck.
    before=$(wc -c < "$AUDIT" 2>/dev/null || echo 0)
    code=$(curl -s -o /dev/null -w '%{http_code}' -H 'X-Correlation-Id: edge-healthcheck-detect' "${BASE}/_edge/canary?p=${PAYLOAD}" || echo 000)
    [ "$code" = "200" ] || { echo "edge: canario en detection -> $code (esperado 200)"; exit 1; }
    sleep 1
    after=$(wc -c < "$AUDIT" 2>/dev/null || echo 0)
    [ "$after" -gt "$before" ] || { echo "edge: el audit log no creció tras el canario: el motor no detecta"; exit 1; }
    ;;
  *) echo "edge: EDGE_MODE inválido"; exit 1 ;;
esac
exit 0
