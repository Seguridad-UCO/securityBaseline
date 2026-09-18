package co.edu.uco.seguridad.pdp.authorization.infrastructure.config;

import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationMustExistForTenantValidator;
import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationNameLookupValidator;
import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationOwnerLookupValidator;
import co.edu.uco.seguridad.pdp.applications.application.usecase.RemoveApplicationUseCase;
import co.edu.uco.seguridad.pdp.applications.application.usecase.RotateApplicationCredentialUseCase;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.AssignApplicationAdministratorUseCase;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.ListApplicationAdministratorsUseCase;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.RemoveApplicationAdministratorUseCase;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.ResolveActiveRolesUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerRoleDefinitionUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerResourceGrantUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.impl.AdministerRoleDefinitionUseCaseImpl;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.impl.AdministerResourceGrantUseCaseImpl;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.AdministerRoleDefinitionInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.AdministerResourceGrantInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl.AdministerRoleDefinitionInteractorImpl;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl.AdministerResourceGrantInteractorImpl;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.RoleApplicationLookupValidator;
import co.edu.uco.seguridad.pdp.roles.application.usecase.DefineRoleUseCase;
import co.edu.uco.seguridad.pdp.roles.application.usecase.GrantResourceToRoleUseCase;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.ResolveAuthorizationSubjectFactsUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.ActiveRoleNamesLookupValidator;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.impl.ActiveRoleNamesLookupValidatorImpl;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.impl.MfaAwareApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.impl.PrincipalMustBeApplicationAdministratorValidatorImpl;
import co.edu.uco.seguridad.pdp.authorization.application.secondaryport.AccessAuditRepository;
import co.edu.uco.seguridad.shared.audit.AdministrationAuditRepository;
import co.edu.uco.seguridad.pdp.authorization.application.secondaryport.AdministrationDecisionPort;
import co.edu.uco.seguridad.pdp.authorization.application.secondaryport.PolicyDecisionPort;
import co.edu.uco.seguridad.pdp.authorization.application.service.AuthorizationContextResolver;
import co.edu.uco.seguridad.pdp.authorization.application.service.impl.AuthorizationContextResolverImpl;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerApplicationCredentialRotationUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerApplicationRemovalUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AuthorizeAdministrationUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AuthorizeUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.EvaluateInternalAccessUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.impl.AdministerApplicationCredentialRotationUseCaseImpl;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.impl.AdministerApplicationRemovalUseCaseImpl;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.impl.AuthorizeAdministrationUseCaseImpl;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.impl.AuthorizeUseCaseImpl;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.impl.EvaluateInternalAccessUseCaseImpl;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.ApplicationCredentialRotationInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.ApplicationRemovalInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.AuthorizeInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.InternalAccessDecisionInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl.ApplicationCredentialRotationInteractorImpl;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl.ApplicationRemovalInteractorImpl;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl.AuthorizeInteractorImpl;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl.InternalAccessDecisionInteractorImpl;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.persistence.repository.SurrealAccessAuditRepository;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.persistence.repository.SurrealAdministrationAuditRepository;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.persistence.schema.SurrealAccessEventSchemaInitializer;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.observability.ObservedAccessAuditRepository;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.observability.ObservedAdministrationAuditRepository;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.policy.OpaPolicyDecisionAdapter;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.policy.OpaAdministrationDecisionAdapter;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.properties.OpaProperties;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.SubjectUserIdLookupValidator;
import co.edu.uco.seguridad.pdp.identity.application.usecase.ResolveExternalIdentityUseCase;
import co.edu.uco.seguridad.pdp.resources.application.rule.validator.ProtectedResourceMustExistValidator;
import co.edu.uco.seguridad.pdp.resources.application.rule.validator.ProtectedResourceIdLookupValidator;
import co.edu.uco.seguridad.pdp.resources.application.usecase.RegisterProtectedResourceUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerResourceRegistrationUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.impl.AdministerResourceRegistrationUseCaseImpl;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.AdministerResourceRegistrationInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl.AdministerResourceRegistrationInteractorImpl;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.RoleNamesLookupValidator;
import co.edu.uco.seguridad.pdp.assignments.application.rule.validator.AssignmentApplicationLookupValidator;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.AssignRoleUseCase;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.RevokeAssignmentUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerAssignmentCreationUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerAssignmentRevocationUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.impl.AdministerAssignmentCreationUseCaseImpl;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.impl.AdministerAssignmentRevocationUseCaseImpl;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.AdministerAssignmentCreationInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.AdministerAssignmentRevocationInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl.AdministerAssignmentCreationInteractorImpl;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl.AdministerAssignmentRevocationInteractorImpl;
import co.edu.uco.seguridad.pdp.assignments.application.rule.validator.ProfileAssignmentApplicationLookupValidator;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.AssignProfileUseCase;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.RevokeProfileAssignmentUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerProfileDefinitionUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerProfileRoleAdditionUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerProfileAssignmentCreationUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerProfileAssignmentRevocationUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.impl.AdministerProfileDefinitionUseCaseImpl;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.impl.AdministerProfileRoleAdditionUseCaseImpl;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.impl.AdministerProfileAssignmentCreationUseCaseImpl;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.impl.AdministerProfileAssignmentRevocationUseCaseImpl;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.AdministerProfileDefinitionInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.AdministerProfileRoleAdditionInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.AdministerProfileAssignmentCreationInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.AdministerProfileAssignmentRevocationInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl.AdministerProfileDefinitionInteractorImpl;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl.AdministerProfileRoleAdditionInteractorImpl;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl.AdministerProfileAssignmentCreationInteractorImpl;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl.AdministerProfileAssignmentRevocationInteractorImpl;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerApplicationAdministratorAssignmentUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerApplicationAdministratorRemovalUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerApplicationAdministratorListUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.impl.AdministerApplicationAdministratorAssignmentUseCaseImpl;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.impl.AdministerApplicationAdministratorRemovalUseCaseImpl;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.impl.AdministerApplicationAdministratorListUseCaseImpl;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.AdministerApplicationAdministratorAssignmentInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.AdministerApplicationAdministratorRemovalInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.AdministerApplicationAdministratorListInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl.AdministerApplicationAdministratorAssignmentInteractorImpl;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl.AdministerApplicationAdministratorRemovalInteractorImpl;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl.AdministerApplicationAdministratorListInteractorImpl;
import co.edu.uco.seguridad.pdp.profiles.application.rule.validator.ProfileApplicationLookupValidator;
import co.edu.uco.seguridad.pdp.profiles.application.usecase.AddRoleToProfileUseCase;
import co.edu.uco.seguridad.pdp.profiles.application.usecase.DefineProfileUseCase;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import co.edu.uco.seguridad.shared.observability.ReactiveTelemetry;
import co.edu.uco.seguridad.shared.security.mfa.MfaEvidenceProperties;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.ObjectMapper;

/** La unica clase consciente de Spring del modulo. */
@Configuration
@EnableConfigurationProperties({OpaProperties.class, MfaEvidenceProperties.class})
public class AuthorizationConfiguration {

    // HU-006 (D9 del handoff PDP-PEP-OPA): reemplaza a DenyByDefaultPolicyDecisionAdapter, que se
    // elimina — la denegacion por defecto pasa a vivir en la politica Rego, no en el PDP.
    @Bean
    PolicyDecisionPort policyDecisionPort(OpaProperties properties, ObjectMapper objectMapper,
            WebClient.Builder builder, IdentifierGenerator identifiers, TimeProvider time) {
        WebClient webClient = builder.clone().baseUrl(properties.baseUrl()).build();
        return new OpaPolicyDecisionAdapter(webClient, objectMapper, properties, identifiers, time);
    }

    @Bean
    ActiveRoleNamesLookupValidator activeRoleNamesLookupValidator(ResolveActiveRolesUseCase resolveActiveRoles,
            RoleNamesLookupValidator roleNamesLookup) {
        return new ActiveRoleNamesLookupValidatorImpl(resolveActiveRoles, roleNamesLookup);
    }

    // HU-007 — evidencia de auditoría.
    @Bean
    AccessAuditRepository accessAuditRepository(SurrealDbClient client, MeterRegistry metrics) {
        return new ObservedAccessAuditRepository(new SurrealAccessAuditRepository(client), metrics);
    }

    // HU-021 — auditoría de operaciones administrativas.
    @Bean
    AdministrationAuditRepository administrationAuditRepository(SurrealDbClient client, MeterRegistry metrics) {
        return new ObservedAdministrationAuditRepository(new SurrealAdministrationAuditRepository(client), metrics);
    }

    @Bean
    ApplicationRunner accessEventSchemaInitializer(SurrealDbClient client) {
        return new SurrealAccessEventSchemaInitializer(client);
    }

    @Bean
    AuthorizationContextResolver authorizationContextResolver(ResolveAuthorizationSubjectFactsUseCase facts,
            ProtectedResourceIdLookupValidator resourceIds) {
        return new AuthorizationContextResolverImpl(facts, resourceIds);
    }

    @Bean
    AuthorizeUseCase authorizeUseCase(ApplicationMustExistForTenantValidator applicationMustExist,
            ProtectedResourceMustExistValidator resourceMustExist, ActiveRoleNamesLookupValidator rolesLookup,
            AuthorizationContextResolver contextResolver, AccessAuditRepository audit,
            PolicyDecisionPort policyDecisionPort, IdentifierGenerator identifiers,
            TimeProvider time, ObservationRegistry observations) {
        var delegate = new AuthorizeUseCaseImpl(applicationMustExist, resourceMustExist, rolesLookup, contextResolver,
                audit, policyDecisionPort, identifiers, time);
        return input -> ReactiveTelemetry.observe("security.authorization", observations,
                io.micrometer.common.KeyValues.of("decision", "none", "reason", "none"), () -> delegate.execute(input),
                (observation, decision) -> observation.lowCardinalityKeyValue("decision", decision.state().name())
                        .lowCardinalityKeyValue("reason", decision.reasonCode().name()));
    }

    @Bean
    AuthorizeInteractor authorizeInteractor(AuthorizeUseCase useCase) {
        return new AuthorizeInteractorImpl(useCase);
    }

    // HU-003 — canal interno para el PEP (D1: mismo AuthorizeUseCase, segundo adaptador primario).
    @Bean
    EvaluateInternalAccessUseCase evaluateInternalAccessUseCase(ApplicationNameLookupValidator applicationLookup,
            AuthorizeUseCase authorizeUseCase, IdentifierGenerator identifiers, TimeProvider time) {
        return new EvaluateInternalAccessUseCaseImpl(applicationLookup, authorizeUseCase, identifiers, time);
    }

    @Bean
    InternalAccessDecisionInteractor internalAccessDecisionInteractor(EvaluateInternalAccessUseCase useCase,
            ResolveExternalIdentityUseCase identities) {
        return new InternalAccessDecisionInteractorImpl(useCase, identities);
    }

    @Bean
    AdministrationDecisionPort administrationDecisionPort(OpaProperties properties, ObjectMapper objectMapper,
            WebClient.Builder builder) {
        WebClient webClient = builder.clone().baseUrl(properties.baseUrl()).build();
        return new OpaAdministrationDecisionAdapter(webClient, objectMapper, properties);
    }

    @Bean
    AuthorizeAdministrationUseCase authorizeAdministrationUseCase(ActiveRoleNamesLookupValidator rolesLookup,
            AdministrationDecisionPort administrationDecisionPort) {
        return new AuthorizeAdministrationUseCaseImpl(rolesLookup, administrationDecisionPort);
    }

    // HU-024 — MFA como step-up: decora la implementación de producción con el gate de evidencia,
    // sin que ninguno de los 12 Administer*UseCaseImpl (todos inyectan la interfaz, nunca la clase
    // concreta) necesite cambiar (PLAN-HU-024.md §1.2).
    @Bean
    PrincipalMustBeApplicationAdministratorValidator principalMustBeApplicationAdministratorValidator(
            AuthorizeAdministrationUseCase useCase, MfaEvidenceProperties mfaProperties) {
        return new MfaAwareApplicationAdministratorValidator(
                new PrincipalMustBeApplicationAdministratorValidatorImpl(useCase), mfaProperties);
    }

    // HU-015 — endpoints administrativos gateados. Viven aquí, no en "applications": es el módulo
    // que ya depende de ella (ver PLAN-HU-015.md §0).
    @Bean
    AdministerApplicationRemovalUseCase administerApplicationRemovalUseCase(
            PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator, RemoveApplicationUseCase removeApplication,
            AdministrationAuditRepository audit, IdentifierGenerator identifiers, TimeProvider time) {
        return new AdministerApplicationRemovalUseCaseImpl(mustBeAdministrator, removeApplication, audit, identifiers, time);
    }

    @Bean
    ApplicationRemovalInteractor applicationRemovalInteractor(AdministerApplicationRemovalUseCase useCase,
            SubjectUserIdLookupValidator subjectUserIdLookup) {
        return new ApplicationRemovalInteractorImpl(useCase, subjectUserIdLookup);
    }

    // Caso de uso e interactor listos; el @PostMapping de credential-rotations sigue en
    // ApplicationController (applications) hasta que el implementador lo traslade aquí en el mismo
    // cambio que lo retira de allí (ver PLAN-HU-015.md árbol §8).
    @Bean
    AdministerApplicationCredentialRotationUseCase administerApplicationCredentialRotationUseCase(
            PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator,
            RotateApplicationCredentialUseCase rotateCredential, AdministrationAuditRepository audit,
            IdentifierGenerator identifiers, TimeProvider time) {
        return new AdministerApplicationCredentialRotationUseCaseImpl(mustBeAdministrator, rotateCredential, audit,
                identifiers, time);
    }

    @Bean
    ApplicationCredentialRotationInteractor applicationCredentialRotationInteractor(
            AdministerApplicationCredentialRotationUseCase useCase, SubjectUserIdLookupValidator subjectUserIdLookup) {
        return new ApplicationCredentialRotationInteractorImpl(useCase, subjectUserIdLookup);
    }

    // HU-016 — gatea DefineRole/GrantResourceToRole (slice roles), mismo motivo que HU-015: vive
    // aquí porque authorization ya depende de roles. El controller que expone estas rutas
    // (RoleAdministrationController) y la baja de RoleController.define()/grantResource() son [M]
    // del implementador (PLAN-HU-016.md §8) — de momento estos beans no quedan enrutados.
    @Bean
    AdministerRoleDefinitionUseCase administerRoleDefinitionUseCase(
            PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator, DefineRoleUseCase defineRole,
            AdministrationAuditRepository audit, IdentifierGenerator identifiers, TimeProvider time) {
        return new AdministerRoleDefinitionUseCaseImpl(mustBeAdministrator, defineRole, audit, identifiers, time);
    }

    @Bean
    AdministerRoleDefinitionInteractor administerRoleDefinitionInteractor(AdministerRoleDefinitionUseCase useCase,
            SubjectUserIdLookupValidator subjectUserIdLookup) {
        return new AdministerRoleDefinitionInteractorImpl(useCase, subjectUserIdLookup);
    }

    @Bean
    AdministerResourceGrantUseCase administerResourceGrantUseCase(
            PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator, GrantResourceToRoleUseCase grantResource,
            AdministrationAuditRepository audit, IdentifierGenerator identifiers, TimeProvider time) {
        return new AdministerResourceGrantUseCaseImpl(mustBeAdministrator, grantResource, audit, identifiers, time);
    }

    @Bean
    AdministerResourceGrantInteractor administerResourceGrantInteractor(AdministerResourceGrantUseCase useCase,
            SubjectUserIdLookupValidator subjectUserIdLookup, RoleApplicationLookupValidator roleApplicationLookup) {
        return new AdministerResourceGrantInteractorImpl(useCase, subjectUserIdLookup, roleApplicationLookup);
    }

    // HU-017 — gatea RegisterProtectedResourceUseCase (slice resources), mismo motivo que HU-015/HU-016:
    // vive aquí porque authorization ya depende de resources. El controller que expone esta ruta
    // (ResourceAdministrationController) y la baja de ProtectedResourceController.register() son [M]
    // del implementador (PLAN-HU-017.md §8) — de momento estos beans no quedan enrutados.
    @Bean
    AdministerResourceRegistrationUseCase administerResourceRegistrationUseCase(
            PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator,
            RegisterProtectedResourceUseCase registerResource, AdministrationAuditRepository audit,
            IdentifierGenerator identifiers, TimeProvider time) {
        return new AdministerResourceRegistrationUseCaseImpl(mustBeAdministrator, registerResource, audit, identifiers, time);
    }

    @Bean
    AdministerResourceRegistrationInteractor administerResourceRegistrationInteractor(
            AdministerResourceRegistrationUseCase useCase, SubjectUserIdLookupValidator subjectUserIdLookup) {
        return new AdministerResourceRegistrationInteractorImpl(useCase, subjectUserIdLookup);
    }

    // HU-018 — gatea AssignRoleUseCase/RevokeAssignmentUseCase (slice assignments), mismo motivo que
    // HU-015/016/017. El controller (AssignmentAdministrationController) y la baja de
    // AssignmentController.assign()/revoke() son [M] del implementador (PLAN-HU-018.md §8) — de
    // momento estos beans no quedan enrutados.
    @Bean
    AdministerAssignmentCreationUseCase administerAssignmentCreationUseCase(
            PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator, AssignRoleUseCase assignRole,
            AdministrationAuditRepository audit, IdentifierGenerator identifiers, TimeProvider time) {
        return new AdministerAssignmentCreationUseCaseImpl(mustBeAdministrator, assignRole, audit, identifiers, time);
    }

    @Bean
    AdministerAssignmentCreationInteractor administerAssignmentCreationInteractor(
            AdministerAssignmentCreationUseCase useCase, SubjectUserIdLookupValidator subjectUserIdLookup) {
        return new AdministerAssignmentCreationInteractorImpl(useCase, subjectUserIdLookup);
    }

    @Bean
    AdministerAssignmentRevocationUseCase administerAssignmentRevocationUseCase(
            PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator, RevokeAssignmentUseCase revokeAssignment,
            AdministrationAuditRepository audit, IdentifierGenerator identifiers, TimeProvider time) {
        return new AdministerAssignmentRevocationUseCaseImpl(mustBeAdministrator, revokeAssignment, audit, identifiers, time);
    }

    @Bean
    AdministerAssignmentRevocationInteractor administerAssignmentRevocationInteractor(
            AdministerAssignmentRevocationUseCase useCase, SubjectUserIdLookupValidator subjectUserIdLookup,
            AssignmentApplicationLookupValidator assignmentApplicationLookup) {
        return new AdministerAssignmentRevocationInteractorImpl(useCase, subjectUserIdLookup, assignmentApplicationLookup);
    }

    // HU-019 — gatea DefineProfileUseCase/AddRoleToProfileUseCase (slice profiles) y
    // AssignProfileUseCase/RevokeProfileAssignmentUseCase (slice assignments), mismo motivo que
    // HU-015/016/017/018. Los dos controllers (ProfileAdministrationController,
    // ProfileAssignmentAdministrationController) y la baja de las escrituras movidas en
    // ProfileController/ProfileAssignmentController son [M] del implementador (PLAN-HU-019.md §8) —
    // de momento estos beans no quedan enrutados.
    @Bean
    AdministerProfileDefinitionUseCase administerProfileDefinitionUseCase(
            PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator, DefineProfileUseCase defineProfile,
            AdministrationAuditRepository audit, IdentifierGenerator identifiers, TimeProvider time) {
        return new AdministerProfileDefinitionUseCaseImpl(mustBeAdministrator, defineProfile, audit, identifiers, time);
    }

    @Bean
    AdministerProfileDefinitionInteractor administerProfileDefinitionInteractor(
            AdministerProfileDefinitionUseCase useCase, SubjectUserIdLookupValidator subjectUserIdLookup) {
        return new AdministerProfileDefinitionInteractorImpl(useCase, subjectUserIdLookup);
    }

    @Bean
    AdministerProfileRoleAdditionUseCase administerProfileRoleAdditionUseCase(
            PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator, AddRoleToProfileUseCase addRoleToProfile,
            AdministrationAuditRepository audit, IdentifierGenerator identifiers, TimeProvider time) {
        return new AdministerProfileRoleAdditionUseCaseImpl(mustBeAdministrator, addRoleToProfile, audit, identifiers, time);
    }

    @Bean
    AdministerProfileRoleAdditionInteractor administerProfileRoleAdditionInteractor(
            AdministerProfileRoleAdditionUseCase useCase, SubjectUserIdLookupValidator subjectUserIdLookup,
            ProfileApplicationLookupValidator profileApplicationLookup) {
        return new AdministerProfileRoleAdditionInteractorImpl(useCase, subjectUserIdLookup, profileApplicationLookup);
    }

    @Bean
    AdministerProfileAssignmentCreationUseCase administerProfileAssignmentCreationUseCase(
            PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator, AssignProfileUseCase assignProfile,
            AdministrationAuditRepository audit, IdentifierGenerator identifiers, TimeProvider time) {
        return new AdministerProfileAssignmentCreationUseCaseImpl(mustBeAdministrator, assignProfile, audit, identifiers, time);
    }

    @Bean
    AdministerProfileAssignmentCreationInteractor administerProfileAssignmentCreationInteractor(
            AdministerProfileAssignmentCreationUseCase useCase, SubjectUserIdLookupValidator subjectUserIdLookup) {
        return new AdministerProfileAssignmentCreationInteractorImpl(useCase, subjectUserIdLookup);
    }

    @Bean
    AdministerProfileAssignmentRevocationUseCase administerProfileAssignmentRevocationUseCase(
            PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator,
            RevokeProfileAssignmentUseCase revokeProfileAssignment, AdministrationAuditRepository audit,
            IdentifierGenerator identifiers, TimeProvider time) {
        return new AdministerProfileAssignmentRevocationUseCaseImpl(mustBeAdministrator, revokeProfileAssignment, audit,
                identifiers, time);
    }

    @Bean
    AdministerProfileAssignmentRevocationInteractor administerProfileAssignmentRevocationInteractor(
            AdministerProfileAssignmentRevocationUseCase useCase, SubjectUserIdLookupValidator subjectUserIdLookup,
            ProfileAssignmentApplicationLookupValidator profileAssignmentApplicationLookup) {
        return new AdministerProfileAssignmentRevocationInteractorImpl(useCase, subjectUserIdLookup,
                profileAssignmentApplicationLookup);
    }

    // HU-020 — autoservicio de administradores.
    @Bean
    AdministerApplicationAdministratorAssignmentUseCase administerApplicationAdministratorAssignmentUseCase(
            PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator,
            AssignApplicationAdministratorUseCase assignApplicationAdministrator, AdministrationAuditRepository audit,
            IdentifierGenerator identifiers, TimeProvider time) {
        return new AdministerApplicationAdministratorAssignmentUseCaseImpl(mustBeAdministrator,
                assignApplicationAdministrator, audit, identifiers, time);
    }

    @Bean
    AdministerApplicationAdministratorRemovalUseCase administerApplicationAdministratorRemovalUseCase(
            PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator,
            RemoveApplicationAdministratorUseCase removeApplicationAdministrator, AdministrationAuditRepository audit,
            IdentifierGenerator identifiers, TimeProvider time) {
        return new AdministerApplicationAdministratorRemovalUseCaseImpl(mustBeAdministrator,
                removeApplicationAdministrator, audit, identifiers, time);
    }

    @Bean
    AdministerApplicationAdministratorListUseCase administerApplicationAdministratorListUseCase(
            PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator,
            ListApplicationAdministratorsUseCase listApplicationAdministrators) {
        return new AdministerApplicationAdministratorListUseCaseImpl(mustBeAdministrator, listApplicationAdministrators);
    }

    @Bean
    AdministerApplicationAdministratorAssignmentInteractor administerApplicationAdministratorAssignmentInteractor(
            ApplicationOwnerLookupValidator ownerLookup, SubjectUserIdLookupValidator subjectUserIdLookup,
            AdministerApplicationAdministratorAssignmentUseCase useCase) {
        return new AdministerApplicationAdministratorAssignmentInteractorImpl(ownerLookup, subjectUserIdLookup, useCase);
    }

    @Bean
    AdministerApplicationAdministratorRemovalInteractor administerApplicationAdministratorRemovalInteractor(
            ApplicationOwnerLookupValidator ownerLookup, SubjectUserIdLookupValidator subjectUserIdLookup,
            AdministerApplicationAdministratorRemovalUseCase useCase) {
        return new AdministerApplicationAdministratorRemovalInteractorImpl(ownerLookup, subjectUserIdLookup, useCase);
    }

    @Bean
    AdministerApplicationAdministratorListInteractor administerApplicationAdministratorListInteractor(
            ApplicationOwnerLookupValidator ownerLookup, SubjectUserIdLookupValidator subjectUserIdLookup,
            AdministerApplicationAdministratorListUseCase useCase) {
        return new AdministerApplicationAdministratorListInteractorImpl(ownerLookup, subjectUserIdLookup, useCase);
    }
}
