#!/bin/sh
# Prepara, en el Keycloak del stack de pruebas del edge (NO el realm real), lo que una aplicacion host necesita para
# el canal Bearer (ADR-031): un cliente publico con audience del PDP y un segundo usuario sin rol administrativo.
# Idempotente. Uso: docker exec -i edge-keycloak sh < e2e/provision-keycloak.sh
set -e
K=/opt/keycloak/bin/kcadm.sh
$K config credentials --server http://localhost:8080 --realm master --user admin --password admin >/dev/null
R=security-baseline
if [ -n "$($K get clients -r $R -q clientId=e2e-host --fields id --format csv --noquotes)" ]; then
  CID0=$($K get clients -r $R -q clientId=e2e-host --fields id --format csv --noquotes)
  $K delete clients/$CID0 -r $R >/dev/null; echo "cliente e2e-host recreado"
fi
if [ -z "$($K get clients -r $R -q clientId=e2e-host --fields id --format csv --noquotes)" ]; then
  $K create clients -r $R -s clientId=e2e-host -s publicClient=true -s directAccessGrantsEnabled=true \
    -s standardFlowEnabled=false -s 'redirectUris=["http://localhost:5190/*"]' -s 'webOrigins=["http://localhost:5190"]' >/dev/null
  CID=$($K get clients -r $R -q clientId=e2e-host --fields id --format csv --noquotes)
  $K create clients/$CID/protocol-mappers/models -r $R -s name=pdp-audience -s protocol=openid-connect \
    -s protocolMapper=oidc-audience-mapper \
    -s 'config."included.custom.audience"=security-baseline-pdp' -s 'config."access.token.claim"=true' \
    -s 'config."id.token.claim"=false' >/dev/null
  # El PDP toma el tenant del claim `tenant` del token (PdpPrincipal.from). En produccion sale de un atributo del usuario
  # (oidc-usermodel-attribute-mapper); en el laboratorio se fija con un claim constante.
  $K create clients/$CID/protocol-mappers/models -r $R -s name=tenant -s protocol=openid-connect     -s protocolMapper=oidc-hardcoded-claim-mapper     -s 'config."claim.name"=tenant' -s 'config."claim.value"=universidad-uco' -s 'config."jsonType.label"=String'     -s 'config."access.token.claim"=true' -s 'config."id.token.claim"=false' >/dev/null
  echo "cliente e2e-host creado"
else echo "cliente e2e-host ya existe"; fi
if [ -z "$($K get users -r $R -q username=e2e-nonadmin --fields id --format csv --noquotes)" ]; then
  $K create users -r $R -s username=e2e-nonadmin -s enabled=true -s emailVerified=true \
    -s email=e2e-nonadmin@ci.invalid -s firstName=E2E -s lastName=NoAdmin >/dev/null
  $K set-password -r $R --username e2e-nonadmin --new-password 'E2eNoAdmin#2026' >/dev/null
  echo "usuario e2e-nonadmin creado"
else echo "usuario e2e-nonadmin ya existe"; fi
