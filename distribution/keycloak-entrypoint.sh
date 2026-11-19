#!/usr/bin/env bash
set -euo pipefail

# Keycloak importa un realm solo si aún no existe. Las instalaciones que conservan el volumen H2 de
# una versión anterior, por tanto, no reciben automáticamente nuevos clientes del JSON de import.
# Esta migración es idempotente y conserva el resto del realm tal como está.
readonly keycloak_pid_file=/tmp/security-baseline-keycloak.pid
readonly migration_marker=/tmp/security-baseline-keycloak-migration-complete
readonly admin_cli_config=/tmp/security-baseline-kcadm.config
readonly realm=security-baseline
readonly provisioner_client=security-pdp-provisioner
readonly provisioner_secret="${SECURITY_BASELINE_PROVISIONER_CLIENT_SECRET:-change-me-in-production}"

if [[ "${1:-}" != "start" && "${1:-}" != "start-dev" ]]; then
  exec /opt/keycloak/bin/kc.sh "$@"
fi

keycloak_pid=''
cleanup() {
  if [[ -n "$keycloak_pid" ]]; then
    kill "$keycloak_pid" 2>/dev/null || true
  fi
}
trap cleanup EXIT INT TERM

rm -f "$migration_marker" "$admin_cli_config"
/opt/keycloak/bin/kc.sh "$@" &
keycloak_pid=$!
printf '%s' "$keycloak_pid" > "$keycloak_pid_file"

for _ in $(seq 1 120); do
  if ! kill -0 "$keycloak_pid" 2>/dev/null; then
    wait "$keycloak_pid"
    exit $?
  fi
  if bash -c '</dev/tcp/127.0.0.1/8080' 2>/dev/null; then
    break
  fi
  sleep 1
done

if ! bash -c '</dev/tcp/127.0.0.1/8080' 2>/dev/null; then
  echo 'Keycloak no abrió el puerto de administración a tiempo.' >&2
  exit 1
fi

for _ in $(seq 1 60); do
  if /opt/keycloak/bin/kcadm.sh config credentials --config "$admin_cli_config" \
      --server http://127.0.0.1:8080 --realm master \
      --user "${KC_BOOTSTRAP_ADMIN_USERNAME:?KC_BOOTSTRAP_ADMIN_USERNAME es requerido}" \
      --password "${KC_BOOTSTRAP_ADMIN_PASSWORD:?KC_BOOTSTRAP_ADMIN_PASSWORD es requerido}" >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

if [[ ! -s "$admin_cli_config" ]]; then
  echo 'No fue posible autenticarse como administrador para migrar el realm de Keycloak.' >&2
  exit 1
fi

client_id=$(/opt/keycloak/bin/kcadm.sh get clients --config "$admin_cli_config" -r "$realm" \
  -q "clientId=$provisioner_client" --fields id --format csv --noquotes | tail -n 1)

if [[ -z "$client_id" ]]; then
  /opt/keycloak/bin/kcadm.sh create clients --config "$admin_cli_config" -r "$realm" \
    -s "clientId=$provisioner_client" -s enabled=true -s protocol=openid-connect \
    -s publicClient=false -s serviceAccountsEnabled=true -s standardFlowEnabled=false \
    -s directAccessGrantsEnabled=false -s "secret=$provisioner_secret" \
    -s 'defaultClientScopes=["security-baseline-claims"]' >/dev/null
  echo "Migración: cliente $provisioner_client creado."
else
  /opt/keycloak/bin/kcadm.sh update "clients/$client_id" --config "$admin_cli_config" -r "$realm" \
    -s enabled=true -s publicClient=false -s serviceAccountsEnabled=true -s standardFlowEnabled=false \
    -s directAccessGrantsEnabled=false >/dev/null
  echo "Migración: cliente $provisioner_client ya existía; configuración verificada."
fi

/opt/keycloak/bin/kcadm.sh add-roles --config "$admin_cli_config" -r "$realm" \
  --uusername "service-account-$provisioner_client" --cclientid realm-management \
  --rolename manage-clients >/dev/null

touch "$migration_marker"
rm -f "$admin_cli_config" "$keycloak_pid_file"
wait "$keycloak_pid"
