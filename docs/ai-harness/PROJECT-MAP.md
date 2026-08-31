# PROJECT-MAP

> Generado por `.claude/tools/mapa.ps1`. **No editar a mano** - se regenera desde el codigo.
> Es el nivel 0 del grafo de conocimiento: responde "que existe y donde va lo nuevo".

- Clases de produccion: **215**
- Clases de prueba: **64**
- Slices de negocio: **5** (applications, commons, identity, resources, tenants)

---

## Slices de negocio (`pdp`)

### `applications` - 40 clases

| Rol | Clases |
|---|---|
| Excepcion de dominio | `InvalidApplicationBaseUrlException` |
| Dominio (entidad / VO / enum) | `Application`, `ApplicationBaseUrl`, `ApplicationCriteria` |
| Caso de uso (impl) | `ListApplicationsUseCaseImpl`, `RegisterApplicationUseCaseImpl`, `RemoveApplicationUseCaseImpl` |
| Caso de uso (contrato) | `ListApplicationsUseCase`, `RegisterApplicationUseCase`, `RemoveApplicationUseCase` |
| Regla (impl) | `ApplicationNameMustBeUniqueForTenantRuleImpl`, `ApplicationNameMustNotBeReservedRuleImpl` |
| Regla (contrato) | `ApplicationNameMustBeUniqueForTenantRule`, `ApplicationNameMustNotBeReservedRule` |
| Coordinador de reglas | `RegisterApplicationRulesValidator`, `RegisterApplicationRulesValidatorImpl` |
| Puerto de salida | `ApplicationRepository` |
| DTO de entrada al nucleo | `ListApplicationsRequest`, `RegisterApplicationRequest` |
| DTO de salida del nucleo | `RegisteredApplicationResponse` |
| Excepcion de aplicacion | `ApplicationNotFoundException`, `DuplicateApplicationException`, `ReservedApplicationNameException` |
| Catalogo de mensajes | `ApplicationsMessages` |
| Controller | `ApplicationController` |
| DTO crudo HTTP | `ListApplicationsRawRequest`, `RegisterApplicationRawRequest` |
| DTO de respuesta HTTP | `ApplicationWebResponse` |
| Interactor (impl) | `ListApplicationsInteractorImpl`, `RegisterApplicationInteractorImpl` |
| Interactor (contrato) | `ListApplicationsInteractor`, `RegisterApplicationInteractor` |
| Mapper web | `ApplicationResponseMapper`, `ListApplicationsRequestMapper`, `RegisterApplicationRequestMapper` |
| Adaptador de repositorio | `SurrealApplicationRepository` |
| Esquema de tabla | `ApplicationSchema`, `SurrealApplicationSchemaInitializer` |
| Cableado (@Bean) | `ApplicationsConfiguration` |
| Propiedades | `ApplicationCatalogProperties` |

### `commons` - 16 clases

| Rol | Clases |
|---|---|
| Otro | `AggregateRoot`, `ApplicationId`, `ApplicationName`, `BusinessRuleViolationException`, `ConflictBusinessRuleException`, `DomainException`, `InvalidApplicationNameException`, `InvalidIdentifierException`, `InvalidPageWindowException`, `InvalidTenantIdException`, `InvalidValueException`, `PageWindow`, `ResourceId`, `ResultPage`, `TenantId`, `ValueObjectMessages` |

### `identity` - 31 clases

| Rol | Clases |
|---|---|
| Excepcion de dominio | `InvalidEmailException` |
| Dominio (entidad / VO / enum) | `Email`, `ExternalIdentity`, `SecurityUser`, `UserId` |
| Caso de uso (impl) | `AssignTenantUseCaseImpl`, `ListUsersUseCaseImpl`, `ProvisionIdentityUseCaseImpl` |
| Caso de uso (contrato) | `AssignTenantUseCase`, `ListUsersUseCase`, `ProvisionIdentityUseCase` |
| Puerto de salida | `SecurityUserRepository` |
| DTO de entrada al nucleo | `AssignTenantRequest`, `ProvisionIdentityRequest` |
| DTO de salida del nucleo | `UserResponse` |
| Excepcion de aplicacion | `UserNotFoundException` |
| Controller | `UserController` |
| DTO crudo HTTP | `AssignTenantBodyRequest`, `AssignTenantRawRequest` |
| DTO de respuesta HTTP | `UserWebResponse` |
| Interactor (impl) | `AssignTenantInteractorImpl`, `ListUsersInteractorImpl` |
| Interactor (contrato) | `AssignTenantInteractor`, `ListUsersInteractor` |
| Mapper web | `AssignTenantRequestMapper`, `UserResponseMapper` |
| Adaptador de repositorio | `SurrealSecurityUserRepository` |
| Esquema de tabla | `IdentitySchema`, `SurrealIdentitySchemaInitializer` |
| Cableado (@Bean) | `IdentityConfiguration` |
| Propiedades | `IdentityProvisioningProperties` |

### `resources` - 36 clases

| Rol | Clases |
|---|---|
| Excepcion de dominio | `InvalidResourcePathException`, `UnsupportedHttpMethodException` |
| Evento de dominio | `ProtectedResourceRegistered` |
| Dominio (entidad / VO / enum) | `HttpVerb`, `ProtectedResource`, `ResourcePath` |
| Caso de uso (impl) | `ListProtectedResourcesUseCaseImpl`, `RegisterProtectedResourceUseCaseImpl` |
| Caso de uso (contrato) | `ListProtectedResourcesUseCase`, `RegisterProtectedResourceUseCase` |
| Regla (impl) | `ProtectedResourceMustBeUniqueRuleImpl` |
| Regla (contrato) | `ProtectedResourceMustBeUniqueRule` |
| Coordinador de reglas | `RegisterProtectedResourceRulesValidator`, `RegisterProtectedResourceRulesValidatorImpl` |
| Puerto de salida | `ProtectedResourceRepository` |
| DTO de entrada al nucleo | `RegisterProtectedResourceRequest` |
| DTO de salida del nucleo | `RegisteredProtectedResourceResponse` |
| Excepcion de aplicacion | `DuplicateProtectedResourceException` |
| Catalogo de mensajes | `ResourcesMessages` |
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

### `tenants` - 37 clases

| Rol | Clases |
|---|---|
| Excepcion de dominio | `InvalidTenantNameException` |
| Dominio (entidad / VO / enum) | `Tenant`, `TenantName`, `TenantStatus` |
| Caso de uso (impl) | `CreateTenantUseCaseImpl`, `ListTenantsUseCaseImpl` |
| Caso de uso (contrato) | `CreateTenantUseCase`, `ListTenantsUseCase` |
| Regla (impl) | `TenantCodeMustBeUniqueRuleImpl`, `TenantMustBeActiveRuleImpl`, `TenantStatusMustBeActiveRuleImpl` |
| Regla (contrato) | `TenantCodeMustBeUniqueRule`, `TenantMustBeActiveRule`, `TenantStatusMustBeActiveRule` |
| Puerto de salida | `TenantRepository` |
| DTO de entrada al nucleo | `CreateTenantRequest` |
| DTO de salida del nucleo | `TenantResponse` |
| Excepcion de aplicacion | `DuplicateTenantException`, `TenantNotActiveException`, `TenantNotFoundException` |
| Catalogo de mensajes | `TenantsMessages` |
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
| `config` | `EventPublisherConfiguration`, `KeycloakSecurityConfiguration`, `ProjectPackages`, `SecurityConfiguration`, `SharedPortsConfiguration`, `SurrealDbConfiguration` |
| `contract` | `Operation`, `OperationWithoutResult`, `ReactiveOperation`, `ReactiveOperationWithoutInput`, `ReactiveOperationWithoutResult`, `ReactiveStreamOperation` |
| `event` | `DomainEvent`, `DomainEventPublisher`, `SpringDomainEventPublisher` |
| `message` | `RequiredArgumentMessages` |
| `observability` | `ReactiveLogContext` |
| `persistence/surrealdb` | `SurrealDbClient`, `SurrealDbException`, `SurrealDbProperties`, `SurrealRecordId` |
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
| `ApplicationRepository` | `SurrealApplicationRepository` |
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
| `pdp/applications` | `ApplicationBaseUrlTests`, `ApplicationCatalogPropertiesTests`, `ApplicationControllerTests`, `ApplicationCriteriaTests`, `ApplicationHttpTests`, `ApplicationRegistrationRuleTests`, `ApplicationResponseMapperTests`, `ListApplicationsRequestMapperTests`, `ListApplicationsUseCaseImplTests`, `RegisterApplicationRequestMapperTests`, `RegisterApplicationUseCaseImplTests`, `RemoveApplicationUseCaseImplTests` |
| `pdp/commons` | `PageWindowTests`, `ResultPageTests`, `ValueObjectTests` |
| `pdp/identity` | `AssignTenantRequestMapperTests`, `AssignTenantUseCaseImplTests`, `EmailTests`, `ProvisionIdentityUseCaseImplTests`, `UserControllerTests`, `UserResponseMapperTests` |
| `pdp/resources` | `HttpVerbTests`, `InMemoryAuditAdapterTests`, `ProtectedResourceControllerTests`, `ProtectedResourceMustBeUniqueRuleImplTests`, `ProtectedResourceResponseMapperTests`, `RegisterProtectedResourceRequestMapperTests`, `RegisterProtectedResourceUseCaseImplTests`, `ResourcePathTests` |
| `pdp/tenants` | `CreateTenantRequestMapperTests`, `CreateTenantUseCaseImplTests`, `TenantCatalogPropertiesTests`, `TenantControllerTests`, `TenantMustBeActiveRuleImplTests`, `TenantNameTests`, `TenantResponseMapperTests` |
| `raiz` | `AbstractSurrealDbIntegrationTest`, `LayeredArchitectureTests`, `ModulithStructureTests`, `PdpApplicationTests` |
| `shared/auth` | `KeycloakLoginControllerTests`, `KeycloakOidcSessionServiceTests`, `KeycloakRegistrationControllerTests`, `OidcAuthenticationFailureHandlerTests`, `OidcAuthenticationSuccessHandlerTests`, `OidcAuthorizationFlowServiceTests` |
| `shared/config` | `CorsConfigurationTests`, `KeycloakSecurityConfigurationTests`, `SecurityConfigurationTests` |
| `shared/persistence` | `SurrealDbClientTests`, `SurrealRecordIdTests`, `SurrealRepositoryIntegrationTests` |
| `shared/security` | `ApiAccessDeniedHandlerTests`, `ApiAuthenticationEntryPointTests`, `CorsPropertiesTests`, `JwtSecurityPropertiesTests`, `PdpPrincipalSecurityContextTests`, `SecurityWebFilterChainTests`, `TestJwtSupport` |
| `shared/web` | `ApiErrorHandlerTests`, `CorrelationWebFilterTests`, `KeycloakLogoutControllerTests`, `SessionControllerTests`, `WebContractMessagesTests` |
