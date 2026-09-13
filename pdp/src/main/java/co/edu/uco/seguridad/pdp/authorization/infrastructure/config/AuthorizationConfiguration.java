package co.edu.uco.seguridad.pdp.authorization.infrastructure.config;

import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationMustExistForTenantValidator;
import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationOwnerLookupValidator;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.ResolveActiveRolesUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.ActiveRoleNamesLookupValidator;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.impl.ActiveRoleNamesLookupValidatorImpl;
import co.edu.uco.seguridad.pdp.authorization.application.secondaryport.AccessAuditRepository;
import co.edu.uco.seguridad.pdp.authorization.application.secondaryport.PolicyDecisionPort;
import co.edu.uco.seguridad.pdp.authorization.application.service.AuthorizationContextResolver;
import co.edu.uco.seguridad.pdp.authorization.application.service.impl.AuthorizationContextResolverImpl;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AuthorizeUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.EvaluateInternalAccessUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.impl.AuthorizeUseCaseImpl;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.impl.EvaluateInternalAccessUseCaseImpl;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.AuthorizeInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.InternalAccessDecisionInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl.AuthorizeInteractorImpl;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl.InternalAccessDecisionInteractorImpl;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.persistence.repository.SurrealAccessAuditRepository;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.persistence.schema.SurrealAccessEventSchemaInitializer;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.policy.OpaPolicyDecisionAdapter;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.properties.OpaProperties;
import co.edu.uco.seguridad.pdp.resources.application.rule.validator.ProtectedResourceMustExistValidator;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.RoleNamesLookupValidator;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import co.edu.uco.seguridad.shared.observability.ReactiveTelemetry;
import io.micrometer.observation.ObservationRegistry;
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
    AccessAuditRepository accessAuditRepository(SurrealDbClient client) {
        return new SurrealAccessAuditRepository(client);
    }

    @Bean
    ApplicationRunner accessEventSchemaInitializer(SurrealDbClient client) {
        return new SurrealAccessEventSchemaInitializer(client);
    }

    @Bean
    AuthorizationContextResolver authorizationContextResolver() {
        return new AuthorizationContextResolverImpl();
    }

    @Bean
    AuthorizeUseCase authorizeUseCase(ApplicationMustExistForTenantValidator applicationMustExist,
            ProtectedResourceMustExistValidator resourceMustExist, ActiveRoleNamesLookupValidator rolesLookup,
            AuthorizationContextResolver contextResolver, AccessAuditRepository audit, PolicyDecisionPort policyDecisionPort, IdentifierGenerator identifiers,
            TimeProvider time, ObservationRegistry observations) {
        var delegate = new AuthorizeUseCaseImpl(applicationMustExist, resourceMustExist, rolesLookup, contextResolver, audit,
                policyDecisionPort, identifiers, time);
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
}
