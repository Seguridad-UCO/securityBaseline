#!/bin/sh
# Arranque del edge (PLAN-HU-027 §7.1 / §7.4). Traduce las variables del contrato a directivas y
# FALLA CERRADO ante cualquier valor que no sepa interpretar: un edge mal configurado no sirve.
set -eu

fail() { echo "edge: $*" >&2; exit 1; }

case "${EDGE_MODE:-}" in
  detection) RULE_ENGINE=DetectionOnly ;;
  blocking)  RULE_ENGINE=On ;;
  *) fail "EDGE_MODE debe ser 'detection' o 'blocking' (recibido: '${EDGE_MODE:-}')" ;;
esac

[ -n "${EDGE_UPSTREAM:-}" ] || fail "EDGE_UPSTREAM es obligatoria (p. ej. http://pdp:8080)"
case "${EDGE_PARANOIA:-1}" in 1|2|3|4) ;; *) fail "EDGE_PARANOIA debe ser 1..4" ;; esac
case "${EDGE_ANOMALY_INBOUND:-5}" in ''|*[!0-9]*) fail "EDGE_ANOMALY_INBOUND debe ser un entero" ;; esac

# N/ventana -> events N, window ventana (caddy-ratelimit)
split_rate() { # $1=valor $2=nombre
  case "$1" in
    */*) ;;
    *) fail "$2 debe tener la forma N/ventana (p. ej. 60/1m), recibido '$1'" ;;
  esac
  echo "${1%%/*} ${1#*/}"
}
set -- $(split_rate "${EDGE_RATE_AUTHORIZE:-60/1m}" EDGE_RATE_AUTHORIZE); export EDGE_RATE_AUTHORIZE_EVENTS=$1 EDGE_RATE_AUTHORIZE_WINDOW=$2
set -- $(split_rate "${EDGE_RATE_SESSION:-30/1m}" EDGE_RATE_SESSION);     export EDGE_RATE_SESSION_EVENTS=$1 EDGE_RATE_SESSION_WINDOW=$2
set -- $(split_rate "${EDGE_RATE_DEFAULT:-300/1m}" EDGE_RATE_DEFAULT);    export EDGE_RATE_DEFAULT_EVENTS=$1 EDGE_RATE_DEFAULT_WINDOW=$2

# trusted_proxies vacío no es válido en el Caddyfile: se emite "private_ranges" solo si se pidió,
# y si no, un rango imposible (loopback) que nunca casa con un cliente real.
if [ -n "${EDGE_TRUSTED_PROXIES:-}" ]; then
  export EDGE_TRUSTED_PROXIES_DIRECTIVE="$(echo "${EDGE_TRUSTED_PROXIES}" | tr ',' ' ')"
else
  export EDGE_TRUSTED_PROXIES_DIRECTIVE="127.0.0.1/32"
fi

mkdir -p "${EDGE_LOG_DIR}" /etc/coraza/generated
cat > /etc/coraza/generated/00-engine.conf <<CONF
# Generado por edge-entrypoint.sh desde EDGE_MODE=${EDGE_MODE} — no editar a mano.
SecRuleEngine ${RULE_ENGINE}
CONF
cat > /etc/coraza/generated/01-tuning.conf <<CONF
# Generado por edge-entrypoint.sh — paranoia y umbral del contrato (PLAN-HU-027 §7.1).
SecAction "id:900000,phase:1,pass,t:none,nolog,tag:'OWASP_CRS',ver:'OWASP_CRS/4.0',setvar:tx.blocking_paranoia_level=${EDGE_PARANOIA}"
SecAction "id:900110,phase:1,pass,t:none,nolog,tag:'OWASP_CRS',ver:'OWASP_CRS/4.0',setvar:tx.inbound_anomaly_score_threshold=${EDGE_ANOMALY_INBOUND},setvar:tx.outbound_anomaly_score_threshold=4"
CONF
export EDGE_RULE_ENGINE="${RULE_ENGINE}"

echo "edge: modo=${EDGE_MODE} (SecRuleEngine ${RULE_ENGINE}) paranoia=${EDGE_PARANOIA} umbral=${EDGE_ANOMALY_INBOUND} upstream=${EDGE_UPSTREAM} coraza-caddy=${CORAZA_CADDY_VERSION:-?}"
caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile || fail "Caddyfile inválido: no se arranca"
exec caddy run --config /etc/caddy/Caddyfile --adapter caddyfile
