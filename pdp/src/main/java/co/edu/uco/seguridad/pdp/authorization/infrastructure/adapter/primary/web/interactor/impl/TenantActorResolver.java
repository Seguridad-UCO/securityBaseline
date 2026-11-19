package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.TenantAdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.NotAuthorizedToAdministerTenantCatalogException;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.SubjectUserIdLookupValidator;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.security.PdpPrincipal;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.Optional;

/**
 * Convierte el principal en un actor de inquilino (ADR-036). Fail-closed: un principal sin
 * {@code UserId} resoluble no puede administrar el catálogo y recibe {@code 403}, no un {@code 500}.
 */
public final class TenantActorResolver {

    private final SubjectUserIdLookupValidator subjectUserIdLookup;

    public TenantActorResolver(SubjectUserIdLookupValidator subjectUserIdLookup) {
        this.subjectUserIdLookup = Objects.requireNonNull(subjectUserIdLookup,
                RequiredArgumentMessages.SUBJECT_USER_ID_LOOKUP_VALIDATOR);
    }

    public Mono<TenantAdministrationRequest> resolve(PdpPrincipal principal) {
        return principal.userId()
                .map(Mono::just)
                .orElseGet(() -> subjectUserIdLookup.execute(principal.subject()))
                .switchIfEmpty(Mono.error(() -> new NotAuthorizedToAdministerTenantCatalogException(
                        principal.tenantId(), Optional.empty())))
                .map(userId -> new TenantAdministrationRequest(principal.tenantId(), userId, principal.subject(),
                        principal.authenticationContext()));
    }
}
