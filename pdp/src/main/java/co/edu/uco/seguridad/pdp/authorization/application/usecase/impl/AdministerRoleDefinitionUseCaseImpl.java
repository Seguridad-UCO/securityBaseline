package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerRoleDefinitionRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerRoleDefinitionUseCase;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.response.RoleResponse;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.NotAuthorizedToAdministerException;
import co.edu.uco.seguridad.pdp.roles.application.usecase.DefineRoleUseCase;
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
 * Implementación de {@link AdministerRoleDefinitionUseCase} (HU-016). Gatea solo si
 * {@code administration} está presente (rol de alcance {@code APPLICATION}) — un rol
 * {@code TENANT} delega directo, sin gate ni auditoría (no hay `subject`/`tenantId` de
 * administración con qué construir el evento). Retrofit HU-021: audita {@code ROLE_DEFINED} en
 * ambas ramas del camino gateado, sin cambiar el resultado.
 */
public final class AdministerRoleDefinitionUseCaseImpl implements AdministerRoleDefinitionUseCase {

    private final PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator;
    private final DefineRoleUseCase defineRole;
    private final AdministrationAuditRepository audit;
    private final IdentifierGenerator identifiers;
    private final TimeProvider time;

    public AdministerRoleDefinitionUseCaseImpl(PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator,
            DefineRoleUseCase defineRole, AdministrationAuditRepository audit, IdentifierGenerator identifiers,
            TimeProvider time) {
        this.mustBeAdministrator = Objects.requireNonNull(mustBeAdministrator,
                RequiredArgumentMessages.PRINCIPAL_MUST_BE_APPLICATION_ADMINISTRATOR_VALIDATOR);
        this.defineRole = Objects.requireNonNull(defineRole, RequiredArgumentMessages.DEFINE_ROLE_USE_CASE);
        this.audit = Objects.requireNonNull(audit, RequiredArgumentMessages.ADMINISTRATION_AUDIT_REPOSITORY);
        this.identifiers = Objects.requireNonNull(identifiers, RequiredArgumentMessages.IDENTIFIER_GENERATOR);
        this.time = Objects.requireNonNull(time, RequiredArgumentMessages.TIME_PROVIDER);
    }

    @Override
    public Mono<RoleResponse> execute(AdministerRoleDefinitionRequest input) {
        Mono<Void> gate = input.administration().map(mustBeAdministrator::execute).orElseGet(Mono::empty);
        Mono<RoleResponse> result = gate.then(Mono.defer(() -> defineRole.execute(input.role())));
        return input.administration().map(administration -> audited(result, administration)).orElse(result);
    }

    private Mono<RoleResponse> audited(Mono<RoleResponse> result, AdministrationRequest administration) {
        return result
                .flatMap(response -> recordAudit(administration, AdministrationOutcome.ALLOWED).thenReturn(response))
                .onErrorResume(NotAuthorizedToAdministerException.class,
                        error -> recordAudit(administration, AdministrationOutcome.DENIED).then(Mono.error(error)));
    }

    private Mono<Void> recordAudit(AdministrationRequest context, AdministrationOutcome outcome) {
        UUID eventId = identifiers.next();
        AdministrationEvent event = new AdministrationEvent(eventId, eventId.toString(), context.tenantId(),
                context.applicationId(), context.subject(), AdministrationOperation.ROLE_DEFINED, outcome, time.now());
        return Mono.defer(() -> audit.save(event)).onErrorResume(error -> Mono.empty());
    }
}
