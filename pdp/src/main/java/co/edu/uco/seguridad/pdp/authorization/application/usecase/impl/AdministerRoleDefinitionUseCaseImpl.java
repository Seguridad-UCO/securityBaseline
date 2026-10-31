package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerRoleDefinitionRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.CatalogAdministration;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.TenantAdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustAdministerAtLeastOneApplicationValidator;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerRoleDefinitionUseCase;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.NotAuthorizedToAdministerException;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.NotAuthorizedToAdministerTenantCatalogException;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.response.RoleResponse;
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
 * Implementación de {@link AdministerRoleDefinitionUseCase} (HU-016, HU-036). El tipo sellado
 * {@link CatalogAdministration} obliga a elegir el gate: un rol {@code APPLICATION} exige administrar esa
 * aplicación (ADR-023); un rol {@code TENANT} exige administrar al menos una aplicación del inquilino
 * (ADR-036) y se audita atribuido a la aplicación que concedió el permiso, o a la primera evaluada si se
 * denegó. Retrofit HU-021: audita {@code ROLE_DEFINED} en ambas ramas sin cambiar el resultado.
 */
public final class AdministerRoleDefinitionUseCaseImpl implements AdministerRoleDefinitionUseCase {

    private final PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator;
    private final PrincipalMustAdministerAtLeastOneApplicationValidator mustAdministerSomeApplication;
    private final DefineRoleUseCase defineRole;
    private final AdministrationAuditRepository audit;
    private final IdentifierGenerator identifiers;
    private final TimeProvider time;

    public AdministerRoleDefinitionUseCaseImpl(PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator,
                                               PrincipalMustAdministerAtLeastOneApplicationValidator mustAdministerSomeApplication,
                                               DefineRoleUseCase defineRole, AdministrationAuditRepository audit, IdentifierGenerator identifiers,
                                               TimeProvider time) {
        this.mustBeAdministrator = Objects.requireNonNull(mustBeAdministrator,
                RequiredArgumentMessages.PRINCIPAL_MUST_BE_APPLICATION_ADMINISTRATOR_VALIDATOR);
        this.mustAdministerSomeApplication = Objects.requireNonNull(mustAdministerSomeApplication,
                RequiredArgumentMessages.PRINCIPAL_MUST_ADMINISTER_AT_LEAST_ONE_APPLICATION_VALIDATOR);
        this.defineRole = Objects.requireNonNull(defineRole, RequiredArgumentMessages.DEFINE_ROLE_USE_CASE);
        this.audit = Objects.requireNonNull(audit, RequiredArgumentMessages.ADMINISTRATION_AUDIT_REPOSITORY);
        this.identifiers = Objects.requireNonNull(identifiers, RequiredArgumentMessages.IDENTIFIER_GENERATOR);
        this.time = Objects.requireNonNull(time, RequiredArgumentMessages.TIME_PROVIDER);
    }

    @Override
    public Mono<RoleResponse> execute(AdministerRoleDefinitionRequest input) {
        return switch (input.administration()) {
            case CatalogAdministration.OfApplication application -> applicationScoped(application.administration(), input);
            case CatalogAdministration.OfTenant tenant -> tenantScoped(tenant.actor(), input);
        };
    }

    private Mono<RoleResponse> applicationScoped(AdministrationRequest administration, AdministerRoleDefinitionRequest input) {
        return mustBeAdministrator.execute(administration)
                .then(Mono.defer(() -> defineRole.execute(input.role())))
                .flatMap(response -> recordAudit(administration.tenantId(), administration.applicationId(),
                        administration.subject(), AdministrationOutcome.ALLOWED).thenReturn(response))
                .onErrorResume(NotAuthorizedToAdministerException.class,
                        error -> recordAudit(administration.tenantId(), administration.applicationId(),
                                administration.subject(), AdministrationOutcome.DENIED).then(Mono.error(error)));
    }

    private Mono<RoleResponse> tenantScoped(TenantAdministrationRequest actor, AdministerRoleDefinitionRequest input) {
        return mustAdministerSomeApplication.execute(actor)
                .onErrorResume(NotAuthorizedToAdministerTenantCatalogException.class,
                        error -> auditTenantDenial(actor, error).then(Mono.error(error)))
                .flatMap(grantedBy -> defineRole.execute(input.role())
                        .flatMap(response -> recordAudit(actor.tenantId(), grantedBy, actor.subject(),
                                AdministrationOutcome.ALLOWED).thenReturn(response)));
    }

    private Mono<Void> auditTenantDenial(TenantAdministrationRequest actor, NotAuthorizedToAdministerTenantCatalogException denial) {
        return denial.evaluatedApplication()
                .map(evaluated -> recordAudit(actor.tenantId(), evaluated, actor.subject(), AdministrationOutcome.DENIED))
                .orElseGet(Mono::empty);
    }

    private Mono<Void> recordAudit(TenantId tenantId, ApplicationId applicationId,
                                   String subject, AdministrationOutcome outcome) {
        UUID eventId = identifiers.next();
        AdministrationEvent event = new AdministrationEvent(eventId, eventId.toString(), tenantId, applicationId,
                subject, AdministrationOperation.ROLE_DEFINED, outcome, time.now());
        return Mono.defer(() -> audit.save(event)).onErrorResume(error -> Mono.empty());
    }
}
