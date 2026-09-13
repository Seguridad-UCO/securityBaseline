# Guía interna del starter de integración PEP para Spring Boot WebFlux

## Objetivo y frontera

El artefacto `co.edu.uco:security-pep-integration-spring-boot-starter` da de alta técnicamente una aplicación
Spring Boot WebFlux ante un PEP remoto al iniciar. No instala filtros, anotaciones, un Resource Server ni
interceptores dentro del backend. El PEP externo conserva la publicación y la protección de la ruta.

Esta frontera es deliberada: el starter no puede conocer políticas, IdP, roles o modelo de negocio de todas las
aplicaciones. El backend debe aceptar tráfico solo desde la red del PEP; de lo contrario, un consumidor que lo
llame directamente evita el punto de enforcement.

## Piezas del JAR

| Pieza | Responsabilidad |
|---|---|
| `META-INF/spring/...AutoConfiguration.imports` | Permite el descubrimiento automático de Spring Boot. |
| `PepRegistrationAutoConfiguration` | Crea el `ApplicationRunner` de alta. |
| `PepRegistrationProperties` | Hace binding y valida `security.pep.registration.*`, incluida la credencial del PDP. |

La auto-configuración crea el runner solamente con estas dos condiciones:

```properties
security.enabled=true
security.pep.registration.enabled=true
```

`security.enabled` vale `true` cuando se omite. Con `security.enabled=false` no se crea
`pepRegistrationRunner`, no hay llamada al PEP y tampoco hay reintentos. No controla los controladores ni el
puerto de la aplicación.

## Secuencia de inicio

```mermaid
sequenceDiagram
    participant SB as Spring Boot
    participant AC as Auto-configuración
    participant R as ApplicationRunner
    participant P as PEP
    SB->>AC: descubre el starter en el classpath
    AC->>AC: evalúa ambas propiedades enabled
    AC->>R: crea runner si ambas son true
    SB->>R: ejecuta al terminar el arranque
    R->>R: valida la configuración
    R->>P: PUT /internal/v1/integrations/{app}/{environment}
    alt 2xx
        P-->>R: prefijo y URL pública
        R->>R: log PEP integration active
    else configuración inválida o 4xx
        R->>R: log sanitizado; no reintenta
    else red o 5xx
        R->>R: retry backoff 1 a 60 segundos
    end
```

El runner no bloquea ni impide arrancar la aplicación. Un 4xx se considera una configuración o credencial que
debe corregirse y reiniciarse. Para red o 5xx se mantiene un reintento indefinido, con backoff exponencial entre
uno y sesenta segundos.

## Protocolo de alta

La llamada construida por el starter es:

```text
PUT {pep-url}/internal/v1/integrations/{application-id}/{environment}
Authorization: Bearer {token-opaco}
Content-Type: application/json

{"backendUrl":"https://backend.internal","audience":"backend-api"}
```

Ese token es la credencial de integración emitida por el PDP, no un JWT de usuario. El PEP la valida mediante
`POST /internal/v1/applications/{application-id}/credential-validations` sobre su canal mTLS con el PDP; para ese
canal el PEP usa su propia evidencia JWT de servicio (`pep.integration.pdp-evidence-token`). El PEP no persiste
el secreto ni su hash. Al aceptarla, valida la URL de backend, asigna el prefijo `/apps/{application-id}`,
persiste la ruta y devuelve su URL pública.

Una alta posterior de la misma aplicación y entorno actualiza backend/audiencia. No permite registrar una pareja
distinta ni ocupar prefijos que se solapen. `backendUrl` es un origen sin credenciales, path, query ni fragmento.

## Configuración del starter

| Propiedad | Obligatoria | Efecto |
|---|---|---|
| `security.enabled` | No; default true | Interruptor global del starter. |
| `security.pep.registration.enabled` | Sí | Activa el runner. |
| `pep-url` | Sí | Origen HTTPS del PEP; HTTP solo con permiso explícito. |
| `application-id` | Sí | Identificador estable de la aplicación. |
| `environment` | Sí | Entorno: `dev`, `test`, `prod`, etc. |
| `backend-url` | Sí | Origen privado sin path/query/fragment/credenciales. |
| `audience` | Sí | Audiencia JWT requerida por la ruta PEP. |
| `token` | Sí | Credencial de aplicación emitida por el PDP y conservada en un secreto. |
| `allow-insecure-http` | No; default false | Permite HTTP al PEP solo en desarrollo local. |

La validación se hace dentro del runner para no impedir el inicio por un error de properties. Si falla, queda un
log sanitizado y no se realiza el registro. En producción `pep-url` debe usar HTTPS y el token no debe figurar en
repositorio, argumentos de proceso ni logs.

## Configuración correspondiente en el PEP

El administrador habilita y prepara la recepción de altas. La conexión PEP→PDP sigue usando
`pep.pdp.*` (URL, CA, certificado y llave mTLS); además, debe proporcionar una evidencia JWT de servicio válida
para la audiencia interna que configura el PDP. No se configura ningún hash de credencial en el PEP:

```properties
pep.integration.enabled=true
pep.integration.registry-file=/var/lib/security-pep/integrations.json
pep.integration.public-base-url=https://security.example.org
pep.integration.pdp-evidence-token=${PEP_INTEGRATION_PDP_EVIDENCE_TOKEN}
```

El archivo se guarda en un volumen privado; contiene rutas, nunca secretos. La credencial de aplicación en claro
se entrega una vez al equipo consumidor al crear su aplicación en el PDP. La versión actual guarda rutas
localmente: una topología de múltiples réplicas necesita almacenamiento o sincronización compartida, que el
starter no implementa.

## Qué significa un alta exitosa

`PEP integration active` significa que el PEP conoce el origen y la audiencia de la aplicación. No significa que
exista una política PDP, que OPA esté disponible ni que se haya autorizado alguna solicitud. El cliente final usa
la URL PEP (`/apps/{application-id}/...`); el PEP elimina el prefijo, consulta al PDP y recién después reenvía
al backend. Si el PDP no está disponible, la URL pública devuelve 503 sin alcanzar la aplicación.

El detalle de ese flujo está en
[GUIA-PEP-ARQUITECTURA-Y-FLUJO.md](GUIA-PEP-ARQUITECTURA-Y-FLUJO.md).
