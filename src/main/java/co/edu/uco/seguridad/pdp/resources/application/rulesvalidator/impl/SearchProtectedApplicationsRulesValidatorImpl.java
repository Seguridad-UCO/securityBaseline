package co.edu.uco.seguridad.pdp.resources.application.rulesvalidator.impl;

import co.edu.uco.seguridad.pdp.resources.application.port.primary.dto.request.SearchProtectedApplicationsRequest;
import co.edu.uco.seguridad.pdp.resources.application.rulesvalidator.SearchProtectedApplicationsRulesValidator;
import co.edu.uco.seguridad.pdp.tenants.application.rule.TenantMustBeActiveRule;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * El tenant de la consulta viene del token autenticado (ADR-0003), nunca de un parámetro que el
 * llamador pueda omitir, así que la regla de inquilino activo se aplica siempre — ya no hay una
 * consulta "sin tenant" que la deje sin ejercer.
 */
public final class SearchProtectedApplicationsRulesValidatorImpl
        implements SearchProtectedApplicationsRulesValidator {

    private final TenantMustBeActiveRule tenantMustBeActive;

    public SearchProtectedApplicationsRulesValidatorImpl(TenantMustBeActiveRule tenantMustBeActive) {
        this.tenantMustBeActive = Objects.requireNonNull(tenantMustBeActive, RequiredArgumentMessages.TENANT_RULE);
    }

    @Override
    public Mono<Void> execute(SearchProtectedApplicationsRequest dto) {
        return tenantMustBeActive.execute(dto.criteria().tenantId()).then();
    }
}
