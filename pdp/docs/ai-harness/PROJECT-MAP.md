# PROJECT-MAP

> Generado por `.claude/tools/mapa.ps1`. **No editar a mano** - se regenera desde el codigo.
> Es el nivel 0 del grafo de conocimiento: responde "que existe y donde va lo nuevo".

- Clases de produccion: **570**
- Clases de prueba: **180**
- Slices de negocio: **9** (applications, assignments, authorization, commons, identity, profiles, resources, roles, tenants)

---

## Slices de negocio (`pdp`)

### `applications` - 76 clases

| Rol | Clases |
|---|---|
| Excepcion de dominio | `ApplicationNotFoundException`, `DuplicateApplicationException`, `InvalidApplicationBaseUrlException`, `InvalidApplicationCredentialException`, `InvalidApplicationCredentialHashException`, `ReservedApplicationNameException` |
| Catalogo de mensajes | `ApplicationsMessages` |
| Value object | `ApplicationBaseUrl`, `ApplicationCredentialHash` |
| Regla de dominio (impl) | `ApplicationCredentialMustBeValidRuleImpl`, `ApplicationMustExistForTenantRuleImpl`, `ApplicationNameMustBeUniqueForTenantRuleImpl`, `ApplicationNameMustNotBeReservedRuleImpl` |
| Dato de regla (hecho resuelto) | `ApplicationCredentialValidity`, `ApplicationExistence`, `ApplicationNameAvailability` |
| Regla de dominio (contrato) | `ApplicationCredentialMustBeValidRule`, `ApplicationMustExistForTenantRule`, `ApplicationNameMustBeUniqueForTenantRule`, `ApplicationNameMustNotBeReservedRule` |
| Dominio (agregado / criteria) | `Application`, `ApplicationCriteria` |
| Caso de uso (impl) | `ListApplicationsUseCaseImpl`, `RegisterApplicationUseCaseImpl`, `RemoveApplicationUseCaseImpl`, `RotateApplicationCredentialUseCaseImpl`, `ValidateApplicationCredentialUseCaseImpl` |
| Caso de uso (contrato) | `ListApplicationsUseCase`, `RegisterApplicationUseCase`, `RemoveApplicationUseCase`, `RotateApplicationCredentialUseCase`, `ValidateApplicationCredentialUseCase` |
| Validador de reglas (impl) | `ApplicationMustExistForTenantValidatorImpl`, `ApplicationOwnerLookupValidatorImpl`, `RegisterApplicationRulesValidatorImpl` |
| Validador de reglas (contrato) | `ApplicationMustExistForTenantValidator`, `ApplicationOwnerLookupValidator`, `RegisterApplicationRulesValidator` |
| Puerto de salida | `ApplicationRepository` |
| DTO de entrada al nucleo | `ApplicationOwnershipQuery`, `ListApplicationsRequest`, `RegisterApplicationRequest`, `RotateApplicationCredentialRequest`, `ValidateApplicationCredentialRequest` |
| DTO de salida del nucleo | `ApplicationRegistrationResponse`, `RegisteredApplicationResponse` |
| Controller | `ApplicationController`, `InternalApplicationCredentialController` |
| DTO crudo HTTP | `ListApplicationsRawRequest`, `RegisterApplicationRawRequest`, `RotateApplicationCredentialRawRequest`, `ValidateApplicationCredentialRawRequest` |
| DTO de respuesta HTTP | `ApplicationCredentialValidationWebResponse`, `ApplicationRegisteredWebResponse`, `ApplicationWebResponse` |
| Interactor (impl) | `ListApplicationsInteractorImpl`, `RegisterApplicationInteractorImpl`, `RotateApplicationCredentialInteractorImpl`, `ValidateApplicationCredentialInteractorImpl` |
| Interactor (contrato) | `ListApplicationsInteractor`, `RegisterApplicationInteractor`, `RotateApplicationCredentialInteractor`, `ValidateApplicationCredentialInteractor` |
| Mapper web | `ApplicationCredentialValidationResponseMapper`, `ApplicationResponseMapper`, `ListApplicationsRequestMapper`, `RegisterApplicationRequestMapper`, `RotateApplicationCredentialRequestMapper`, `ValidateApplicationCredentialRequestMapper` |
| Entidad de persistencia | `ApplicationEntity` |
| Mapper de persistencia | `ApplicationPersistenceMapper` |
| Adaptador de repositorio | `SurrealApplicationRepository` |
| Esquema de tabla | `ApplicationSchema`, `SurrealApplicationSchemaInitializer` |
| Cableado (@Bean) | `ApplicationsConfiguration` |
| Propiedades | `ApplicationCatalogProperties` |

### `assignments` - 92 clases

| Rol | Clases |
|---|---|
| Excepcion de dominio | `AssignmentNotFoundException`, `DuplicateAssignmentException`, `DuplicateProfileAssignmentException`, `InvalidValidityException`, `ProfileAssignmentNotFoundException` |
| Catalogo de mensajes | `AssignmentsMessages` |
| Value object | `AssignmentId`, `ProfileAssignmentId`, `Validity` |
| Regla de dominio (impl) | `AssignmentMustExistForTenantRuleImpl`, `AssignmentMustNotDuplicateActiveRuleImpl`, `ProfileAssignmentMustExistForTenantRuleImpl`, `ProfileAssignmentMustNotDuplicateActiveRuleImpl` |
| Dato de regla (hecho resuelto) | `ActiveAssignmentAvailability`, `ActiveProfileAssignmentAvailability`, `AssignmentExistence`, `ProfileAssignmentExistence` |
| Regla de dominio (contrato) | `AssignmentMustExistForTenantRule`, `AssignmentMustNotDuplicateActiveRule`, `ProfileAssignmentMustExistForTenantRule`, `ProfileAssignmentMustNotDuplicateActiveRule` |
| Dominio (agregado / criteria) | `Assignment`, `AssignmentCriteria`, `ProfileAssignment` |
| Caso de uso (impl) | `AssignProfileUseCaseImpl`, `AssignRoleUseCaseImpl`, `ListAssignmentsUseCaseImpl`, `ResolveActiveRolesUseCaseImpl`, `RevokeAssignmentUseCaseImpl`, `RevokeProfileAssignmentUseCaseImpl` |
| Caso de uso (contrato) | `AssignProfileUseCase`, `AssignRoleUseCase`, `ListAssignmentsUseCase`, `ResolveActiveRolesUseCase`, `RevokeAssignmentUseCase`, `RevokeProfileAssignmentUseCase` |
| Validador de reglas (impl) | `AssignProfileRulesValidatorImpl`, `AssignRoleRulesValidatorImpl`, `RevokeAssignmentRulesValidatorImpl`, `RevokeProfileAssignmentRulesValidatorImpl` |
| Validador de reglas (contrato) | `AssignProfileRulesValidator`, `AssignRoleRulesValidator`, `RevokeAssignmentRulesValidator`, `RevokeProfileAssignmentRulesValidator` |
| Puerto de salida | `AssignmentRepository`, `ProfileAssignmentRepository` |
| DTO de entrada al nucleo | `AssignProfileRequest`, `AssignRoleRequest`, `ListAssignmentsRequest`, `ResolveActiveRolesRequest`, `RevokeAssignmentRequest`, `RevokeProfileAssignmentRequest` |
| DTO de salida del nucleo | `ActiveRolesResponse`, `AssignmentResponse`, `ProfileAssignmentResponse` |
| Controller | `AssignmentController`, `ProfileAssignmentController` |
| DTO crudo HTTP | `AssignProfileRawRequest`, `AssignRoleRawRequest`, `ListAssignmentsRawRequest`, `RevokeAssignmentRawRequest`, `RevokeProfileAssignmentRawRequest` |
| DTO de respuesta HTTP | `AssignmentWebResponse`, `ProfileAssignmentWebResponse` |
| Interactor (impl) | `AssignProfileInteractorImpl`, `AssignRoleInteractorImpl`, `ListAssignmentsInteractorImpl`, `RevokeAssignmentInteractorImpl`, `RevokeProfileAssignmentInteractorImpl` |
| Interactor (contrato) | `AssignProfileInteractor`, `AssignRoleInteractor`, `ListAssignmentsInteractor`, `RevokeAssignmentInteractor`, `RevokeProfileAssignmentInteractor` |
| Mapper web | `AssignmentResponseMapper`, `AssignProfileRequestMapper`, `AssignRoleRequestMapper`, `ListAssignmentsRequestMapper`, `ProfileAssignmentResponseMapper`, `RevokeAssignmentRequestMapper`, `RevokeProfileAssignmentRequestMapper` |
| Entidad de persistencia | `AssignmentEntity`, `ProfileAssignmentEntity` |
| Mapper de persistencia | `AssignmentPersistenceMapper`, `ProfileAssignmentPersistenceMapper` |
| Adaptador de repositorio | `SurrealAssignmentRepository`, `SurrealProfileAssignmentRepository` |
| Esquema de tabla | `AssignmentSchema`, `ProfileAssignmentSchema`, `SurrealAssignmentSchemaInitializer`, `SurrealProfileAssignmentSchemaInitializer` |
| Cableado (@Bean) | `AssignmentsConfiguration` |

### `authorization` - 60 clases

| Rol | Clases |
|---|---|
| Excepcion de dominio | `NotAuthorizedToAdministerException` |
| Catalogo de mensajes | `AuthorizationMessages` |
| Value object | `DecisionState`, `PolicyReference`, `ReasonCode` |
| Evento de dominio | `AccessEvent` |
| Caso de uso (impl) | `AuthorizeAdministrationUseCaseImpl`, `AuthorizeUseCaseImpl`, `EvaluateInternalAccessUseCaseImpl` |
| Caso de uso (contrato) | `AuthorizeAdministrationUseCase`, `AuthorizeUseCase`, `EvaluateInternalAccessUseCase` |
| Validador de reglas (impl) | `ActiveRoleNamesLookupValidatorImpl`, `PrincipalMustBeApplicationAdministratorValidatorImpl` |
| Validador de reglas (contrato) | `ActiveRoleNamesLookupValidator`, `PrincipalMustBeApplicationAdministratorValidator` |
| Puerto de salida | `AccessAuditRepository`, `AdministrationDecisionPort`, `PolicyDecisionPort` |
| DTO de entrada al nucleo | `AccessRequest`, `AdministrationRequest`, `InternalAccessRequest` |
| DTO de salida del nucleo | `AccessDecision`, `AdministrationDecision` |
| Controller | `AuthorizationController`, `InternalAccessDecisionController` |
| DTO crudo HTTP | `AccessDecisionRawRequest`, `AuthorizeRawRequest` |
| DTO de respuesta HTTP | `AccessDecisionInternalWebResponse`, `AccessDecisionWebResponse`, `PolicyReferenceWebResponse` |
| Interactor (impl) | `AuthorizeInteractorImpl`, `InternalAccessDecisionInteractorImpl` |
| Interactor (contrato) | `AuthorizeInteractor`, `InternalAccessDecisionInteractor` |
| Mapper web | `AccessDecisionInternalResponseMapper`, `AccessDecisionRawRequestMapper`, `AccessDecisionResponseMapper`, `AuthorizeRequestMapper` |
| Entidad de persistencia | `AccessEventEntity` |
| Mapper de persistencia | `AccessEventPersistenceMapper` |
| Adaptador de repositorio | `SurrealAccessAuditRepository` |
| Esquema de tabla | `AccessEventSchema`, `SurrealAccessEventSchemaInitializer` |
| Cableado (@Bean) | `AuthorizationConfiguration` |
| Propiedades | `OpaProperties` |
| Otro | `OpaAdministrationDecisionAdapter`, `OpaAdministrationEvaluationInput`, `OpaAdministrationEvaluationRequest`, `OpaApplication`, `OpaEvaluationInput`, `OpaEvaluationRequest`, `OpaPolicyDecisionAdapter`, `OpaPolicyDecisionPayload`, `OpaPolicyReference`, `OpaRequestInfo`, `OpaResource`, `OpaResponse`, `OpaSubject`, `OpaTenant` |

### `commons` - 19 clases

| Rol | Clases |
|---|---|
| Excepcion de dominio | `BusinessRuleViolationException`, `ConflictBusinessRuleException`, `DomainException`, `InvalidApplicationNameException`, `InvalidIdentifierException`, `InvalidPageWindowException`, `InvalidTenantIdException`, `InvalidValueException` |
| Catalogo de mensajes | `ValueObjectMessages` |
| Value object | `ApplicationId`, `ApplicationName`, `PageWindow`, `ProfileId`, `ResourceId`, `ResultPage`, `RoleId`, `TenantId`, `UserId` |
| Otro | `AggregateRoot` |

### `identity` - 39 clases

| Rol | Clases |
|---|---|
| Excepcion de dominio | `InvalidEmailException`, `UserNotFoundException` |
| Catalogo de mensajes | `IdentityMessages` |
| Value object | `Email`, `ExternalIdentity` |
| Regla de dominio (impl) | `UserMustExistRuleImpl` |
| Dato de regla (hecho resuelto) | `UserExistence` |
| Regla de dominio (contrato) | `UserMustExistRule` |
| Dominio (agregado / criteria) | `SecurityUser` |
| Caso de uso (impl) | `AssignTenantUseCaseImpl`, `ListUsersUseCaseImpl`, `ProvisionIdentityUseCaseImpl` |
| Caso de uso (contrato) | `AssignTenantUseCase`, `ListUsersUseCase`, `ProvisionIdentityUseCase` |
| Validador de reglas (impl) | `UserMustExistValidatorImpl` |
| Validador de reglas (contrato) | `UserMustExistValidator` |
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

### `profiles` - 52 clases

| Rol | Clases |
|---|---|
| Excepcion de dominio | `DuplicateProfileNameException`, `InvalidProfileNameException`, `ProfileNotFoundException` |
| Catalogo de mensajes | `ProfilesMessages` |
| Value object | `ProfileName` |
| Regla de dominio (impl) | `ProfileMustExistForTenantRuleImpl`, `ProfileNameMustBeUniqueInScopeRuleImpl` |
| Dato de regla (hecho resuelto) | `ProfileExistence`, `ProfileNameAvailability` |
| Regla de dominio (contrato) | `ProfileMustExistForTenantRule`, `ProfileNameMustBeUniqueInScopeRule` |
| Dominio (agregado / criteria) | `Profile`, `ProfileCriteria` |
| Caso de uso (impl) | `AddRoleToProfileUseCaseImpl`, `DefineProfileUseCaseImpl`, `ListProfilesUseCaseImpl` |
| Caso de uso (contrato) | `AddRoleToProfileUseCase`, `DefineProfileUseCase`, `ListProfilesUseCase` |
| Validador de reglas (impl) | `AddRoleToProfileRulesValidatorImpl`, `DefineProfileRulesValidatorImpl`, `ProfileRolesLookupValidatorImpl` |
| Validador de reglas (contrato) | `AddRoleToProfileRulesValidator`, `DefineProfileRulesValidator`, `ProfileRolesLookupValidator` |
| Puerto de salida | `ProfileRepository` |
| DTO de entrada al nucleo | `AddRoleToProfileRequest`, `DefineProfileRequest`, `ListProfilesRequest`, `ProfileOwnershipQuery` |
| DTO de salida del nucleo | `ProfileResponse` |
| Controller | `ProfileController` |
| DTO crudo HTTP | `AddRoleToProfileRawRequest`, `DefineProfileRawRequest`, `ListProfilesRawRequest` |
| DTO de respuesta HTTP | `ProfileWebResponse` |
| Interactor (impl) | `AddRoleToProfileInteractorImpl`, `DefineProfileInteractorImpl`, `ListProfilesInteractorImpl` |
| Interactor (contrato) | `AddRoleToProfileInteractor`, `DefineProfileInteractor`, `ListProfilesInteractor` |
| Mapper web | `AddRoleToProfileRequestMapper`, `DefineProfileRequestMapper`, `ListProfilesRequestMapper`, `ProfileResponseMapper` |
| Entidad de persistencia | `ProfileEntity` |
| Mapper de persistencia | `ProfilePersistenceMapper` |
| Adaptador de repositorio | `SurrealProfileRepository` |
| Esquema de tabla | `ProfileSchema`, `SurrealProfileSchemaInitializer` |
| Cableado (@Bean) | `ProfilesConfiguration` |

### `resources` - 57 clases

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
| Caso de uso (impl) | `ListProtectedResourcesUseCaseImpl`, `RegisterApplicationWithInitialResourceUseCaseImpl`, `RegisterProtectedResourceUseCaseImpl` |
| Caso de uso (contrato) | `ListProtectedResourcesUseCase`, `RegisterApplicationWithInitialResourceUseCase`, `RegisterProtectedResourceUseCase` |
| Validador de reglas (impl) | `ProtectedResourceMustExistValidatorImpl`, `ProtectedResourceOwnerLookupValidatorImpl`, `RegisterProtectedResourceRulesValidatorImpl` |
| Validador de reglas (contrato) | `ProtectedResourceMustExistValidator`, `ProtectedResourceOwnerLookupValidator`, `RegisterProtectedResourceRulesValidator` |
| Puerto de salida | `ProtectedResourceRepository` |
| DTO de entrada al nucleo | `ProtectedResourceLookup`, `RegisterApplicationWithInitialResourceRequest`, `RegisterProtectedResourceRequest` |
| DTO de salida del nucleo | `ApplicationWithInitialResourceRegistrationResponse`, `RegisteredProtectedResourceResponse` |
| Controller | `ApplicationWithInitialResourceController`, `ProtectedResourceController` |
| DTO crudo HTTP | `RegisterApplicationWithInitialResourceRawRequest`, `RegisterProtectedResourceBodyRequest`, `RegisterProtectedResourceRawRequest` |
| DTO de respuesta HTTP | `ApplicationWithInitialResourceWebResponse`, `ProtectedResourceWebResponse` |
| Interactor (impl) | `ListProtectedResourcesInteractorImpl`, `RegisterApplicationWithInitialResourceInteractorImpl`, `RegisterProtectedResourceInteractorImpl` |
| Interactor (contrato) | `ListProtectedResourcesInteractor`, `RegisterApplicationWithInitialResourceInteractor`, `RegisterProtectedResourceInteractor` |
| Mapper web | `ApplicationWithInitialResourceResponseMapper`, `ProtectedResourceResponseMapper`, `RegisterApplicationWithInitialResourceRequestMapper`, `RegisterProtectedResourceRequestMapper` |
| Entidad de persistencia | `ProtectedResourceEntity` |
| Mapper de persistencia | `ProtectedResourcePersistenceMapper` |
| Adaptador de repositorio | `SurrealProtectedResourceRepository` |
| Esquema de tabla | `ProtectedResourceSchema`, `SurrealProtectedResourceSchemaInitializer` |
| Adaptador de auditoria | `InMemoryAuditAdapter` |
| Cableado (@Bean) | `ResourcesConfiguration` |

### `roles` - 68 clases

| Rol | Clases |
|---|---|
| Excepcion de dominio | `ApplicationOutsideRoleScopeException`, `DuplicateRoleNameException`, `InvalidRoleNameException`, `InvalidRoleScopeException`, `ResourceOutsideRoleScopeException`, `RoleNotFoundException` |
| Catalogo de mensajes | `RolesMessages` |
| Value object | `RoleName`, `RoleScope`, `RoleScopeLevel` |
| Regla de dominio (impl) | `RoleMustExistForTenantRuleImpl`, `RoleNameMustBeUniqueInScopeRuleImpl`, `RoleScopeMustCoverApplicationRuleImpl`, `RoleScopeMustCoverResourceRuleImpl` |
| Dato de regla (hecho resuelto) | `ApplicationCoverage`, `ResourceCoverage`, `RoleExistence`, `RoleNameAvailability` |
| Regla de dominio (contrato) | `RoleMustExistForTenantRule`, `RoleNameMustBeUniqueInScopeRule`, `RoleScopeMustCoverApplicationRule`, `RoleScopeMustCoverResourceRule` |
| Dominio (agregado / criteria) | `Role`, `RoleCriteria` |
| Caso de uso (impl) | `DefineRoleUseCaseImpl`, `GrantResourceToRoleUseCaseImpl`, `ListRolesUseCaseImpl` |
| Caso de uso (contrato) | `DefineRoleUseCase`, `GrantResourceToRoleUseCase`, `ListRolesUseCase` |
| Validador de reglas (impl) | `DefineRoleRulesValidatorImpl`, `GrantResourceRulesValidatorImpl`, `RoleMustExistForTenantValidatorImpl`, `RoleNamesLookupValidatorImpl`, `RoleScopeMustCoverApplicationValidatorImpl` |
| Validador de reglas (contrato) | `DefineRoleRulesValidator`, `GrantResourceRulesValidator`, `RoleMustExistForTenantValidator`, `RoleNamesLookupValidator`, `RoleScopeMustCoverApplicationValidator` |
| Puerto de salida | `RoleRepository` |
| DTO de entrada al nucleo | `DefineRoleRequest`, `GrantResourceRequest`, `ListRolesRequest`, `RoleCoverageQuery`, `RoleOwnershipQuery` |
| DTO de salida del nucleo | `RoleResponse` |
| Controller | `RoleController` |
| DTO crudo HTTP | `DefineRoleRawRequest`, `GrantResourceRawRequest`, `ListRolesRawRequest` |
| DTO de respuesta HTTP | `RoleWebResponse` |
| Interactor (impl) | `DefineRoleInteractorImpl`, `GrantResourceToRoleInteractorImpl`, `ListRolesInteractorImpl` |
| Interactor (contrato) | `DefineRoleInteractor`, `GrantResourceToRoleInteractor`, `ListRolesInteractor` |
| Mapper web | `DefineRoleRequestMapper`, `GrantResourceRequestMapper`, `ListRolesRequestMapper`, `RoleResponseMapper` |
| Entidad de persistencia | `RoleEntity` |
| Mapper de persistencia | `RolePersistenceMapper` |
| Adaptador de repositorio | `SurrealRoleRepository` |
| Esquema de tabla | `RoleSchema`, `SurrealRoleSchemaInitializer` |
| Cableado (@Bean) | `RolesConfiguration` |

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
| `config` | `EventPublisherConfiguration`, `InternalSecurityConfiguration`, `KeycloakSecurityConfiguration`, `ProjectPackages`, `SecurityConfiguration`, `SharedPortsConfiguration`, `SurrealDbConfiguration`, `TelemetryConfiguration` |
| `contract` | `Operation`, `OperationWithoutResult`, `ReactiveOperation`, `ReactiveOperationWithoutInput`, `ReactiveOperationWithoutResult`, `ReactiveStreamOperation` |
| `event` | `DomainEvent`, `DomainEventPublisher`, `SpringDomainEventPublisher` |
| `message` | `RequiredArgumentMessages` |
| `observability` | `ReactiveLogContext`, `ReactiveTelemetry` |
| `persistence/surrealdb` | `SurrealDbClient`, `SurrealDbException`, `SurrealDbHealthIndicator`, `SurrealDbProperties`, `SurrealRecordId`, `SurrealSchemaInitializer` |
| `port` | `CredentialHasher`, `IdentifierGenerator`, `SecretGenerator`, `TimeProvider` |
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
| `AccessAuditRepository` | `SurrealAccessAuditRepository` |
| `AdministrationDecisionPort` | `OpaAdministrationDecisionAdapter` |
| `ApplicationRepository` | `SurrealApplicationRepository` |
| `AssignmentRepository` | `SurrealAssignmentRepository` |
| `PolicyDecisionPort` | `OpaPolicyDecisionAdapter` |
| `ProfileAssignmentRepository` | `SurrealProfileAssignmentRepository` |
| `ProfileRepository` | `SurrealProfileRepository` |
| `ProtectedResourceRepository` | `SurrealProtectedResourceRepository` |
| `RoleRepository` | `SurrealRoleRepository` |
| `SecurityUserRepository` | `SurrealSecurityUserRepository` |
| `TenantRepository` | `SurrealTenantRepository` |

---

## Endpoints

| Verbo | Ruta | Controller |
|---|---|---|
| GET | `/api/v1/applications` | `ApplicationController` |
| POST | `/api/v1/applications` | `ApplicationController` |
| POST | `/api/v1/applications/{applicationId}/credential-rotations` | `ApplicationController` |
| GET | `/api/v1/applications/{applicationId}/resources` | `ProtectedResourceController` |
| POST | `/api/v1/applications/{applicationId}/resources` | `ProtectedResourceController` |
| POST | `/api/v1/applications/with-initial-resource` | `ApplicationWithInitialResourceController` |
| POST | `/api/v1/authorize` | `AuthorizationController` |
| GET | `/api/v1/profiles` | `ProfileController` |
| POST | `/api/v1/profiles` | `ProfileController` |
| POST | `/api/v1/profiles/{profileId}/assignments` | `ProfileAssignmentController` |
| DELETE | `/api/v1/profiles/{profileId}/assignments/{profileAssignmentId}` | `ProfileAssignmentController` |
| POST | `/api/v1/profiles/{profileId}/roles` | `ProfileController` |
| GET | `/api/v1/roles` | `RoleController` |
| POST | `/api/v1/roles` | `RoleController` |
| GET | `/api/v1/roles/{roleId}/assignments` | `AssignmentController` |
| POST | `/api/v1/roles/{roleId}/assignments` | `AssignmentController` |
| DELETE | `/api/v1/roles/{roleId}/assignments/{assignmentId}` | `AssignmentController` |
| POST | `/api/v1/roles/{roleId}/resources` | `RoleController` |
| GET | `/api/v1/session` | `SessionController` |
| GET | `/api/v1/session/logout` | `KeycloakLogoutController` |
| GET | `/api/v1/tenants` | `TenantController` |
| POST | `/api/v1/tenants` | `TenantController` |
| GET | `/api/v1/users` | `UserController` |
| PUT | `/api/v1/users/{id}/tenant` | `UserController` |
| POST | `/internal/v1/access-decisions` | `InternalAccessDecisionController` |
| POST | `/internal/v1/applications/{applicationId}/credential-validations` | `InternalApplicationCredentialController` |
| GET | `/oauth2/authorization/keycloak` | `KeycloakLoginController` |
| GET | `/oauth2/authorization/keycloak/register` | `KeycloakRegistrationController` |

---

## Pruebas por area

| Area | Clases de prueba |
|---|---|
| `pdp/applications` | `ApplicationBaseUrlTests`, `ApplicationCatalogPropertiesTests`, `ApplicationControllerTests`, `ApplicationCredentialHashTests`, `ApplicationCredentialMustBeValidRuleImplTests`, `ApplicationCredentialValidationResponseMapperTests`, `ApplicationCriteriaTests`, `ApplicationHttpTests`, `ApplicationMustExistForTenantValidatorTests`, `ApplicationOwnerLookupValidatorImplTests`, `ApplicationPersistenceMapperTests`, `ApplicationResponseMapperTests`, `ApplicationRuleTests`, `ApplicationTests`, `InternalApplicationCredentialControllerTests`, `ListApplicationsRequestMapperTests`, `ListApplicationsUseCaseImplTests`, `RegisterApplicationRequestMapperTests`, `RegisterApplicationRulesValidatorTests`, `RegisterApplicationUseCaseImplTests`, `RemoveApplicationUseCaseImplTests`, `RotateApplicationCredentialRequestMapperTests`, `RotateApplicationCredentialUseCaseImplTests`, `ValidateApplicationCredentialInteractorImplTests`, `ValidateApplicationCredentialRequestMapperTests`, `ValidateApplicationCredentialUseCaseImplTests` |
| `pdp/assignments` | `AssignmentControllerTests`, `AssignmentCriteriaTests`, `AssignmentHttpTests`, `AssignmentMustExistForTenantRuleImplTests`, `AssignmentMustNotDuplicateActiveRuleImplTests`, `AssignmentPersistenceMapperTests`, `AssignmentResponseMapperTests`, `AssignmentTests`, `AssignProfileRequestMapperTests`, `AssignProfileUseCaseImplTests`, `AssignRoleRequestMapperTests`, `AssignRoleRulesValidatorImplTests`, `AssignRoleUseCaseImplTests`, `ListAssignmentsRequestMapperTests`, `ListAssignmentsUseCaseImplTests`, `ProfileAssignmentControllerTests`, `ProfileAssignmentHttpTests`, `ProfileAssignmentMustExistForTenantRuleImplTests`, `ProfileAssignmentMustNotDuplicateActiveRuleImplTests`, `ProfileAssignmentPersistenceMapperTests`, `ProfileAssignmentResponseMapperTests`, `ProfileAssignmentTests`, `ResolveActiveRolesUseCaseImplTests`, `RevokeAssignmentRequestMapperTests`, `RevokeAssignmentRulesValidatorImplTests`, `RevokeAssignmentUseCaseImplTests`, `RevokeProfileAssignmentUseCaseImplTests`, `ValidityTests` |
| `pdp/authorization` | `AccessDecisionInternalResponseMapperTests`, `AccessDecisionRawRequestMapperTests`, `AccessDecisionResponseMapperTests`, `AccessEventPersistenceMapperTests`, `AccessEventTests`, `ActiveRoleNamesLookupValidatorImplTests`, `AuthorizationControllerTests`, `AuthorizationHttpTests`, `AuthorizeAdministrationUseCaseImplTests`, `AuthorizeRequestMapperTests`, `AuthorizeUseCaseImplTests`, `DecisionStateTests`, `EvaluateInternalAccessUseCaseImplTests`, `InternalAccessDecisionControllerTests`, `InternalAccessDecisionInteractorImplTests`, `InternalSecurityChainIntegrationTests`, `OpaAdministrationDecisionAdapterTests`, `OpaFixtureServer`, `OpaPolicyDecisionAdapterTests`, `PrincipalMustBeApplicationAdministratorValidatorImplTests` |
| `pdp/commons` | `PageWindowTests`, `ResultPageTests`, `ValueObjectTests` |
| `pdp/identity` | `AssignTenantRequestMapperTests`, `AssignTenantUseCaseImplTests`, `EmailTests`, `ProvisionIdentityUseCaseImplTests`, `SecurityUserPersistenceMapperTests`, `UserControllerTests`, `UserMustExistRuleTests`, `UserMustExistValidatorImplTests`, `UserResponseMapperTests` |
| `pdp/profiles` | `AddRoleToProfileRequestMapperTests`, `AddRoleToProfileUseCaseImplTests`, `DefineProfileRequestMapperTests`, `DefineProfileUseCaseImplTests`, `ListProfilesRequestMapperTests`, `ListProfilesUseCaseImplTests`, `ProfileControllerTests`, `ProfileCriteriaTests`, `ProfileHttpTests`, `ProfileMustExistForTenantRuleImplTests`, `ProfileNameMustBeUniqueInScopeRuleImplTests`, `ProfileNameTests`, `ProfilePersistenceMapperTests`, `ProfileResponseMapperTests`, `ProfileRolesLookupValidatorImplTests`, `ProfileTests` |
| `pdp/resources` | `ApplicationWithInitialResourceControllerTests`, `ApplicationWithInitialResourceHttpTests`, `HttpVerbTests`, `InMemoryAuditAdapterTests`, `ProtectedResourceControllerTests`, `ProtectedResourceMustBeUniqueRuleTests`, `ProtectedResourceMustExistRuleTests`, `ProtectedResourceMustExistValidatorTests`, `ProtectedResourceOwnerLookupValidatorImplTests`, `ProtectedResourceResponseMapperTests`, `RegisterApplicationWithInitialResourceRequestMapperTests`, `RegisterApplicationWithInitialResourceUseCaseImplTests`, `RegisterProtectedResourceRequestMapperTests`, `RegisterProtectedResourceRulesValidatorTests`, `RegisterProtectedResourceUseCaseImplTests`, `ResourcePathTests` |
| `pdp/roles` | `DefineRoleRequestMapperTests`, `DefineRoleRulesValidatorImplTests`, `DefineRoleUseCaseImplTests`, `GrantResourceRequestMapperTests`, `GrantResourceRulesValidatorImplTests`, `GrantResourceToRoleUseCaseImplTests`, `ListRolesRequestMapperTests`, `ListRolesUseCaseImplTests`, `RoleControllerTests`, `RoleCriteriaTests`, `RoleHttpTests`, `RoleMustExistForTenantRuleImplTests`, `RoleMustExistForTenantValidatorImplTests`, `RoleNameMustBeUniqueInScopeRuleImplTests`, `RoleNamesLookupValidatorImplTests`, `RoleNameTests`, `RolePersistenceMapperTests`, `RoleResponseMapperTests`, `RoleScopeLevelTests`, `RoleScopeMustCoverApplicationRuleImplTests`, `RoleScopeMustCoverApplicationValidatorImplTests`, `RoleScopeMustCoverResourceRuleImplTests`, `RoleScopeTests`, `RoleTests` |
| `pdp/tenants` | `CreateTenantRequestMapperTests`, `CreateTenantUseCaseImplTests`, `ListTenantsUseCaseImplTests`, `TenantCatalogPropertiesTests`, `TenantControllerTests`, `TenantMustBeActiveValidatorTests`, `TenantNameTests`, `TenantResponseMapperTests`, `TenantRuleTests` |
| `raiz` | `AbstractSurrealDbIntegrationTest`, `LayeredArchitectureTests`, `ModulithStructureTests`, `PdpApplicationTests` |
| `shared/auth` | `KeycloakLoginControllerTests`, `KeycloakOidcSessionServiceTests`, `KeycloakRegistrationControllerTests`, `OidcAuthenticationFailureHandlerTests`, `OidcAuthenticationSuccessHandlerTests`, `OidcAuthorizationFlowServiceTests` |
| `shared/config` | `CorsConfigurationTests`, `KeycloakSecurityConfigurationTests`, `SecurityConfigurationTests` |
| `shared/persistence` | `SurrealDbClientTests`, `SurrealRecordIdTests`, `SurrealRepositoryIntegrationTests` |
| `shared/security` | `ApiAccessDeniedHandlerTests`, `ApiAuthenticationEntryPointTests`, `CorsPropertiesTests`, `InternalMtlsWebFilterTests`, `JwtSecurityPropertiesTests`, `PdpPrincipalSecurityContextTests`, `SecurityWebFilterChainTests`, `TestJwtSupport` |
| `shared/web` | `ApiErrorHandlerTests`, `CorrelationWebFilterTests`, `KeycloakLogoutControllerTests`, `SessionControllerTests`, `WebContractMessagesTests` |
