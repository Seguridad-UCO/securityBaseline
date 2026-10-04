# Security Baseline local

Esta es la única guía de arranque. Keycloak importa automáticamente el realm, sus clientes y claims; no requiere certificados porque este laboratorio usa HTTP local.

## Descargar y ejecutar

~~~sh
mkdir security-baseline-local && cd security-baseline-local
curl -LO https://github.com/Seguridad-UCO/securityBaseline/releases/download/v0.1.0/compose.yaml
curl -Lo .env https://github.com/Seguridad-UCO/securityBaseline/releases/download/v0.1.0/.env.example
# Cambie solo SECURITY_DOCKER_ORG y SECURITY_VERSION
docker compose up -d
docker compose ps
~~~

Servicios: Keycloak `http://localhost:9090`, PDP `http://localhost:8080` y PEP `http://localhost:8081`.
El usuario local de demostración es `security-demo` / `security-demo`. No incluye ninguna aplicación de ejemplo ni backend de notas.

## Comprobar y actualizar

~~~sh
curl -fsS http://localhost:8080/actuator/health
curl -fsS http://localhost:8081/actuator/health
docker compose pull && docker compose up -d
~~~

Use una versión publicada concreta, no `latest`. Para borrar el laboratorio: `docker compose down -v`.
Lea [MANUAL.md](MANUAL.md) antes de integrar una aplicación.
