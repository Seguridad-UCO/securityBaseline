package co.edu.uco.seguridad.pdp.authorization.infrastructure.config;

import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationMustExistForTenantValidator;
import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationOwnerLookupValidator;
import co.edu.uco.seguridad.pdp.applications.application.usecase.RemoveApplicationUseCase;
import co.edu.uco.seguridad.pdp.applications.application.usecase.RotateApplicationCredentialUseCase;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.ResolveActiveRolesUseCase;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.ResolveAuthorizationSubjectFactsUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.ActiveRoleNamesLookupValidator;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.impl.ActiveRoleNamesLookupValidatorImpl;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.impl.PrincipalMustBeApplicationAdministratorValidatorImpl;
import co.edu.uco.seguridad.pdp.authorization.application.secondaryport.AccessAuditRepository;
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
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.persistence.schema.SurrealAccessEventSchemaInitializer;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.observability.ObservedAccessAuditRepository;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.policy.OpaPolicyDecisionAdapter;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.policy.OpaAdministrationDecisionAdapter;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.properties.OpaProperties;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.SubjectUserIdLookupValidator;
import co.edu.uco.seguridad.pdp.resources.application.rule.validator.ProtectedResourceMustExistValidator;
import co.edu.uco.seguridad.pdp.resources.application.rule.validator.ProtectedResourceIdLookupValidator;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.RoleNamesLookupValidator;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import co.edu.uco.seguridad.shared.observability.ReactiveTelemetry;
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
@EnableConfigurationProperties(OpaProperties.class)
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
    EvaluateInternalAccessUseCase evaluateInternalAccessUseCase(ApplicationOwnerLookupValidator ownerLookup,
            AuthorizeUseCase authorizeUseCase, IdentifierGenerator identifiers, TimeProvider time) {
        return new EvaluateInternalAccessUseCaseImpl(ownerLookup, authorizeUseCase, identifiers, time);
    }

    @Bean
    InternalAccessDecisionInteractor internalAccessDecisionInteractor(EvaluateInternalAccessUseCase useCase) {
        return new InternalAccessDecisionInteractorImpl(useCase);
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

    @Bean
    PrincipalMustBeApplicationAdministratorValidator principalMustBeApplicationAdministratorValidator(
            AuthorizeAdministrationUseCase useCase) {
        return new PrincipalMustBeApplicationAdministratorValidatorImpl(useCase);
    }

    // HU-015 — endpoints administrativos gateados. Viven aquí, no en "applications": es el módulo
    // que ya depende de ella (ver PLAN-HU-015.md §0).
    @Bean
    AdministerApplicationRemovalUseCase administerApplicationRemovalUseCase(
            PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator, RemoveApplicationUseCase removeApplication) {
        return new AdministerApplicationRemovalUseCaseImpl(mustBeAdministrator, removeApplication);
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
            RotateApplicationCredentialUseCase rotateCredential) {
        return new AdministerApplicationCredentialRotationUseCaseImpl(mustBeAdministrator, rotateCredential);
    }

    @Bean
    ApplicationCredentialRotationInteractor applicationCredentialRotationInteractor(
            AdministerApplicationCredentialRotationUseCase useCase, SubjectUserIdLookupValidator subjectUserIdLookup) {
        return new ApplicationCredentialRotationInteractorImpl(useCase, subjectUserIdLookup);
    }
}
