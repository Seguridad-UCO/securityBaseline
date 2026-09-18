# PROJECT-MAP

> Generado por `.claude/tools/mapa.ps1`. **No editar a mano** - se regenera desde el codigo.
> Es el nivel 0 del grafo de conocimiento: responde "que existe y donde va lo nuevo".

- Clases de produccion: **720**
- Clases de prueba: **238**
- Slices de negocio: **9** (applications, assignments, authorization, commons, identity, profiles, resources, roles, tenants)

---

## Slices de negocio (`pdp`)

### `applications` - 70 clases

| Rol | Clases |
|---|---|
| Excepcion de dominio | `AmbiguousApplicationNameException`, `ApplicationNotFoundException`, `DuplicateApplicationException`, `InvalidApplicationBaseUrlException`, `InvalidApplicationCredentialException`, `InvalidApplicationCredentialHashException`, `ReservedApplicationNameException` |
| Catalogo de mensajes | `ApplicationsMessages` |
| Value object | `ApplicationBaseUrl`, `ApplicationCredentialHash` |
| Regla de dominio (impl) | `ApplicationCredentialMustBeValidRuleImpl`, `ApplicationMustExistForTenantRuleImpl`, `ApplicationNameMustBeUniqueForTenantRuleImpl`, `ApplicationNameMustNotBeReservedRuleImpl` |
| Dato de regla (hecho resuelto) | `ApplicationCredentialValidity`, `ApplicationExistence`, `ApplicationNameAvailability` |
| Regla de dominio (contrato) | `ApplicationCredentialMustBeValidRule`, `ApplicationMustExistForTenantRule`, `ApplicationNameMustBeUniqueForTenantRule`, `ApplicationNameMustNotBeReservedRule` |
| Dominio (agregado / criteria) | `Application`, `ApplicationCriteria` |
| Caso de uso (impl) | `ListApplicationsUseCaseImpl`, `RegisterApplicationUseCaseImpl`, `RemoveApplicationUseCaseImpl`, `RotateApplicationCredentialUseCaseImpl`, `ValidateApplicationCredentialUseCaseImpl` |
| Caso de uso (contrato) | `ListApplicationsUseCase`, `RegisterApplicationUseCase`, `RemoveApplicationUseCase`, `RotateApplicationCredentialUseCase`, `ValidateApplicationCredentialUseCase` |
| Validador de reglas (impl) | `ApplicationMustExistForTenantValidatorImpl`, `ApplicationNameLookupValidatorImpl`, `ApplicationOwnerLookupValidatorImpl`, `RegisterApplicationRulesValidatorImpl` |
| Validador de reglas (contrato) | `ApplicationMustExistForTenantValidator`, `ApplicationNameLookupValidator`, `ApplicationOwnerLookupValidator`, `RegisterApplicationRulesValidator` |
| Puerto de salida | `ApplicationRepository` |
| DTO de entrada al nucleo | `ApplicationOwnershipQuery`, `ListApplicationsRequest`, `RegisterApplicationRequest`, `RotateApplicationCredentialRequest`, `ValidateApplicationCredentialRequest` |
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

### `assignments` - 121 clases

| Rol | Clases |
|---|---|
| Excepcion de dominio | `AssignmentNotFoundException`, `CannotRemoveLastAdministratorException`, `DuplicateAssignmentException`, `DuplicateProfileAssignmentException`, `InvalidValidityException`, `ProfileAssignmentNotFoundException` |
| Catalogo de mensajes | `AssignmentsMessages` |
| Value object | `AssignmentId`, `ProfileAssignmentId`, `Validity` |
| Regla de dominio (impl) | `AssignmentMustExistForTenantRuleImpl`, `AssignmentMustNotDuplicateActiveRuleImpl`, `LastAdministratorMustNotBeRevokedRuleImpl`, `ProfileAssignmentMustExistForTenantRuleImpl`, `ProfileAssignmentMustNotDuplicateActiveRuleImpl` |
| Dato de regla (hecho resuelto) | `ActiveAssignmentAvailability`, `ActiveProfileAssignmentAvailability`, `AdministratorRevocationEligibility`, `AssignmentExistence`, `ProfileAssignmentExistence` |
| Regla de dominio (contrato) | `AssignmentMustExistForTenantRule`, `AssignmentMustNotDuplicateActiveRule`, `LastAdministratorMustNotBeRevokedRule`, `ProfileAssignmentMustExistForTenantRule`, `ProfileAssignmentMustNotDuplicateActiveRule` |
| Dominio (agregado / criteria) | `Assignment`, `AssignmentCriteria`, `ProfileAssignment`, `ProfileAssignmentCriteria` |
| Caso de uso (impl) | `AssignApplicationAdministratorUseCaseImpl`, `AssignProfileUseCaseImpl`, `AssignRoleUseCaseImpl`, `ListApplicationAdministratorsUseCaseImpl`, `ListAssignmentsUseCaseImpl`, `ListProfileAssignmentsUseCaseImpl`, `RegisterApplicationWithFirstAdministratorUseCaseImpl`, `RemoveApplicationAdministratorUseCaseImpl`, `ResolveActiveRolesUseCaseImpl`, `ResolveAuthorizationSubjectFactsUseCaseImpl`, `RevokeAssignmentUseCaseImpl`, `RevokeProfileAssignmentUseCaseImpl` |
| Caso de uso (contrato) | `AssignApplicationAdministratorUseCase`, `AssignProfileUseCase`, `AssignRoleUseCase`, `ListApplicationAdministratorsUseCase`, `ListAssignmentsUseCase`, `ListProfileAssignmentsUseCase`, `RegisterApplicationWithFirstAdministratorUseCase`, `RemoveApplicationAdministratorUseCase`, `ResolveActiveRolesUseCase`, `ResolveAuthorizationSubjectFactsUseCase`, `RevokeAssignmentUseCase`, `RevokeProfileAssignmentUseCase` |
| Validador de reglas (impl) | `AssignmentApplicationLookupValidatorImpl`, `AssignProfileRulesValidatorImpl`, `AssignRoleRulesValidatorImpl`, `ProfileAssignmentApplicationLookupValidatorImpl`, `RevokeAssignmentRulesValidatorImpl`, `RevokeProfileAssignmentRulesValidatorImpl` |
| Validador de reglas (contrato) | `AssignmentApplicationLookupValidator`, `AssignProfileRulesValidator`, `AssignRoleRulesValidator`, `ProfileAssignmentApplicationLookupValidator`, `RevokeAssignmentRulesValidator`, `RevokeProfileAssignmentRulesValidator` |
| Puerto de salida | `AssignmentRepository`, `ProfileAssignmentRepository` |
| DTO de entrada al nucleo | `AssignApplicationAdministratorRequest`, `AssignmentOwnershipQuery`, `AssignProfileRequest`, `AssignRoleRequest`, `ListApplicationAdministratorsRequest`, `ListAssignmentsRequest`, `ListProfileAssignmentsRequest`, `ProfileAssignmentOwnershipQuery`, `RegisterApplicationWithFirstAdministratorRequest`, `RemoveApplicationAdministratorRequest`, `ResolveActiveRolesRequest`, `ResolveAuthorizationSubjectFactsRequest`, `RevokeAssignmentRequest`, `RevokeProfileAssignmentRequest` |
| DTO de salida del nucleo | `ActiveRolesResponse`, `AssignmentResponse`, `AuthorizationSubjectFactsResponse`, `ProfileAssignmentResponse` |
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

### `authorization` - 173 clases

| Rol | Clases |
|---|---|
| Excepcion de dominio | `MfaEvidenceRequiredException`, `NotAuthorizedToAdministerException` |
| Catalogo de mensajes | `AuthorizationMessages` |
| Value object | `DecisionState`, `PolicyReference`, `ReasonCode` |
| Evento de dominio | `AccessEvent` |
| Caso de uso (impl) | `AdministerApplicationAdministratorAssignmentUseCaseImpl`, `AdministerApplicationAdministratorListUseCaseImpl`, `AdministerApplicationAdministratorRemovalUseCaseImpl`, `AdministerApplicationCredentialRotationUseCaseImpl`, `AdministerApplicationRemovalUseCaseImpl`, `AdministerAssignmentCreationUseCaseImpl`, `AdministerAssignmentRevocationUseCaseImpl`, `AdministerProfileAssignmentCreationUseCaseImpl`, `AdministerProfileAssignmentRevocationUseCaseImpl`, `AdministerProfileDefinitionUseCaseImpl`, `AdministerProfileRoleAdditionUseCaseImpl`, `AdministerResourceGrantUseCaseImpl`, `AdministerResourceRegistrationUseCaseImpl`, `AdministerRoleDefinitionUseCaseImpl`, `AuthorizeAdministrationUseCaseImpl`, `AuthorizeUseCaseImpl`, `EvaluateInternalAccessUseCaseImpl` |
| Caso de uso (contrato) | `AdministerApplicationAdministratorAssignmentUseCase`, `AdministerApplicationAdministratorListUseCase`, `AdministerApplicationAdministratorRemovalUseCase`, `AdministerApplicationCredentialRotationUseCase`, `AdministerApplicationRemovalUseCase`, `AdministerAssignmentCreationUseCase`, `AdministerAssignmentRevocationUseCase`, `AdministerProfileAssignmentCreationUseCase`, `AdministerProfileAssignmentRevocationUseCase`, `AdministerProfileDefinitionUseCase`, `AdministerProfileRoleAdditionUseCase`, `AdministerResourceGrantUseCase`, `AdministerResourceRegistrationUseCase`, `AdministerRoleDefinitionUseCase`, `AuthorizeAdministrationUseCase`, `AuthorizeUseCase`, `EvaluateInternalAccessUseCase` |
| Validador de reglas (impl) | `ActiveRoleNamesLookupValidatorImpl`, `MfaAwareApplicationAdministratorValidator`, `PrincipalMustBeApplicationAdministratorValidatorImpl` |
| Validador de reglas (contrato) | `ActiveRoleNamesLookupValidator`, `PrincipalMustBeApplicationAdministratorValidator` |
| Puerto de salida | `AccessAuditRepository`, `AdministrationDecisionPort`, `PolicyDecisionPort` |
| DTO de entrada al nucleo | `AccessRequest`, `AdministerApplicationAdministratorAssignmentRequest`, `AdministerApplicationAdministratorListRequest`, `AdministerApplicationAdministratorRemovalRequest`, `AdministerAssignmentCreationRequest`, `AdministerAssignmentRevocationRequest`, `AdministerProfileAssignmentCreationRequest`, `AdministerProfileAssignmentRevocationRequest`, `AdministerProfileDefinitionRequest`, `AdministerProfileRoleAdditionRequest`, `AdministerResourceGrantRequest`, `AdministerResourceRegistrationRequest`, `AdministerRoleDefinitionRequest`, `AdministrationRequest`, `InternalAccessRequest`, `PolicyEvaluationInput` |
| DTO de salida del nucleo | `AccessDecision`, `AdministrationDecision` |
| Controller | `ApplicationAdministrationController`, `ApplicationAdministratorController`, `AssignmentAdministrationController`, `AuthorizationController`, `InternalAccessDecisionController`, `ProfileAdministrationController`, `ProfileAssignmentAdministrationController`, `ResourceAdministrationController`, `RoleAdministrationController` |
| DTO crudo HTTP | `AccessDecisionRawRequest`, `AddRoleToProfileRawRequest`, `ApplicationAdministrationRawRequest`, `AssignApplicationAdministratorRawRequest`, `AssignProfileRawRequest`, `AssignRoleRawRequest`, `AuthorizeRawRequest`, `DefineProfileRawRequest`, `DefineRoleRawRequest`, `GrantResourceRawRequest`, `ListApplicationAdministratorsRawRequest`, `RegisterProtectedResourceBodyRequest`, `RegisterProtectedResourceRawRequest`, `RemoveApplicationAdministratorRawRequest`, `RevokeAssignmentRawRequest`, `RevokeProfileAssignmentRawRequest` |
| DTO de respuesta HTTP | `AccessDecisionInternalWebResponse`, `AccessDecisionWebResponse`, `AdministeredApplicationWebResponse`, `AdministeredResourceWebResponse`, `ApplicationAdministratorWebResponse`, `AssignmentAdministrationWebResponse`, `PolicyReferenceInternalWebResponse`, `PolicyReferenceWebResponse`, `ProfileAdministrationWebResponse`, `ProfileAssignmentAdministrationWebResponse`, `RoleAdministrationWebResponse` |
| Interactor (impl) | `AdministerApplicationAdministratorAssignmentInteractorImpl`, `AdministerApplicationAdministratorListInteractorImpl`, `AdministerApplicationAdministratorRemovalInteractorImpl`, `AdministerAssignmentCreationInteractorImpl`, `AdministerAssignmentRevocationInteractorImpl`, `AdministerProfileAssignmentCreationInteractorImpl`, `AdministerProfileAssignmentRevocationInteractorImpl`, `AdministerProfileDefinitionInteractorImpl`, `AdministerProfileRoleAdditionInteractorImpl`, `AdministerResourceGrantInteractorImpl`, `AdministerResourceRegistrationInteractorImpl`, `AdministerRoleDefinitionInteractorImpl`, `ApplicationCredentialRotationInteractorImpl`, `ApplicationRemovalInteractorImpl`, `AuthorizeInteractorImpl`, `InternalAccessDecisionInteractorImpl` |
| Interactor (contrato) | `AdministerApplicationAdministratorAssignmentInteractor`, `AdministerApplicationAdministratorListInteractor`, `AdministerApplicationAdministratorRemovalInteractor`, `AdministerAssignmentCreationInteractor`, `AdministerAssignmentRevocationInteractor`, `AdministerProfileAssignmentCreationInteractor`, `AdministerProfileAssignmentRevocationInteractor`, `AdministerProfileDefinitionInteractor`, `AdministerProfileRoleAdditionInteractor`, `AdministerResourceGrantInteractor`, `AdministerResourceRegistrationInteractor`, `AdministerRoleDefinitionInteractor`, `ApplicationCredentialRotationInteractor`, `ApplicationRemovalInteractor`, `AuthorizeInteractor`, `InternalAccessDecisionInteractor` |
| Mapper web | `AccessDecisionInternalResponseMapper`, `AccessDecisionRawRequestMapper`, `AccessDecisionResponseMapper`, `AdministeredApplicationResponseMapper`, `AdministeredResourceResponseMapper`, `ApplicationAdministrationRequestMapper`, `AuthorizeRequestMapper`, `DefineRoleRequestMapper`, `GrantResourceRequestMapper`, `RegisterProtectedResourceRequestMapper`, `RoleAdministrationResponseMapper` |
| Entidad de persistencia | `AccessEventEntity` |
| Mapper de persistencia | `AccessEventPersistenceMapper` |
| Adaptador de repositorio | `SurrealAccessAuditRepository`, `SurrealAdministrationAuditRepository` |
| Esquema de tabla | `AccessEventSchema`, `AdministrationEventSchema`, `SurrealAccessEventSchemaInitializer` |
| Cableado (@Bean) | `AuthorizationConfiguration` |
| Propiedades | `OpaProperties` |
| Otro | `AuthorizationContextResolver`, `AuthorizationContextResolverImpl`, `ObservedAccessAuditRepository`, `ObservedAdministrationAuditRepository`, `OpaAdministrationDecisionAdapter`, `OpaAdministrationEvaluationInput`, `OpaAdministrationEvaluationRequest`, `OpaApplication`, `OpaEvaluationInput`, `OpaEvaluationRequest`, `OpaPolicyDecisionAdapter`, `OpaPolicyDecisionPayload`, `OpaPolicyReference`, `OpaRequestInfo`, `OpaResource`, `OpaResponse`, `OpaSubject`, `OpaTenant` |

### `commons` - 19 clases

| Rol | Clases |
|---|---|
| Excepcion de dominio | `BusinessRuleViolationException`, `ConflictBusinessRuleException`, `DomainException`, `InvalidApplicationNameException`, `InvalidIdentifierException`, `InvalidPageWindowException`, `InvalidTenantIdException`, `InvalidValueException` |
| Catalogo de mensajes | `ValueObjectMessages` |
| Value object | `ApplicationId`, `ApplicationName`, `PageWindow`, `ProfileId`, `ResourceId`, `ResultPage`, `RoleId`, `TenantId`, `UserId` |
| Otro | `AggregateRoot` |

### `identity` - 44 clases

| Rol | Clases |
|---|---|
| Excepcion de dominio | `InvalidEmailException`, `UserNotFoundException` |
| Catalogo de mensajes | `IdentityMessages` |
| Value object | `Email`, `ExternalIdentity` |
| Regla de dominio (impl) | `UserMustExistRuleImpl` |
| Dato de regla (hecho resuelto) | `UserExistence` |
| Regla de dominio (contrato) | `UserMustExistRule` |
| Dominio (agregado / criteria) | `SecurityUser` |
| Caso de uso (impl) | `AssignTenantUseCaseImpl`, `ListUsersUseCaseImpl`, `ProvisionIdentityUseCaseImpl`, `ResolveExternalIdentityUseCaseImpl` |
| Caso de uso (contrato) | `AssignTenantUseCase`, `ListUsersUseCase`, `ProvisionIdentityUseCase`, `ResolveExternalIdentityUseCase` |
| Validador de reglas (impl) | `SubjectUserIdLookupValidatorImpl`, `UserMustExistValidatorImpl` |
| Validador de reglas (contrato) | `SubjectUserIdLookupValidator`, `UserMustExistValidator` |
| Puerto de salida | `SecurityUserRepository` |
| DTO de entrada al nucleo | `AssignTenantRequest`, `ProvisionIdentityRequest`, `ResolveExternalIdentityRequest` |
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

### `profiles` - 48 clases

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
| Validador de reglas (impl) | `AddRoleToProfileRulesValidatorImpl`, `DefineProfileRulesValidatorImpl`, `ProfileApplicationLookupValidatorImpl`, `ProfileNamesLookupValidatorImpl`, `ProfileRolesLookupValidatorImpl` |
| Validador de reglas (contrato) | `AddRoleToProfileRulesValidator`, `DefineProfileRulesValidator`, `ProfileApplicationLookupValidator`, `ProfileNamesLookupValidator`, `ProfileRolesLookupValidator` |
| Puerto de salida | `ProfileRepository` |
| DTO de entrada al nucleo | `AddRoleToProfileRequest`, `DefineProfileRequest`, `ListProfilesRequest`, `ProfileOwnershipQuery` |
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

### `resources` - 54 clases

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
| Validador de reglas (impl) | `ProtectedResourceIdLookupValidatorImpl`, `ProtectedResourceMustExistValidatorImpl`, `ProtectedResourceOwnerLookupValidatorImpl`, `RegisterProtectedResourceRulesValidatorImpl` |
| Validador de reglas (contrato) | `ProtectedResourceIdLookupValidator`, `ProtectedResourceMustExistValidator`, `ProtectedResourceOwnerLookupValidator`, `RegisterProtectedResourceRulesValidator` |
| Puerto de salida | `ProtectedResourceRepository` |
| DTO de entrada al nucleo | `ProtectedResourceLookup`, `RegisterApplicationWithInitialResourceRequest`, `RegisterProtectedResourceRequest` |
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

### `roles` - 67 clases

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
| Validador de reglas (impl) | `DefineRoleRulesValidatorImpl`, `GrantResourceRulesValidatorImpl`, `RoleApplicationLookupValidatorImpl`, `RoleLookupByNameInScopeValidatorImpl`, `RoleMustExistForTenantValidatorImpl`, `RoleNamesLookupValidatorImpl`, `RoleResourcesLookupValidatorImpl`, `RoleScopeMustCoverApplicationValidatorImpl` |
| Validador de reglas (contrato) | `DefineRoleRulesValidator`, `GrantResourceRulesValidator`, `RoleApplicationLookupValidator`, `RoleLookupByNameInScopeValidator`, `RoleMustExistForTenantValidator`, `RoleNamesLookupValidator`, `RoleResourcesLookupValidator`, `RoleScopeMustCoverApplicationValidator` |
| Puerto de salida | `RoleRepository` |
| DTO de entrada al nucleo | `DefineRoleRequest`, `GrantResourceRequest`, `ListRolesRequest`, `RoleCoverageQuery`, `RoleNameInScopeQuery`, `RoleOwnershipQuery` |
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
| `audit` | `AdministrationAuditRepository`, `AdministrationEvent`, `AdministrationOperation`, `AdministrationOutcome` |
| `auth/model` | `OidcFlowIntent` |
| `auth/service` | `KeycloakOidcSessionService`, `OidcAuthenticationFailureHandler`, `OidcAuthenticationSuccessHandler`, `OidcAuthorizationFlowService`, `OidcFlowStateService`, `OidcRedirectPolicy`, `OidcReturnTargetPolicy` |
| `auth/web` | `KeycloakLoginController`, `KeycloakRegistrationController` |
| `cache` | `ActiveRolesCacheRetentionProperties`, `DistributedCachePort`, `ObservedDistributedCachePort`, `RedisDistributedCachePort` |
| `config` | `EventPublisherConfiguration`, `InternalSecurityConfiguration`, `KeycloakSecurityConfiguration`, `ProjectPackages`, `RedisConfiguration`, `SecurityConfiguration`, `SharedPortsConfiguration`, `SurrealDbConfiguration`, `TelemetryConfiguration` |
| `contract` | `Operation`, `OperationWithoutResult`, `ReactiveOperation`, `ReactiveOperationWithoutInput`, `ReactiveOperationWithoutResult`, `ReactiveStreamOperation` |
| `event` | `DomainEvent`, `DomainEventPublisher`, `SpringDomainEventPublisher` |
| `message` | `RequiredArgumentMessages` |
| `observability` | `ReactiveLogContext`, `ReactiveTelemetry` |
| `persistence/surrealdb` | `SurrealDbClient`, `SurrealDbException`, `SurrealDbHealthIndicator`, `SurrealDbProperties`, `SurrealRecordId`, `SurrealSchemaInitializer` |
| `port` | `CredentialHasher`, `IdentifierGenerator`, `SecretGenerator`, `TimeProvider` |
| `security` | `ApiAccessDeniedHandler`, `ApiAuthenticationEntryPoint`, `CorsProperties`, `InternalEvidenceJwtProperties`, `InternalMtlsProperties`, `InternalMtlsWebFilter`, `JwtSecurityProperties`, `KeycloakSessionProperties`, `LocalUserPrincipal`, `PdpPrincipal`, `RevocationRetentionProperties`, `SecurityContext` |
| `security/mfa` | `AuthenticationContextEvidence`, `MfaEvidenceProperties` |
| `security/revocation` | `RedisTokenRevocationAdapter`, `RevocationAwareJwtDecoder`, `TokenRevocationPort` |
| `web` | `ApiResponse`, `CorrelationWebFilter`, `PageResponse`, `RequestContext`, `RequestFieldParser` |
| `web/exception` | `ConflictingRequestParametersException`, `MalformedRequestFieldException`, `MissingRequestFieldException`, `RequestContractException` |
| `web/exceptionhandler` | `ApiErrorHandler` |
| `web/message` | `WebContractMessages` |
| `web/session` | `InternalBffSessionTokenController`, `KeycloakLogoutController`, `SessionController`, `SessionResponse` |

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
| GET | `/api/v1/applications/{applicationId}/administrators` | `ApplicationAdministratorController` |
| POST | `/api/v1/applications/{applicationId}/administrators` | `ApplicationAdministratorController` |
| DELETE | `/api/v1/applications/{applicationId}/administrators/{userId}` | `ApplicationAdministratorController` |
| POST | `/api/v1/applications/{applicationId}/credential-rotations` | `ApplicationAdministrationController` |
| GET | `/api/v1/applications/{applicationId}/resources` | `ProtectedResourceController` |
| POST | `/api/v1/applications/{applicationId}/resources` | `ResourceAdministrationController` |
| POST | `/api/v1/applications/with-initial-resource` | `ApplicationWithInitialResourceController` |
| POST | `/api/v1/authorize` | `AuthorizationController` |
| GET | `/api/v1/profiles` | `ProfileController` |
| POST | `/api/v1/profiles` | `ProfileAdministrationController` |
| GET | `/api/v1/profiles/{profileId}/assignments` | `ProfileAssignmentController` |
| POST | `/api/v1/profiles/{profileId}/assignments` | `ProfileAssignmentAdministrationController` |
| DELETE | `/api/v1/profiles/{profileId}/assignments/{profileAssignmentId}` | `ProfileAssignmentAdministrationController` |
| POST | `/api/v1/profiles/{profileId}/roles` | `ProfileAdministrationController` |
| GET | `/api/v1/roles` | `RoleController` |
| POST | `/api/v1/roles` | `RoleAdministrationController` |
| GET | `/api/v1/roles/{roleId}/assignments` | `AssignmentController` |
| POST | `/api/v1/roles/{roleId}/assignments` | `AssignmentAdministrationController` |
| DELETE | `/api/v1/roles/{roleId}/assignments/{assignmentId}` | `AssignmentAdministrationController` |
| POST | `/api/v1/roles/{roleId}/resources` | `RoleAdministrationController` |
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
| `pdp/applications` | `ApplicationBaseUrlTests`, `ApplicationCatalogPropertiesTests`, `ApplicationControllerTests`, `ApplicationCredentialHashTests`, `ApplicationCredentialMustBeValidRuleImplTests`, `ApplicationCredentialValidationResponseMapperTests`, `ApplicationCriteriaTests`, `ApplicationHttpTests`, `ApplicationMustExistForTenantValidatorTests`, `ApplicationOwnerLookupValidatorImplTests`, `ApplicationPersistenceMapperTests`, `ApplicationRepositoryTests`, `ApplicationResponseMapperTests`, `ApplicationRuleTests`, `ApplicationTests`, `InternalApplicationCredentialControllerTests`, `ListApplicationsRequestMapperTests`, `ListApplicationsUseCaseImplTests`, `RegisterApplicationRulesValidatorTests`, `RegisterApplicationUseCaseImplTests`, `RemoveApplicationUseCaseImplTests`, `RotateApplicationCredentialUseCaseImplTests`, `ValidateApplicationCredentialInteractorImplTests`, `ValidateApplicationCredentialRequestMapperTests`, `ValidateApplicationCredentialUseCaseImplTests` |
| `pdp/assignments` | `ApplicationRegistrationControllerTests`, `AssignApplicationAdministratorInteractorImplTests`, `AssignApplicationAdministratorUseCaseImplTests`, `AssignmentApplicationLookupValidatorImplTests`, `AssignmentControllerTests`, `AssignmentCriteriaTests`, `AssignmentHttpTests`, `AssignmentMustExistForTenantRuleImplTests`, `AssignmentMustNotDuplicateActiveRuleImplTests`, `AssignmentPersistenceMapperTests`, `AssignmentResponseMapperTests`, `AssignmentTests`, `AssignProfileUseCaseImplTests`, `AssignRoleRulesValidatorImplTests`, `AssignRoleUseCaseImplTests`, `InternalApplicationAdministratorControllerTests`, `LastAdministratorMustNotBeRevokedRuleImplTests`, `ListApplicationAdministratorsUseCaseImplTests`, `ListAssignmentsRequestMapperTests`, `ListAssignmentsUseCaseImplTests`, `ListProfileAssignmentsRequestMapperTests`, `ListProfileAssignmentsUseCaseImplTests`, `ProfileAssignmentApplicationLookupValidatorImplTests`, `ProfileAssignmentControllerTests`, `ProfileAssignmentCriteriaTests`, `ProfileAssignmentHttpTests`, `ProfileAssignmentMustExistForTenantRuleImplTests`, `ProfileAssignmentMustNotDuplicateActiveRuleImplTests`, `ProfileAssignmentPersistenceMapperTests`, `ProfileAssignmentRepositoryTests`, `ProfileAssignmentResponseMapperTests`, `ProfileAssignmentTests`, `RegisterApplicationWithFirstAdministratorRequestMapperTests`, `RegisterApplicationWithFirstAdministratorUseCaseImplTests`, `RemoveApplicationAdministratorUseCaseImplTests`, `ResolveActiveRolesUseCaseImplTests`, `RevokeAssignmentRulesValidatorImplTests`, `RevokeAssignmentUseCaseImplTests`, `RevokeProfileAssignmentUseCaseImplTests`, `ValidityTests` |
| `pdp/authorization` | `AccessDecisionInternalResponseMapperTests`, `AccessDecisionRawRequestMapperTests`, `AccessDecisionResponseMapperTests`, `AccessEventPersistenceMapperTests`, `AccessEventTests`, `ActiveRoleNamesLookupValidatorImplTests`, `AdministerApplicationAdministratorAssignmentInteractorImplTests`, `AdministerApplicationAdministratorAssignmentUseCaseImplTests`, `AdministerApplicationAdministratorListInteractorImplTests`, `AdministerApplicationAdministratorListUseCaseImplTests`, `AdministerApplicationAdministratorRemovalInteractorImplTests`, `AdministerApplicationAdministratorRemovalUseCaseImplTests`, `AdministerApplicationCredentialRotationUseCaseImplTests`, `AdministerApplicationRemovalUseCaseImplTests`, `AdministerAssignmentCreationInteractorImplTests`, `AdministerAssignmentCreationUseCaseImplTests`, `AdministerAssignmentRevocationInteractorImplTests`, `AdministerAssignmentRevocationUseCaseImplTests`, `AdministerProfileAssignmentCreationInteractorImplTests`, `AdministerProfileAssignmentCreationUseCaseImplTests`, `AdministerProfileAssignmentRevocationInteractorImplTests`, `AdministerProfileAssignmentRevocationUseCaseImplTests`, `AdministerProfileDefinitionInteractorImplTests`, `AdministerProfileDefinitionUseCaseImplTests`, `AdministerProfileRoleAdditionInteractorImplTests`, `AdministerProfileRoleAdditionUseCaseImplTests`, `AdministerResourceGrantInteractorImplTests`, `AdministerResourceGrantUseCaseImplTests`, `AdministerResourceRegistrationInteractorImplTests`, `AdministerResourceRegistrationUseCaseImplTests`, `AdministerRoleDefinitionInteractorImplTests`, `AdministerRoleDefinitionUseCaseImplTests`, `ApplicationAdministrationControllerTests`, `ApplicationAdministrationRequestMapperTests`, `ApplicationAdministratorControllerTests`, `AssignmentAdministrationControllerTests`, `AuthorizationControllerTests`, `AuthorizationHttpTests`, `AuthorizeAdministrationUseCaseImplTests`, `AuthorizeRequestMapperTests`, `AuthorizeUseCaseImplTests`, `DecisionStateTests`, `DefineRoleRequestMapperTests`, `EvaluateInternalAccessUseCaseImplTests`, `GrantResourceRequestMapperTests`, `InternalAccessDecisionControllerTests`, `InternalAccessDecisionInteractorImplTests`, `InternalSecurityChainIntegrationTests`, `MfaAwareApplicationAdministratorValidatorTests`, `ObservedAccessAuditRepositoryTests`, `OpaAdministrationDecisionAdapterTests`, `OpaFixtureServer`, `OpaPolicyDecisionAdapterTests`, `PrincipalMustBeApplicationAdministratorValidatorImplTests`, `ProfileAdministrationControllerTests`, `ProfileAssignmentAdministrationControllerTests`, `RegisterProtectedResourceRequestMapperTests`, `ResourceAdministrationControllerTests`, `RoleAdministrationControllerTests` |
| `pdp/commons` | `PageWindowTests`, `ResultPageTests`, `ValueObjectTests` |
| `pdp/identity` | `AssignTenantRequestMapperTests`, `AssignTenantUseCaseImplTests`, `EmailTests`, `ProvisionIdentityUseCaseImplTests`, `SecurityUserPersistenceMapperTests`, `SubjectUserIdLookupValidatorImplTests`, `UserControllerTests`, `UserMustExistRuleTests`, `UserMustExistValidatorImplTests`, `UserResponseMapperTests` |
| `pdp/profiles` | `AddRoleToProfileUseCaseImplTests`, `DefineProfileUseCaseImplTests`, `ListProfilesInteractorImplTests`, `ListProfilesRequestMapperTests`, `ListProfilesUseCaseImplTests`, `ProfileApplicationLookupValidatorImplTests`, `ProfileControllerTests`, `ProfileCriteriaTests`, `ProfileHttpTests`, `ProfileMustExistForTenantRuleImplTests`, `ProfileNameMustBeUniqueInScopeRuleImplTests`, `ProfileNameTests`, `ProfilePersistenceMapperTests`, `ProfileRepositoryTests`, `ProfileResponseMapperTests`, `ProfileRolesLookupValidatorImplTests`, `ProfileTests` |
| `pdp/resources` | `ApplicationWithInitialResourceControllerTests`, `ApplicationWithInitialResourceHttpTests`, `HttpVerbTests`, `InMemoryAuditAdapterTests`, `ProtectedResourceControllerTests`, `ProtectedResourceMustBeUniqueRuleTests`, `ProtectedResourceMustExistRuleTests`, `ProtectedResourceMustExistValidatorTests`, `ProtectedResourceOwnerLookupValidatorImplTests`, `ProtectedResourceResponseMapperTests`, `RegisterApplicationWithInitialResourceRequestMapperTests`, `RegisterApplicationWithInitialResourceUseCaseImplTests`, `RegisterProtectedResourceRulesValidatorTests`, `RegisterProtectedResourceUseCaseImplTests`, `ResourcePathTests` |
| `pdp/roles` | `DefineRoleRulesValidatorImplTests`, `DefineRoleUseCaseImplTests`, `GrantResourceRulesValidatorImplTests`, `GrantResourceToRoleUseCaseImplTests`, `ListRolesRequestMapperTests`, `ListRolesUseCaseImplTests`, `RoleApplicationLookupValidatorImplTests`, `RoleControllerTests`, `RoleCriteriaTests`, `RoleHttpTests`, `RoleMustExistForTenantRuleImplTests`, `RoleMustExistForTenantValidatorImplTests`, `RoleNameMustBeUniqueInScopeRuleImplTests`, `RoleNamesLookupValidatorImplTests`, `RoleNameTests`, `RolePersistenceMapperTests`, `RoleResponseMapperTests`, `RoleScopeLevelTests`, `RoleScopeMustCoverApplicationRuleImplTests`, `RoleScopeMustCoverApplicationValidatorImplTests`, `RoleScopeMustCoverResourceRuleImplTests`, `RoleScopeTests`, `RoleTests` |
| `pdp/tenants` | `CreateTenantRequestMapperTests`, `CreateTenantUseCaseImplTests`, `ListTenantsUseCaseImplTests`, `TenantCatalogPropertiesTests`, `TenantControllerTests`, `TenantMustBeActiveValidatorTests`, `TenantNameTests`, `TenantResponseMapperTests`, `TenantRuleTests` |
| `raiz` | `AbstractRedisIntegrationTest`, `AbstractSurrealDbIntegrationTest`, `LayeredArchitectureTests`, `ModulithStructureTests`, `PdpApplicationTests` |
| `shared/audit` | `TestAdministrationAuditRepositories` |
| `shared/auth` | `KeycloakLoginControllerTests`, `KeycloakOidcSessionServiceTests`, `KeycloakRegistrationControllerTests`, `OidcAuthenticationFailureHandlerTests`, `OidcAuthenticationSuccessHandlerTests`, `OidcAuthorizationFlowServiceTests`, `OidcReturnTargetPolicyTests` |
| `shared/cache` | `ObservedDistributedCachePortTests`, `RedisDistributedCachePortTests` |
| `shared/config` | `CorsConfigurationTests`, `KeycloakSecurityConfigurationTests`, `SecurityConfigurationTests` |
| `shared/persistence` | `SurrealDbClientTests`, `SurrealRecordIdTests`, `SurrealRepositoryIntegrationTests` |
| `shared/security` | `ApiAccessDeniedHandlerTests`, `ApiAuthenticationEntryPointTests`, `CorsPropertiesTests`, `InternalMtlsWebFilterTests`, `JwtSecurityPropertiesTests`, `MfaEvidencePropertiesTests`, `PdpPrincipalSecurityContextTests`, `RedisTokenRevocationAdapterTests`, `RevocationAwareJwtDecoderTests`, `SecurityWebFilterChainTests`, `TestJwtSupport` |
| `shared/web` | `ApiErrorHandlerTests`, `CorrelationWebFilterTests`, `KeycloakLogoutControllerTests`, `SessionControllerTests`, `WebContractMessagesTests` |
