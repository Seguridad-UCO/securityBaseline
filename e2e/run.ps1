# Ejecuta el E2E Bearer contra el stack de pruebas del edge (docker compose -p edge ...). Ver README.md.
Set-Location (Split-Path -Parent $PSScriptRoot)
Get-Content e2e/provision-keycloak.sh -Raw | docker exec -i edge-keycloak sh
$rid = "e2e$((Get-Random -Maximum 99999))"
$common = @('run','--rm','--network','edge_default','-v',"${PWD}/e2e:/e2e:ro",'-v',"${PWD}/edge/tests:/tests:ro",'-w','/e2e',
  'ghcr.io/orange-opensource/hurl:7.1.0','--test','--jobs','1','--variables-file','/tests/hurl.env.compose')
docker @common --variable login_user=e2e-nonadmin --variable 'login_password=E2eNoAdmin#2026' first-login.hurl
docker @common --variable run_id=$rid --variable 'other_password=E2eNoAdmin#2026' host-bearer.hurl
