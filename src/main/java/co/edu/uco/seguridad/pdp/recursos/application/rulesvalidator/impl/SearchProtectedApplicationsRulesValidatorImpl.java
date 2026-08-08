package co.edu.uco.seguridad.pdp.recursos.application.rulesvalidator.impl;

import co.edu.uco.seguridad.pdp.recursos.application.port.primary.dto.request.SearchProtectedApplicationsRequest;
import co.edu.uco.seguridad.pdp.recursos.application.rulesvalidator.SearchProtectedApplicationsRulesValidator;
import co.edu.uco.seguridad.pdp.tenants.application.rule.TenantMustBeActiveRule;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Aplica la regla de inquilino solo cuando la consulta está acotada a un inquilino, así un inquilino desconocido
 * se reporta como tal en lugar de devolver silenciosamente una página vacía — una página vacía permitiría a un
 * llamador sondear qué identificadores de inquilino existen.
 */
public final class SearchProtectedApplicationsRulesValidatorImpl
        implements SearchProtectedApplicationsRulesValidator {

    private final TenantMustBeActiveRule tenantMustBeActive;

    public SearchProtectedApplicationsRulesValidatorImpl(TenantMustBeActiveRule tenantMustBeActive) {
        this.tenantMustBeActive = Objects.requireNonNull(tenantMustBeActive, "se requiere regla de inquilino");
    }

    @Override
    public Mono<Void> execute(SearchProtectedApplicationsRequest dto) {
        return Mono.justOrEmpty(dto.criteria().tenantId())
                .flatMap(tenantMustBeActive::execute)
                .then();
    }
}
