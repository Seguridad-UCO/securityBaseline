package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.ApplicationOwnershipQuery;
import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationMustExistForTenantValidator;
import co.edu.uco.seguridad.pdp.applications.domain.exception.ApplicationNotFoundException;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ResolveActiveRolesRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AccessRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.response.AccessDecision;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.ActiveRoleNamesLookupValidator;
import co.edu.uco.seguridad.pdp.authorization.application.secondaryport.AccessAuditRepository;
import co.edu.uco.seguridad.pdp.authorization.application.secondaryport.PolicyDecisionPort;
import co.edu.uco.seguridad.pdp.authorization.application.service.AuthorizationContextResolver;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AuthorizeUseCase;
import co.edu.uco.seguridad.pdp.authorization.domain.event.AccessEvent;
import co.edu.uco.seguridad.pdp.authorization.domain.model.DecisionState;
import co.edu.uco.seguridad.pdp.authorization.domain.model.ReasonCode;
import co.edu.uco.seguridad.pdp.resources.application.primaryport.request.ProtectedResourceLookup;
import co.edu.uco.seguridad.pdp.resources.application.rule.validator.ProtectedResourceMustExistValidator;
import co.edu.uco.seguridad.pdp.resources.domain.exception.ProtectedResourceNotFoundException;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

/**
 * Orquesta: comprueba aplicacion (regla 1) y recurso (regla 2, corta si la 1 ya fallo), y delega
 * en {@link PolicyDecisionPort}. No decide nada: traduce el rechazo ya decidido por el validador
 * ajeno a un {@link ReasonCode}. Fail-closed: cualquier fallo tecnico (no de negocio) en la
 * resolucion del contexto se traduce a INDETERMINATE, nunca a un error que llegue al cliente.
 */
public final class AuthorizeUseCaseImpl implements AuthorizeUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(AuthorizeUseCaseImpl.class);

    private final ApplicationMustExistForTenantValidator applicationMustExist;
    private final ProtectedResourceMustExistValidator resourceMustExist;
    private final ActiveRoleNamesLookupValidator rolesLookup;
    private final AuthorizationContextResolver contextResolver;
    private final AccessAuditRepository audit;
    private final PolicyDecisionPort policyDecisionPort;
    private final IdentifierGenerator identifiers;
    private final TimeProvider time;

    public AuthorizeUseCaseImpl(ApplicationMustExistForTenantValidator applicationMustExist,
                                ProtectedResourceMustExistValidator resourceMustExist, ActiveRoleNamesLookupValidator rolesLookup,
                                AuthorizationContextResolver contextResolver,
                                AccessAuditRepository audit, PolicyDecisionPort policyDecisionPort, IdentifierGenerator identifiers,
                                TimeProvider time) {
        this.applicationMustExist = Objects.requireNonNull(applicationMustExist,
                RequiredArgumentMessages.APPLICATION_EXISTS_VALIDATOR);
        this.resourceMustExist = Objects.requireNonNull(resourceMustExist,
                RequiredArgumentMessages.PROTECTED_RESOURCE_EXISTS_VALIDATOR);
        this.rolesLookup = Objects.requireNonNull(rolesLookup, RequiredArgumentMessages.ACTIVE_ROLE_NAMES_LOOKUP_VALIDATOR);
        this.contextResolver = Objects.requireNonNull(contextResolver);
        this.audit = Objects.requireNonNull(audit, RequiredArgumentMessages.ACCESS_AUDIT_REPOSITORY);
        this.policyDecisionPort = Objects.requireNonNull(policyDecisionPort,
                RequiredArgumentMessages.POLICY_DECISION_PORT);
        this.identifiers = Objects.requireNonNull(identifiers, RequiredArgumentMessages.IDENTIFIER_GENERATOR);
        this.time = Objects.requireNonNull(time, RequiredArgumentMessages.TIME_PROVIDER);
    }

    /**
     * Compatibility constructor for existing in-process callers while the resolver becomes explicit.
     */
    public AuthorizeUseCaseImpl(ApplicationMustExistForTenantValidator applicationMustExist,
                                ProtectedResourceMustExistValidator resourceMustExist, ActiveRoleNamesLookupValidator rolesLookup,
                                AccessAuditRepository audit, PolicyDecisionPort policyDecisionPort, IdentifierGenerator identifiers,
                                TimeProvider time) {
        this(applicationMustExist, resourceMustExist, rolesLookup,
                new co.edu.uco.seguridad.pdp.authorization.application.service.impl.AuthorizationContextResolverImpl(),
                audit, policyDecisionPort, identifiers, time);
    }

    @Override
    public Mono<AccessDecision> execute(AccessRequest input) {
        // Cada paso posterior al primero va en Mono.defer: sin el, la llamada a .execute(...) se
        // evalua al construir la cadena (Java evalua argumentos antes de invocar .then), no cuando
        // Reactor suscribe — el mismo defecto que switchIfEmpty sin defer, aqui aplicado a .then.
        return applicationMustExist.execute(new ApplicationOwnershipQuery(input.tenantId(), input.applicationId()))
                .then(Mono.defer(() -> resourceMustExist.execute(
                        new ProtectedResourceLookup(input.applicationId(), input.resourcePath(), input.action()))))
                .then(Mono.defer(() -> resolveRoles(input)))
                .flatMap(contextResolver::execute)
                .flatMap(policyDecisionPort::decide)
                .onErrorResume(ApplicationNotFoundException.class,
                        error -> Mono.just(deny(input, ReasonCode.TENANT_MISMATCH)))
                .onErrorResume(ProtectedResourceNotFoundException.class,
                        error -> Mono.just(deny(input, ReasonCode.NO_APPLICABLE_POLICY)))
                .onErrorResume(error -> Mono.just(indeterminate(input)))
                .flatMap(decision -> recordAudit(input, decision));
    }

    /**
     * Se alcanza sin importar qué camino produjo la decisión — el último paso de la cadena, después
     * de los tres {@code onErrorResume} (HU-007, criterio 1). Un fallo al auditar se registra por
     * log y nunca cambia la decisión ya calculada (criterio 5): {@code onErrorResume} aquí no es
     * fail-closed hacia el cliente, es "la auditoría es un problema de auditoría, no de la petición".
     */
    private Mono<AccessDecision> recordAudit(AccessRequest input, AccessDecision decision) {
        AccessEvent event = new AccessEvent(identifiers.next(), decision.decisionId(), input.requestId(),
                input.correlationId(), input.tenantId(), input.applicationId(), input.subject(),
                input.resourcePath(), input.action(), decision.state(), decision.reasonCode(), time.now());
        return audit.save(event)
                .onErrorResume(error -> {
                    LOG.error("no se pudo registrar la evidencia de auditoría para la decisión {}",
                            decision.decisionId(), error);
                    return Mono.empty();
                })
                .thenReturn(decision);
    }

    /**
     * Sin {@code subjectUserId} (canal interno, o BFF sin sesión resuelta) no hay con qué preguntar
     * por roles — el {@code AccessRequest} sigue con {@code subjectRoles} vacío, exactamente como
     * llegó. Con él, delega en {@link #rolesLookup} (HU-008, §0 del plan) y devuelve el mismo
     * request enriquecido con los nombres resueltos.
     */
    private Mono<AccessRequest> resolveRoles(AccessRequest input) {
        return input.subjectUserId()
                .map(userId -> rolesLookup.execute(new ResolveActiveRolesRequest(userId, input.applicationId()))
                        .map(input::withSubjectRoles))
                .orElseGet(() -> Mono.just(input));
    }

    private AccessDecision deny(AccessRequest input, ReasonCode reasonCode) {
        return new AccessDecision(identifiers.next(), DecisionState.DENY, reasonCode, List.of(),
                input.requestId(), input.correlationId(), time.now());
    }

    private AccessDecision indeterminate(AccessRequest input) {
        return new AccessDecision(identifiers.next(), DecisionState.INDETERMINATE, ReasonCode.CONTEXT_UNAVAILABLE,
                List.of(), input.requestId(), input.correlationId(), time.now());
    }
}
