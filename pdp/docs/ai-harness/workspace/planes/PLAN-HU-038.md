# PLAN: El canal Bearer reconoce al usuario — aprovisiona su identidad y la usa al autorizar

## Metadata

- **ID:** HU-038
- **Slice:** `identity` (consumido desde `shared/security`; no toca `authorization`)
- **Tipo:** Mixto (escritura idempotente de identidad + propagación al principal)
- **Fecha:** 2026-10-30
- **Rama sugerida:** `fix/HU-038-identidad-bearer` (la rama de trabajo ya existe)
- **Fuentes:** historia `pdp/docs/ai-harness/workspace/HU-038.md` (decisiones cerradas 1-7); código leído: `RevocationAwareJwtDecoder`, `KeycloakSecurityConfiguration`, `SecurityConfiguration`, `InternalSecurityConfiguration`, `PdpPrincipal`, `SecurityContext`, `LocalUserPrincipal`, `ProvisionIdentityUseCaseImpl`, `SubjectUserIdLookupValidator(+Impl)`, `SurrealSecurityUserRepository`, `SurrealIdentitySchemaInitializer`, `AuthorizeInteractorImpl`, `AuthorizationContextResolverImpl`, `AuthorizeUseCaseImpl`, `OidcAuthenticationSuccessHandler`, realm `security-baseline-realm.json`. No se consultó `security-platform-architecture` (la historia es un defecto de cableado interno, sin regla de negocio nueva; ADR-026/034 solo se respetan).
- **Criterios de la línea base que toca:** 1, 2, 3, 9, 11, 12, 21, 22 (mínimos) y 23 (cobertura de las piezas nuevas)

## 1. Resumen funcional

El decoder Bearer de Keycloak resuelve —o aprovisiona de forma idempotente y atómica— la identidad local del `sub` **antes** de comprobar la revocación, y entrega el `UserId` al principal mediante un claim interno propio (`pdp_user_id`) que el decoder siempre sobrescribe. Con ello `POST /api/v1/authorize` por Bearer deja de ser `DENY` permanente (el `AccessRequest` ya trae `subjectUserId`) y una aplicación host puede usar a un usuario que nunca pasó por el panel.

**No cubre:** el canal cookie BFF ni la semántica de revocación (decisión 6); el canal interno `/internal/v1/**` (sigue con búsqueda sin aprovisionar: su token no trae `tenant`); cache de la resolución; sincronizar nombre/correo en llamadas posteriores; reconciliar el tenant del usuario ya existente con el del claim.

## 2. Criterios de aceptacion

| # | Criterio | Resultado esperado |
|---|---|---|
| 1 | Token válido de un usuario nunca visto | `200` en la ruta protegida; existe 1 `security_user` + 1 `external_identity` en el tenant del claim, sin pasar por el panel |
| 2 | Token sin `tenant`, tenant inexistente o suspendido, correo ya ajeno, o fallo al aprovisionar | `401` uniforme (`ApiAuthenticationEntryPoint`, code `UNAUTHORIZED`); no se crea usuario ni identidad |
| 3 | Dos primeras llamadas simultáneas | Una sola identidad y un solo usuario; ambas llamadas resuelven el mismo `UserId` |
| 4 | Revocación (ADR-026, ADR-034) del usuario recién aprovisionado | Un token anterior a `revokeAllSince`, `jti` revocado o `azp` revocado sigue dando `401` |
| 5 | `POST /api/v1/authorize` por Bearer | `DENY` antes de asignar, `ALLOW` después (el principal trae `userId`) |
| 6 | `e2e/` sin el paso `first-login` | Ver sección 11 (la carpeta `e2e/` no existe en esta rama) |
| 7 | Un `pdp_user_id` incluido en el token del cliente | Se ignora siempre: el decoder lo sobrescribe/elimina; nunca suplanta a otro usuario |

## 3. Reglas de negocio

| # | Regla | Donde vive (VO / Rule) | Puerto que trae el dato | Excepcion → HTTP |
|---|---|---|---|---|
| 1 | `tenant` del claim bien formado y presente | VO `TenantId` (existente), invocado en `BearerIdentityRequestMapper` | — | `InvalidTenantIdException` → el decoder lo traduce a `JwtValidationException` → **401** |
| 2 | El tenant del claim existe y está activo | Validador publicado `TenantMustBeActiveValidator` (existente, `tenants :: rule`) | `TenantRepository.findStatusById` (vía validador) | `TenantNotFoundException` / `TenantNotActiveException` → decoder → **401** |
| 3 | El correo con que se crea un usuario nuevo no es de otro usuario | `UserEmailMustBeAvailableRule` (**síncrona**, [N]) | `SecurityUserRepository.findByEmail` (lo consulta el use case y arma `UserEmailAvailability`) | `UserEmailAlreadyRegisteredException extends ConflictBusinessRuleException` ([M]*) → decoder → **401** (409 solo si algún día se expone por HTTP de negocio) |
| 4 | Correo ausente, inválido o `email_verified != true` ⇒ correo seguro derivado del sujeto | Política del caso de uso (construcción del `Email` placeholder `{sub}@bearer.invalid`) — no rechaza | — | Si `{sub}` no forma un correo válido: `InvalidEmailException` → decoder → **401** |
| 5 | Sin identidad previa y sin condiciones de rechazo ⇒ crear usuario + vínculo en una sola transacción; si pierde una carrera, devolver el `UserId` ganador | Puerto: `createUserWithIdentityIfAbsent` ([M]) | `SecurityUserRepository` | Error de persistencia sin ganador → decoder → **401** |
| 6 | Todo fallo no-`JwtException` de la resolución es fail-closed (excepto `Error` de la JVM) | `RevocationAwareJwtDecoder` ([M]) | — | `JwtValidationException` → **401**; un `Error` se propaga |

> No hay regla que compare el tenant del usuario ya existente con el del claim (ver riesgo R3).

## 4. Modelo de dominio afectado

### Entidad / agregado

`SecurityUser` (existente, sin cambios): se crea con `SecurityUser.provision(id, tenantId, email, name, now)`. `ExternalIdentity` (existente) enlaza `issuer`+`sub`.

### Value objects

| VO | Nuevo o existente | Invariantes | Vive en |
|---|---|---|---|
| `TenantId` | existente | formato `[a-zA-Z0-9][a-zA-Z0-9_-]{1,63}` | `pdp/commons/model` |
| `UserId` | existente | UUID | `pdp/commons/model` |
| `Email` | existente | `usuario@dominio`, minúsculas | `identity/domain/model` |

### Enums

Ninguno.

## 5. Persistencia

- **Tablas:** `security_user` y `external_identity` (constantes en `IdentitySchema`, **sin cambios**).
- **Índices únicos existentes (verificados en `SurrealIdentitySchemaInitializer`):** `external_identity_issuer_subject ON (issuer, subject) UNIQUE` y `security_user_email ON email UNIQUE`. Suficientes: garantizan que dos creaciones concurrentes no dejen dos vínculos para el mismo `(issuer, sub)` ni dos usuarios con el mismo correo. **No hace falta índice nuevo.**
- **Defecto de concurrencia hoy (`ProvisionIdentityUseCaseImpl.createAndLink`):** `save(user)` y `linkIdentity` son dos llamadas separadas; si la segunda falla por unicidad queda un `security_user` huérfano, y el perdedor de la carrera recibe un error espurio. Por eso la ruta Bearer **no reutiliza** esa secuencia.
- **Consulta nueva en el puerto** ([M], método `default` que lanza `UnsupportedOperationException`, mismo patrón que `findPageByTenant`, para no romper fakes de pruebas):

  ```java
  // identity/application/secondaryport/repository/SecurityUserRepository.java
  /**
   * Crea de forma atómica el usuario y su vínculo externo. Si ya existe una identidad para ese
   * (issuer, subject) —carrera perdida— no crea nada y devuelve la existente.
   */
  default Mono<ExternalIdentity> createUserWithIdentityIfAbsent(SecurityUser user, ExternalIdentity identity) {
      return Mono.error(new UnsupportedOperationException("pendiente: HU-038"));
  }
  ```
- **Implementación Surreal ([M], `SurrealSecurityUserRepository`):** una sola petición `/sql` con `BEGIN TRANSACTION; CREATE type::record('security_user', $userId) SET ...; CREATE type::record('external_identity', $identityId) SET ...; COMMIT TRANSACTION;` (parámetros siempre ligados, `CREATE` y no `UPSERT` para que choque con lo existente). Si `SurrealDbException` (violación de unicidad), releer `findIdentity(issuer, subject)`: presente → devolverla (carrera perdida); ausente → re-lanzar el error. El `id` de la fila de identidad sale del `IdentifierGenerator` del use case (no `UUID.randomUUID()` en el adaptador nuevo; `ExternalIdentity` no lleva id, así que el adaptador puede seguir generando el id de fila como hoy `linkIdentity` — decisión del implementador, no cambia el contrato).
- **Inicializador de esquema:** existente, sin cambios.

## 6. Endpoint

No se crea ningún endpoint. Efecto sobre los existentes:

| Verbo | Ruta | Cambio observable |
|---|---|---|
| cualquiera bajo `/api/v1/**` con `Authorization: Bearer` (perfil `keycloak`) | — | Un `sub` sin identidad ya no recibe 401: queda aprovisionado en la primera llamada |
| POST | `/api/v1/authorize` | Bearer ahora puede devolver `ALLOW` (el `AccessRequest` lleva `subjectUserId`) |

- **Autorización:** requiere token. El tenant sale del claim `tenant` del token, nunca del body (se conserva ADR-018).
- **Errores esperados (todos en el decoder, antes de cualquier controller):**

| Condición | HTTP | Cuerpo |
|---|---|---|
| Sin `tenant` / `tenant` malformado | 401 | `ProblemDetail` `UNAUTHORIZED` |
| Tenant inexistente o suspendido | 401 | idem |
| Correo del token ya pertenece a otro usuario | 401 | idem |
| Fallo de SurrealDB / Redis | 401 | idem (fail-closed, decisión 4) |
| Token revocado | 401 | idem (sin cambios) |

  Se usa **401 uniforme** porque `RevocationAwareJwtDecoder` solo puede lanzar `JwtException` (el criterio 2 de la historia admite «401/403»). El motivo concreto va solo al log del servidor, nunca al cliente. Los detalles no se filtran.

## 7. SPEC — el contrato

### Decisiones del planificador

| Tema | Decisión |
|---|---|
| **Nombre del claim interno** | `pdp_user_id` (constante `BearerIdentityClaim.NAME`). Snake case como los claims OIDC; prefijo `pdp_` para que no choque con un claim del IdP (aunque daría igual: se sobrescribe siempre). |
| **Quién lo escribe** | Todo `ReactiveJwtDecoder` que alimente una cadena con `oauth2ResourceServer` y termine en `PdpPrincipal.from(Jwt)`: `keycloakBearerJwtDecoder` (perfil `keycloak`, **aprovisiona**) y `jwtDecoder` HMAC (perfil por defecto, **solo sella si ya existe**, no aprovisiona ni rechaza). `internalEvidenceJwtDecoder` no usa `PdpPrincipal` (el interactor interno lee el `Jwt` directo), conserva su comportamiento y no se toca. |
| **Cómo se sobrescribe** | `BearerIdentityClaim.stamp(jwt, Optional<UserId>)`: reconstruye el `Jwt` (headers y claims copiados) con el claim fijado a la resolución o **eliminado** si no hay; si no hay `userId` y el claim ya estaba ausente devuelve la **misma instancia** (no rompe pruebas que comparan el `Jwt`). |
| **Dependencia entre módulos** | `shared` ⇒ `identity` mediante `ResolveBearerIdentityUseCase` (use case público nuevo en la interfaz nombrada `usecase`, request en `dto`, ambas ya publicadas; igual que `OidcAuthenticationSuccessHandler` → `ProvisionIdentityUseCase`). `identity` ⇒ `tenants :: rule` ya estaba permitido. **No se modifica ningún `allowedDependencies` ni se añade `@NamedInterface`** (`ModulithStructureTests` verificado en verde tras los esqueletos). |
| **Por qué un use case nuevo y no `ProvisionIdentityUseCase`** | Éste usa el tenant por defecto, devuelve `LocalUserPrincipal` y exige correo; además `linkAndLogin` enlaza por correo entre tenants. Cambiar su `ProvisionIdentityRequest` rompería al `OidcAuthenticationSuccessHandler` y sus pruebas. |
| **Resolución previa a revocación** | `findIdentityBySubject(sub)` primero (misma semántica que `SubjectUserIdLookupValidator`, así el `UserId` coincide con el que obtienen los 20+ interactores que hacen `principal.userId().orElse(lookup)`); solo si no hay identidad se aprovisiona. |
| **Sin enlace por correo** | A diferencia del panel, el Bearer **no** vincula un usuario existente por correo (se evita vincular a otro tenant). Un correo ajeno ⇒ 401 y nada creado. |
| **Correo seguro** | Se usa el correo del token solo si `email_verified == true`; si falta, es inválido o no verificado: `{sub}@bearer.invalid` (TLD reservado, único por sujeto). `name` ausente ⇒ `""`. `provider` = claim `identity_provider` en minúsculas o `keycloak-local` (igual que el panel). |
| **Refresco de último login** | No se actualiza `lastLoginAt` por petición Bearer (evita una escritura por llamada). |
| **`Error` de la JVM** | `onErrorMap` restringido a `Exception`: un `Error` no se traduce a 401. |
| **Constructores existentes** | Los tres constructores públicos de `RevocationAwareJwtDecoder` se conservan intactos (con su comportamiento: solo búsqueda); se añade fábrica estática para la ruta con aprovisionamiento. |

### Contratos nuevos [N]

```java
// pdp/identity/application/usecase/ResolveBearerIdentityUseCase.java
public interface ResolveBearerIdentityUseCase extends ReactiveOperation<ResolveBearerIdentityRequest, UserId> { }

// pdp/identity/domain/rule/UserEmailMustBeAvailableRule.java
public interface UserEmailMustBeAvailableRule extends OperationWithoutResult<UserEmailAvailability> { }
```

### Firmas de value objects y entidades [N]

```java
// pdp/identity/domain/rule/model/UserEmailAvailability.java
public record UserEmailAvailability(Email email, boolean taken) { }   // invariante: email no nulo
```

### Firmas de DTOs [N]

```java
// pdp/identity/application/primaryport/request/ResolveBearerIdentityRequest.java
public record ResolveBearerIdentityRequest(String issuer, String subject, TenantId tenantId,
                                           Optional<Email> email, boolean emailVerified,
                                           String name, String provider) { }
// invariantes: issuer, subject, tenantId, email (el Optional), provider no nulos; name nulo => ""
```

### Clases nuevas con esqueleto [N]

```java
// pdp/identity/application/usecase/impl/ResolveBearerIdentityUseCaseImpl.java
public final class ResolveBearerIdentityUseCaseImpl implements ResolveBearerIdentityUseCase {
    public ResolveBearerIdentityUseCaseImpl(SecurityUserRepository repository,
                                            TenantMustBeActiveValidator tenantMustBeActive,
                                            UserEmailMustBeAvailableRule emailMustBeAvailable,
                                            IdentifierGenerator identifiers, TimeProvider time);
    public Mono<UserId> execute(ResolveBearerIdentityRequest input);
}
// Algoritmo: findIdentityBySubject(sub) -> presente: devolver identity.userId() (sin escribir).
//   vacío: Mono.defer -> tenantMustBeActive.execute(tenantId)
//          -> email efectivo (token verificado | placeholder) -> findByEmail -> emailMustBeAvailable.execute(new UserEmailAvailability(email, taken))
//          -> SecurityUser.provision(new UserId(identifiers.next()), tenantId, email, name, time.now())
//          -> repository.createUserWithIdentityIfAbsent(user, new ExternalIdentity(user.id(), issuer, subject, provider)).map(ExternalIdentity::userId)

// pdp/identity/domain/rule/impl/UserEmailMustBeAvailableRuleImpl.java
public final class UserEmailMustBeAvailableRuleImpl implements UserEmailMustBeAvailableRule { public void execute(UserEmailAvailability input); }

// pdp/shared/security -> co/edu/uco/seguridad/shared/security/BearerIdentityClaim.java
public final class BearerIdentityClaim {
    public static final String NAME = "pdp_user_id";
    public static Jwt stamp(Jwt jwt, Optional<UserId> userId);
    public static Optional<UserId> read(Jwt jwt);          // valor no-UUID => InvalidIdentifierException
}

// co/edu/uco/seguridad/shared/security/BearerIdentityRequestMapper.java
public final class BearerIdentityRequestMapper { public static ResolveBearerIdentityRequest toRequest(Jwt jwt); }
//   issuer = jwt.getIssuer(); subject = jwt.getSubject(); tenantId = new TenantId(claim "tenant") (propaga la excepción del VO)
//   email = claim "email" parseado a Email, vacío si no hay o es inválido; emailVerified = claim booleano "email_verified" (ausente => false)
//   name = claim "name" (ausente => ""); provider = claim "identity_provider" en minúsculas, ausente/vacío => "keycloak-local"

// co/edu/uco/seguridad/shared/security/LocalIdentityStampingJwtDecoder.java
public final class LocalIdentityStampingJwtDecoder implements ReactiveJwtDecoder {
    public LocalIdentityStampingJwtDecoder(ReactiveJwtDecoder delegate, SubjectUserIdLookupValidator subjectUserIdLookup);
    public Mono<Jwt> decode(String token);   // delegate.decode -> lookup(sub) -> BearerIdentityClaim.stamp(jwt, resultado|vacío); fallo del lookup => JwtValidationException
}
```

Los cuatro registros `@Bean` nuevos de `IdentityConfiguration` ya existen (`userEmailMustBeAvailableRule`, `resolveBearerIdentityUseCase`).

### Modificaciones [M] — contrato exacto (las aplica el implementador, en un solo paso con sus consumidores)

```java
// shared/security/PdpPrincipal.java — from(Jwt): userId pasa de Optional.empty() a BearerIdentityClaim.read(jwt)
public static PdpPrincipal from(Jwt jwt);        // misma firma; el record y from(OidcUser) NO cambian

// shared/security/revocation/RevocationAwareJwtDecoder.java
//   (a) constructores existentes: firmas y comportamiento intactos
//   (b) fábrica nueva (ruta Bearer con aprovisionamiento)
public static RevocationAwareJwtDecoder provisioning(ReactiveJwtDecoder delegate, TokenRevocationPort revocation,
                                                     ResolveBearerIdentityUseCase identities);
//   decode(): delegate -> BearerIdentityRequestMapper.toRequest(jwt) dentro de Mono.defer -> identities.execute
//             -> UserId -> revocation (isRevoked, isTokenRevoked, clientRevoked, igual que hoy)
//             -> BearerIdentityClaim.stamp(jwt, Optional.of(userId)).
//   El token técnico del PEP (azp == technicalClientId) sigue pasando sin identidad (ruta de constructores existentes).
//   onErrorMap: `error instanceof Exception && !(error instanceof JwtException)` => JwtValidationException; Error se propaga.

// shared/config/KeycloakSecurityConfiguration.java
ReactiveJwtDecoder keycloakBearerJwtDecoder(JwtSecurityProperties properties, TokenRevocationPort revocation,
                                            ResolveBearerIdentityUseCase identities);   // devuelve RevocationAwareJwtDecoder.provisioning(...)
//   Además añadir el validador `tenant` presente: new JwtClaimValidator<String>("tenant", StringUtils::hasText)

// shared/config/SecurityConfiguration.java (perfil !keycloak)
ReactiveJwtDecoder jwtDecoder(JwtSecurityProperties properties, SubjectUserIdLookupValidator subjectUserIdLookup);
//   devuelve new LocalIdentityStampingJwtDecoder(decoder, subjectUserIdLookup)

// pdp/identity/application/secondaryport/repository/SecurityUserRepository.java — método default (sección 5)
// pdp/identity/infrastructure/adapter/secondary/persistence/repository/SurrealSecurityUserRepository.java — override transaccional (sección 5)

// [M]* clases nuevas que el planificador no puede esqueletizar (no son interfaz/record/impl-que-lanza):
// pdp/identity/domain/exception/UserEmailAlreadyRegisteredException.java
public final class UserEmailAlreadyRegisteredException extends ConflictBusinessRuleException {
    public UserEmailAlreadyRegisteredException(Email email);       // code "USER_EMAIL_ALREADY_REGISTERED"; texto de IdentityMessages
}
// pdp/identity/domain/message/IdentityMessages.java: añadir  public static String userEmailAlreadyRegistered(String email)
```

> `AuthorizeInteractorImpl`, `AuthorizationContextResolverImpl`, `SecurityContext` y los ~20 interactores con fallback `principal.userId().orElse(lookup)` **no cambian**: reciben `Optional<UserId>` poblado y el fallback pasa a ser código muerto en Bearer (queda para el canal OIDC/dev sin identidad). `PdpPrincipal` mantiene `Optional<UserId>`.

## 8. Arbol de archivos

```
shared/
├── message/RequiredArgumentMessages.java                  [M aditivo, hecho: 2 constantes]
├── security/
│   ├── BearerIdentityClaim.java                           [N]
│   ├── BearerIdentityRequestMapper.java                   [N]
│   ├── LocalIdentityStampingJwtDecoder.java               [N]
│   ├── PdpPrincipal.java                                  [M] from(Jwt)
│   └── revocation/RevocationAwareJwtDecoder.java          [M] fábrica provisioning + onErrorMap(Exception)
└── config/
    ├── KeycloakSecurityConfiguration.java                 [M] decoder + validador tenant
    └── SecurityConfiguration.java                         [M] jwtDecoder estampa (perfil dev/HMAC)
pdp/identity/
├── domain/
│   ├── exception/UserEmailAlreadyRegisteredException.java [M]*
│   ├── message/IdentityMessages.java                      [M] +userEmailAlreadyRegistered
│   └── rule/
│       ├── UserEmailMustBeAvailableRule.java              [N]
│       ├── impl/UserEmailMustBeAvailableRuleImpl.java     [N]
│       └── model/UserEmailAvailability.java               [N]
├── application/
│   ├── primaryport/request/ResolveBearerIdentityRequest.java   [N] (paquete ya publicado: "dto")
│   ├── secondaryport/repository/SecurityUserRepository.java    [M] +createUserWithIdentityIfAbsent (default)
│   └── usecase/
│       ├── ResolveBearerIdentityUseCase.java              [N] (paquete ya publicado: "usecase")
│       └── impl/ResolveBearerIdentityUseCaseImpl.java     [N]
└── infrastructure/
    ├── config/IdentityConfiguration.java                  [N-registro hecho] +userEmailMustBeAvailableRule, +resolveBearerIdentityUseCase
    └── adapter/secondary/persistence/repository/SurrealSecurityUserRepository.java  [M] override transaccional
```

Sin `@NamedInterface` nuevo, sin cambios en ningún `package-info.java` ni `allowedDependencies`.

## 9. Casos de prueba esperados

> Presupuesto de `sb-testing`; sin Mockito (fakes y lambdas). Las pruebas con SurrealDB son de integración y corren solo donde haya Docker.

| Capa | Clase de prueba | Casos |
|---|---|---|
| domain | `UserEmailMustBeAvailableRuleImplTests` | (a) no lanza si `taken=false`; (b) lanza `UserEmailAlreadyRegisteredException` si `taken=true` |
| application | `ResolveBearerIdentityUseCaseImplTests` (fake de repositorio) | (a) identidad existente: devuelve su `UserId` y no escribe; (b) sujeto nuevo: crea usuario+vínculo en el tenant del request con `provision` y devuelve el `UserId` generado; (c) tenant inactivo/inexistente: error del validador y no crea nada; (d) correo ya ajeno: `UserEmailAlreadyRegisteredException` y no crea; (e) sin correo / no verificado: usa `{sub}@bearer.invalid`; (f) carrera perdida (el puerto devuelve la identidad ganadora): devuelve ese `UserId`; (g) error del puerto se propaga |
| shared/security | `BearerIdentityClaimTests` | (a) `stamp` sobrescribe un valor entrante; (b) `stamp` con vacío elimina el claim entrante; (c) `stamp` vacío sin claim devuelve la misma instancia; (d) `read` ausente → vacío; (e) `read` no-UUID → `InvalidIdentifierException`; (f) conserva headers, `iat`/`exp`, `jti`, `sub` y demás claims |
| shared/security | `BearerIdentityRequestMapperTests` | tenant ausente → `InvalidTenantIdException`; tenant válido; email válido+verificado; email inválido → vacío; `email_verified` ausente → `false`; `identity_provider` y su valor por defecto; `name` ausente → `""` |
| shared/security | `PdpPrincipalSecurityContextTests` (**ya existe**; el implementador actualiza la aserción `userId().isEmpty()` del `Jwt` sin claim, que sigue vacía, y añade) | `from(Jwt)` con claim → `userId` presente; sin claim → vacío; claim basura → excepción |
| shared/security | `LocalIdentityStampingJwtDecoderTests` | (a) sujeto con identidad: claim = su `UserId`; (b) sujeto sin identidad: pasa y el claim queda ausente; (c) token con `pdp_user_id` ajeno y sin identidad: claim eliminado; (d) token con claim ajeno y con identidad: claim = el real; (e) fallo del lookup → `JwtValidationException`; (f) fallo del delegate se propaga sin consultar el lookup |
| shared/security/revocation | `RevocationAwareJwtDecoderTests` (**ya existe**, no se toca) + `RevocationAwareJwtDecoderProvisioningTests` | (a) usuario nuevo: llama a `identities` **antes** de `revocation` y entrega el `Jwt` sellado; (b) sin `tenant`: error, `identities` y `revocation` no se invocan; (c) tenant suspendido (error del use case): `JwtValidationException`; (d) `pdp_user_id` entrante se sobrescribe; (e) revocado (`isRevoked`/`isTokenRevoked`/`isClientRevoked`) para el recién aprovisionado → `JwtValidationException`; (f) un `Error` de la JVM no se traga; (g) token técnico del PEP no exige identidad ni `tenant` |
| infrastructure (integración, Surreal) | `SurrealSecurityUserRepositoryProvisioningTests` | (a) crea usuario+vínculo; (b) dos invocaciones **concurrentes** con el mismo `(issuer, sub)` dejan 1 usuario y 1 vínculo y ambas devuelven el mismo `UserId`; (c) fallo de unicidad por correo no deja identidad ni usuario huérfano; (d) no deja un `security_user` sin vínculo si el vínculo choca |
| infrastructure (HTTP) | `BearerAuthorizationHttpTests` | `@TestConfiguration` que reemplaza `jwtDecoder` por `RevocationAwareJwtDecoder.provisioning(hmacDecoder, revocación en memoria, ResolveBearerIdentityUseCase real)`: (a) primer uso de un `sub` desconocido con `tenant` activo ⇒ `200` y fila creada; (b) `POST /api/v1/authorize` ⇒ `DENY` antes de asignar rol/recurso y `ALLOW` después (usar la fixture de OPA como `AuthorizationHttpTests`); (c) tenant desconocido ⇒ `401` y sin filas; (d) token con `pdp_user_id` de otro usuario con permisos ⇒ no obtiene `ALLOW`. Además, con el decoder por defecto (`LocalIdentityStampingJwtDecoder`) y `linkTestIdentity`, `authorize` por Bearer llega a `ALLOW` y los HTTP tests existentes siguen verdes |
| e2e | `e2e/host-authorize.hurl`, `e2e/host-bearer.hurl` | Sin paso `first-login` (carpeta ausente en esta rama; ver sección 11) |

## 10. Trazabilidad

| Fase | Estado | Fecha |
|---|---|---|
| Plan | ✅ Generado | 2026-10-30 |
| Contrato aprobado (gate 1) | ⏳ Pendiente | |
| Pruebas en rojo | ⏳ Pendiente | |
| Implementacion en verde | ⏳ Pendiente | |
| Validacion | ⏳ Pendiente | |
| Entrega (gate 2) | ⏳ Pendiente | |

## 11. Ambiguedades pendientes

Ninguna bloqueante. Observaciones para el gate 1:

- **R1 — Riesgo de suplantación por claim.** Si algún `ReactiveJwtDecoder` futuro alimenta una cadena que use `PdpPrincipal.from(Jwt)` sin sellar/eliminar `pdp_user_id`, un token con ese claim suplanta a otro usuario. Hoy los dos únicos decoders de esas cadenas lo sellan; el caso HTTP 9(d) y las pruebas de `BearerIdentityClaim` lo fijan. El canal `/internal/v1/**` no usa `PdpPrincipal` (no afectado).
- **R2 — `SubjectUserIdLookupValidator` busca solo por `sub`** (sin emisor) y el índice único es `(issuer, sub)`. Se conserva esa semántica para que el `UserId` coincida con el de los fallbacks existentes; implica asumir un único emisor por despliegue.
- **R3 — Tenant del usuario ya existente vs claim.** Un usuario reasignado por `AssignTenantUseCase` a otro tenant puede llegar con un token cuyo `tenant` sea el anterior. No se rechaza (costaría un `findById` extra por petición y cambiaría el comportamiento actual); el principal usa el tenant del claim y el resolvedor de hechos filtra por `(userId, tenantId, aplicación)`. Decisión abierta si se quisiera endurecer.
- **R4 — Un correo ajeno ⇒ 401 permanente** para ese `sub` hasta que un administrador lo resuelva (no hay enlace por correo en Bearer). Un correo no verificado no bloquea (usa placeholder).
- **R5 — Transacciones SurrealQL por `/sql`:** el comportamiento ante violación de unicidad dentro de `BEGIN/COMMIT` (todas las sentencias en `ERR`) debe confirmarse en el test de integración del apartado 9 con la versión real de SurrealDB; no se pudo ejecutar aquí (sin sockets).
- **R6 — Latencia:** una consulta SurrealDB por petición Bearer (la misma que ya hacía el lookup de revocación); no se añade cache en esta historia.
- **R7 — `e2e/` no existe en esta rama.** El criterio 6 (hurl sin `first-login`) no es verificable aquí; el hallazgo origen está en otra rama/repo.
- **R8 — Extensión sobre las decisiones cerradas:** el perfil HMAC (`SecurityConfiguration.jwtDecoder`) también sella el claim (suave, sin aprovisionar). Sin ello el desarrollo local sin Keycloak seguiría con `DENY` permanente y no habría forma de ejercitar el `ALLOW` por Bearer en las pruebas HTTP existentes. No contradice las decisiones; requiere confirmación en gate 1.
- **R9 — Cambio de comportamiento menor del decoder:** `onErrorMap` pasa de capturar todo `Throwable` a solo `Exception` (decisión 4: un `Error` no se traga). Las pruebas actuales lanzan `RuntimeException`, no se afectan.
