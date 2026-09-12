# PROJECT-MAP

> Generado por `.claude/tools/mapa.ps1`. **No editar a mano** - se regenera desde el codigo.
> Es el nivel 0 del grafo de conocimiento: responde "que existe y donde va lo nuevo".

- Clases de produccion: **280**
- Clases de prueba: **88**
- Slices de negocio: **6** (applications, authorization, commons, identity, resources, tenants)

---

## Slices de negocio (`pdp`)

### `applications` - 51 clases

| Rol | Clases |
|---|---|
| Excepcion de dominio | `ApplicationNotFoundException`, `DuplicateApplicationException`, `InvalidApplicationBaseUrlException`, `ReservedApplicationNameException` |
| Catalogo de mensajes | `ApplicationsMessages` |
| Value object | `ApplicationBaseUrl` |
| Regla de dominio (impl) | `ApplicationMustExistForTenantRuleImpl`, `ApplicationNameMustBeUniqueForTenantRuleImpl`, `ApplicationNameMustNotBeReservedRuleImpl` |
| Dato de regla (hecho resuelto) | `ApplicationExistence`, `ApplicationNameAvailability` |
| Regla de dominio (contrato) | `ApplicationMustExistForTenantRule`, `ApplicationNameMustBeUniqueForTenantRule`, `ApplicationNameMustNotBeReservedRule` |
| Dominio (agregado / criteria) | `Application`, `ApplicationCriteria` |
| Caso de uso (impl) | `ListApplicationsUseCaseImpl`, `RegisterApplicationUseCaseImpl`, `RemoveApplicationUseCaseImpl` |
| Caso de uso (contrato) | `ListApplicationsUseCase`, `RegisterApplicationUseCase`, `RemoveApplicationUseCase` |
| Validador de reglas (impl) | `ApplicationMustExistForTenantValidatorImpl`, `ApplicationOwnerLookupValidatorImpl`, `RegisterApplicationRulesValidatorImpl` |
| Validador de reglas (contrato) | `ApplicationMustExistForTenantValidator`, `ApplicationOwnerLookupValidator`, `RegisterApplicationRulesValidator` |
| Puerto de salida | `ApplicationRepository` |
| DTO de entrada al nucleo | `ApplicationOwnershipQuery`, `ListApplicationsRequest`, `RegisterApplicationRequest` |
| DTO de salida del nucleo | `RegisteredApplicationResponse` |
| Controller | `ApplicationController` |
| DTO crudo HTTP | `ListApplicationsRawRequest`, `RegisterApplicationRawRequest` |
| DTO de respuesta HTTP | `ApplicationWebResponse` |
| Interactor (impl) | `ListApplicationsInteractorImpl`, `RegisterApplicationInteractorImpl` |
| Interactor (contrato) | `ListApplicationsInteractor`, `RegisterApplicationInteractor` |
| Mapper web | `ApplicationResponseMapper`, `ListApplicationsRequestMapper`, `RegisterApplicationRequestMapper` |
| Entidad de persistencia | `ApplicationEntity` |
| Mapper de persistencia | `ApplicationPersistenceMapper` |
| Adaptador de repositorio | `SurrealApplicationRepository` |
| Esquema de tabla | `ApplicationSchema`, `SurrealApplicationSchemaInitializer` |
| Cableado (@Bean) | `ApplicationsConfiguration` |
| Propiedades | `ApplicationCatalogProperties` |

### `authorization` - 28 clases

| Rol | Clases |
|---|---|
| Value object | `DecisionState`, `PolicyReference`, `ReasonCode` |
| Caso de uso (impl) | `AuthorizeUseCaseImpl`, `EvaluateInternalAccessUseCaseImpl` |
| Caso de uso (contrato) | `AuthorizeUseCase`, `EvaluateInternalAccessUseCase` |
| Puerto de salida | `PolicyDecisionPort` |
| DTO de entrada al nucleo | `AccessRequest`, `InternalAccessRequest` |
| DTO de salida del nucleo | `AccessDecision` |
| Controller | `AuthorizationController`, `InternalAccessDecisionController` |
| DTO crudo HTTP | `AccessDecisionRawRequest`, `AuthorizeRawRequest` |
| DTO de respuesta HTTP | `AccessDecisionInternalWebResponse`, `AccessDecisionWebResponse`, `PolicyReferenceWebResponse` |
| Interactor (impl) | `AuthorizeInteractorImpl`, `InternalAccessDecisionInteractorImpl` |
| Interactor (contrato) | `AuthorizeInteractor`, `InternalAccessDecisionInteractor` |
| Mapper web | `AccessDecisionInternalResponseMapper`, `AccessDecisionRawRequestMapper`, `AccessDecisionResponseMapper`, `AuthorizeRequestMapper` |
| Cableado (@Bean) | `AuthorizationConfiguration` |
| Otro | `DenyByDefaultPolicyDecisionAdapter` |

### `commons` - 16 clases

| Rol | Clases |
|---|---|
| Excepcion de dominio | `BusinessRuleViolationException`, `ConflictBusinessRuleException`, `DomainException`, `InvalidApplicationNameException`, `InvalidIdentifierException`, `InvalidPageWindowException`, `InvalidTenantIdException`, `InvalidValueException` |
| Catalogo de mensajes | `ValueObjectMessages` |
| Value object | `ApplicationId`, `ApplicationName`, `PageWindow`, `ResourceId`, `ResultPage`, `TenantId` |
| Otro | `AggregateRoot` |

### `identity` - 38 clases

| Rol | Clases |
|---|---|
| Excepcion de dominio | `InvalidEmailException`, `UserNotFoundException` |
| Catalogo de mensajes | `IdentityMessages` |
| Value object | `Email`, `ExternalIdentity`, `UserId` |
| Regla de dominio (impl) | `UserMustExistRuleImpl` |
| Dato de regla (hecho resuelto) | `UserExistence` |
| Regla de dominio (contrato) | `UserMustExistRule` |
| Dominio (agregado / criteria) | `SecurityUser` |
| Caso de uso (impl) | `AssignTenantUseCaseImpl`, `ListUsersUseCaseImpl`, `ProvisionIdentityUseCaseImpl` |
| Caso de uso (contrato) | `AssignTenantUseCase`, `ListUsersUseCase`, `ProvisionIdentityUseCase` |
| Puerto de salida | `SecurityUserRepository` |
| DTO de entrada al nucleo | `AssignTenantRequest`, `ProvisionIdentityRequest` |
| DTO de salida del nucleo | `UserResponse` |
| Controller | `UserController` |
| DTO crudo HTTP | `AssignTenantBodyRequest`, `AssignTenantRawRequest` |
| DTO de respuesta HTTP | `UserWebResponse` |
| Interactor (impl) | `AssignTenantInteractorImpl`, `ListUsersInteractorImpl` |
| Interactor (contrato) | `AssignTenantInteractor`, `ListUsersInteractor` |
| Mapper web | `AssignTenantRequestMapper`, `UserResponseMapper` |
| Entidad de persistencia | `ExternalIdentityEntity`, `SecurityUserEntity` |
| Mapper de persistencia | `SecurityUserPersistenceMapper` |
| Adaptador de repositorio | `SurrealSecurityUserRepository` |
| Esquema de tabla | `IdentitySchema`, `SurrealIdentitySchemaInitializer` |
| Cableado (@Bean) | `IdentityConfiguration` |
| Propiedades | `IdentityProvisioningProperties` |

### `resources` - 44 clases

| Rol | Clases |
|---|---|
| Excepcion de dominio | `DuplicateProtectedResourceException`, `InvalidResourcePathException`, `ProtectedResourceNotFoundException`, `UnsupportedHttpMethodException` |
| Catalogo de mensajes | `ResourcesMessages` |
| Value object | `HttpVerb`, `ResourcePath` |
| Regla de dominio (impl) | `ProtectedResourceMustBeUniqueRuleImpl`, `ProtectedResourceMustExistRuleImpl` |
| Dato de regla (hecho resuelto) | `ProtectedResourceAvailability`, `ProtectedResourceExistence` |
| Regla de dominio (contrato) | `ProtectedResourceMustBeUniqueRule`, `ProtectedResourceMustExistRule` |
| Evento de dominio | `ProtectedResourceRegistered` |
| Dominio (agregado / criteria) | `ProtectedResource` |
| Caso de uso (impl) | `ListProtectedResourcesUseCaseImpl`, `RegisterProtectedResourceUseCaseImpl` |
| Caso de uso (contrato) | `ListProtectedResourcesUseCase`, `RegisterProtectedResourceUseCase` |
| Validador de reglas (impl) | `ProtectedResourceMustExistValidatorImpl`, `RegisterProtectedResourceRulesValidatorImpl` |
| Validador de reglas (contrato) | `ProtectedResourceMustExistValidator`, `RegisterProtectedResourceRulesValidator` |
| Puerto de salida | `ProtectedResourceRepository` |
| DTO de entrada al nucleo | `ProtectedResourceLookup`, `RegisterProtectedResourceRequest` |
| DTO de salida del nucleo | `RegisteredProtectedResourceResponse` |
| Controller | `ProtectedResourceController` |
| DTO crudo HTTP | `RegisterProtectedResourceBodyRequest`, `RegisterProtectedResourceRawRequest` |
| DTO de respuesta HTTP | `ProtectedResourceWebResponse` |
| Interactor (impl) | `ListProtectedResourcesInteractorImpl`, `RegisterProtectedResourceInteractorImpl` |
| Interactor (contrato) | `ListProtectedResourcesInteractor`, `RegisterProtectedResourceInteractor` |
| Mapper web | `ProtectedResourceResponseMapper`, `RegisterProtectedResourceRequestMapper` |
| Entidad de persistencia | `ProtectedResourceEntity` |
| Mapper de persistencia | `ProtectedResourcePersistenceMapper` |
| Adaptador de repositorio | `SurrealProtectedResourceRepository` |
| Esquema de tabla | `ProtectedResourceSchema`, `SurrealProtectedResourceSchemaInitializer` |
| Adaptador de auditoria | `InMemoryAuditAdapter` |
| Cableado (@Bean) | `ResourcesConfiguration` |

### `tenants` - 42 clases

| Rol | Clases |
|---|---|
| Excepcion de dominio | `DuplicateTenantException`, `InvalidTenantNameException`, `TenantNotActiveException`, `TenantNotFoundException` |
| Catalogo de mensajes | `TenantsMessages` |
| Value object | `TenantName`, `TenantStatus` |
| Regla de dominio (impl) | `TenantCodeMustBeUniqueRuleImpl`, `TenantMustExistRuleImpl`, `TenantStatusMustBeActiveRuleImpl` |
| Dato de regla (hecho resuelto) | `TenantActivation`, `TenantCodeAvailability`, `TenantExistence` |
| Regla de dominio (contrato) | `TenantCodeMustBeUniqueRule`, `TenantMustExistRule`, `TenantStatusMustBeActiveRule` |
| Dominio (agregado / criteria) | `Tenant` |
| Caso de uso (impl) | `CreateTenantUseCaseImpl`, `ListTenantsUseCaseImpl` |
| Caso de uso (contrato) | `CreateTenantUseCase`, `ListTenantsUseCase` |
| Validador de reglas (impl) | `TenantMustBeActiveValidatorImpl` |
| Validador de reglas (contrato) | `TenantMustBeActiveValidator` |
| Puerto de salida | `TenantRepository` |
| DTO de entrada al nucleo | `CreateTenantRequest` |
| DTO de salida del nucleo | `TenantResponse` |
| Controller | `TenantController` |
| DTO crudo HTTP | `CreateTenantRawRequest` |
| DTO de respuesta HTTP | `TenantWebResponse` |
| Interactor (impl) | `CreateTenantInteractorImpl`, `ListTenantsInteractorImpl` |
| Interactor (contrato) | `CreateTenantInteractor`, `ListTenantsInteractor` |
| Mapper web | `CreateTenantRequestMapper`, `TenantResponseMapper` |
| Entidad de persistencia | `TenantEntity` |
| Mapper de persistencia | `TenantPersistenceMapper` |
| Adaptador de repositorio | `SurrealTenantRepository` |
| Esquema de tabla | `SurrealTenantSchemaInitializer`, `TenantSchema` |
| Cableado (@Bean) | `TenantsConfiguration` |
| Propiedades | `TenantCatalogProperties` |

---

## Capacidades tecnicas (`shared`)

| Subpaquete | Clases |
|---|---|
| `auth/model` | `OidcFlowIntent` |
| `auth/service` | `KeycloakOidcSessionService`, `OidcAuthenticationFailureHandler`, `OidcAuthenticationSuccessHandler`, `OidcAuthorizationFlowService`, `OidcFlowStateService`, `OidcRedirectPolicy` |
| `auth/web` | `KeycloakLoginController`, `KeycloakRegistrationController` |
| `config` | `EventPublisherConfiguration`, `InternalSecurityConfiguration`, `KeycloakSecurityConfiguration`, `ProjectPackages`, `SecurityConfiguration`, `SharedPortsConfiguration`, `SurrealDbConfiguration` |
| `contract` | `Operation`, `OperationWithoutResult`, `ReactiveOperation`, `ReactiveOperationWithoutInput`, `ReactiveOperationWithoutResult`, `ReactiveStreamOperation` |
| `event` | `DomainEvent`, `DomainEventPublisher`, `SpringDomainEventPublisher` |
| `message` | `RequiredArgumentMessages` |
| `observability` | `ReactiveLogContext` |
| `persistence/surrealdb` | `SurrealDbClient`, `SurrealDbException`, `SurrealDbHealthIndicator`, `SurrealDbProperties`, `SurrealRecordId`, `SurrealSchemaInitializer` |
| `port` | `IdentifierGenerator`, `TimeProvider` |
| `security` | `ApiAccessDeniedHandler`, `ApiAuthenticationEntryPoint`, `CorsProperties`, `InternalEvidenceJwtProperties`, `InternalMtlsProperties`, `InternalMtlsWebFilter`, `JwtSecurityProperties`, `KeycloakSessionProperties`, `LocalUserPrincipal`, `PdpPrincipal`, `SecurityContext` |
| `web` | `ApiResponse`, `CorrelationWebFilter`, `PageResponse`, `RequestContext`, `RequestFieldParser` |
| `web/exception` | `ConflictingRequestParametersException`, `MalformedRequestFieldException`, `MissingRequestFieldException`, `RequestContractException` |
| `web/exceptionhandler` | `ApiErrorHandler` |
| `web/message` | `WebContractMessages` |
| `web/session` | `KeycloakLogoutController`, `SessionController`, `SessionResponse` |

---

## Puertos de salida y sus implementaciones

| Puerto | Implementado por |
|---|---|
| `ApplicationRepository` | `SurrealApplicationRepository` |
| `PolicyDecisionPort` | `DenyByDefaultPolicyDecisionAdapter` |
| `ProtectedResourceRepository` | `SurrealProtectedResourceRepository` |
| `SecurityUserRepository` | `SurrealSecurityUserRepository` |
| `TenantRepository` | `SurrealTenantRepository` |

---

## Endpoints

| Verbo | Ruta | Controller |
|---|---|---|
| GET | `/api/v1/applications` | `ApplicationController` |
| POST | `/api/v1/applications` | `ApplicationController` |
| GET | `/api/v1/applications/{applicationId}/resources` | `ProtectedResourceController` |
| POST | `/api/v1/applications/{applicationId}/resources` | `ProtectedResourceController` |
| POST | `/api/v1/authorize` | `AuthorizationController` |
| GET | `/api/v1/session` | `SessionController` |
| GET | `/api/v1/session/logout` | `KeycloakLogoutController` |
| GET | `/api/v1/tenants` | `TenantController` |
| POST | `/api/v1/tenants` | `TenantController` |
| GET | `/api/v1/users` | `UserController` |
| PUT | `/api/v1/users/{id}/tenant` | `UserController` |
| POST | `/internal/v1/access-decisions` | `InternalAccessDecisionController` |
| GET | `/oauth2/authorization/keycloak` | `KeycloakLoginController` |
| GET | `/oauth2/authorization/keycloak/register` | `KeycloakRegistrationController` |

---

## Pruebas por area

| Area | Clases de prueba |
|---|---|
| `pdp/applications` | `ApplicationBaseUrlTests`, `ApplicationCatalogPropertiesTests`, `ApplicationControllerTests`, `ApplicationCriteriaTests`, `ApplicationHttpTests`, `ApplicationMustExistForTenantValidatorTests`, `ApplicationOwnerLookupValidatorImplTests`, `ApplicationPersistenceMapperTests`, `ApplicationResponseMapperTests`, `ApplicationRuleTests`, `ListApplicationsRequestMapperTests`, `ListApplicationsUseCaseImplTests`, `RegisterApplicationRequestMapperTests`, `RegisterApplicationRulesValidatorTests`, `RegisterApplicationUseCaseImplTests`, `RemoveApplicationUseCaseImplTests` |
| `pdp/authorization` | `AccessDecisionInternalResponseMapperTests`, `AccessDecisionRawRequestMapperTests`, `AccessDecisionResponseMapperTests`, `AuthorizationControllerTests`, `AuthorizationHttpTests`, `AuthorizeRequestMapperTests`, `AuthorizeUseCaseImplTests`, `DecisionStateTests`, `EvaluateInternalAccessUseCaseImplTests`, `InternalAccessDecisionControllerTests`, `InternalAccessDecisionInteractorImplTests`, `InternalSecurityChainIntegrationTests` |
| `pdp/commons` | `PageWindowTests`, `ResultPageTests`, `ValueObjectTests` |
| `pdp/identity` | `AssignTenantRequestMapperTests`, `AssignTenantUseCaseImplTests`, `EmailTests`, `ProvisionIdentityUseCaseImplTests`, `SecurityUserPersistenceMapperTests`, `UserControllerTests`, `UserMustExistRuleTests`, `UserResponseMapperTests` |
| `pdp/resources` | `HttpVerbTests`, `InMemoryAuditAdapterTests`, `ProtectedResourceControllerTests`, `ProtectedResourceMustBeUniqueRuleTests`, `ProtectedResourceMustExistRuleTests`, `ProtectedResourceMustExistValidatorTests`, `ProtectedResourceResponseMapperTests`, `RegisterProtectedResourceRequestMapperTests`, `RegisterProtectedResourceRulesValidatorTests`, `RegisterProtectedResourceUseCaseImplTests`, `ResourcePathTests` |
| `pdp/tenants` | `CreateTenantRequestMapperTests`, `CreateTenantUseCaseImplTests`, `ListTenantsUseCaseImplTests`, `TenantCatalogPropertiesTests`, `TenantControllerTests`, `TenantMustBeActiveValidatorTests`, `TenantNameTests`, `TenantResponseMapperTests`, `TenantRuleTests` |
| `raiz` | `AbstractSurrealDbIntegrationTest`, `LayeredArchitectureTests`, `ModulithStructureTests`, `PdpApplicationTests` |
| `shared/auth` | `KeycloakLoginControllerTests`, `KeycloakOidcSessionServiceTests`, `KeycloakRegistrationControllerTests`, `OidcAuthenticationFailureHandlerTests`, `OidcAuthenticationSuccessHandlerTests`, `OidcAuthorizationFlowServiceTests` |
| `shared/config` | `CorsConfigurationTests`, `KeycloakSecurityConfigurationTests`, `SecurityConfigurationTests` |
| `shared/persistence` | `SurrealDbClientTests`, `SurrealRecordIdTests`, `SurrealRepositoryIntegrationTests` |
| `shared/security` | `ApiAccessDeniedHandlerTests`, `ApiAuthenticationEntryPointTests`, `CorsPropertiesTests`, `InternalMtlsWebFilterTests`, `JwtSecurityPropertiesTests`, `PdpPrincipalSecurityContextTests`, `SecurityWebFilterChainTests`, `TestJwtSupport` |
| `shared/web` | `ApiErrorHandlerTests`, `CorrelationWebFilterTests`, `KeycloakLogoutControllerTests`, `SessionControllerTests`, `WebContractMessagesTests` |
