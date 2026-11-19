#!/bin/sh
# Rotacion por tamano del audit log de Coraza (PLAN-HU-034 §5.5, criterio 4).
#
# Coraza abre SecAuditLog UNA vez (O_APPEND) y no lo reabre: un rename deja a Coraza escribiendo en el
# archivo renombrado. La unica rotacion soportada sin reiniciar el edge es COPYTRUNCATE: copiar el
# archivo, truncarlo en el acto y comprimir despues la copia.
#
# Variables (validadas por entrypoint.sh; los defaults los fija el Dockerfile):
#   EDGE_AUDIT_MAX_BYTES        tamano a partir del cual se rota (default 10485760 = 10 MiB)
#   EDGE_AUDIT_KEEP             cuantos .N.gz se conservan (default 5)
#   EDGE_AUDIT_ROTATE_INTERVAL  segundos entre comprobaciones (default 60)
#
# Perdida acotada (documentada en edge/README.md): lo que Coraza escriba entre el `cp` y el truncado
# (milisegundos) y lo que el colector aun no haya leido si estuvo caido. Lo rotado queda en .json.1.gz.
# Los archivos rotados contienen Matched Data (evidencia sensible): se crean con permisos 600.
set -u
MAX="${EDGE_AUDIT_MAX_BYTES:-10485760}"
KEEP="${EDGE_AUDIT_KEEP:-5}"
INTERVAL="${EDGE_AUDIT_ROTATE_INTERVAL:-60}"
AUDIT="${EDGE_LOG_DIR:-/var/log/security}/security-edge-audit.json"

rotate() {
  # Desplaza .N.gz -> .(N+1).gz descartando el que supere KEEP.
  i=$KEEP
  rm -f "${AUDIT}.${i}.gz"
  while [ "$i" -gt 1 ]; do
    prev=$((i - 1))
    [ -f "${AUDIT}.${prev}.gz" ] && mv -f "${AUDIT}.${prev}.gz" "${AUDIT}.${i}.gz"
    i=$prev
  done
  rm -f "${AUDIT}.1"
  # copytruncate: la ventana de perdida es solo entre estas dos lineas.
  cp "$AUDIT" "${AUDIT}.1" || return 1
  : > "$AUDIT"
  chmod 600 "${AUDIT}.1"
  gzip -f "${AUDIT}.1" && chmod 600 "${AUDIT}.1.gz"
  echo "edge-audit-rotate: rotado ${AUDIT} (conserva ${KEEP})" >&2
}

while :; do
  sleep "$INTERVAL"
  [ -f "$AUDIT" ] || continue
  size=$(stat -c %s "$AUDIT" 2>/dev/null || echo 0)
  if [ "$size" -ge "$MAX" ]; then
    rotate || echo "edge-audit-rotate: fallo al rotar" >&2
  fi
done
