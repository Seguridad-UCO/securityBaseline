FROM quay.io/keycloak/keycloak:26.7.2
COPY pdp/keycloak/import/security-baseline-realm.json /opt/keycloak/data/import/security-baseline-realm.json
