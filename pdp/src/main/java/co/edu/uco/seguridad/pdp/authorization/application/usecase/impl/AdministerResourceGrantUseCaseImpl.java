package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerResourceGrantRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerResourceGrantUseCase;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.NotAuthorizedToAdministerException;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.RevokeResourceRequest;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.response.RoleResponse;
import co.edu.uco.seguridad.pdp.roles.application.usecase.GrantResourceToRoleUseCase;
import co.edu.uco.seguridad.pdp.roles.application.usecase.RevokeResourceFromRoleUseCase;
import co.edu.uco.seguridad.shared.audit.AdministrationAuditRepository;
import co.edu.uco.seguridad.shared.audit.AdministrationEvent;
import co.edu.uco.seguridad.shared.audit.AdministrationOperation;
import co.edu.uco.seguridad.shared.audit.AdministrationOutcome;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

/**
 * Implementación de {@link AdministerResourceGrantUseCase} (HU-016). Retrofit HU-021: audita
 * {@code RESOURCE_GRANTED} en ambas ramas del camino gateado, sin cambiar el resultado — sin
 * {@code administration} (rol TENANT) no hay gate ni auditoría.
 */
public final class AdministerResourceGrantUseCaseImpl implements AdministerResourceGrantUseCase {

    private final PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator;
    private final GrantResourceToRoleUseCase grantResource;
    private final RevokeResourceFromRoleUseCase revokeResource;
    private final AdministrationAuditRepository audit;
    private final IdentifierGenerator identifiers;
    private final TimeProvider time;

    public AdministerResourceGrantUseCaseImpl(PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator,
                                              GrantResourceToRoleUseCase grantResource, RevokeResourceFromRoleUseCase revokeResource, AdministrationAuditRepository audit,
                                              IdentifierGenerator identifiers, TimeProvider time) {
        this.mustBeAdministrator = Objects.requireNonNull(mustBeAdministrator,
                RequiredArgumentMessages.PRINCIPAL_MUST_BE_APPLICATION_ADMINISTRATOR_VALIDATOR);
        this.grantResource = Objects.requireNonNull(grantResource, RequiredArgumentMessages.GRANT_RESOURCE_TO_ROLE_USE_CASE);
        this.revokeResource = Objects.requireNonNull(revokeResource, "revokeResource");
        this.audit = Objects.requireNonNull(audit, RequiredArgumentMessages.ADMINISTRATION_AUDIT_REPOSITORY);
        this.identifiers = Objects.requireNonNull(identifiers, RequiredArgumentMessages.IDENTIFIER_GENERATOR);
        this.time = Objects.requireNonNull(time, RequiredArgumentMessages.TIME_PROVIDER);
    }

    /**
     * Conserva el contrato de construcción de la operación de concesión. La revocación solo se
     * habilita desde el adaptador que aporta explícitamente su caso de uso.
     */
    public AdministerResourceGrantUseCaseImpl(PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator,
                                              GrantResourceToRoleUseCase grantResource, AdministrationAuditRepository audit,
                                              IdentifierGenerator identifiers, TimeProvider time) {
        this(mustBeAdministrator, grantResource,
                input -> Mono.error(new IllegalStateException("La revocación de recursos no está configurada")),
                audit, identifiers, time);
    }

    @Override
    public Mono<RoleResponse> execute(AdministerResourceGrantRequest input) {
        Mono<Void> gate = input.administration().map(mustBeAdministrator::execute).orElseGet(Mono::empty);
        Mono<RoleResponse> result = gate.then(Mono.defer(() -> input.revocation()
                ? revokeResource.execute(new RevokeResourceRequest(input.grant().tenantId(), input.grant().roleId(), input.grant().resourceId()))
                : grantResource.execute(input.grant())));
        AdministrationOperation operation = input.revocation() ? AdministrationOperation.RESOURCE_REVOKED
                : AdministrationOperation.RESOURCE_GRANTED;
        return input.administration().map(administration -> audited(result, administration, operation)).orElse(result);
    }

    private Mono<RoleResponse> audited(Mono<RoleResponse> result, AdministrationRequest administration,
                                       AdministrationOperation operation) {
        return result
                .flatMap(response -> recordAudit(administration, operation, AdministrationOutcome.ALLOWED).thenReturn(response))
                .onErrorResume(NotAuthorizedToAdministerException.class,
                        error -> recordAudit(administration, operation, AdministrationOutcome.DENIED).then(Mono.error(error)));
    }

    private Mono<Void> recordAudit(AdministrationRequest context, AdministrationOperation operation,
                                   AdministrationOutcome outcome) {
        UUID eventId = identifiers.next();
        AdministrationEvent event = new AdministrationEvent(eventId, eventId.toString(), context.tenantId(),
                context.applicationId(), context.subject(), operation, outcome, time.now());
        return Mono.defer(() -> audit.save(event)).onErrorResume(error -> Mono.empty());
    }

}
