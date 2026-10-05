# Security Baseline local

Esta es la única guía de arranque. Keycloak importa automáticamente el realm, sus clientes y claims; no requiere certificados porque este laboratorio usa HTTP local.

## Descargar y ejecutar

~~~sh
mkdir security-baseline-local && cd security-baseline-local
curl -LO https://github.com/Seguridad-UCO/securityBaseline/releases/download/v0.1.5/compose.yaml
curl -Lo .env https://github.com/Seguridad-UCO/securityBaseline/releases/download/v0.1.5/.env.example
# En .env configure SECURITY_DOCKER_ORG y SECURITY_VERSION con la versión descargada.
docker compose up -d
docker compose ps
~~~

Servicios: Keycloak `http://localhost:19090`, PDP/BFF `http://localhost:18080` y PEP `http://localhost:18081`. Estos puertos se reservan para el baseline y evitan conflictos con las aplicaciones consumidoras, que suelen usar `8080`.
El usuario local de demostración es `security-demo` / `security-demo`. No incluye ninguna aplicación de ejemplo ni backend de notas.

MFA queda desactivado por defecto solo en este laboratorio local, para no bloquear las pruebas de administración. No se requiere configurar OTP, Keycloak ni claims. Si se quiere probar MFA, cambie `PDP_SECURITY_MFA_ENABLED=true` en `.env` y configure OTP/WebAuthn antes de reiniciar el stack.

## Comprobar y actualizar

~~~sh
curl -fsS http://localhost:18080/actuator/health
curl -fsS http://localhost:18081/actuator/health
docker compose pull && docker compose up -d
~~~

Use una versión publicada concreta, no `latest`. Para borrar el laboratorio: `docker compose down -v`.
Lea [MANUAL.md](MANUAL.md) antes de integrar una aplicación.
