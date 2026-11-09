# PEP de la Plataforma Central de Seguridad

> Panorama de los tres componentes (PDP, PEP, OPA) y cómo encajan hoy:
> [`../docs/PLATAFORMA.md`](../docs/PLATAFORMA.md).

Proxy inverso independiente: valida Bearer JWT, construye SolicitudAcceso, consulta el PDP y reenvía
únicamente ante un ALLOW válido. El PEP no evalúa roles, políticas Rego ni catálogos.

El PDP existente vive en [`../pdp/`](../pdp/). Este proyecto tiene su propio POM, JAR,
proceso, configuración, imagen y pipeline. No depende del artefacto ni de shared/security del PDP.

**Estado de integración:** el cliente HTTP y el enforcement están implementados; el PDP actual todavía
no implementa [el contrato v1](../contracts/pep-pdp/v1/README.md). La demostración utiliza servidores
externos de prueba. No equivale a autorización OPA ni incluye auditoría central.

## Compilar y verificar

Requisitos: Java 25, wrapper Maven del repositorio, OpenSSL para la prueba mTLS.
Desde la raíz:

```sh
./mvnw -f pep/pom.xml verify
```

Los tests HTTP levantan puertos efímeros sin Docker. Los tests de arquitectura verifican Modulith,
dependencias hacia el dominio y ausencia de clases del PDP en el PEP.

## Demostración con procesos separados

Preparación:

```sh
./mvnw -f pep/pom.xml verify dependency:copy-dependencies -DincludeScope=test
```

Terminal 1, simulador de PDP/JWKS en 18080 y aplicación de prueba en 18081:

```sh
java -cp 'pep/target/test-classes:pep/target/classes:pep/target/dependency/*' co.edu.uco.seguridad.pep.fixture.FixtureServers
```

Terminal 2, PEP en 8081:

```sh
java -jar pep/target/security-pep-0.1.0-SNAPSHOT.jar --spring.config.additional-location=file:./pep/config/local.properties
```

Terminal 3:

```sh
PEP_TEST_TOKEN=$(curl -fsS http://127.0.0.1:18080/token)
curl -i -H "Authorization: Bearer $PEP_TEST_TOKEN" http://localhost:8081/apps/demo/notes/77
curl -fsS http://127.0.0.1:18080/__fixture/mode/DENY
curl -i -H "Authorization: Bearer $PEP_TEST_TOKEN" http://localhost:8081/apps/demo/notes/77
curl -fsS http://127.0.0.1:18080/__fixture/mode/ALLOW
unset PEP_TEST_TOKEN
```

El primer acceso devuelve 200 del destino; el segundo, 403 del PEP. El token es efímero, firmado con una
clave generada por el simulador al arrancar. GET /token y /__fixture/* existen únicamente en el servidor
de pruebas. No se empaquetan en el JAR productivo.

Alternativa Docker:

```sh
docker compose -f pep/compose.integration.yml up --build
```

Esta composición es independiente de [`pdp/docker-compose.yml`](../pdp/docker-compose.yml). Publica PEP 8081 y control
de fixtures
18080 solo en loopback; la aplicación 18081 queda únicamente en la red interna.
Detener con docker compose -f pep/compose.integration.yml down; no requiere borrar volúmenes del PDP.

## Integrar una aplicación real

1. Registrar la aplicación y sus recursos en el catálogo del PDP.
2. Configurar un prefijo público único, application-id, environment, target y audiencias JWT permitidas.
   Las URLs target son orígenes HTTP(S) sin path, usuario, query o fragmento. No se resuelven desde la petición.
3. La aplicación obtiene tokens mediante su IdP/OIDC. El PEP valida RS256, firma, issuer, exp, nbf y sub;
   exige una audiencia permitida para la ruta. No requiere un tenant en el token ni confía en roles del cliente.
4. El cliente utiliza la URL del PEP. /apps/academic/notes/77 se autoriza y reenvía como /notes/77.
5. Restringir el acceso al backend a la red del PEP. Un puerto alternativo público permite saltarse el proxy.
6. Conectar el PDP que implemente v1, con mTLS y la cadena JWT propia de su endpoint interno.

Usar config/production.properties.example como plantilla externa. El arranque exige issuer/JWKS/rutas
y certificados mTLS; no hay credenciales o rutas permisivas por defecto. HTTP requiere habilitación
explícita de desarrollo en ingress y PDP. Servir el PEP por TLS directamente con server.ssl.* o detrás de
un ingress TLS con listener privado y política de red. No hay trust-all ni seguimiento de redirecciones.

El panel/BFF y las sesiones HttpOnly actuales del PDP no se trasladan al PEP. El tráfico protegido usa
Bearer; CORS admite únicamente los orígenes configurados y no habilita credenciales automáticas del navegador.

## Contrato y límites funcionales

- ALLOW válido → proxy; DENY → 403; TOKEN_INVALID → 401; INDETERMINATE/fallo PDP → 503.
- PDP HTTP distinto de 200 (salvo 401), JSON inválido, campos duplicados, IDs incorrectos o respuesta incompleta → 503.
- Una obligación no soportada nunca se ignora para permitir acceso. No se almacena ni reutiliza una decisión.
- Fallo de conexión del backend → 502; timeout → 504. Las respuestas HTTP del backend se conservan.
- Solo HTTP finito: GET, POST, PUT, PATCH, DELETE, HEAD y OPTIONS. Upgrade, CONNECT y SSE no se soportan.
- Rutas ASCII conservadoras: se rechazan %, barras dobles, backslash, segmentos . y .. y parámetros con ;.
  Query se conserva como bytes de URI, incluidos parámetros repetidos, pero no se utiliza para autorizar.
- Cuerpo y query no forman parte del contexto de política v1. No integrar una operación cuya autorización
  dependa de IDs o atributos allí presentes sin extender primero el contrato/contexto del PDP.
- JWT/JWKS se cachea según el proveedor de claves de Spring; esto no es caché de autorización ni revocación.
- No hay login, refresh, sesiones, SDK, OPA, DB ni cliente de auditoría en el PEP.

### Headers y streaming

El transporte conserva cuerpos binarios y no los agrega completos en memoria. Reenvía headers de
contenido, Accept, condicionales HTTP, Range, Cache-Control, Pragma, User-Agent, Origin e Idempotency-Key.
No reenvía cabeceras arbitrarias de identidad o negocio. Ampliar esta lista requiere revisar explícitamente
su semántica. Las cabeceras hop-by-hop y las nombradas por Connection siempre se eliminan.

Authorization, Cookie y Set-Cookie se eliminan por defecto. forward-bearer y forward-cookies por ruta
permiten su propagación explícita hacia un backend confiable; no crean autenticación por cookies en el PEP.
X-Request-Id, X-Correlation-Id y X-Decision-Id se establecen en el PEP; Forwarded se descarta y
X-Forwarded-For/Proto se reconstruyen usando el peer/listener reales. No se confía en headers del proxy
anterior: detrás de otro ingress, el límite IP agrupa por ese peer, no por usuarios finales.

Content-Length mayor al límite se rechaza antes de consultar el PDP. En cuerpos chunked, el límite se
comprueba mientras se transmiten bytes, después de ALLOW; al excederlo se cancela la transferencia.
El backend podría haber recibido un prefijo del cuerpo y debe evitar confirmar operaciones incompletas.
Si un error ocurre después de enviar headers de respuesta al cliente, se aborta la conexión:
HTTP ya no permite sustituir ese estado por 502/504. No se reintenta una operación de negocio.

## Configuración operativa inicial

| Propiedad                               | Default                    |
|-----------------------------------------|----------------------------|
| server.port                             | 8081                       |
| pep.pdp.connect-timeout / timeout       | 1s / 3s                    |
| pep.proxy.connect-timeout / timeout     | 2s / 30s totales           |
| server.max-http-request-header-size     | 16KB                       |
| pep.ingress.max-body-bytes              | 10485760                   |
| pep.pdp.max-response-bytes              | 65536                      |
| pep.ingress.max-concurrent              | 200                        |
| pep.ingress.requests-per-second / burst | 100 / 200 por IP inmediata |
| pep.ingress.max-rate-keys               | 10000; inactividad 60s     |
| pep.ingress.jwks-timeout                | 3s                         |

El rate limit es local por réplica, sin Redis; no es una cuota global. Saturación produce 503; exceso de
tasa, 429 con Retry-After. Son defaults configurables, no SLOs de latencia o capacidad demostrados.
Las conexiones y consultas se cancelan al desconectarse el cliente. Ningún camino productivo usa block().

GET /actuator/health/liveness refleja el proceso; /actuator/health/readiness incorpora conectividad del PDP.
No usar readiness para reiniciar el contenedor. La salud de OPA/DB debe reflejarla el PDP real.
No se exponen métricas públicamente: MeterRegistry registra pep.http.duration, pep.pdp.duration y
pep.pdp.decisions; exportarlas requiere configurar el backend de observabilidad del despliegue.

Logs: IDs y resultado, sin tokens, cookies, payload o query. Los logs HTTP de bibliotecas que pueden
incluir URLs completas están deshabilitados; el PEP registra fallos sanitizados. Los IDs no se usan como
etiquetas de métricas. Estos logs no sustituyen la evidencia durable de auditoría que deberá emitir el PDP.

## Evolución a SDK

NormalizeAccessUseCase y EnforceAccessUseCase son operaciones públicas separadas del transporte. Sus DTOs,
AccessRequest y AccessDecision no dependen de WebFlux. Un futuro starter podrá reutilizar o extraer
esas capacidades y aplicar ALLOW continuando una cadena de filtros local. Cada lenguaje requiere su
adaptador. Importar una librería no registra recursos, configura el IdP ni elimina la necesidad de
proteger rutas y validar datos de negocio.

Ver [ADR del PEP](../pdp/docs/architecture/adr-pep-v1.md) para el mapa de dependencias y decisiones.
La [evidencia de validación](VERIFICATION.md) registra resultados y límites de la comprobación local.

## Alta automática de aplicaciones WebFlux

El starter local `co.edu.uco:security-pep-integration-spring-boot-starter:0.1.0-SNAPSHOT` registra la
integración técnica ante un PEP habilitado. Compilarlo e instalarlo con:

```sh
./mvnw -f pep/starter/pom.xml install
```

La aplicación integrada añade la dependencia y configura, mediante secretos de despliegue,
`security.pep.registration.enabled=true`, `pep-url`, `application-id`, `environment`, `backend-url`,
`audience` y `token`. El starter no instala filtros ni impide el arranque: registra `backend-url` en
segundo plano y reintenta fallos transitorios. El PEP genera `/apps/{application-id}` bajo su URL pública
y es la única entrada pública hacia el backend.

```xml
<dependency>
  <groupId>co.edu.uco</groupId>
  <artifactId>security-pep-integration-spring-boot-starter</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

```properties
security.pep.registration.enabled=true
security.pep.registration.pep-url=https://security.example.org
security.pep.registration.application-id=academic
security.pep.registration.environment=prod
security.pep.registration.backend-url=https://academic.internal
security.pep.registration.audience=academic-api
security.pep.registration.token=${PEP_REGISTRATION_TOKEN}
```

El administrador del PEP habilita `pep.integration.enabled`, asigna `registry-file` a un volumen privado,
configura `public-base-url` y entrega un token opaco por aplicación y entorno. Solo se guarda el hash
BCrypt del token. El archivo de rutas permite una réplica PEP en esta versión; no hay sincronización
distribuida ni alta de recursos, roles, perfiles, tenants u OPA. Esos elementos permanecen bajo
administración manual del sistema central.

La guía completa de preparación del PEP y consumo desde otra aplicación está en
[starter/README.md](starter/README.md). La aplicación de consumo reproducible está en el directorio hermano
`../pep-webflux-sample`; su README incluye los comandos de prueba. Para completar el extremo remoto sin
modificar todavía el PDP, consultar [la guía PEP–PDP v1](../contracts/pep-pdp/v1/PDP-INTEGRATION-GUIDE.md).

## Guías detalladas

- [Arquitectura y flujo completo del PEP](docs/GUIA-PEP-ARQUITECTURA-Y-FLUJO.md)
- [Funcionamiento interno del starter WebFlux](docs/GUIA-STARTER-INTERNA.md)
- [Implementación del starter para equipos consumidores](docs/GUIA-STARTER-PARA-EQUIPOS.md)
