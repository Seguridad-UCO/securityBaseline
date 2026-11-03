package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerResourceGrantRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.CatalogAdministration;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.TenantAdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustAdministerAtLeastOneApplicationValidator;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerResourceGrantUseCase;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.NotAuthorizedToAdministerException;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.NotAuthorizedToAdministerTenantCatalogException;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
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
 * Implementación de {@link AdministerResourceGrantUseCase} (HU-016, HU-036). Conceder o revocar un recurso
 * edita el rol: si es de alcance {@code APPLICATION} se exige administrar esa aplicación; si es
 * {@code TENANT}, administrar al menos una aplicación del inquilino (ADR-036). Retrofit HU-021: audita
 * {@code RESOURCE_GRANTED} / {@code RESOURCE_REVOKED}.
 */
public final class AdministerResourceGrantUseCaseImpl implements AdministerResourceGrantUseCase {

    private final PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator;
    private final PrincipalMustAdministerAtLeastOneApplicationValidator mustAdministerSomeApplication;
    private final GrantResourceToRoleUseCase grantResource;
    private final RevokeResourceFromRoleUseCase revokeResource;
    private final AdministrationAuditRepository audit;
    private final IdentifierGenerator identifiers;
    private final TimeProvider time;

    public AdministerResourceGrantUseCaseImpl(PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator,
                                              PrincipalMustAdministerAtLeastOneApplicationValidator mustAdministerSomeApplication,
                                              GrantResourceToRoleUseCase grantResource, RevokeResourceFromRoleUseCase revokeResource, AdministrationAuditRepository audit,
                                              IdentifierGenerator identifiers, TimeProvider time) {
        this.mustBeAdministrator = Objects.requireNonNull(mustBeAdministrator,
                RequiredArgumentMessages.PRINCIPAL_MUST_BE_APPLICATION_ADMINISTRATOR_VALIDATOR);
        this.mustAdministerSomeApplication = Objects.requireNonNull(mustAdministerSomeApplication,
                RequiredArgumentMessages.PRINCIPAL_MUST_ADMINISTER_AT_LEAST_ONE_APPLICATION_VALIDATOR);
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
                                              PrincipalMustAdministerAtLeastOneApplicationValidator mustAdministerSomeApplication,
                                              GrantResourceToRoleUseCase grantResource, AdministrationAuditRepository audit,
                                              IdentifierGenerator identifiers, TimeProvider time) {
        this(mustBeAdministrator, mustAdministerSomeApplication, grantResource,
                input -> Mono.error(new IllegalStateException("La revocación de recursos no está configurada")),
                audit, identifiers, time);
    }

    @Override
    public Mono<RoleResponse> execute(AdministerResourceGrantRequest input) {
        AdministrationOperation operation = input.revocation() ? AdministrationOperation.RESOURCE_REVOKED
                : AdministrationOperation.RESOURCE_GRANTED;
        return switch (input.administration()) {
            case CatalogAdministration.OfApplication application ->
                    applicationScoped(application.administration(), input, operation);
            case CatalogAdministration.OfTenant tenant -> tenantScoped(tenant.actor(), input, operation);
        };
    }

    private Mono<RoleResponse> applicationScoped(AdministrationRequest administration, AdministerResourceGrantRequest input,
                                                 AdministrationOperation operation) {
        return mustBeAdministrator.execute(administration)
                .then(Mono.defer(() -> grantOrRevoke(input)))
                .flatMap(response -> recordAudit(administration.tenantId(), administration.applicationId(),
                        administration.subject(), operation, AdministrationOutcome.ALLOWED).thenReturn(response))
                .onErrorResume(NotAuthorizedToAdministerException.class,
                        error -> recordAudit(administration.tenantId(), administration.applicationId(),
                                administration.subject(), operation, AdministrationOutcome.DENIED).then(Mono.error(error)));
    }

    private Mono<RoleResponse> tenantScoped(TenantAdministrationRequest actor, AdministerResourceGrantRequest input,
                                            AdministrationOperation operation) {
        return mustAdministerSomeApplication.execute(actor)
                .onErrorResume(NotAuthorizedToAdministerTenantCatalogException.class,
                        error -> auditTenantDenial(actor, operation, error).then(Mono.error(error)))
                .flatMap(grantedBy -> grantOrRevoke(input)
                        .flatMap(response -> recordAudit(actor.tenantId(), grantedBy, actor.subject(), operation,
                                AdministrationOutcome.ALLOWED).thenReturn(response)));
    }

    private Mono<RoleResponse> grantOrRevoke(AdministerResourceGrantRequest input) {
        return input.revocation()
                ? revokeResource.execute(new RevokeResourceRequest(input.grant().tenantId(), input.grant().roleId(), input.grant().resourceId()))
                : grantResource.execute(input.grant());
    }

    private Mono<Void> auditTenantDenial(TenantAdministrationRequest actor, AdministrationOperation operation,
                                         NotAuthorizedToAdministerTenantCatalogException denial) {
        return denial.evaluatedApplication()
                .map(evaluated -> recordAudit(actor.tenantId(), evaluated, actor.subject(), operation,
                        AdministrationOutcome.DENIED))
                .orElseGet(Mono::empty);
    }

    private Mono<Void> recordAudit(TenantId tenantId, ApplicationId applicationId, String subject,
                                   AdministrationOperation operation, AdministrationOutcome outcome) {
        UUID eventId = identifiers.next();
        AdministrationEvent event = new AdministrationEvent(eventId, eventId.toString(), tenantId, applicationId,
                subject, operation, outcome, time.now());
        return Mono.defer(() -> audit.save(event)).onErrorResume(error -> Mono.empty());
    }

}
