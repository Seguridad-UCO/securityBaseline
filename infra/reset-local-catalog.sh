#!/usr/bin/env bash
set -euo pipefail

# Solo desarrollo local. El nuevo modelo endpoint(path+method) no puede inferirse del catálogo anterior.
curl --fail --silent --show-error \
  -u root:root \
  -H 'surreal-ns: pdp' -H 'surreal-db: pdp' -H 'Content-Type: text/plain' \
  --data-binary $'DELETE protected_resource; DELETE application; REMOVE INDEX protected_resource_grant ON protected_resource; DEFINE INDEX protected_resource_endpoint ON protected_resource COLUMNS applicationId, path, method UNIQUE;' \
  http://localhost:8000/sql
echo 'Catálogo local de aplicaciones y endpoints reiniciado. Los tenants se conservaron.'
