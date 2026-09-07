# PROJECT-MAP

> Generado por `.claude/tools/mapa.ps1`. **No editar a mano** - se regenera desde el codigo.
> Es el nivel 0 del grafo de conocimiento: responde "que existe y donde va lo nuevo".

- Clases de produccion: **264**
- Clases de prueba: **80**
- Slices de negocio: **6** (applications, authorization, commons, identity, resources, tenants)

---

## Slices de negocio (`pdp`)

### `applications` - 49 clases

| Rol | Clases |
|---|---|
| Excepcion de dominio | `ApplicationNotFoundException`, `DuplicateApplicationException`, `InvalidApplicationBaseUrlException`, `ReservedApplicationNameException` |
| Dominio (entidad / VO / enum) | `Application`, `ApplicationBaseUrl`, `ApplicationCriteria`, `ApplicationExistence`, `ApplicationMustExistForTenantRule`, `ApplicationMustExistForTenantRuleImpl`, `ApplicationNameAvailability`, `ApplicationNameMustBeUniqueForTenantRule`, `ApplicationNameMustBeUniqueForTenantRuleImpl`, `ApplicationNameMustNotBeReservedRule`, `ApplicationNameMustNotBeReservedRuleImpl`, `ApplicationsMessages` |
| Caso de uso (impl) | `ListApplicationsUseCaseImpl`, `RegisterApplicationUseCaseImpl`, `RemoveApplicationUseCaseImpl` |
| Caso de uso (contrato) | `ListApplicationsUseCase`, `RegisterApplicationUseCase`, `RemoveApplicationUseCase` |
| Regla (contrato) | `ApplicationMustExistForTenantValidator`, `ApplicationMustExistForTenantValidatorImpl`, `RegisterApplicationRulesValidator`, `RegisterApplicationRulesValidatorImpl` |
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
| Otro | `ApplicationOwnershipQuery`, `ApplicationRepository`, `ListApplicationsRequest`, `RegisterApplicationRequest`, `RegisteredApplicationResponse` |

### `authorization` - 18 clases

| Rol | Clases |
|---|---|
| Dominio (entidad / VO / enum) | `DecisionState`, `PolicyReference`, `ReasonCode` |
| Caso de uso (impl) | `AuthorizeUseCaseImpl` |
| Caso de uso (contrato) | `AuthorizeUseCase` |
| Controller | `AuthorizationController` |
| DTO crudo HTTP | `AuthorizeRawRequest` |
| DTO de respuesta HTTP | `AccessDecisionWebResponse`, `PolicyReferenceWebResponse` |
| Interactor (impl) | `AuthorizeInteractorImpl` |
| Interactor (contrato) | `AuthorizeInteractor` |
| Mapper web | `AccessDecisionResponseMapper`, `AuthorizeRequestMapper` |
| Cableado (@Bean) | `AuthorizationConfiguration` |
| Otro | `AccessDecision`, `AccessRequest`, `DenyByDefaultPolicyDecisionAdapter`, `PolicyDecisionPort` |

### `commons` - 16 clases

| Rol | Clases |
|---|---|
| Otro | `AggregateRoot`, `ApplicationId`, `ApplicationName`, `BusinessRuleViolationException`, `ConflictBusinessRuleException`, `DomainException`, `InvalidApplicationNameException`, `InvalidIdentifierException`, `InvalidPageWindowException`, `InvalidTenantIdException`, `InvalidValueException`, `PageWindow`, `ResourceId`, `ResultPage`, `TenantId`, `ValueObjectMessages` |

### `identity` - 38 clases

| Rol | Clases |
|---|---|
| Excepcion de dominio | `InvalidEmailException`, `UserNotFoundException` |
| Dominio (entidad / VO / enum) | `Email`, `ExternalIdentity`, `IdentityMessages`, `SecurityUser`, `UserExistence`, `UserId`, `UserMustExistRule`, `UserMustExistRuleImpl` |
| Caso de uso (impl) | `AssignTenantUseCaseImpl`, `ListUsersUseCaseImpl`, `ProvisionIdentityUseCaseImpl` |
| Caso de uso (contrato) | `AssignTenantUseCase`, `ListUsersUseCase`, `ProvisionIdentityUseCase` |
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
| Otro | `AssignTenantRequest`, `ProvisionIdentityRequest`, `SecurityUserRepository`, `UserResponse` |

### `resources` - 44 clases

| Rol | Clases |
|---|---|
| Excepcion de dominio | `DuplicateProtectedResourceException`, `InvalidResourcePathException`, `ProtectedResourceNotFoundException`, `UnsupportedHttpMethodException` |
| Evento de dominio | `ProtectedResourceRegistered` |
| Dominio (entidad / VO / enum) | `HttpVerb`, `ProtectedResource`, `ProtectedResourceAvailability`, `ProtectedResourceExistence`, `ProtectedResourceMustBeUniqueRule`, `ProtectedResourceMustBeUniqueRuleImpl`, `ProtectedResourceMustExistRule`, `ProtectedResourceMustExistRuleImpl`, `ResourcePath`, `ResourcesMessages` |
| Caso de uso (impl) | `ListProtectedResourcesUseCaseImpl`, `RegisterProtectedResourceUseCaseImpl` |
| Caso de uso (contrato) | `ListProtectedResourcesUseCase`, `RegisterProtectedResourceUseCase` |
| Regla (contrato) | `ProtectedResourceMustExistValidator`, `ProtectedResourceMustExistValidatorImpl`, `RegisterProtectedResourceRulesValidator`, `RegisterProtectedResourceRulesValidatorImpl` |
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
| Otro | `ProtectedResourceLookup`, `ProtectedResourceRepository`, `RegisteredProtectedResourceResponse`, `RegisterProtectedResourceRequest` |

### `tenants` - 42 clases

| Rol | Clases |
|---|---|
| Excepcion de dominio | `DuplicateTenantException`, `InvalidTenantNameException`, `TenantNotActiveException`, `TenantNotFoundException` |
| Dominio (entidad / VO / enum) | `Tenant`, `TenantActivation`, `TenantCodeAvailability`, `TenantCodeMustBeUniqueRule`, `TenantCodeMustBeUniqueRuleImpl`, `TenantExistence`, `TenantMustExistRule`, `TenantMustExistRuleImpl`, `TenantName`, `TenantsMessages`, `TenantStatus`, `TenantStatusMustBeActiveRule`, `TenantStatusMustBeActiveRuleImpl` |
| Caso de uso (impl) | `CreateTenantUseCaseImpl`, `ListTenantsUseCaseImpl` |
| Caso de uso (contrato) | `CreateTenantUseCase`, `ListTenantsUseCase` |
| Regla (contrato) | `TenantMustBeActiveValidator`, `TenantMustBeActiveValidatorImpl` |
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
| Otro | `CreateTenantRequest`, `TenantRepository`, `TenantResponse` |

---

## Capacidades tecnicas (`shared`)

| Subpaquete | Clases |
|---|---|
| `auth/model` | `OidcFlowIntent` |
| `auth/service` | `KeycloakOidcSessionService`, `OidcAuthenticationFailureHandler`, `OidcAuthenticationSuccessHandler`, `OidcAuthorizationFlowService`, `OidcFlowStateService`, `OidcRedirectPolicy` |
| `auth/web` | `KeycloakLoginController`, `KeycloakRegistrationController` |
| `config` | `EventPublisherConfiguration`, `KeycloakSecurityConfiguration`, `ProjectPackages`, `SecurityConfiguration`, `SharedPortsConfiguration`, `SurrealDbConfiguration` |
| `contract` | `Operation`, `OperationWithoutResult`, `ReactiveOperation`, `ReactiveOperationWithoutInput`, `ReactiveOperationWithoutResult`, `ReactiveStreamOperation` |
| `event` | `DomainEvent`, `DomainEventPublisher`, `SpringDomainEventPublisher` |
| `message` | `RequiredArgumentMessages` |
| `observability` | `ReactiveLogContext` |
| `persistence/surrealdb` | `SurrealDbClient`, `SurrealDbException`, `SurrealDbHealthIndicator`, `SurrealDbProperties`, `SurrealRecordId`, `SurrealSchemaInitializer` |
| `port` | `IdentifierGenerator`, `TimeProvider` |
| `security` | `ApiAccessDeniedHandler`, `ApiAuthenticationEntryPoint`, `CorsProperties`, `JwtSecurityProperties`, `KeycloakSessionProperties`, `LocalUserPrincipal`, `PdpPrincipal`, `SecurityContext` |
| `web` | `ApiResponse`, `CorrelationWebFilter`, `PageResponse`, `RequestContext`, `RequestFieldParser` |
| `web/exception` | `ConflictingRequestParametersException`, `MalformedRequestFieldException`, `MissingRequestFieldException`, `RequestContractException` |
| `web/exceptionhandler` | `ApiErrorHandler` |
| `web/message` | `WebContractMessages` |
| `web/session` | `KeycloakLogoutController`, `SessionController`, `SessionResponse` |

---

## Puertos de salida y sus implementaciones

| Puerto | Implementado por |
|---|---|

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
| GET | `/oauth2/authorization/keycloak` | `KeycloakLoginController` |
| GET | `/oauth2/authorization/keycloak/register` | `KeycloakRegistrationController` |

---

## Pruebas por area

| Area | Clases de prueba |
|---|---|
| `pdp/applications` | `ApplicationBaseUrlTests`, `ApplicationCatalogPropertiesTests`, `ApplicationControllerTests`, `ApplicationCriteriaTests`, `ApplicationHttpTests`, `ApplicationMustExistForTenantValidatorTests`, `ApplicationPersistenceMapperTests`, `ApplicationResponseMapperTests`, `ApplicationRuleTests`, `ListApplicationsRequestMapperTests`, `ListApplicationsUseCaseImplTests`, `RegisterApplicationRequestMapperTests`, `RegisterApplicationRulesValidatorTests`, `RegisterApplicationUseCaseImplTests`, `RemoveApplicationUseCaseImplTests` |
| `pdp/authorization` | `AccessDecisionResponseMapperTests`, `AuthorizationControllerTests`, `AuthorizationHttpTests`, `AuthorizeRequestMapperTests`, `AuthorizeUseCaseImplTests`, `DecisionStateTests` |
| `pdp/commons` | `PageWindowTests`, `ResultPageTests`, `ValueObjectTests` |
| `pdp/identity` | `AssignTenantRequestMapperTests`, `AssignTenantUseCaseImplTests`, `EmailTests`, `ProvisionIdentityUseCaseImplTests`, `SecurityUserPersistenceMapperTests`, `UserControllerTests`, `UserMustExistRuleTests`, `UserResponseMapperTests` |
| `pdp/resources` | `HttpVerbTests`, `InMemoryAuditAdapterTests`, `ProtectedResourceControllerTests`, `ProtectedResourceMustBeUniqueRuleTests`, `ProtectedResourceMustExistRuleTests`, `ProtectedResourceMustExistValidatorTests`, `ProtectedResourceResponseMapperTests`, `RegisterProtectedResourceRequestMapperTests`, `RegisterProtectedResourceRulesValidatorTests`, `RegisterProtectedResourceUseCaseImplTests`, `ResourcePathTests` |
| `pdp/tenants` | `CreateTenantRequestMapperTests`, `CreateTenantUseCaseImplTests`, `ListTenantsUseCaseImplTests`, `TenantCatalogPropertiesTests`, `TenantControllerTests`, `TenantMustBeActiveValidatorTests`, `TenantNameTests`, `TenantResponseMapperTests`, `TenantRuleTests` |
| `raiz` | `AbstractSurrealDbIntegrationTest`, `LayeredArchitectureTests`, `ModulithStructureTests`, `PdpApplicationTests` |
| `shared/auth` | `KeycloakLoginControllerTests`, `KeycloakOidcSessionServiceTests`, `KeycloakRegistrationControllerTests`, `OidcAuthenticationFailureHandlerTests`, `OidcAuthenticationSuccessHandlerTests`, `OidcAuthorizationFlowServiceTests` |
| `shared/config` | `CorsConfigurationTests`, `KeycloakSecurityConfigurationTests`, `SecurityConfigurationTests` |
| `shared/persistence` | `SurrealDbClientTests`, `SurrealRecordIdTests`, `SurrealRepositoryIntegrationTests` |
| `shared/security` | `ApiAccessDeniedHandlerTests`, `ApiAuthenticationEntryPointTests`, `CorsPropertiesTests`, `JwtSecurityPropertiesTests`, `PdpPrincipalSecurityContextTests`, `SecurityWebFilterChainTests`, `TestJwtSupport` |
| `shared/web` | `ApiErrorHandlerTests`, `CorrelationWebFilterTests`, `KeycloakLogoutControllerTests`, `SessionControllerTests`, `WebContractMessagesTests` |
