# PROJECT-MAP

> Generado por `.claude/tools/mapa.ps1`. **No editar a mano** - se regenera desde el codigo.
> Es el nivel 0 del grafo de conocimiento: responde "que existe y donde va lo nuevo".

- Clases de produccion: **914**
- Clases de prueba: **256**
- Slices de negocio: **9** (applications, assignments, authorization, commons, identity, profiles, resources, roles, tenants)

---

## Slices de negocio (`pdp`)

### `applications` - 84 clases

| Rol | Clases |
|---|---|
| Excepcion de dominio | `AmbiguousApplicationNameException`, `ApplicationNotFoundException`, `ApplicationWithoutEnabledEnvironmentException`, `DuplicateApplicationException`, `InvalidApplicationBaseUrlException`, `InvalidApplicationCredentialException`, `InvalidApplicationCredentialHashException`, `InvalidApplicationEnvironmentException`, `ReservedApplicationNameException` |
| Catalogo de mensajes | `ApplicationsMessages` |
| Value object | `ApplicationBaseUrl`, `ApplicationCredentialHash`, `ApplicationEnvironment` |
| Regla de dominio (impl) | `ApplicationCredentialMustBeValidRuleImpl`, `ApplicationMustExistForTenantRuleImpl`, `ApplicationMustHaveAtLeastOneEnabledEnvironmentRuleImpl`, `ApplicationNameMustBeUniqueForTenantRuleImpl`, `ApplicationNameMustNotBeReservedRuleImpl` |
| Dato de regla (hecho resuelto) | `ApplicationCredentialValidity`, `ApplicationEnvironmentSelection`, `ApplicationExistence`, `ApplicationNameAvailability` |
| Regla de dominio (contrato) | `ApplicationCredentialMustBeValidRule`, `ApplicationMustExistForTenantRule`, `ApplicationMustHaveAtLeastOneEnabledEnvironmentRule`, `ApplicationNameMustBeUniqueForTenantRule`, `ApplicationNameMustNotBeReservedRule` |
| Dominio (agregado / criteria) | `Application`, `ApplicationCriteria` |
| Caso de uso (impl) | `ListApplicationsUseCaseImpl`, `RegisterApplicationUseCaseImpl`, `RemoveApplicationUseCaseImpl`, `RotateApplicationCredentialUseCaseImpl`, `UpdateApplicationUseCaseImpl`, `ValidateApplicationCredentialUseCaseImpl` |
| Caso de uso (contrato) | `ListApplicationsUseCase`, `RegisterApplicationUseCase`, `RemoveApplicationUseCase`, `RotateApplicationCredentialUseCase`, `UpdateApplicationUseCase`, `ValidateApplicationCredentialUseCase` |
| Validador de reglas (impl) | `ApplicationDetailsLookupValidatorImpl`, `ApplicationMustExistForTenantValidatorImpl`, `ApplicationNameForTenantLookupValidatorImpl`, `ApplicationNameLookupValidatorImpl`, `ApplicationOwnerLookupValidatorImpl`, `RegisterApplicationRulesValidatorImpl` |
| Validador de reglas (contrato) | `ApplicationDetailsLookupValidator`, `ApplicationMustExistForTenantValidator`, `ApplicationNameForTenantLookupValidator`, `ApplicationNameLookupValidator`, `ApplicationOwnerLookupValidator`, `RegisterApplicationRulesValidator` |
| Puerto de salida | `ApplicationRepository` |
| DTO de entrada al nucleo | `ApplicationNameForTenantQuery`, `ApplicationOwnershipQuery`, `ListApplicationsRequest`, `RegisterApplicationRequest`, `RotateApplicationCredentialRequest`, `UpdateApplicationRequest`, `ValidateApplicationCredentialRequest` |
| DTO de salida del nucleo | `ApplicationRegistrationResponse`, `RegisteredApplicationResponse` |
| Controller | `ApplicationController`, `InternalApplicationCredentialController` |
| DTO crudo HTTP | `ListApplicationsRawRequest`, `ValidateApplicationCredentialRawRequest` |
| DTO de respuesta HTTP | `ApplicationCredentialValidationWebResponse`, `ApplicationWebResponse` |
| Interactor (impl) | `ListApplicationsInteractorImpl`, `ValidateApplicationCredentialInteractorImpl` |
| Interactor (contrato) | `ListApplicationsInteractor`, `ValidateApplicationCredentialInteractor` |
| Mapper web | `ApplicationCredentialValidationResponseMapper`, `ApplicationResponseMapper`, `ListApplicationsRequestMapper`, `ValidateApplicationCredentialRequestMapper` |
| Entidad de persistencia | `ApplicationEntity` |
| Mapper de persistencia | `ApplicationPersistenceMapper` |
| Adaptador de repositorio | `SurrealApplicationRepository` |
| Esquema de tabla | `ApplicationSchema`, `SurrealApplicationSchemaInitializer` |
| Cableado (@Bean) | `ApplicationsConfiguration` |
| Propiedades | `ApplicationCatalogProperties` |

### `assignments` - 151 clases

| Rol | Clases |
|---|---|
| Excepcion de dominio | `AssignmentNotFoundException`, `CannotRemoveLastAdministratorException`, `DuplicateAssignmentException`, `DuplicateProfileAssignmentException`, `InvalidValidityException`, `ProfileAssignmentNotFoundException` |
| Catalogo de mensajes | `AssignmentsMessages` |
| Value object | `AssignmentId`, `ProfileAssignmentId`, `Validity` |
| Regla de dominio (impl) | `AssignmentMustExistForTenantRuleImpl`, `AssignmentMustNotDuplicateActiveRuleImpl`, `LastAdministratorMustNotBeRevokedRuleImpl`, `ProfileAssignmentMustExistForTenantRuleImpl`, `ProfileAssignmentMustNotDuplicateActiveRuleImpl` |
| Dato de regla (hecho resuelto) | `ActiveAssignmentAvailability`, `ActiveProfileAssignmentAvailability`, `AdministratorRevocationEligibility`, `AssignmentExistence`, `ProfileAssignmentExistence` |
| Regla de dominio (contrato) | `AssignmentMustExistForTenantRule`, `AssignmentMustNotDuplicateActiveRule`, `LastAdministratorMustNotBeRevokedRule`, `ProfileAssignmentMustExistForTenantRule`, `ProfileAssignmentMustNotDuplicateActiveRule` |
| Dominio (agregado / criteria) | `Assignment`, `AssignmentCriteria`, `ProfileAssignment`, `ProfileAssignmentCriteria` |
| Caso de uso (impl) | `AssignApplicationAdministratorUseCaseImpl`, `AssignProfileUseCaseImpl`, `AssignRoleUseCaseImpl`, `ListActiveProfileAssignmentsByProfilePageUseCaseImpl`, `ListActiveProfileAssignmentsByUserPageUseCaseImpl`, `ListActiveRoleAssignmentsByRolePageUseCaseImpl`, `ListActiveRoleAssignmentsByUserPageUseCaseImpl`, `ListApplicationAdministratorsPageUseCaseImpl`, `ListApplicationAdministratorsUseCaseImpl`, `ListApplicationProfileAssignmentsPageUseCaseImpl`, `ListApplicationRoleAssignmentsPageUseCaseImpl`, `ListAssignmentsUseCaseImpl`, `ListProfileAssignmentsUseCaseImpl`, `ReadApplicationAssignmentCountsUseCaseImpl`, `RegisterApplicationWithFirstAdministratorUseCaseImpl`, `RemoveApplicationAdministratorUseCaseImpl`, `ResolveActiveRolesUseCaseImpl`, `ResolveAuthorizationSubjectFactsUseCaseImpl`, `ResolveUserApplicationsUseCaseImpl`, `RevokeAssignmentUseCaseImpl`, `RevokeProfileAssignmentUseCaseImpl` |
| Caso de uso (contrato) | `AssignApplicationAdministratorUseCase`, `AssignProfileUseCase`, `AssignRoleUseCase`, `ListActiveProfileAssignmentsByProfilePageUseCase`, `ListActiveProfileAssignmentsByUserPageUseCase`, `ListActiveRoleAssignmentsByRolePageUseCase`, `ListActiveRoleAssignmentsByUserPageUseCase`, `ListApplicationAdministratorsPageUseCase`, `ListApplicationAdministratorsUseCase`, `ListApplicationProfileAssignmentsPageUseCase`, `ListApplicationRoleAssignmentsPageUseCase`, `ListAssignmentsUseCase`, `ListProfileAssignmentsUseCase`, `ReadApplicationAssignmentCountsUseCase`, `RegisterApplicationWithFirstAdministratorUseCase`, `RemoveApplicationAdministratorUseCase`, `ResolveActiveRolesUseCase`, `ResolveAuthorizationSubjectFactsUseCase`, `ResolveUserApplicationsUseCase`, `RevokeAssignmentUseCase`, `RevokeProfileAssignmentUseCase` |
| Validador de reglas (impl) | `ApplicationDeletionDependencyValidatorImpl`, `AssignmentApplicationLookupValidatorImpl`, `AssignProfileRulesValidatorImpl`, `AssignRoleRulesValidatorImpl`, `ProfileAssignmentApplicationLookupValidatorImpl`, `ProfileDeletionDependencyValidatorImpl`, `RevokeAssignmentRulesValidatorImpl`, `RevokeProfileAssignmentRulesValidatorImpl`, `RoleDeletionDependencyValidatorImpl` |
| Validador de reglas (contrato) | `ApplicationDeletionDependencyValidator`, `AssignmentApplicationLookupValidator`, `AssignProfileRulesValidator`, `AssignRoleRulesValidator`, `ProfileAssignmentApplicationLookupValidator`, `ProfileDeletionDependencyValidator`, `RevokeAssignmentRulesValidator`, `RevokeProfileAssignmentRulesValidator`, `RoleDeletionDependencyValidator` |
| Puerto de salida | `AssignmentRepository`, `ProfileAssignmentRepository` |
| DTO de entrada al nucleo | `ApplicationAssignmentCountsRequest`, `AssignApplicationAdministratorRequest`, `AssignmentOwnershipQuery`, `AssignProfileRequest`, `AssignRoleRequest`, `ListActiveAssignmentsPageRequest`, `ListApplicationAdministratorsRequest`, `ListApplicationAssignmentsPageRequest`, `ListAssignmentsRequest`, `ListProfileAssignmentsRequest`, `ProfileAssignmentOwnershipQuery`, `RegisterApplicationWithFirstAdministratorRequest`, `RemoveApplicationAdministratorRequest`, `ResolveActiveRolesRequest`, `ResolveAuthorizationSubjectFactsRequest`, `ResolveUserApplicationsRequest`, `RevokeAssignmentRequest`, `RevokeProfileAssignmentRequest` |
| DTO de salida del nucleo | `ActiveRolesResponse`, `ApplicationAssignmentCountsResponse`, `AssignmentResponse`, `AuthorizationSubjectFactsResponse`, `ProfileAssignmentResponse`, `UserApplicationAssignmentsResponse` |
| Controller | `ApplicationRegistrationController`, `AssignmentController`, `InternalApplicationAdministratorController`, `ProfileAssignmentController` |
| DTO crudo HTTP | `AssignApplicationAdministratorRawRequest`, `ListAssignmentsRawRequest`, `ListProfileAssignmentsRawRequest`, `RegisterApplicationWithFirstAdministratorRawRequest` |
| DTO de respuesta HTTP | `ApplicationRegisteredWebResponse`, `AssignmentWebResponse`, `ProfileAssignmentWebResponse` |
| Interactor (impl) | `AssignApplicationAdministratorInteractorImpl`, `ListAssignmentsInteractorImpl`, `ListProfileAssignmentsInteractorImpl`, `RegisterApplicationWithFirstAdministratorInteractorImpl` |
| Interactor (contrato) | `AssignApplicationAdministratorInteractor`, `ListAssignmentsInteractor`, `ListProfileAssignmentsInteractor`, `RegisterApplicationWithFirstAdministratorInteractor` |
| Mapper web | `ApplicationRegisteredResponseMapper`, `AssignmentResponseMapper`, `ListAssignmentsRequestMapper`, `ListProfileAssignmentsRequestMapper`, `ProfileAssignmentResponseMapper`, `RegisterApplicationWithFirstAdministratorRequestMapper` |
| Entidad de persistencia | `AssignmentEntity`, `ProfileAssignmentEntity` |
| Mapper de persistencia | `AssignmentPersistenceMapper`, `ProfileAssignmentPersistenceMapper` |
| Adaptador de repositorio | `SurrealAssignmentRepository`, `SurrealProfileAssignmentRepository` |
| Esquema de tabla | `AssignmentSchema`, `ProfileAssignmentSchema`, `SurrealAssignmentSchemaInitializer`, `SurrealProfileAssignmentSchemaInitializer` |
| Cableado (@Bean) | `AssignmentsConfiguration` |

### `authorization` - 254 clases

| Rol | Clases |
|---|---|
| Excepcion de dominio | `CurrentUserApplicationsAccessDeniedException`, `MfaEvidenceRequiredException`, `NotAuthorizedToAdministerException` |
| Catalogo de mensajes | `AuthorizationMessages` |
| Value object | `DecisionState`, `PolicyReference`, `ReasonCode` |
| Evento de dominio | `AccessEvent` |
| Caso de uso (impl) | `AdministerApplicationAdministratorAssignmentUseCaseImpl`, `AdministerApplicationAdministratorListUseCaseImpl`, `AdministerApplicationAdministratorRemovalUseCaseImpl`, `AdministerApplicationCredentialRotationUseCaseImpl`, `AdministerApplicationRemovalUseCaseImpl`, `AdministerAssignmentCreationUseCaseImpl`, `AdministerAssignmentRevocationUseCaseImpl`, `AdministerProfileAssignmentCreationUseCaseImpl`, `AdministerProfileAssignmentRevocationUseCaseImpl`, `AdministerProfileDefinitionUseCaseImpl`, `AdministerProfileRoleAdditionUseCaseImpl`, `AdministerResourceGrantUseCaseImpl`, `AdministerResourceRegistrationUseCaseImpl`, `AdministerRoleDefinitionUseCaseImpl`, `AuthorizeAdministrationUseCaseImpl`, `AuthorizeUseCaseImpl`, `EvaluateInternalAccessUseCaseImpl`, `ListApplicationProfileAssignmentsUseCaseImpl`, `ListApplicationRoleAssignmentsUseCaseImpl`, `ListApplicationSecurityAdministratorsUseCaseImpl`, `ListApplicationSecurityProfilesUseCaseImpl`, `ListApplicationSecurityResourcesUseCaseImpl`, `ListApplicationSecurityRolesUseCaseImpl`, `ListProfileRolesUseCaseImpl`, `ListRoleResourcesUseCaseImpl`, `ReadApplicationSecuritySummaryUseCaseImpl`, `ReadCurrentUserApplicationsUseCaseImpl`, `SearchApplicationSecurityUsersUseCaseImpl` |
| Caso de uso (contrato) | `AdministerApplicationAdministratorAssignmentUseCase`, `AdministerApplicationAdministratorListUseCase`, `AdministerApplicationAdministratorRemovalUseCase`, `AdministerApplicationCredentialRotationUseCase`, `AdministerApplicationRemovalUseCase`, `AdministerAssignmentCreationUseCase`, `AdministerAssignmentRevocationUseCase`, `AdministerProfileAssignmentCreationUseCase`, `AdministerProfileAssignmentRevocationUseCase`, `AdministerProfileDefinitionUseCase`, `AdministerProfileRoleAdditionUseCase`, `AdministerResourceGrantUseCase`, `AdministerResourceRegistrationUseCase`, `AdministerRoleDefinitionUseCase`, `AuthorizeAdministrationUseCase`, `AuthorizeUseCase`, `EvaluateInternalAccessUseCase`, `ListApplicationProfileAssignmentsUseCase`, `ListApplicationRoleAssignmentsUseCase`, `ListApplicationSecurityAdministratorsUseCase`, `ListApplicationSecurityProfilesUseCase`, `ListApplicationSecurityResourcesUseCase`, `ListApplicationSecurityRolesUseCase`, `ListProfileRolesUseCase`, `ListRoleResourcesUseCase`, `ReadApplicationSecuritySummaryUseCase`, `ReadCurrentUserApplicationsUseCase`, `SearchApplicationSecurityUsersUseCase` |
| Validador de reglas (impl) | `ActiveRoleNamesLookupValidatorImpl`, `MfaAwareApplicationAdministratorValidator`, `PrincipalMustBeApplicationAdministratorValidatorImpl` |
| Validador de reglas (contrato) | `ActiveRoleNamesLookupValidator`, `PrincipalMustBeApplicationAdministratorValidator` |
| Puerto de salida | `AccessAuditRepository`, `AdministrationDecisionPort`, `PolicyDecisionPort` |
| DTO de entrada al nucleo | `AccessRequest`, `AdministerApplicationAdministratorAssignmentRequest`, `AdministerApplicationAdministratorListRequest`, `AdministerApplicationAdministratorRemovalRequest`, `AdministerAssignmentCreationRequest`, `AdministerAssignmentRevocationRequest`, `AdministerProfileAssignmentCreationRequest`, `AdministerProfileAssignmentRevocationRequest`, `AdministerProfileDefinitionRequest`, `AdministerProfileRoleAdditionRequest`, `AdministerResourceGrantRequest`, `AdministerResourceRegistrationRequest`, `AdministerRoleDefinitionRequest`, `AdministrationRequest`, `InternalAccessRequest`, `ListApplicationProfileAssignmentsRequest`, `ListApplicationRoleAssignmentsRequest`, `ListApplicationSecurityAdministratorsRequest`, `ListApplicationSecurityProfilesRequest`, `ListApplicationSecurityResourcesRequest`, `ListApplicationSecurityRolesRequest`, `ListProfileRolesRequest`, `ListRoleResourcesRequest`, `PolicyEvaluationInput`, `ReadApplicationSecuritySummaryRequest`, `ReadCurrentUserApplicationsRequest`, `SearchApplicationSecurityUsersRequest` |
| DTO de salida del nucleo | `AccessDecision`, `AdministrationDecision`, `ApplicationSecuritySummaryResponse`, `CurrentUserApplicationsResponse` |
| Controller | `ApplicationAdministrationController`, `ApplicationAdministratorController`, `ApplicationSecurityController`, `AssignmentAdministrationController`, `AuthorizationController`, `BulkResourceController`, `CatalogMaintenanceController`, `CurrentUserApplicationsController`, `InternalAccessDecisionController`, `ProfileAdministrationController`, `ProfileAssignmentAdministrationController`, `ResourceAdministrationController`, `RoleAdministrationController` |
| DTO crudo HTTP | `AccessDecisionRawRequest`, `AddRoleToProfileRawRequest`, `ApplicationAdministrationRawRequest`, `ApplicationSecurityPageRawRequest`, `AssignApplicationAdministratorRawRequest`, `AssignProfileRawRequest`, `AssignRoleRawRequest`, `AuthorizeRawRequest`, `DefineProfileRawRequest`, `DefineRoleRawRequest`, `GrantResourceRawRequest`, `ListApplicationAdministratorsRawRequest`, `ListApplicationSecurityRawRequest`, `ListApplicationSecurityRelationRawRequest`, `RegisterProtectedResourceBodyRequest`, `RegisterProtectedResourceRawRequest`, `RemoveApplicationAdministratorRawRequest`, `RevokeAssignmentRawRequest`, `RevokeProfileAssignmentRawRequest`, `SearchApplicationSecurityUsersRawRequest` |
| DTO de respuesta HTTP | `AccessDecisionInternalWebResponse`, `AccessDecisionWebResponse`, `AdministeredApplicationWebResponse`, `AdministeredResourceWebResponse`, `ApplicationAdministratorWebResponse`, `ApplicationSecurityProfileAssignmentWebResponse`, `ApplicationSecurityProfileWebResponse`, `ApplicationSecurityResourceWebResponse`, `ApplicationSecurityRoleAssignmentWebResponse`, `ApplicationSecurityRoleWebResponse`, `ApplicationSecuritySummaryWebResponse`, `ApplicationSecurityUserAssignmentsWebResponse`, `ApplicationSecurityUserWebResponse`, `AssignmentAdministrationWebResponse`, `BulkResourceValidationResponse`, `CurrentUserApplicationsWebResponse`, `PolicyReferenceInternalWebResponse`, `PolicyReferenceWebResponse`, `ProfileAdministrationWebResponse`, `ProfileAssignmentAdministrationWebResponse`, `RoleAdministrationWebResponse` |
| Interactor (impl) | `AdministerApplicationAdministratorAssignmentInteractorImpl`, `AdministerApplicationAdministratorListInteractorImpl`, `AdministerApplicationAdministratorRemovalInteractorImpl`, `AdministerAssignmentCreationInteractorImpl`, `AdministerAssignmentRevocationInteractorImpl`, `AdministerProfileAssignmentCreationInteractorImpl`, `AdministerProfileAssignmentRevocationInteractorImpl`, `AdministerProfileDefinitionInteractorImpl`, `AdministerProfileRoleAdditionInteractorImpl`, `AdministerResourceGrantInteractorImpl`, `AdministerResourceRegistrationInteractorImpl`, `AdministerRoleDefinitionInteractorImpl`, `ApplicationCredentialRotationInteractorImpl`, `ApplicationRemovalInteractorImpl`, `AssignmentDetailReadInteractorImpl`, `AuthorizeInteractorImpl`, `InternalAccessDecisionInteractorImpl`, `ListApplicationProfileAssignmentsInteractorImpl`, `ListApplicationRoleAssignmentsInteractorImpl`, `ListApplicationSecurityAdministratorsInteractorImpl`, `ListApplicationSecurityProfilesInteractorImpl`, `ListApplicationSecurityResourcesInteractorImpl`, `ListApplicationSecurityRolesInteractorImpl`, `ListProfileRolesInteractorImpl`, `ListRoleResourcesInteractorImpl`, `ReadApplicationSecuritySummaryInteractorImpl`, `ReadCurrentUserApplicationsInteractorImpl`, `SearchApplicationSecurityUsersInteractorImpl` |
| Interactor (contrato) | `AdministerApplicationAdministratorAssignmentInteractor`, `AdministerApplicationAdministratorListInteractor`, `AdministerApplicationAdministratorRemovalInteractor`, `AdministerAssignmentCreationInteractor`, `AdministerAssignmentRevocationInteractor`, `AdministerProfileAssignmentCreationInteractor`, `AdministerProfileAssignmentRevocationInteractor`, `AdministerProfileDefinitionInteractor`, `AdministerProfileRoleAdditionInteractor`, `AdministerResourceGrantInteractor`, `AdministerResourceRegistrationInteractor`, `AdministerRoleDefinitionInteractor`, `ApplicationCredentialRotationInteractor`, `ApplicationRemovalInteractor`, `AssignmentDetailReadInteractor`, `AuthorizeInteractor`, `InternalAccessDecisionInteractor`, `ListApplicationProfileAssignmentsInteractor`, `ListApplicationRoleAssignmentsInteractor`, `ListApplicationSecurityAdministratorsInteractor`, `ListApplicationSecurityProfilesInteractor`, `ListApplicationSecurityResourcesInteractor`, `ListApplicationSecurityRolesInteractor`, `ListProfileRolesInteractor`, `ListRoleResourcesInteractor`, `ReadApplicationSecuritySummaryInteractor`, `ReadCurrentUserApplicationsInteractor`, `SearchApplicationSecurityUsersInteractor` |
| Mapper web | `AccessDecisionInternalResponseMapper`, `AccessDecisionRawRequestMapper`, `AccessDecisionResponseMapper`, `AdministeredApplicationResponseMapper`, `AdministeredResourceResponseMapper`, `ApplicationAdministrationRequestMapper`, `ApplicationSecurityPageRequestMapper`, `AuthorizeRequestMapper`, `DefineRoleRequestMapper`, `GrantResourceRequestMapper`, `RegisterProtectedResourceRequestMapper`, `RoleAdministrationResponseMapper` |
| Entidad de persistencia | `AccessEventEntity` |
| Mapper de persistencia | `AccessEventPersistenceMapper` |
| Adaptador de repositorio | `SurrealAccessAuditRepository`, `SurrealAdministrationAuditRepository` |
| Esquema de tabla | `AccessEventSchema`, `AdministrationEventSchema`, `SurrealAccessEventSchemaInitializer` |
| Cableado (@Bean) | `AuthorizationConfiguration` |
| Propiedades | `OpaProperties` |
| Otro | `AuthorizationContextResolver`, `AuthorizationContextResolverImpl`, `BulkResourceImportService`, `BulkResourceWorkbookService`, `ObservedAccessAuditRepository`, `ObservedAdministrationAuditRepository`, `OpaAdministrationDecisionAdapter`, `OpaAdministrationEvaluationInput`, `OpaAdministrationEvaluationRequest`, `OpaApplication`, `OpaEvaluationInput`, `OpaEvaluationRequest`, `OpaPolicyDecisionAdapter`, `OpaPolicyDecisionPayload`, `OpaPolicyReference`, `OpaRequestInfo`, `OpaResource`, `OpaResponse`, `OpaSubject`, `OpaTenant` |

### `commons` - 20 clases

| Rol | Clases |
|---|---|
| Excepcion de dominio | `BusinessRuleViolationException`, `CatalogItemInUseException`, `ConflictBusinessRuleException`, `DomainException`, `InvalidApplicationNameException`, `InvalidIdentifierException`, `InvalidPageWindowException`, `InvalidTenantIdException`, `InvalidValueException` |
| Catalogo de mensajes | `ValueObjectMessages` |
| Value object | `ApplicationId`, `ApplicationName`, `PageWindow`, `ProfileId`, `ResourceId`, `ResultPage`, `RoleId`, `TenantId`, `UserId` |
| Otro | `AggregateRoot` |

### `identity` - 52 clases

| Rol | Clases |
|---|---|
| Excepcion de dominio | `InvalidEmailException`, `UserNotFoundException` |
| Catalogo de mensajes | `IdentityMessages` |
| Value object | `Email`, `ExternalIdentity` |
| Regla de dominio (impl) | `UserMustExistRuleImpl` |
| Dato de regla (hecho resuelto) | `UserExistence` |
| Regla de dominio (contrato) | `UserMustExistRule` |
| Dominio (agregado / criteria) | `SecurityUser` |
| Caso de uso (impl) | `AssignTenantUseCaseImpl`, `FindAuthenticatedUserBySubjectUseCaseImpl`, `FindUserByIdUseCaseImpl`, `ListUsersUseCaseImpl`, `ProvisionIdentityUseCaseImpl`, `ResolveExternalIdentityUseCaseImpl`, `SearchUsersPageUseCaseImpl` |
| Caso de uso (contrato) | `AssignTenantUseCase`, `FindAuthenticatedUserBySubjectUseCase`, `FindUserByIdUseCase`, `ListUsersUseCase`, `ProvisionIdentityUseCase`, `ResolveExternalIdentityUseCase`, `SearchUsersPageUseCase` |
| Validador de reglas (impl) | `SubjectUserIdLookupValidatorImpl`, `UserMustExistValidatorImpl` |
| Validador de reglas (contrato) | `SubjectUserIdLookupValidator`, `UserMustExistValidator` |
| Puerto de salida | `SecurityUserRepository` |
| DTO de entrada al nucleo | `AssignTenantRequest`, `ProvisionIdentityRequest`, `ResolveExternalIdentityRequest`, `SearchUsersPageRequest` |
| DTO de salida del nucleo | `AuthenticatedUserResponse`, `UserResponse` |
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

### `profiles` - 66 clases

| Rol | Clases |
|---|---|
| Excepcion de dominio | `DuplicateProfileNameException`, `InvalidProfileNameException`, `ProfileNotFoundException` |
| Catalogo de mensajes | `ProfilesMessages` |
| Value object | `ProfileName` |
| Regla de dominio (impl) | `ProfileMustExistForTenantRuleImpl`, `ProfileNameMustBeUniqueInScopeRuleImpl` |
| Dato de regla (hecho resuelto) | `ProfileExistence`, `ProfileNameAvailability` |
| Regla de dominio (contrato) | `ProfileMustExistForTenantRule`, `ProfileNameMustBeUniqueInScopeRule` |
| Dominio (agregado / criteria) | `Profile`, `ProfileCriteria` |
| Caso de uso (impl) | `AddRoleToProfileUseCaseImpl`, `CountApplicationProfilesUseCaseImpl`, `DefineProfileUseCaseImpl`, `ListApplicationProfilesPageUseCaseImpl`, `ListProfilesUseCaseImpl`, `RemoveProfileUseCaseImpl`, `RemoveRoleFromProfileUseCaseImpl`, `UpdateProfileUseCaseImpl` |
| Caso de uso (contrato) | `AddRoleToProfileUseCase`, `CountApplicationProfilesUseCase`, `DefineProfileUseCase`, `ListApplicationProfilesPageUseCase`, `ListProfilesUseCase`, `RemoveProfileUseCase`, `RemoveRoleFromProfileUseCase`, `UpdateProfileUseCase` |
| Validador de reglas (impl) | `AddRoleToProfileRulesValidatorImpl`, `ApplicationDeletionDependencyValidatorImpl`, `DefineProfileRulesValidatorImpl`, `ProfileApplicationLookupValidatorImpl`, `ProfileNamesLookupValidatorImpl`, `ProfileRolesLookupValidatorImpl`, `RoleDeletionDependencyValidatorImpl` |
| Validador de reglas (contrato) | `AddRoleToProfileRulesValidator`, `ApplicationDeletionDependencyValidator`, `DefineProfileRulesValidator`, `ProfileApplicationLookupValidator`, `ProfileNamesLookupValidator`, `ProfileRolesLookupValidator`, `RoleDeletionDependencyValidator` |
| Puerto de salida | `ProfileRepository` |
| DTO de entrada al nucleo | `AddRoleToProfileRequest`, `ApplicationProfileCountRequest`, `DefineProfileRequest`, `ListApplicationProfilesPageRequest`, `ListProfilesRequest`, `ProfileOwnershipQuery`, `RemoveRoleFromProfileRequest`, `UpdateProfileRequest` |
| DTO de salida del nucleo | `ProfileResponse` |
| Controller | `ProfileController` |
| DTO crudo HTTP | `ListProfilesRawRequest` |
| DTO de respuesta HTTP | `ProfileWebResponse` |
| Interactor (impl) | `ListProfilesInteractorImpl` |
| Interactor (contrato) | `ListProfilesInteractor` |
| Mapper web | `ListProfilesRequestMapper`, `ProfileResponseMapper` |
| Entidad de persistencia | `ProfileEntity` |
| Mapper de persistencia | `ProfilePersistenceMapper` |
| Adaptador de repositorio | `SurrealProfileRepository` |
| Esquema de tabla | `ProfileSchema`, `SurrealProfileSchemaInitializer` |
| Cableado (@Bean) | `ProfilesConfiguration` |

### `resources` - 68 clases

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
| Caso de uso (impl) | `CountApplicationProtectedResourcesUseCaseImpl`, `ListProtectedResourcesPageUseCaseImpl`, `ListProtectedResourcesUseCaseImpl`, `RegisterApplicationWithInitialResourceUseCaseImpl`, `RegisterProtectedResourcesInBulkUseCaseImpl`, `RegisterProtectedResourceUseCaseImpl`, `RemoveProtectedResourceUseCaseImpl`, `UpdateProtectedResourceUseCaseImpl` |
| Caso de uso (contrato) | `CountApplicationProtectedResourcesUseCase`, `ListProtectedResourcesPageUseCase`, `ListProtectedResourcesUseCase`, `RegisterApplicationWithInitialResourceUseCase`, `RegisterProtectedResourcesInBulkUseCase`, `RegisterProtectedResourceUseCase`, `RemoveProtectedResourceUseCase`, `UpdateProtectedResourceUseCase` |
| Validador de reglas (impl) | `ApplicationDeletionDependencyValidatorImpl`, `ProtectedResourceIdLookupValidatorImpl`, `ProtectedResourceMustExistValidatorImpl`, `ProtectedResourceOwnerLookupValidatorImpl`, `RegisterProtectedResourceRulesValidatorImpl` |
| Validador de reglas (contrato) | `ApplicationDeletionDependencyValidator`, `ProtectedResourceIdLookupValidator`, `ProtectedResourceMustExistValidator`, `ProtectedResourceOwnerLookupValidator`, `RegisterProtectedResourceRulesValidator` |
| Puerto de salida | `ProtectedResourceRepository` |
| DTO de entrada al nucleo | `ListProtectedResourcesPageRequest`, `ProtectedResourceLookup`, `RegisterApplicationWithInitialResourceRequest`, `RegisterProtectedResourceRequest`, `UpdateProtectedResourceRequest` |
| DTO de salida del nucleo | `ApplicationWithInitialResourceRegistrationResponse`, `RegisteredProtectedResourceResponse` |
| Controller | `ApplicationWithInitialResourceController`, `ProtectedResourceController` |
| DTO crudo HTTP | `RegisterApplicationWithInitialResourceRawRequest` |
| DTO de respuesta HTTP | `ApplicationWithInitialResourceWebResponse`, `ProtectedResourceWebResponse` |
| Interactor (impl) | `ListProtectedResourcesInteractorImpl`, `RegisterApplicationWithInitialResourceInteractorImpl` |
| Interactor (contrato) | `ListProtectedResourcesInteractor`, `RegisterApplicationWithInitialResourceInteractor` |
| Mapper web | `ApplicationWithInitialResourceResponseMapper`, `ProtectedResourceResponseMapper`, `RegisterApplicationWithInitialResourceRequestMapper` |
| Entidad de persistencia | `ProtectedResourceEntity` |
| Mapper de persistencia | `ProtectedResourcePersistenceMapper` |
| Adaptador de repositorio | `SurrealProtectedResourceRepository` |
| Esquema de tabla | `ProtectedResourceSchema`, `SurrealProtectedResourceSchemaInitializer` |
| Adaptador de auditoria | `InMemoryAuditAdapter` |
| Cableado (@Bean) | `ResourcesConfiguration` |

### `roles` - 86 clases

| Rol | Clases |
|---|---|
| Excepcion de dominio | `ApplicationOutsideRoleScopeException`, `DuplicateRoleNameException`, `InvalidRoleNameException`, `InvalidRoleScopeException`, `ProtectedRoleException`, `ResourceOutsideRoleScopeException`, `RoleNotFoundException` |
| Catalogo de mensajes | `RolesMessages` |
| Value object | `RoleName`, `RoleScope`, `RoleScopeLevel` |
| Regla de dominio (impl) | `RoleMustExistForTenantRuleImpl`, `RoleNameMustBeUniqueInScopeRuleImpl`, `RoleScopeMustCoverApplicationRuleImpl`, `RoleScopeMustCoverResourceRuleImpl` |
| Dato de regla (hecho resuelto) | `ApplicationCoverage`, `ResourceCoverage`, `RoleExistence`, `RoleNameAvailability` |
| Regla de dominio (contrato) | `RoleMustExistForTenantRule`, `RoleNameMustBeUniqueInScopeRule`, `RoleScopeMustCoverApplicationRule`, `RoleScopeMustCoverResourceRule` |
| Dominio (agregado / criteria) | `Role`, `RoleCriteria` |
| Caso de uso (impl) | `CountApplicationRolesUseCaseImpl`, `DefineRoleUseCaseImpl`, `GrantResourceToRoleUseCaseImpl`, `ListApplicationRolesPageUseCaseImpl`, `ListRolesUseCaseImpl`, `RemoveRoleUseCaseImpl`, `RevokeResourceFromRoleUseCaseImpl`, `UpdateRoleUseCaseImpl` |
| Caso de uso (contrato) | `CountApplicationRolesUseCase`, `DefineRoleUseCase`, `GrantResourceToRoleUseCase`, `ListApplicationRolesPageUseCase`, `ListRolesUseCase`, `RemoveRoleUseCase`, `RevokeResourceFromRoleUseCase`, `UpdateRoleUseCase` |
| Validador de reglas (impl) | `ApplicationDeletionDependencyValidatorImpl`, `DefineRoleRulesValidatorImpl`, `GrantResourceRulesValidatorImpl`, `ResourceDeletionDependencyValidatorImpl`, `RoleApplicationLookupValidatorImpl`, `RoleLookupByNameInScopeValidatorImpl`, `RoleMustExistForTenantValidatorImpl`, `RoleNamesLookupValidatorImpl`, `RoleResourcesLookupValidatorImpl`, `RoleScopeMustCoverApplicationValidatorImpl` |
| Validador de reglas (contrato) | `ApplicationDeletionDependencyValidator`, `DefineRoleRulesValidator`, `GrantResourceRulesValidator`, `ResourceDeletionDependencyValidator`, `RoleApplicationLookupValidator`, `RoleLookupByNameInScopeValidator`, `RoleMustExistForTenantValidator`, `RoleNamesLookupValidator`, `RoleResourcesLookupValidator`, `RoleScopeMustCoverApplicationValidator` |
| Puerto de salida | `RoleRepository` |
| DTO de entrada al nucleo | `ApplicationRoleCountRequest`, `DefineRoleRequest`, `GrantResourceRequest`, `ListApplicationRolesPageRequest`, `ListRolesRequest`, `RevokeResourceRequest`, `RoleCoverageQuery`, `RoleNameInScopeQuery`, `RoleOwnershipQuery`, `UpdateRoleRequest` |
| DTO de salida del nucleo | `RoleResponse` |
| Controller | `RoleController` |
| DTO crudo HTTP | `ListRolesRawRequest` |
| DTO de respuesta HTTP | `RoleWebResponse` |
| Interactor (impl) | `ListRolesInteractorImpl` |
| Interactor (contrato) | `ListRolesInteractor` |
| Mapper web | `ListRolesRequestMapper`, `RoleResponseMapper` |
| Entidad de persistencia | `RoleEntity` |
| Mapper de persistencia | `RolePersistenceMapper` |
| Adaptador de repositorio | `SurrealRoleRepository` |
| Esquema de tabla | `RoleSchema`, `SurrealRoleSchemaInitializer` |
| Cableado (@Bean) | `RolesConfiguration` |

### `tenants` - 45 clases

| Rol | Clases |
|---|---|
| Excepcion de dominio | `DuplicateTenantException`, `InvalidTenantNameException`, `TenantNotActiveException`, `TenantNotFoundException` |
| Catalogo de mensajes | `TenantsMessages` |
| Value object | `TenantName`, `TenantStatus` |
| Regla de dominio (impl) | `TenantCodeMustBeUniqueRuleImpl`, `TenantMustExistRuleImpl`, `TenantStatusMustBeActiveRuleImpl` |
| Dato de regla (hecho resuelto) | `TenantActivation`, `TenantCodeAvailability`, `TenantExistence` |
| Regla de dominio (contrato) | `TenantCodeMustBeUniqueRule`, `TenantMustExistRule`, `TenantStatusMustBeActiveRule` |
| Dominio (agregado / criteria) | `Tenant` |
| Caso de uso (impl) | `CreateTenantUseCaseImpl`, `FindTenantNameByCodeUseCaseImpl`, `ListTenantsUseCaseImpl` |
| Caso de uso (contrato) | `CreateTenantUseCase`, `FindTenantNameByCodeUseCase`, `ListTenantsUseCase` |
| Validador de reglas (impl) | `TenantMustBeActiveValidatorImpl` |
| Validador de reglas (contrato) | `TenantMustBeActiveValidator` |
| Puerto de salida | `TenantRepository` |
| DTO de entrada al nucleo | `CreateTenantRequest` |
| DTO de salida del nucleo | `TenantNameResponse`, `TenantResponse` |
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
| `audit` | `AdministrationAuditRepository`, `AdministrationEvent`, `AdministrationOperation`, `AdministrationOutcome` |
| `auth/model` | `BffSessionExpiredException`, `OidcFlowIntent`, `TokenRenewalUnavailableException` |
| `auth/service` | `BffSessionTokenRenewer`, `KeycloakOidcSessionService`, `OidcAuthenticationFailureHandler`, `OidcAuthenticationSuccessHandler`, `OidcAuthorizationFlowService`, `OidcFlowStateService`, `OidcRedirectPolicy`, `OidcReturnTargetPolicy` |
| `auth/web` | `KeycloakLoginController`, `KeycloakRegistrationController` |
| `cache` | `ActiveRolesCacheRetentionProperties`, `DistributedCachePort`, `ObservedDistributedCachePort`, `RedisDistributedCachePort` |
| `config` | `EventPublisherConfiguration`, `InternalSecurityConfiguration`, `KeycloakSecurityConfiguration`, `ProjectPackages`, `RedisConfiguration`, `SecurityConfiguration`, `SessionTokenRenewalConfiguration`, `SharedPortsConfiguration`, `SurrealDbConfiguration`, `TelemetryConfiguration` |
| `contract` | `Operation`, `OperationWithoutResult`, `ReactiveOperation`, `ReactiveOperationWithoutInput`, `ReactiveOperationWithoutResult`, `ReactiveStreamOperation` |
| `event` | `DomainEvent`, `DomainEventPublisher`, `SpringDomainEventPublisher` |
| `message` | `RequiredArgumentMessages` |
| `observability` | `ReactiveLogContext`, `ReactiveTelemetry` |
| `persistence/surrealdb` | `SurrealDbClient`, `SurrealDbException`, `SurrealDbHealthIndicator`, `SurrealDbProperties`, `SurrealRecordId`, `SurrealSchemaInitializer` |
| `port` | `CredentialHasher`, `IdentifierGenerator`, `SecretGenerator`, `TimeProvider` |
| `security` | `ApiAccessDeniedHandler`, `ApiAuthenticationEntryPoint`, `CorsProperties`, `InternalEvidenceJwtProperties`, `InternalMtlsProperties`, `InternalMtlsWebFilter`, `JwtSecurityProperties`, `KeycloakSessionProperties`, `LocalUserPrincipal`, `PdpPrincipal`, `RevocationRetentionProperties`, `SecurityContext`, `SessionTokenRenewalProperties` |
| `security/mfa` | `AuthenticationContextEvidence`, `MfaEvidenceProperties` |
| `security/revocation` | `RedisTokenRevocationAdapter`, `RevocationAwareJwtDecoder`, `TokenRevocationPort` |
| `web` | `ApiResponse`, `CorrelationWebFilter`, `PageResponse`, `RequestContext`, `RequestFieldParser` |
| `web/exception` | `ConflictingRequestParametersException`, `MalformedRequestFieldException`, `MissingRequestFieldException`, `RequestContractException` |
| `web/exceptionhandler` | `ApiErrorHandler` |
| `web/message` | `WebContractMessages` |
| `web/session` | `InternalBffSessionTokenController`, `KeycloakLogoutController`, `SessionController`, `SessionResponse`, `SessionRevocationController` |

---

## Puertos de salida y sus implementaciones

| Puerto | Implementado por |
|---|---|
| `AccessAuditRepository` | `ObservedAccessAuditRepository`, `SurrealAccessAuditRepository` |
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
| POST | `/api/v1/applications` | `ApplicationRegistrationController` |
| DELETE | `/api/v1/applications/{applicationId}` | `ApplicationAdministrationController` |
| PATCH | `/api/v1/applications/{applicationId}` | `CatalogMaintenanceController` |
| GET | `/api/v1/applications/{applicationId}/administrators` | `ApplicationAdministratorController` |
| POST | `/api/v1/applications/{applicationId}/administrators` | `ApplicationAdministratorController` |
| DELETE | `/api/v1/applications/{applicationId}/administrators/{userId}` | `ApplicationAdministratorController` |
| POST | `/api/v1/applications/{applicationId}/credential-rotations` | `ApplicationAdministrationController` |
| GET | `/api/v1/applications/{applicationId}/resources` | `ProtectedResourceController` |
| POST | `/api/v1/applications/{applicationId}/resources` | `ResourceAdministrationController` |
| DELETE | `/api/v1/applications/{applicationId}/resources/{resourceId}` | `CatalogMaintenanceController` |
| PATCH | `/api/v1/applications/{applicationId}/resources/{resourceId}` | `CatalogMaintenanceController` |
| GET | `/api/v1/applications/{applicationId}/security/administrators` | `ApplicationSecurityController` |
| GET | `/api/v1/applications/{applicationId}/security/profile-assignments` | `ApplicationSecurityController` |
| GET | `/api/v1/applications/{applicationId}/security/profiles` | `ApplicationSecurityController` |
| GET | `/api/v1/applications/{applicationId}/security/profiles/{profileId}/assignments` | `ApplicationSecurityController` |
| GET | `/api/v1/applications/{applicationId}/security/profiles/{profileId}/roles` | `ApplicationSecurityController` |
| GET | `/api/v1/applications/{applicationId}/security/resources` | `ApplicationSecurityController` |
| GET | `/api/v1/applications/{applicationId}/security/role-assignments` | `ApplicationSecurityController` |
| GET | `/api/v1/applications/{applicationId}/security/roles` | `ApplicationSecurityController` |
| GET | `/api/v1/applications/{applicationId}/security/roles/{roleId}/assignments` | `ApplicationSecurityController` |
| GET | `/api/v1/applications/{applicationId}/security/roles/{roleId}/resources` | `ApplicationSecurityController` |
| GET | `/api/v1/applications/{applicationId}/security/summary` | `ApplicationSecurityController` |
| GET | `/api/v1/applications/{applicationId}/security/users` | `ApplicationSecurityController` |
| GET | `/api/v1/applications/{applicationId}/security/users/{userId}/assignments` | `ApplicationSecurityController` |
| POST | `/api/v1/applications/with-initial-resource` | `ApplicationWithInitialResourceController` |
| POST | `/api/v1/authorize` | `AuthorizationController` |
| GET | `/api/v1/me/applications` | `CurrentUserApplicationsController` |
| GET | `/api/v1/profiles` | `ProfileController` |
| POST | `/api/v1/profiles` | `ProfileAdministrationController` |
| DELETE | `/api/v1/profiles/{profileId}` | `CatalogMaintenanceController` |
| PATCH | `/api/v1/profiles/{profileId}` | `CatalogMaintenanceController` |
| GET | `/api/v1/profiles/{profileId}/assignments` | `ProfileAssignmentController` |
| POST | `/api/v1/profiles/{profileId}/assignments` | `ProfileAssignmentAdministrationController` |
| DELETE | `/api/v1/profiles/{profileId}/assignments/{profileAssignmentId}` | `ProfileAssignmentAdministrationController` |
| POST | `/api/v1/profiles/{profileId}/roles` | `ProfileAdministrationController` |
| DELETE | `/api/v1/profiles/{profileId}/roles/{roleId}` | `ProfileAdministrationController` |
| POST | `/api/v1/resources/bulk` | `BulkResourceController` |
| POST | `/api/v1/resources/bulk` | `BulkResourceController` |
| POST | `/api/v1/resources/bulk` | `BulkResourceController` |
| GET | `/api/v1/resources/bulk/template` | `BulkResourceController` |
| GET | `/api/v1/roles` | `RoleController` |
| POST | `/api/v1/roles` | `RoleAdministrationController` |
| DELETE | `/api/v1/roles/{roleId}` | `CatalogMaintenanceController` |
| PATCH | `/api/v1/roles/{roleId}` | `CatalogMaintenanceController` |
| GET | `/api/v1/roles/{roleId}/assignments` | `AssignmentController` |
| POST | `/api/v1/roles/{roleId}/assignments` | `AssignmentAdministrationController` |
| DELETE | `/api/v1/roles/{roleId}/assignments/{assignmentId}` | `AssignmentAdministrationController` |
| POST | `/api/v1/roles/{roleId}/resources` | `RoleAdministrationController` |
| DELETE | `/api/v1/roles/{roleId}/resources/{resourceId}` | `RoleAdministrationController` |
| DELETE | `/api/v1/session` | `SessionRevocationController` |
| GET | `/api/v1/session` | `SessionController` |
| GET | `/api/v1/session/logout` | `KeycloakLogoutController` |
| GET | `/api/v1/tenants` | `TenantController` |
| POST | `/api/v1/tenants` | `TenantController` |
| GET | `/api/v1/users` | `UserController` |
| PUT | `/api/v1/users/{id}/tenant` | `UserController` |
| POST | `/internal/v1/access-decisions` | `InternalAccessDecisionController` |
| POST | `/internal/v1/applications/{applicationId}/administrators` | `InternalApplicationAdministratorController` |
| POST | `/internal/v1/applications/names/{applicationName}/credential-validations` | `InternalApplicationCredentialController` |
| GET | `/internal/v1/bff-session-token` | `InternalBffSessionTokenController` |
| GET | `/oauth2/authorization/keycloak` | `KeycloakLoginController` |
| GET | `/oauth2/authorization/keycloak/register` | `KeycloakRegistrationController` |

---

## Pruebas por area

| Area | Clases de prueba |
|---|---|
| `pdp/applications` | `ApplicationBaseUrlTests`, `ApplicationCatalogPropertiesTests`, `ApplicationControllerTests`, `ApplicationCredentialHashTests`, `ApplicationCredentialMustBeValidRuleImplTests`, `ApplicationCredentialValidationResponseMapperTests`, `ApplicationCriteriaTests`, `ApplicationHttpTests`, `ApplicationMustExistForTenantValidatorTests`, `ApplicationMustHaveAtLeastOneEnabledEnvironmentRuleImplTests`, `ApplicationOwnerLookupValidatorImplTests`, `ApplicationPersistenceMapperTests`, `ApplicationRepositoryTests`, `ApplicationResponseMapperTests`, `ApplicationRuleTests`, `ApplicationTests`, `InternalApplicationCredentialControllerTests`, `ListApplicationsRequestMapperTests`, `ListApplicationsUseCaseImplTests`, `RegisterApplicationRulesValidatorTests`, `RegisterApplicationUseCaseImplTests`, `RemoveApplicationUseCaseImplTests`, `RotateApplicationCredentialUseCaseImplTests`, `ValidateApplicationCredentialInteractorImplTests`, `ValidateApplicationCredentialRequestMapperTests`, `ValidateApplicationCredentialUseCaseImplTests` |
| `pdp/assignments` | `ApplicationRegistrationControllerTests`, `AssignApplicationAdministratorInteractorImplTests`, `AssignApplicationAdministratorUseCaseImplTests`, `AssignmentApplicationLookupValidatorImplTests`, `AssignmentControllerTests`, `AssignmentCriteriaTests`, `AssignmentHttpTests`, `AssignmentMustExistForTenantRuleImplTests`, `AssignmentMustNotDuplicateActiveRuleImplTests`, `AssignmentPersistenceMapperTests`, `AssignmentResponseMapperTests`, `AssignmentTests`, `AssignProfileUseCaseImplTests`, `AssignRoleRulesValidatorImplTests`, `AssignRoleUseCaseImplTests`, `InternalApplicationAdministratorControllerTests`, `LastAdministratorMustNotBeRevokedRuleImplTests`, `ListApplicationAdministratorsUseCaseImplTests`, `ListAssignmentsRequestMapperTests`, `ListAssignmentsUseCaseImplTests`, `ListProfileAssignmentsRequestMapperTests`, `ListProfileAssignmentsUseCaseImplTests`, `ProfileAssignmentApplicationLookupValidatorImplTests`, `ProfileAssignmentControllerTests`, `ProfileAssignmentCriteriaTests`, `ProfileAssignmentHttpTests`, `ProfileAssignmentMustExistForTenantRuleImplTests`, `ProfileAssignmentMustNotDuplicateActiveRuleImplTests`, `ProfileAssignmentPersistenceMapperTests`, `ProfileAssignmentRepositoryTests`, `ProfileAssignmentResponseMapperTests`, `ProfileAssignmentTests`, `RegisterApplicationWithFirstAdministratorRequestMapperTests`, `RegisterApplicationWithFirstAdministratorUseCaseImplTests`, `RemoveApplicationAdministratorUseCaseImplTests`, `ResolveActiveRolesUseCaseImplTests`, `ResolveUserApplicationsUseCaseImplTests`, `RevokeAssignmentRulesValidatorImplTests`, `RevokeAssignmentUseCaseImplTests`, `RevokeProfileAssignmentUseCaseImplTests`, `ValidityTests` |
| `pdp/authorization` | `AccessDecisionInternalResponseMapperTests`, `AccessDecisionRawRequestMapperTests`, `AccessDecisionResponseMapperTests`, `AccessEventPersistenceMapperTests`, `AccessEventTests`, `ActiveRoleNamesLookupValidatorImplTests`, `AdministerApplicationAdministratorAssignmentInteractorImplTests`, `AdministerApplicationAdministratorAssignmentUseCaseImplTests`, `AdministerApplicationAdministratorListInteractorImplTests`, `AdministerApplicationAdministratorListUseCaseImplTests`, `AdministerApplicationAdministratorRemovalInteractorImplTests`, `AdministerApplicationAdministratorRemovalUseCaseImplTests`, `AdministerApplicationCredentialRotationUseCaseImplTests`, `AdministerApplicationRemovalUseCaseImplTests`, `AdministerAssignmentCreationInteractorImplTests`, `AdministerAssignmentCreationUseCaseImplTests`, `AdministerAssignmentRevocationInteractorImplTests`, `AdministerAssignmentRevocationUseCaseImplTests`, `AdministerProfileAssignmentCreationInteractorImplTests`, `AdministerProfileAssignmentCreationUseCaseImplTests`, `AdministerProfileAssignmentRevocationInteractorImplTests`, `AdministerProfileAssignmentRevocationUseCaseImplTests`, `AdministerProfileDefinitionInteractorImplTests`, `AdministerProfileDefinitionUseCaseImplTests`, `AdministerProfileRoleAdditionInteractorImplTests`, `AdministerProfileRoleAdditionUseCaseImplTests`, `AdministerResourceGrantInteractorImplTests`, `AdministerResourceGrantUseCaseImplTests`, `AdministerResourceRegistrationInteractorImplTests`, `AdministerResourceRegistrationUseCaseImplTests`, `AdministerRoleDefinitionInteractorImplTests`, `AdministerRoleDefinitionUseCaseImplTests`, `ApplicationAdministrationControllerTests`, `ApplicationAdministrationRequestMapperTests`, `ApplicationAdministratorControllerTests`, `ApplicationSecurityHttpTests`, `ApplicationSecurityPageRequestMapperTests`, `AssignmentAdministrationControllerTests`, `AuthorizationContextResolverImplTests`, `AuthorizationControllerTests`, `AuthorizationHttpTests`, `AuthorizeAdministrationUseCaseImplTests`, `AuthorizeRequestMapperTests`, `AuthorizeUseCaseImplTests`, `BulkResourceImportServiceTests`, `BulkResourceWorkbookParsingTests`, `BulkResourceWorkbookServiceTests`, `DecisionStateTests`, `DefineRoleRequestMapperTests`, `EvaluateInternalAccessUseCaseImplTests`, `GrantResourceRequestMapperTests`, `InternalAccessDecisionControllerTests`, `InternalAccessDecisionInteractorImplTests`, `InternalSecurityChainIntegrationTests`, `MfaAwareApplicationAdministratorValidatorTests`, `ObservedAccessAuditRepositoryTests`, `OpaAdministrationDecisionAdapterTests`, `OpaFixtureServer`, `OpaPolicyDecisionAdapterTests`, `PrincipalMustBeApplicationAdministratorValidatorImplTests`, `ProfileAdministrationControllerTests`, `ProfileAssignmentAdministrationControllerTests`, `ReadApplicationSecuritySummaryUseCaseImplTests`, `RegisterProtectedResourceRequestMapperTests`, `ResourceAdministrationControllerTests`, `RoleAdministrationControllerTests`, `UnconfiguredRevocationDefaultsTests` |
| `pdp/commons` | `PageWindowTests`, `RemoveUseCaseRequestTests`, `ResultPageTests`, `UnimplementedRepositoryCapabilitiesTests`, `ValueObjectTests` |
| `pdp/identity` | `AssignTenantRequestMapperTests`, `AssignTenantUseCaseImplTests`, `EmailTests`, `ProvisionIdentityUseCaseImplTests`, `SecurityUserPersistenceMapperTests`, `SubjectUserIdLookupValidatorImplTests`, `UserControllerTests`, `UserMustExistRuleTests`, `UserMustExistValidatorImplTests`, `UserResponseMapperTests` |
| `pdp/profiles` | `AddRoleToProfileUseCaseImplTests`, `DefineProfileUseCaseImplTests`, `ListProfilesInteractorImplTests`, `ListProfilesRequestMapperTests`, `ListProfilesUseCaseImplTests`, `ProfileApplicationLookupValidatorImplTests`, `ProfileControllerTests`, `ProfileCriteriaTests`, `ProfileHttpTests`, `ProfileMustExistForTenantRuleImplTests`, `ProfileNameMustBeUniqueInScopeRuleImplTests`, `ProfileNameTests`, `ProfilePersistenceMapperTests`, `ProfileRepositoryTests`, `ProfileResponseMapperTests`, `ProfileRolesLookupValidatorImplTests`, `ProfileTests` |
| `pdp/resources` | `ApplicationWithInitialResourceControllerTests`, `ApplicationWithInitialResourceHttpTests`, `HttpVerbTests`, `InMemoryAuditAdapterTests`, `ProtectedResourceControllerTests`, `ProtectedResourceMustBeUniqueRuleTests`, `ProtectedResourceMustExistRuleTests`, `ProtectedResourceMustExistValidatorTests`, `ProtectedResourceOwnerLookupValidatorImplTests`, `ProtectedResourceResponseMapperTests`, `RegisterApplicationWithInitialResourceRequestMapperTests`, `RegisterApplicationWithInitialResourceUseCaseImplTests`, `RegisterProtectedResourceRulesValidatorTests`, `RegisterProtectedResourceUseCaseImplTests`, `ResourcePathTests` |
| `pdp/roles` | `DefineRoleRulesValidatorImplTests`, `DefineRoleUseCaseImplTests`, `GrantResourceRulesValidatorImplTests`, `GrantResourceToRoleUseCaseImplTests`, `ListRolesRequestMapperTests`, `ListRolesUseCaseImplTests`, `RoleApplicationLookupValidatorImplTests`, `RoleControllerTests`, `RoleCriteriaTests`, `RoleHttpTests`, `RoleMustExistForTenantRuleImplTests`, `RoleMustExistForTenantValidatorImplTests`, `RoleNameMustBeUniqueInScopeRuleImplTests`, `RoleNamesLookupValidatorImplTests`, `RoleNameTests`, `RolePersistenceMapperTests`, `RoleResponseMapperTests`, `RoleScopeLevelTests`, `RoleScopeMustCoverApplicationRuleImplTests`, `RoleScopeMustCoverApplicationValidatorImplTests`, `RoleScopeMustCoverResourceRuleImplTests`, `RoleScopeTests`, `RoleTests` |
| `pdp/tenants` | `CreateTenantRequestMapperTests`, `CreateTenantUseCaseImplTests`, `ListTenantsUseCaseImplTests`, `TenantCatalogPropertiesTests`, `TenantControllerTests`, `TenantMustBeActiveValidatorTests`, `TenantNameTests`, `TenantResponseMapperTests`, `TenantRuleTests` |
| `raiz` | `AbstractRedisIntegrationTest`, `AbstractSurrealDbIntegrationTest`, `LayeredArchitectureTests`, `ModulithStructureTests`, `PdpApplicationTests` |
| `shared/audit` | `TestAdministrationAuditRepositories` |
| `shared/auth` | `BffSessionTokenRenewerTests`, `KeycloakLoginControllerTests`, `KeycloakOidcSessionServiceTests`, `KeycloakRegistrationControllerTests`, `OidcAuthenticationFailureHandlerTests`, `OidcAuthenticationSuccessHandlerTests`, `OidcAuthorizationFlowServiceTests`, `OidcReturnTargetPolicyTests` |
| `shared/cache` | `ObservedDistributedCachePortTests`, `RedisDistributedCachePortTests` |
| `shared/config` | `BffSessionTokenRenewalIntegrationTests`, `CorsConfigurationTests`, `KeycloakSecurityConfigurationTests`, `SecurityConfigurationTests` |
| `shared/persistence` | `SurrealDbClientTests`, `SurrealRecordIdTests`, `SurrealRepositoryIntegrationTests` |
| `shared/security` | `ApiAccessDeniedHandlerTests`, `ApiAuthenticationEntryPointTests`, `CorsPropertiesTests`, `InternalMtlsWebFilterTests`, `JwtSecurityPropertiesTests`, `MfaEvidencePropertiesTests`, `PdpPrincipalSecurityContextTests`, `RedisTokenRevocationAdapterTests`, `RevocationAwareJwtDecoderTests`, `SecurityWebFilterChainTests`, `SessionTokenRenewalPropertiesTests`, `TestJwtSupport` |
| `shared/web` | `ApiErrorHandlerTests`, `CorrelationWebFilterTests`, `InternalBffSessionTokenControllerTests`, `KeycloakLogoutControllerTests`, `SessionControllerTests`, `SessionLogoutBearerIntegrationTests`, `SessionRevocationControllerTests`, `WebContractMessagesTests` |
