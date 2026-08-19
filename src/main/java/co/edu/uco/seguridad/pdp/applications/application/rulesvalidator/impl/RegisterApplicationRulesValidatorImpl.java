package co.edu.uco.seguridad.pdp.applications.application.rulesvalidator.impl;

import co.edu.uco.seguridad.pdp.applications.application.port.primary.dto.request.RegisterApplicationRequest;
import co.edu.uco.seguridad.pdp.applications.application.rule.ApplicationNameMustBeUniqueForTenantRule;
import co.edu.uco.seguridad.pdp.applications.application.rule.ApplicationNameMustNotBeReservedRule;
import co.edu.uco.seguridad.pdp.applications.application.rulesvalidator.RegisterApplicationRulesValidator;
import co.edu.uco.seguridad.pdp.tenants.application.rule.TenantMustBeActiveRule;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Ejecuta las reglas más baratas primero: las dos decisiones que no necesitan E/S rechazan DTOs malos antes
 * de que se toque cualquier repositorio.
 */
public final class RegisterApplicationRulesValidatorImpl implements RegisterApplicationRulesValidator {

    private final ApplicationNameMustNotBeReservedRule nameMustNotBeReserved;
    private final TenantMustBeActiveRule tenantMustBeActive;
    private final ApplicationNameMustBeUniqueForTenantRule nameMustBeUnique;

    public RegisterApplicationRulesValidatorImpl(ApplicationNameMustNotBeReservedRule nameMustNotBeReserved,
                                                  TenantMustBeActiveRule tenantMustBeActive,
                                                  ApplicationNameMustBeUniqueForTenantRule nameMustBeUnique) {
        this.nameMustNotBeReserved = Objects.requireNonNull(nameMustNotBeReserved);
        this.tenantMustBeActive = Objects.requireNonNull(tenantMustBeActive);
        this.nameMustBeUnique = Objects.requireNonNull(nameMustBeUnique);
    }

    @Override
    public Mono<Void> execute(RegisterApplicationRequest dto) {
        return Mono.fromRunnable(() -> nameMustNotBeReserved.execute(dto.name()))
                .then(tenantMustBeActive.execute(dto.tenantId()))
                .then(nameMustBeUnique.execute(dto));
    }
}
