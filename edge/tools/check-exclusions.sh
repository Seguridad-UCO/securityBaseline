#!/bin/sh
# E4/E5 (PLAN-HU-027 §7.5): una exclusión <-> una prueba, mismo id; ninguna exclusión global.
# Uso: edge/tools/check-exclusions.sh   (desde la raíz del repo o desde edge/)
set -u
here=$(cd "$(dirname "$0")/.." && pwd)
rules="$here/rules/exclusions"
tests="$here/tests/exclusions"
status=0

for conf in "$rules"/*.conf; do
  [ -e "$conf" ] || continue
  base=$(basename "$conf" .conf)
  id=${base%%-*}
  case "$id" in
    9[0-9][0-9][0-9][0-9][0-9]) ;;
    *) echo "FALLA: $conf — el id debe estar en 900000-999999 y el archivo llamarse <id>-<slug>.conf"; status=1 ;;
  esac
  [ -e "$tests/$base.hurl" ] || { echo "FALLA: $conf sin prueba gemela tests/exclusions/$base.hurl"; status=1; }
  if grep -Eq '^[[:space:]]*SecRuleRemoveBy(Id|Tag|Msg)' "$conf"; then
    echo "FALLA: $conf desactiva reglas globalmente (SecRuleRemoveBy*); acótala a una ruta con SecRule REQUEST_URI + ctl:"; status=1
  fi
  if grep -Eq 'tx\.(inbound_anomaly_score_threshold|outbound_anomaly_score_threshold|blocking_paranoia_level|detection_paranoia_level)' "$conf"; then
    echo "FALLA: $conf toca umbral o paranoia; eso no es una exclusión"; status=1
  fi
  if ! grep -Eq 'SecRule[[:space:]]+REQUEST_URI' "$conf"; then
    echo "FALLA: $conf no está acotado a una ruta (falta SecRule REQUEST_URI ...)"; status=1
  fi
  if ! grep -Eq "id:$id[,\"']" "$conf"; then
    echo "FALLA: $conf no declara id:$id (el id del archivo y el de la regla deben coincidir)"; status=1
  fi
done

for hurl in "$tests"/*.hurl; do
  [ -e "$hurl" ] || continue
  base=$(basename "$hurl" .hurl)
  [ -e "$rules/$base.conf" ] || { echo "FALLA: $hurl sin exclusión gemela rules/exclusions/$base.conf"; status=1; }
done

[ $status -eq 0 ] && echo "OK: exclusiones y pruebas emparejadas"
exit $status
