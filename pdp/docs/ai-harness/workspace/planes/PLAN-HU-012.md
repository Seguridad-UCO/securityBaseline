# PLAN: HU-012 — El PDP emite la credencial de una aplicación al registrarla

## Metadata

- **ID:** HU-012
- **Slice:** `applications` (existente), con `[M]` en `shared/port` y `shared/config`
- **Tipo:** Escritura (extiende un caso de uso existente)
- **Fecha:** 2026-09-13
- **Rama sugerida:** `feature/HU-012-credencial-de-aplicacion`
- **Fuentes:**
  - `pdp/docs/ai-harness/workspace/HU-012.md` (historia dictada por Sebastián)
  - Código real leído antes de planificar: `Application.java`, `RegisterApplicationUseCaseImpl.java`,
    `ApplicationRepository.java`, `ApplicationEntity.java`, `SurrealApplicationRepository.java`,
    `ApplicationController.java`, `ApplicationResponseMapper.java`, `SharedPortsConfiguration.java`
    (patrón de `TimeProvider`/`IdentifierGenerator` a espejar), `TenantId.java` +
    `InvalidTenantIdException.java` (patrón exacto de VO + excepción a espejar)
  - `pep/src/main/java/.../RouteRegistry.java:8,34` — confirma que el PEP ya usa
    `BCryptPasswordEncoder` (de `spring-security-crypto`, ya transitiva vía
    `spring-boot-starter-security` en el POM del PDP) para no persistir un secreto en claro; se
    reutiliza el mismo algoritmo para no introducir un segundo estándar de hashing en la plataforma
  - Dos decisiones confirmadas por Sebastián el 2026-09-13 (ver §11): el secreto es **uno por
    aplicación** (sin dimensión de ambiente — se descartó explícitamente ampliar `Application` con
    esa dimensión), y se guarda **solo su hash**, nunca en claro
- **Criterios de la línea base que toca:** 1, 2, 4, 8, 9, 11, 12, 13, 14, 15, 21, 22

## 0. Hallazgos antes de planificar

### Hallazgo 1 — no hace falta ninguna `Rule` nueva

Generar y guardar un secreto no es una restricción que pueda rechazar nada: no hay unicidad que
verificar (cada secreto es aleatorio) ni estado de otro agregado que consultar. Las reglas que ya
rechazan el registro (`ApplicationNameMustNotBeReservedRule`, `TenantMustBeActiveValidator`,
`ApplicationNameMustBeUniqueForTenantRule`) no cambian. Por eso la sección 3 de este plan no declara
ninguna regla — sería inventar una restricción que nadie pidió.

### Hallazgo 2 — el hash y el secreto en claro nunca conviven en el mismo DTO de lectura

`RegisteredApplicationResponse`/`ApplicationWebResponse` ya sirven **dos** casos de uso hoy
(`RegisterApplicationUseCase` y `ListApplicationsUseCase`), y `ListApplicationsUseCase` nunca debe
poder exponer un secreto — ni en claro (no lo tiene) ni su hash (no debe salir jamás por HTTP). Si el
campo `credential` se añadiera a `RegisteredApplicationResponse`, `ListApplicationsUseCaseImpl`
tendría que inventarle un valor a un campo que no existe para esa consulta. Por eso el secreto en
claro vive en un DTO **nuevo y exclusivo** del registro (`ApplicationRegistrationResponse`), que
envuelve la respuesta ya existente sin tocarla: `ListApplicationsUseCase`, `ApplicationWebResponse` y
el endpoint `GET /api/v1/applications` quedan completamente intactos.

### Hallazgo 3 — capacidades transversales nuevas, mismo patrón que `TimeProvider`/`IdentifierGenerator`

Ni generar un secreto de alta entropía ni hashearlo son decisiones de negocio: son capacidades
técnicas, igual que "dame la hora" o "dame un identificador nuevo". Por eso `SecretGenerator` y
`CredentialHasher` van en `shared/port/`, se cablean una sola vez en `SharedPortsConfiguration` (ya
existente) y cualquier caso de uso los recibe por constructor — nunca `new SecureRandom()` ni
`new BCryptPasswordEncoder()` en línea dentro de `application`.

### Hallazgo 4 — el agregado `Application` cambia de firma, pero su identidad no

Añadir `credentialHash` a `Application` es un `[M]`: cambia el `record` y su factoría `register(...)`,
con cuatro consumidores existentes en `pdp/src/main` (`ApplicationPersistenceMapper`,
`RegisterApplicationUseCaseImpl`) y cuatro en `pdp/src/test` (`ApplicationCriteriaTests`,
`ListApplicationsUseCaseImplTests`, `SurrealRepositoryIntegrationTests`, y
`RegisterProtectedResourceUseCaseImplTests` — este último en el slice `resources`, que construye una
`Application` de prueba). Ninguno cambia su significado: `ApplicationId`, `TenantId` y `name` siguen
siendo la identidad; `credentialHash` es un atributo más, igual que `description` o `baseUrl`.

## 1. Resumen funcional

Al registrar una aplicación (`POST /api/v1/applications`), el PDP genera un secreto aleatorio de alta
entropía, lo hashea (mismo algoritmo que ya usa el PEP) y persiste **solo el hash** junto al resto de
la aplicación. La respuesta de ese único registro incluye el secreto **en texto plano, una sola
vez** — ninguna consulta posterior (`GET /api/v1/applications`, ni ninguna futura) vuelve a
exponerlo, en claro o hasheado.

**No cubre:** validar la credencial (HU-013), rotarla (HU-014), ni separarla por ambiente — se
descartó explícitamente ampliar `Application` con esa dimensión (ver §11).

## 2. Criterios de aceptación

| # | Criterio | Resultado esperado |
|---|---|---|
| 1 | Se genera un secreto al registrar | `POST /api/v1/applications` responde 201 con un campo `credential` en texto plano |
| 2 | El secreto no se persiste en claro | `Application.credentialHash()` guarda el resultado de `CredentialHasher.hash(secreto)`, nunca el secreto mismo |
| 3 | El secreto no se puede recuperar después | `GET /api/v1/applications` (`ApplicationWebResponse`) no tiene ningún campo de credencial — ni en claro ni el hash |
| 4 | Nunca en logs | `RegisterApplicationUseCaseImpl` no pasa el secreto en claro a `ReactiveLogContext` ni a ningún `LOG.*`; solo se relaya vía el DTO de respuesta |
| 5 | Camino feliz existente intacto | `ListApplicationsUseCase`, `ApplicationWebResponse` y las reglas de registro (nombre reservado/único, tenant activo) siguen sin cambios de comportamiento |

## 3. Reglas de negocio

Ninguna regla nueva — ver Hallazgo 1. Las invariantes de esta historia son locales al VO nuevo
(`ApplicationCredentialHash`: no nulo, no vacío tras `trim()`), no restricciones de conjunto.

## 4. Modelo de dominio afectado

### Entidad / agregado

- **`Application`** (existente, `[M]`): gana el componente `credentialHash` (tipo
  `ApplicationCredentialHash`). Su factoría `register(...)` gana el mismo parámetro. No cambia su
  identidad (`ApplicationId`) ni ningún otro campo.

### Value objects

| VO | Nuevo o existente | Invariantes | Vive en |
|---|---|---|---|
| `ApplicationCredentialHash` | Nuevo | No nulo; no vacío tras `trim()` — mismo patrón mínimo que `TenantId`, sin restricción de formato porque el valor nunca lo escribe una persona | `pdp/applications/domain/model/` (un solo consumidor) |

## 5. Persistencia

- **Tabla:** `application` (existente, `ApplicationSchema.TABLE`, `SCHEMALESS` — no hace falta tocar
  `SurrealApplicationSchemaInitializer` ni su índice)
- **Campos:** se añade `credentialHash` (string) a la fila existente
- **Consultas nuevas en el puerto:** ninguna — `save(Application)` ya existe; solo cambia qué
  columnas escribe, porque `Application` ganó un campo
- **Inicializador de esquema:** existente, sin cambios (tabla `SCHEMALESS`)

## 6. Endpoint

| Verbo | Ruta | Código de éxito | Cuerpo de entrada | Cuerpo de salida |
|---|---|---|---|---|
| POST | `/api/v1/applications` | 201 (sin cambio) | `{name, description, baseUrl}` (sin cambio) | `ApplicationRegisteredWebResponse` — todos los campos de `ApplicationWebResponse` más `credential` |

`GET /api/v1/applications` no cambia: sigue devolviendo `ApplicationWebResponse`, sin el campo
`credential` ni ningún rastro del hash.

- **Autorización:** sin cambio — requiere token, el inquilino sale del principal.
- **Errores esperados:** sin cambio — las excepciones de `RegisterApplicationRulesValidator` no se
  tocan.

## 7. SPEC — el contrato

### `shared/port` — nuevos `[N]`

```java
// pdp/src/main/java/co/edu/uco/seguridad/shared/port/SecretGenerator.java
@FunctionalInterface
public interface SecretGenerator {
    String next();
}

// pdp/src/main/java/co/edu/uco/seguridad/shared/port/CredentialHasher.java
@FunctionalInterface
public interface CredentialHasher {
    String hash(String plaintext);
}
```

> Nota para el implementador: `CredentialHasher` solo tiene `hash(...)` en esta historia. HU-013
> le añadirá `boolean matches(String plaintext, String hash)` como `[M]` cuando exista quien valide.
> No adelantes ese método ahora — sería una firma que ningún consumidor ejercería todavía.

### `pdp/applications/domain/model` — nuevo `[N]`

```java
// pdp/src/main/java/co/edu/uco/seguridad/pdp/applications/domain/model/ApplicationCredentialHash.java
public record ApplicationCredentialHash(String value) { }
// invariantes: no nulo (ValueObjectMessages.VALUE_REQUIRED), value = value.trim(),
// no vacío tras el trim (ValueObjectMessages.VALUE_REQUIRED) — mismo patrón que TenantId sin la
// validación de formato, porque el valor lo produce CredentialHasher, no una persona.
```

### `pdp/applications/domain/exception` — nuevo `[N]`

```java
// pdp/src/main/java/co/edu/uco/seguridad/pdp/applications/domain/exception/InvalidApplicationCredentialHashException.java
public final class InvalidApplicationCredentialHashException extends co.edu.uco.seguridad.pdp.commons.exception.InvalidValueException { }
// mismo patrón que InvalidTenantIdException: constructor(String reason), código "INVALID_APPLICATION_CREDENTIAL_HASH"
```

### `pdp/applications/application/primaryport/response` — nuevo `[N]`

```java
// pdp/src/main/java/co/edu/uco/seguridad/pdp/applications/application/primaryport/response/ApplicationRegistrationResponse.java
public record ApplicationRegistrationResponse(
        co.edu.uco.seguridad.pdp.applications.application.primaryport.response.RegisteredApplicationResponse application,
        String credential) { }
// invariantes: application no nulo, credential no nulo ni vacío tras trim — mismo criterio que
// ApplicationCredentialHash, pero aquí NO es un VO: es el secreto en claro, vive un instante y
// nunca se reconstruye desde persistencia, así que envolverlo en un tipo propio no aporta nada
// (ver Hallazgo 2).
```

### `pdp/applications/infrastructure/adapter/primary/web/dto/response` — nuevo `[N]`

```java
// pdp/src/main/java/co/edu/uco/seguridad/pdp/applications/infrastructure/adapter/primary/web/dto/response/ApplicationRegisteredWebResponse.java
public record ApplicationRegisteredWebResponse(String id, String tenantId, String name, String description,
        String baseUrl, String credential, java.time.Instant registeredAt) { }
// plano, como ApplicationWebResponse — mismo orden de campos más `credential` antes de registeredAt.
```

### Firmas modificadas `[M]` — el tester las aplica, el planificador no las toca

```java
// pdp/applications/domain/Application.java
public record Application(ApplicationId id, TenantId tenantId, ApplicationName name, String description,
        ApplicationBaseUrl baseUrl, ApplicationCredentialHash credentialHash, Instant registeredAt) {
    public static Application register(ApplicationId id, TenantId tenantId, ApplicationName name,
            String description, ApplicationBaseUrl baseUrl, ApplicationCredentialHash credentialHash,
            Instant registeredAt) { ... }
}

// pdp/applications/application/usecase/RegisterApplicationUseCase.java
public interface RegisterApplicationUseCase
        extends ReactiveOperation<RegisterApplicationRequest, ApplicationRegistrationResponse> { }
// antes: ReactiveOperation<RegisterApplicationRequest, RegisteredApplicationResponse>

// pdp/applications/application/usecase/impl/RegisterApplicationUseCaseImpl.java
public RegisterApplicationUseCaseImpl(RegisterApplicationRulesValidator rules, ApplicationRepository repository,
        IdentifierGenerator identifiers, TimeProvider time, SecretGenerator secretGenerator, CredentialHasher hasher)
// antes: (rules, repository, identifiers, time) — dos parámetros nuevos, al final
public Mono<ApplicationRegistrationResponse> execute(RegisterApplicationRequest dto);
// antes: Mono<RegisteredApplicationResponse>

// pdp/applications/infrastructure/adapter/primary/web/interactor/RegisterApplicationInteractor.java
public interface RegisterApplicationInteractor
        extends ReactiveOperation<RegisterApplicationRawRequest, ApplicationRegisteredWebResponse> { }
// antes: ReactiveOperation<RegisterApplicationRawRequest, ApplicationWebResponse>

// pdp/applications/infrastructure/adapter/primary/web/controller/ApplicationController.java
Mono<ResponseEntity<ApiResponse<ApplicationRegisteredWebResponse>>> register(RegisterApplicationRawRequest body, ServerWebExchange exchange);
// antes: Mono<ResponseEntity<ApiResponse<ApplicationWebResponse>>>

// pdp/applications/infrastructure/adapter/primary/web/mapper/ApplicationResponseMapper.java
public static ApplicationRegisteredWebResponse toRegisteredResponse(ApplicationRegistrationResponse response);
// método NUEVO, aditivo — toResponse(...) y toResponseList(...) no cambian de firma

// pdp/applications/infrastructure/adapter/secondary/persistence/entity/ApplicationEntity.java
public record ApplicationEntity(String id, String tenantId, String name, String description, String baseUrl,
        String credentialHash, String registeredAt) { }
// nuevo campo credentialHash, entre baseUrl y registeredAt

// pdp/applications/infrastructure/adapter/secondary/persistence/mapper/ApplicationPersistenceMapper.java
// toDomain(entity) construye Application(..., new ApplicationCredentialHash(entity.credentialHash()), ...)
```

## 8. Árbol de archivos

```
pdp/src/main/java/co/edu/uco/seguridad/
├── shared/
│   ├── port/
│   │   ├── SecretGenerator.java                                              [N]
│   │   └── CredentialHasher.java                                             [N]
│   ├── config/SharedPortsConfiguration.java                                  [M] -- +2 beans: secretGenerator(), credentialHasher()
│   │                                                                              (ambos con cuerpo `throw new UnsupportedOperationException("pendiente: HU-012")`;
│   │                                                                               la implementación real —SecureRandom/base64, BCryptPasswordEncoder— es
│   │                                                                               del implementador, no del planificador)
│   └── message/RequiredArgumentMessages.java                                 [M] -- +3 constantes (ver §11)
└── pdp/
    ├── commons/message/ValueObjectMessages.java                              -- sin cambios: ApplicationCredentialHash reutiliza VALUE_REQUIRED, no necesita clase anidada propia
    └── applications/
        ├── domain/
        │   ├── Application.java                                              [M] -- +componente credentialHash; NO lo toca el planificador
        │   ├── model/ApplicationCredentialHash.java                          [N]
        │   └── exception/InvalidApplicationCredentialHashException.java      [N]
        ├── application/
        │   ├── usecase/RegisterApplicationUseCase.java                       [M] -- NO lo toca el planificador
        │   ├── usecase/impl/RegisterApplicationUseCaseImpl.java              [M] -- NO lo toca el planificador
        │   └── primaryport/response/ApplicationRegistrationResponse.java     [N]
        └── infrastructure/
            ├── adapter/primary/web/
            │   ├── controller/ApplicationController.java                     [M] -- NO lo toca el planificador
            │   ├── dto/response/ApplicationRegisteredWebResponse.java        [N]
            │   ├── interactor/RegisterApplicationInteractor.java             [M] -- NO lo toca el planificador
            │   ├── interactor/impl/RegisterApplicationInteractorImpl.java    [M] -- NO lo toca el planificador
            │   └── mapper/ApplicationResponseMapper.java                     [M] -- +1 método, aditivo; NO lo toca el planificador
            ├── adapter/secondary/persistence/
            │   ├── entity/ApplicationEntity.java                             [M] -- NO lo toca el planificador
            │   ├── mapper/ApplicationPersistenceMapper.java                  [M] -- NO lo toca el planificador
            │   └── repository/SurrealApplicationRepository.java              [M] -- save()/toDomain() incluyen credentialHash; NO lo toca el planificador
            └── config/ApplicationsConfiguration.java                        [M] -- el bean registerApplicationUseCase gana SecretGenerator+CredentialHasher
                                                                                    como parámetros, junto con el resto del [M] de RegisterApplicationUseCaseImpl;
                                                                                    NO lo toca el planificador (acoplado al constructor que todavía no cambió)
```

> **Por qué el planificador no materializa casi ningún `[M]` esta vez.** A diferencia de historias
> anteriores donde el `[M]` era aditivo (un método nuevo en una interfaz que no rompía nada), aquí
> **todos** los `[M]` de `applications` cambian una firma que ya tiene consumidores compilando hoy
> (`RegisterApplicationUseCaseImpl`, `ApplicationController`, etc.). Tocarlos ahora rompería el build
> hasta que el tester actualice sus pruebas — exactamente lo que la FASE 5 prohíbe ("termina con el
> proyecto compilando"). Por eso quedan documentados aquí con su firma exacta y el tester los aplica
> en su FASE 1, como manda el protocolo.

## 9. Casos de prueba esperados

| Capa | Clase de prueba | Casos |
|---|---|---|
| `applications` domain | `ApplicationCredentialHashTests` (nueva) | rechaza `null`; rechaza vacío/solo-espacios tras `trim()`; acepta un valor no vacío tal cual (sin normalizar mayúsculas ni nada — es un hash opaco) |
| `applications` domain | `ApplicationTests` (nueva — hoy no existe una clase de prueba propia del agregado) | `register` rechaza `credentialHash` nulo con `NullPointerException` |
| `applications` application | `RegisterApplicationUseCaseImplTests` (existente, 2 casos a actualizar + 1 nuevo) | Los dos casos existentes ganan un `SecretGenerator`/`CredentialHasher` fijos en el constructor del caso de uso. **Caso nuevo:** el `Application` guardado tiene `credentialHash()` igual al resultado del `CredentialHasher` fake, **nunca** igual al secreto en claro que produjo el `SecretGenerator` fake — y la `ApplicationRegistrationResponse` devuelta trae ese secreto en claro en `.credential()`. Esta es la prueba que demuestra el criterio 2 (nunca se persiste en claro) |
| `applications` infrastructure | `ApplicationResponseMapperTests` (existente, +1 caso) | `toRegisteredResponse` aplana `ApplicationRegistrationResponse` a `ApplicationRegisteredWebResponse` incluyendo `credential`; `toResponse`/`toResponseList` no cambian (siguen sin `credential`) |
| `applications` infrastructure | `ApplicationPersistenceMapperTests` (existente, 2 casos a actualizar) | `entity(...)` y ambos `@Test` ganan el nuevo campo de `ApplicationEntity`; el camino feliz afirma también `application.credentialHash().value()` |
| `applications` infrastructure | `ApplicationControllerTests` (existente, 1 caso a actualizar) | `register_delegates_to_the_interactor_and_replies_with_201` cambia su fake de retorno a `ApplicationRegisteredWebResponse` |
| `applications` infrastructure (E2E) | `ApplicationHttpTests` (existente, +1 caso sugerido) | El `POST` de registro responde con `$.data.credential` no vacío; una llamada posterior a `GET /api/v1/applications` no incluye `credential` en ningún elemento del listado |
| Ripple mecánico (sin nuevas aserciones, solo mantener la compilación) | `ApplicationCriteriaTests`, `ListApplicationsUseCaseImplTests` (`applications`), `RegisterProtectedResourceUseCaseImplTests` (`resources`) | Sus fábricas privadas de `Application`/`Application.register(...)` ganan el nuevo parámetro |
| `shared/persistence` (integración) | `SurrealRepositoryIntegrationTests` (existente, 1 caso a actualizar + ampliar aserción) | `application_repository_detects_existence_only_after_saving` gana el nuevo parámetro; añade una aserción de que `repository.save(...)` seguido de una relectura conserva el mismo `credentialHash` (round-trip real contra SurrealDB) |

## 10. Trazabilidad

| Fase | Estado | Fecha |
|---|---|---|
| Plan | ✅ Generado | 2026-09-13 |
| Contrato aprobado (gate 1) | ⏳ Pendiente | |
| Pruebas en rojo | ⏳ Pendiente | |
| Implementación en verde | ⏳ Pendiente | |
| Validación | ✅ Aprobado — ver `reportes/REPORTE-HU-012.md` | 2026-09-13 |
| Entrega (gate 2) | ⏳ Pendiente | |

## 11. Ambigüedades pendientes

Ninguna que bloquee — dos decisiones de diseño se confirmaron con Sebastián el 2026-09-13:

1. **Granularidad:** un secreto **por aplicación**, no por (aplicación, ambiente). Se descartó
   explícitamente ampliar `Application` con una dimensión de ambiente — el PDP no modela ambientes
   hoy, y hacerlo habría exigido cambiar la identidad del agregado (uniqueness de `(tenant, nombre)`
   a `(tenant, nombre, ambiente)`), con ripple hacia `resources`/`roles`/`assignments` fuera del
   alcance de esta historia.
2. **Qué se guarda:** solo el hash (mismo algoritmo — BCrypt — que ya usa `pep/RouteRegistry`), nunca
   el secreto en claro.

Lista de constantes nuevas de `RequiredArgumentMessages` para que el implementador no las invente:
`SECRET_GENERATOR`, `CREDENTIAL_HASHER`, `APPLICATION_CREDENTIAL_HASH`.
