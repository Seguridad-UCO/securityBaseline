FROM quay.io/keycloak/keycloak:26.7.2

# Docker inicializa un volumen nombrado vacío copiando el contenido del destino.
# Crear este directorio con el usuario `keycloak` evita que H2 quede sin permisos
# cuando `/opt/keycloak/data/h2` se persiste desde Compose.
RUN mkdir -p /opt/keycloak/data/h2

COPY pdp/keycloak/import/security-baseline-realm.json /opt/keycloak/data/import/security-baseline-realm.json
